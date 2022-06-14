package pl.grzybdev.openmic.client

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.util.Base64
import android.util.Log
import com.gazman.signals.Signals
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import pl.grzybdev.openmic.client.activities.MainActivity
import pl.grzybdev.openmic.client.dataclasses.ServerEntry
import pl.grzybdev.openmic.client.dialogs.AuthDialog
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ConnectorEvent
import pl.grzybdev.openmic.client.enumerators.ServerCompatibility
import pl.grzybdev.openmic.client.enumerators.ServerOS
import pl.grzybdev.openmic.client.interfaces.IConnector
import pl.grzybdev.openmic.client.network.Client
import pl.grzybdev.openmic.client.receivers.USBStateReceiver
import pl.grzybdev.openmic.client.receivers.WifiStateReceiver
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


class OpenMic(context: Context) {

    private lateinit var client: Client
    private lateinit var webSocket: WebSocket

    private val connectSignal = Signals.signal(IConnector::class)
    private var broadcastThread: Thread? = null

    private val deviceID: String

    init {
        val deviceIDKey = context.getString(R.string.PREFERENCE_APP_DEVICE_ID)

        if (!App.appPreferences?.contains(deviceIDKey)!!) {
            val newID = UUID.randomUUID()
            Log.d(javaClass.name, "Device ID was not set, generated new one: $newID")

            with (App.appPreferences!!.edit()) {
                putString(deviceIDKey, newID.toString())
                apply()
            }
        }

        deviceID = App.appPreferences!!.getString(deviceIDKey, "").toString()
        Log.d(javaClass.name, "Device ID: $deviceID")

        AppData.deviceID = deviceID

        // Init dialogs
        AuthDialog()

        restartClient()
    }

    object App {
        var appPreferences: SharedPreferences? = null
        var mainActivity: MainActivity? = null
    }

    private fun restartClient() {
        if (this::webSocket.isInitialized && client.isConnected)
            webSocket.close(1012, App.mainActivity?.getString(R.string.WebSocket_Restart))

        AppData.communicationPort = App.appPreferences?.getInt(
            App.mainActivity?.getString(R.string.PREFERENCE_APP_PORT),
            10000
        )!!

        initUSB()
        initWiFi()
    }

    private fun connectTo(connector: Connector, address: String)
    {
        if (AppData.connectLock) {
            return
        }

        AppData.connectLock = true
        Log.d(javaClass.name, "Trying to connect to $address...")

        client = Client(connector)

        val httpClient = OkHttpClient.Builder()
            .readTimeout(20, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()

        val webRequest = Request.Builder()
            .url("ws://$address:${AppData.communicationPort}")
            .build()

        webSocket = httpClient.newWebSocket(webRequest, client)
        httpClient.dispatcher.executorService.shutdown()
    }

    private fun initUSB()
    {
        connectSignal.addListener { connector, event -> run {
            if (connector == Connector.USB) {
                when (event) {
                    ConnectorEvent.DISABLED -> run {
                        Log.d(javaClass.name, "Disconnected from PC")
                    }

                    ConnectorEvent.CONNECTING -> run {
                        Log.d(javaClass.name, "Connected to PC")
                        connectTo(Connector.USB, "localhost")
                    }

                    else -> {}
                }
            }
        }}

        App.mainActivity?.registerReceiver(USBStateReceiver(), IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun initWiFi()
    {
        broadcastThread?.interrupt()

        val socket = DatagramSocket(AppData.communicationPort, InetAddress.getByName("0.0.0.0"))
        socket.broadcast = true

        val buf = ByteArray(1024)
        val packet = DatagramPacket(buf, buf.size)

        broadcastThread = thread(start = true) {
            while (!Thread.interrupted()) {
                Log.d(javaClass.name, "Waiting for broadcast on port ${AppData.communicationPort}...")
                socket.receive(packet)
                handleBroadcast(packet.data, packet.address.hostAddress!!.toString(), packet.port.toShort())
            }
        }

        @Suppress("DEPRECATION")
        App.mainActivity?.registerReceiver(WifiStateReceiver(), IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    private fun handleBroadcast(encodedData: ByteArray, senderIP: String, serverPort: Short)
    {
        Log.d(javaClass.name, "Received broadcast, analyzing it...")
        val decodedData = String(Base64.decode(encodedData, Base64.DEFAULT))
        val data = decodedData.split(";")

        if (data.size != 5)
        {
            Log.w(javaClass.name, "Received broadcast is invalid, ignoring...")
            return
        }

        val entry = ServerEntry(data[3], senderIP, serverPort, getServerCompatibility(data[0], data[1]), getServerOS(data[2]), data[4])

        if (!AppData.foundServers.contains(entry.serverID)) {
            // First time we received broadcast from this server, just add it to foundServers list
            AppData.foundServers[entry.serverID] = entry
        } else {
            // Second time we received broadcast from this server,
            // if it's the only one - connect to it otherwise show select server button

            if (AppData.foundServers.size == 1)
                connectTo(Connector.WiFi, "${entry.serverIP}:${entry.serverPort}")
            else
                connectSignal.dispatcher.onEvent(Connector.WiFi, ConnectorEvent.NEED_MANUAL_LAUNCH)
        }
    }

    companion object {
        fun getServerCompatibility(serverApp: String, serverVersion: String): ServerCompatibility {
            if (serverApp == App.mainActivity?.getString(R.string.SERVER_APP_NAME)) {
                // It's official app, check if versions match

                if (serverVersion != BuildConfig.VERSION_NAME)
                    return ServerCompatibility.NOT_SUPPORTED

                return ServerCompatibility.OFFICIAL
            }

            return ServerCompatibility.UNOFFICIAL
        }

        fun getServerOS(kernelType: String): ServerOS {
            return ServerOS.values().find { it.kernelType == kernelType }!!
        }
    }
}
