package pl.grzybdev.openmic.client

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
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
import java.lang.Exception
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


class OpenMic(context: Context) {

    private var client: Client? = null
    private lateinit var webSocket: WebSocket

    private val connectSignal = Signals.signal(IConnector::class)

    private val deviceID: String

    private var autoConnectWifi = true
    private var broadcastThread: Thread? = null

    private var usbReceiver: USBStateReceiver? = null
    private var wifiReceiver: WifiStateReceiver? = null

    private var lastConnState: Boolean = false

    init {
        App.context = this
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

        connectSignal.addListener { connector, event -> run {
            when (connector) {
                Connector.USB -> run {
                    if (event == ConnectorEvent.DISABLED) {
                        Log.d(javaClass.name, "USB functionality disabled, disconnected from PC")
                    }

                    if (event == ConnectorEvent.CONNECTING) {
                        Log.d(javaClass.name, "Trying to connect to PC via USB...")
                        connectTo(Connector.USB, "localhost")
                    }
                }

                Connector.WiFi -> run {
                    when (event) {
                        ConnectorEvent.DISABLED -> run {
                            autoConnectWifi = false
                            broadcastThread?.interrupt()
                        }

                        ConnectorEvent.CONNECTING, ConnectorEvent.NEED_MANUAL_LAUNCH -> run {
                            autoConnectWifi = false
                        }

                        ConnectorEvent.CONNECTED_OR_READY -> run {
                            if (!lastConnState && client?.isConnected == true) {
                                Log.d(javaClass.name, "Connected, interrupting broadcast thread...")
                                autoConnectWifi = false
                                broadcastThread?.interrupt()
                            } else if (lastConnState && client?.isConnected == false) {
                                Log.d(javaClass.name, "Not connected, initializing broadcast thread...")
                                autoConnectWifi = true
                                initWiFi()
                            }

                            lastConnState = client?.isConnected == true
                            AppData.foundServers.clear()
                        }
                    }
                }

                Connector.Bluetooth -> run {
                    TODO()
                }
            }
        }}

        restartClient()

        val wm = App.mainActivity?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        wm?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "OpenMic:WifiLock")
    }

    object App {
        var appPreferences: SharedPreferences? = null
        var mainActivity: MainActivity? = null
        var context: OpenMic? = null
    }

    private fun restartClient() {
        if (this::webSocket.isInitialized && client?.isConnected == true)
            webSocket.close(1012, App.mainActivity?.getString(R.string.WebSocket_Restart))

        AppData.communicationPort = App.appPreferences?.getInt(
            App.mainActivity?.getString(R.string.PREFERENCE_APP_PORT),
            10000
        )!!

        initReceivers()

        initWiFi()
        initBT()
    }

    fun connectTo(connector: Connector, address: String)
    {
        if (AppData.connectLock) {
            return
        }

        AppData.connectLock = true

        if (connector != Connector.USB)
            connectSignal.dispatcher.onEvent(connector, ConnectorEvent.CONNECTING)

        Log.d(javaClass.name, "Trying to connect to $address...")

        client = Client(connector)

        val httpClient = OkHttpClient.Builder()
            .readTimeout(20, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()

        val webRequest = Request.Builder()
            .url("ws://$address:${AppData.communicationPort}")
            .build()

        webSocket = httpClient.newWebSocket(webRequest, client!!)
        httpClient.dispatcher.executorService.shutdown()
    }

    private fun initReceivers()
    {
        if (usbReceiver != null) {
            App.mainActivity?.unregisterReceiver(usbReceiver)
            usbReceiver = null
        }

        if (wifiReceiver != null) {
            App.mainActivity?.unregisterReceiver(wifiReceiver)
            wifiReceiver = null
        }

        usbReceiver = USBStateReceiver()
        wifiReceiver = WifiStateReceiver()

        App.mainActivity?.registerReceiver(usbReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        @Suppress("DEPRECATION")
        App.mainActivity?.registerReceiver(wifiReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    private fun initWiFi()
    {
        broadcastThread?.interrupt()
        broadcastThread?.join()

        broadcastThread = thread(start = true) {
            val broadcastSocket = DatagramSocket(AppData.communicationPort, InetAddress.getByName("0.0.0.0"))
            broadcastSocket.broadcast = true
            broadcastSocket.soTimeout = 1000

            val buf = ByteArray(1024)
            val packet = DatagramPacket(buf, buf.size)

            while (!Thread.interrupted()) {
                Log.d(javaClass.name, "Waiting for broadcast on port ${AppData.communicationPort}...")

                try {
                    broadcastSocket.receive(packet)
                } catch (e: SocketTimeoutException) {
                    continue
                }

                handleBroadcast(packet.data, packet.address.hostAddress!!.toString())
            }

            broadcastSocket.close()
        }
    }

    private fun initBT()
    {
        @Suppress("DEPRECATION")
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        val receiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                        Log.d(javaClass.name, "Found device: ${device?.name}")

                        val bluetoothSocket = device?.createRfcommSocketToServiceRecord(UUID.fromString("6b310fa0-ab0a-4008-8b6a-89b41cb1ccad"))
                        bluetoothAdapter?.cancelDiscovery()

                        try {
                            bluetoothSocket?.connect()
                            Log.d(javaClass.name, "Successfully connected")
                        } catch (e: Exception) {
                            Log.d(javaClass.name, "Failed to connect :(")
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        App.mainActivity?.registerReceiver(receiver, filter)
        val success = bluetoothAdapter?.startDiscovery()

        Log.d(javaClass.name, "startDiscovery: $success")
    }

    private fun handleBroadcast(encodedData: ByteArray, senderIP: String)
    {
        if (broadcastThread?.isInterrupted == true)
            return

        Log.d(javaClass.name, "Received broadcast, analyzing it...")
        val decodedData = String(Base64.decode(encodedData, Base64.DEFAULT))
        val data = decodedData.split(";")

        if (data.size != 5)
        {
            Log.w(javaClass.name, "Received broadcast is invalid, ignoring...")
            return
        }

        val entry = ServerEntry(data[3], senderIP, getServerCompatibility(data[0], data[1]), getServerOS(data[2]), data[4])

        AppData.foundServersTimestamps[entry.serverID] = Date().time

        if (!AppData.foundServers.contains(entry.serverID)) {
            // First time we received broadcast from this server, just add it to foundServers list
            Log.d(javaClass.name, "Received first broadcast packet from ${entry.serverID}, starting heartbeat thread...")
            AppData.foundServers[entry.serverID] = entry

            thread(start = true) {
                // Heartbeat thread
                while (true) {
                    val curTime = Date().time
                    val lastPingTime = AppData.foundServersTimestamps[entry.serverID] ?: break

                    if (curTime - lastPingTime > 5000) {
                        Log.d(javaClass.name, "${entry.serverID} did not send broadcast in more than 5 seconds, assuming it's down...")
                        AppData.foundServersTimestamps.remove(entry.serverID)
                        AppData.foundServers.remove(entry.serverID)

                        AppData.serverAdapter.updateData()

                        if (!AppData.connectLock)
                            connectSignal.dispatcher.onEvent(Connector.WiFi, ConnectorEvent.CONNECTED_OR_READY)

                        break
                    }

                    AppData.serverAdapter.updateData()
                    Thread.sleep(1000)
                }
            }
        } else {
            // Second time we received broadcast from this server,
            // if it's the only one - connect to it otherwise show select server button

            if (AppData.foundServers.size == 1 && autoConnectWifi)
            {
                Log.d(javaClass.name, "Received broadcast packet from ${entry.serverID}, trying to connect because it's the only one server that sent broadcast...")
                connectTo(Connector.WiFi, entry.serverIP)
            }
            else
            {
                Log.d(javaClass.name, "Received broadcast packet from ${entry.serverID}, manual connection is needed because either there's more than one server in network or error happened during initial connection...")
                connectSignal.dispatcher.onEvent(Connector.WiFi, ConnectorEvent.NEED_MANUAL_LAUNCH)
            }
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
