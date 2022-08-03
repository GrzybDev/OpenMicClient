package pl.grzybdev.openmic.client

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.util.Base64
import android.util.Log
import com.gazman.signals.Signals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
import pl.grzybdev.openmic.client.network.Listener
import pl.grzybdev.openmic.client.network.messages.ErrorCode
import pl.grzybdev.openmic.client.network.messages.client.ClientPacket
import pl.grzybdev.openmic.client.network.messages.client.SystemGoodbye
import pl.grzybdev.openmic.client.receivers.BTStateReceiver
import pl.grzybdev.openmic.client.receivers.USBStateReceiver
import pl.grzybdev.openmic.client.receivers.WifiStateReceiver
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


class OpenMic(context: Context) {

    var client = Client(null)
    private var listener: Listener? = null
    private lateinit var webSocket: WebSocket

    private val connectSignal = Signals.signal(IConnector::class)

    private lateinit var deviceID: String

    private var autoConnectUSB = true
    private var autoConnectWifi = true

    private var broadcastThread: Thread? = null

    private var usbReceiver: USBStateReceiver? = null
    private var wifiReceiver: WifiStateReceiver? = null
    private var btReceiver: BTStateReceiver? = null

    @Suppress("DEPRECATION")
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var bluetoothSocket: BluetoothSocket

    private var lastConnState: Boolean = false

    init {
        run {
            if (AppData.initialized) {
                Log.d(javaClass.name, "Not initializing OpenMic because it seems that it's already initialized!")
                return@run
            }

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
                            autoConnectUSB = true
                        }

                        if (event == ConnectorEvent.CONNECTED_OR_READY) {
                            if (!autoConnectUSB)
                                connectSignal.dispatcher.onEvent(Connector.USB, ConnectorEvent.NEED_MANUAL_LAUNCH)

                            if (!client.isConnected && autoConnectUSB)
                            {
                                autoConnectUSB = false
                                connectTo(Connector.USB, "localhost")
                            }
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
                                if (client.isConnected && !lastConnState) {
                                    Log.d(javaClass.name, "Connected, interrupting broadcast thread...")
                                    autoConnectWifi = false
                                    broadcastThread?.interrupt()
                                }

                                lastConnState = client.isConnected
                            }
                            else -> {}
                        }
                    }

                    Connector.Bluetooth -> run {
                        when (event) {
                            ConnectorEvent.DISABLED -> run {
                                try {
                                    val result = bluetoothAdapter?.cancelDiscovery()
                                    Log.d(javaClass.name, "cancelDiscovery: $result")
                                } catch (e: SecurityException) {
                                    Log.d(javaClass.name, "Cannot cancel discovery due to security exception")
                                    e.printStackTrace()
                                }
                            }

                            ConnectorEvent.CONNECTING, ConnectorEvent.NEED_MANUAL_LAUNCH -> {
                                // Not used
                            }

                            ConnectorEvent.CONNECTED_OR_READY -> run {
                                if (!client.isConnected) {
                                    try {
                                        val success = bluetoothAdapter?.startDiscovery()
                                        Log.d(javaClass.name, "startDiscovery: $success")

                                        if (!success!!) {
                                            connectSignal.dispatcher.onEvent(Connector.Bluetooth, ConnectorEvent.DISABLED)
                                        }
                                    } catch (e: SecurityException) {
                                        Log.d(javaClass.name, "Application doesn't have permission to start bluetooth discovery!")
                                        e.printStackTrace()
                                        connectSignal.dispatcher.onEvent(Connector.Bluetooth, ConnectorEvent.DISABLED)
                                    }
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }}

            initClient()
        }
    }

    object App {
        var appPreferences: SharedPreferences? = null
        var mainActivity: MainActivity? = null
        var context: OpenMic? = null
    }

    fun initClient() {
        if (this::webSocket.isInitialized && client.isConnected)
            webSocket.close(1012, App.mainActivity?.getString(R.string.WebSocket_Restart))

        AppData.communicationPort = App.appPreferences?.getInt(
            App.mainActivity?.getString(R.string.PREFERENCE_APP_PORT),
            10000
        )!!

        initUSB()
        initWiFi()
        initBT()

        AppData.currentConn = null
        AppData.initialized = true
    }

    fun connectTo(connector: Connector, address: String)
    {
        if (AppData.connectLock) {
            return
        }

        AppData.connectLock = true
        AppData.currentConn = connector
        AppData.foundServers.clear()
        AppData.foundServersTimestamps.clear()

        client = Client(connector)

        connectSignal.dispatcher.onEvent(connector, ConnectorEvent.CONNECTING)

        for (c in Connector.values()) {
            if (c == connector)
                continue

            connectSignal.dispatcher.onEvent(c, ConnectorEvent.DISABLED)
        }

        if (connector != Connector.Bluetooth) {
            Log.d(javaClass.name, "Trying to connect to $address...")

            listener = Listener(connector)

            val httpClient = OkHttpClient.Builder()
                .readTimeout(20, TimeUnit.SECONDS)
                .pingInterval(20, TimeUnit.SECONDS)
                .build()

            val webRequest = Request.Builder()
                .url("ws://$address:${AppData.communicationPort}")
                .build()

            webSocket = httpClient.newWebSocket(webRequest, listener!!)
            httpClient.dispatcher.executorService.shutdown()
        } else {
            val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(address)

            if (device == null) {
                Log.e(javaClass.name, "BluetoothDevice is null!")
                return
            }

            val serviceID = App.appPreferences?.getString(App.mainActivity?.getString(R.string.PREFERENCE_SERVICE_ID), "1bc0f9db-4faf-421d-8b21-455c03d890e1")

            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(serviceID))
                bluetoothSocket.connect()
                client.onOpen(bluetoothSocket)

                thread(start = true) {
                    val buffer = ByteArray(1024)
                    var length: Int

                    while (true) {
                        try {
                            length = bluetoothSocket.inputStream.read(buffer)

                            val message = String(buffer, 0, length)
                            client.onMessage(bluetoothSocket, message)
                        } catch (e: IOException) {
                            e.printStackTrace()
                            break
                        }
                    }

                    client.handleDisconnect()
                }
            } catch (e: SecurityException) {
                Log.d(javaClass.name, "Cannot create bluetooth socket due to missing permissions!")
                e.printStackTrace()

                client.handleDisconnect()
            } catch (e: IOException) {
                Log.d(javaClass.name, "Failed to create bluetooth socket, probably OpenMic Server is not running on target device!")
                e.printStackTrace()

                App.mainActivity?.runOnUiThread {
                    val builder: AlertDialog.Builder = AlertDialog.Builder(App.mainActivity)
                    builder.setTitle(App.mainActivity?.getString(R.string.ErrorDialog_ClientConnectionError))
                    builder.setMessage(App.mainActivity?.getString(R.string.ErrorDialog_ClientConnectionError_BT))
                    builder.setPositiveButton(App.mainActivity?.getString(R.string.ErrorDialog_Button_OK)) { _, _ -> run {}}
                    builder.show()
                }

                client.handleDisconnect()
            }
        }
    }

    private fun initUSB() {
        if (usbReceiver != null) {
            try {
                App.mainActivity?.unregisterReceiver(usbReceiver)
                usbReceiver = null
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }

        usbReceiver = USBStateReceiver()
        App.mainActivity?.registerReceiver(usbReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun initWiFi()
    {
        if (wifiReceiver != null) {
            try {
                App.mainActivity?.unregisterReceiver(wifiReceiver)
                wifiReceiver = null
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }

        broadcastThread?.interrupt()
        broadcastThread?.join()

        wifiReceiver = WifiStateReceiver()

        broadcastThread = thread(start = true) {
            val broadcastSocket = DatagramSocket(AppData.communicationPort, InetAddress.getByName("0.0.0.0"))
            broadcastSocket.broadcast = true
            broadcastSocket.soTimeout = 1000

            val buf = ByteArray(1024)
            val packet = DatagramPacket(buf, buf.size)
            autoConnectWifi = true

            while (!Thread.interrupted()) {
                Log.d(javaClass.name, "Waiting for broadcast on port ${AppData.communicationPort}...")

                try {
                    broadcastSocket.receive(packet)
                } catch (e: SocketTimeoutException) {
                    continue
                }

                handleBroadcast(packet.data, packet.address.hostAddress!!.toString())
            }

            Log.d(javaClass.name, "Broadcast thread finished")
            broadcastSocket.close()
        }

        @Suppress("DEPRECATION")
        App.mainActivity?.registerReceiver(wifiReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    private fun initBT()
    {
        if (btReceiver != null) {
            try {
                App.mainActivity?.unregisterReceiver(btReceiver)
                btReceiver = null
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }

        btReceiver = BTStateReceiver()

        App.mainActivity?.registerReceiver(btReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        App.mainActivity?.registerReceiver(btReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))

        connectSignal.dispatcher.onEvent(Connector.Bluetooth, if (bluetoothAdapter?.isEnabled == true) ConnectorEvent.CONNECTED_OR_READY else ConnectorEvent.DISABLED)
    }

    fun disconnectFromServer() {
        val goodbyePacket: ClientPacket = SystemGoodbye(ErrorCode.NORMAL.code)

        if (AppData.currentConn == Connector.Bluetooth) {
            bluetoothSocket.outputStream.write(Json.encodeToString(goodbyePacket).toByteArray())
            bluetoothSocket.close()
        } else {
            webSocket.send(Json.encodeToString(goodbyePacket))
            webSocket.close(1000, "Normal disconnect")
        }
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

        val entry = ServerEntry(data[3], senderIP, getServerCompatibility(data[0], data[1]), getServerOS(data[2]), data[4], Connector.WiFi)

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

            val entries = mutableListOf<Map.Entry<String, ServerEntry>>()

            AppData.foundServers.forEach { e -> run {
                if (e.value.connector == Connector.WiFi)
                    entries.add(e)
            }}

            if (entries.size == 1 && autoConnectWifi)
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
