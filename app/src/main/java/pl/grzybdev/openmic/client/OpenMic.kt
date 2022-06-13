package pl.grzybdev.openmic.client

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import com.gazman.signals.Signals
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import pl.grzybdev.openmic.client.activities.MainActivity
import pl.grzybdev.openmic.client.dialogs.AuthDialog
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ConnectorEvent
import pl.grzybdev.openmic.client.interfaces.IConnector
import pl.grzybdev.openmic.client.network.Client
import pl.grzybdev.openmic.client.receivers.USBReceiver
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

        App.mainActivity?.registerReceiver(USBReceiver(), IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun initWiFi()
    {
        connectSignal.dispatcher.onEvent(Connector.WiFi, if (checkWifiOnAndConnected()) ConnectorEvent.CONNECTED else ConnectorEvent.DISABLED)
        broadcastThread?.interrupt()

        val socket = DatagramSocket(AppData.communicationPort, InetAddress.getByName("0.0.0.0"))
        socket.broadcast = true

        val buf = ByteArray(1024)
        val packet = DatagramPacket(buf, buf.size)

        broadcastThread = thread(start = true) {
            while (!Thread.interrupted()) {
                Log.d(javaClass.name, "Waiting for broadcast on port ${AppData.communicationPort}...")
                socket.receive(packet)
                handleBroadcast(packet.data)
            }
        }
    }

    private fun checkWifiOnAndConnected(): Boolean {
        var result = false
        val connectivityManager = App.mainActivity?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            connectivityManager.run {
                connectivityManager.activeNetworkInfo?.run {
                    result = type == ConnectivityManager.TYPE_WIFI
                }
            }
        }

        return result
    }

    private fun handleBroadcast(encodedData: ByteArray)
    {
        Log.d(javaClass.name, "Received broadcast, analyzing it...")
        val decodedData = String(Base64.decode(encodedData, Base64.DEFAULT))
    }
}
