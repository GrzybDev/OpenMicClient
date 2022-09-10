package pl.grzybdev.openmic.client

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocketListener
import pl.grzybdev.openmic.client.dataclasses.ServerEntry
import pl.grzybdev.openmic.client.enumerators.DialogType
import pl.grzybdev.openmic.client.enumerators.ServerOS
import pl.grzybdev.openmic.client.enumerators.ServerVersion
import pl.grzybdev.openmic.client.enumerators.network.ConnectionStatus
import pl.grzybdev.openmic.client.enumerators.network.Connector
import pl.grzybdev.openmic.client.enumerators.network.ConnectorState
import pl.grzybdev.openmic.client.interfaces.IConnection
import pl.grzybdev.openmic.client.network.Client
import pl.grzybdev.openmic.client.network.Listener
import pl.grzybdev.openmic.client.network.USBCheckListener
import pl.grzybdev.openmic.client.network.messages.client.AuthClient
import pl.grzybdev.openmic.client.receivers.signals.ConnectionSignalReceiver
import pl.grzybdev.openmic.client.receivers.signals.ConnectorSignalReceiver
import pl.grzybdev.openmic.client.receivers.signals.DialogSignalReceiver
import pl.grzybdev.openmic.client.receivers.signals.RefreshSignalReceiver
import pl.grzybdev.openmic.client.singletons.AppData
import pl.grzybdev.openmic.client.singletons.ServerData
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule
import kotlin.concurrent.thread


class OpenMic : IConnection {

    var client = Client(null, null)

    private val httpClient = OkHttpClient.Builder()
        .readTimeout(20, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var listener: WebSocketListener? = null

    private var broadcastThread: Thread? = null

    private var bluetoothAdapter: BluetoothAdapter? = null

    companion object {
        fun changeConnectionStatus(ctx: Context, status: ConnectionStatus)
        {
            val i = Intent(ctx, ConnectionSignalReceiver::class.java)
            i.action = "UpdateStatus"
            i.putExtra("status", status.ordinal)
            ctx.sendBroadcast(i)
        }

        fun changeConnectorStatus(ctx: Context, connector: Connector, status: ConnectorState)
        {
            val i = Intent(ctx, ConnectorSignalReceiver::class.java)
            i.action = "UpdateState"
            i.putExtra("connector", connector.ordinal)
            i.putExtra("state", status.ordinal)
            ctx.sendBroadcast(i)
        }

        fun showDialog(ctx: Context, type: DialogType, data: String?)
        {
            val i = Intent(ctx, DialogSignalReceiver::class.java)
            i.action = "ShowDialog"
            i.putExtra("type", type.ordinal)
            i.putExtra("data", data)
            ctx.sendBroadcast(i)
        }

        fun refreshUI(ctx: Context)
        {
            val i = Intent(ctx, RefreshSignalReceiver::class.java)
            i.action = "RefreshUI"
            ctx.sendBroadcast(i)
        }

        fun getServerVersion(serverApp: String, serverVersion: String): ServerVersion {
            if (serverApp == AppData.resources?.getString(R.string.PREFERENCE_SERVER_APP_NAME)) {
                // It's official app, check if versions match
                if (serverVersion != BuildConfig.VERSION_NAME)
                    return ServerVersion.MISMATCH

                return ServerVersion.MATCH
            }

            return ServerVersion.UNOFFICIAL
        }

        fun getServerOS(kernelType: String): ServerOS {
            val os = ServerOS.values().find { it.kernelType == kernelType.lowercase() }
            return os ?: ServerOS.LINUX // Assume it's linux if kernelType is not specifically "unknown"
        }

        fun isBluetoothEnabled(ctx: Context): Boolean {
            val btManager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val btAdapter = btManager.adapter

            return btAdapter != null && btAdapter.isEnabled
        }

        fun haveBluetoothPermissions(ctx: Context): Boolean {
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
            }

            return result
        }
    }

    fun connectTo(ctx: Context, connector: Connector, address: String) {
        if (AppData.connectionStatus >= ConnectionStatus.CONNECTING && AppData.connectionStatus <= ConnectionStatus.CONNECTED) {
            // It should never execute as user can connect only on start screen, and when user is connected or connecting start screen is inaccessible
            Log.wtf(javaClass.name, "Can't connect because connection status is ${AppData.connectionStatus} which is illegal at this stage. How did you get here?")
            return
        }

        changeConnectionStatus(ctx, ConnectionStatus.CONNECTING)
        changeConnectorStatus(ctx, connector, ConnectorState.UNKNOWN)

        AppData.connectionListeners.add(this)
        forceDisconnect()

        client = Client(ctx, connector)

        Log.d(javaClass.name, "connectTo: Trying to connect to $address, via $connector...")

        thread(start = true) {
            if (connector != Connector.Bluetooth) {
                listener = Listener(null)

                val webRequest = Request.Builder()
                    .url("ws://$address:${AppData.communicationPort}")
                    .build()

                httpClient.newWebSocket(webRequest, listener as Listener)
            } else {
                val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(address)

                if (device == null) {
                    Log.e(javaClass.name, "BluetoothDevice is null!")
                    return@thread
                }

                val serviceID = "1bc0f9db-4faf-421d-8b21-455c03d890e1"

                try {
                    val bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(serviceID))
                    listener = Listener(bluetoothSocket)

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

                        forceDisconnect()
                    }
                } catch (e: SecurityException) {
                    Log.d(javaClass.name, "Cannot create bluetooth socket due to missing permissions!")
                    e.printStackTrace()

                    forceDisconnect()

                } catch (e: IOException) {
                    Log.d(javaClass.name, "Failed to create bluetooth socket, probably OpenMic Server is not running on target device!")
                    e.printStackTrace()

                    forceDisconnect()
                    showDialog(ctx, DialogType.BLUETOOTH_CONNECTION_ERROR, null)
                }
            }
        }
    }

    fun usbCheck(ctx: Context, uiDelay: Boolean = false) {
        if (AppData.connectionStatus != ConnectionStatus.NOT_CONNECTED)
            return

        Log.d(javaClass.name, "usbCheck: Checking if USB device has OpenMic Server running...")

        changeConnectorStatus(ctx, Connector.USB, ConnectorState.USB_CHECKING)

        listener = USBCheckListener(ctx)

        val webRequest = Request.Builder()
            .url("ws://${AppData.resources?.getString(R.string.INTERNAL_USB_ADDRESS)}:${AppData.communicationPort}")
            .build()

        if (uiDelay) {
            Timer("USBCheckDelay", false).schedule(2500) {
                if (listener is USBCheckListener) {
                    httpClient.newWebSocket(webRequest, listener as USBCheckListener)
                }
            }
        } else {
            httpClient.newWebSocket(webRequest, listener as USBCheckListener)
        }
    }

    fun forceDisconnect(reason: String = "") {
        Log.d(javaClass.name, "forceDisconnect: Disconnecting...")

        if (listener != null)
        {
            if (listener is USBCheckListener)
                (listener as USBCheckListener).forceClose()
            else
                (listener as Listener).handleDisconnect(reason = reason, client_initiated = true)
        }
        else
            Log.w(javaClass.name, "forceDisconnect: No listener to close...")
    }

    override fun onConnectionStateChange(status: ConnectionStatus) {
        if (status == ConnectionStatus.CONNECTED) {
            client.sendPacket(AuthClient())
            AppData.connectionListeners.remove(this)
        } else if (status == ConnectionStatus.DISCONNECTED) {
            AppData.connectionListeners.remove(this)
        }
    }

    fun startWirelessScan(ctx: Activity) {
        if (broadcastThread != null)
            return

        Log.d(javaClass.name, "startWirelessScan: Starting wireless scan...")

        broadcastThread = thread(start = true) {
            if (AppData.connectionStatus != ConnectionStatus.SELECTING_SERVER_WIFI) {
                stopWirelessScan()
                return@thread
            }

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

                handleBroadcast(ctx, packet.data, packet.address.hostAddress!!.toString())
            }

            broadcastSocket.close()
        }
    }

    fun stopWirelessScan() {
        Log.d(javaClass.name, "stopWirelessScan: Stopping broadcast thread...")

        broadcastThread?.interrupt()
        broadcastThread?.join()

        broadcastThread = null
    }

    private fun handleBroadcast(ctx: Activity, encodedData: ByteArray, senderIP: String)
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

        val entry = ServerEntry(data[3], senderIP, getServerVersion(data[0], data[1]), getServerOS(data[2]), data[4], Connector.WiFi)

        if (!ServerData.foundServers.contains(entry.serverID)) {
            // First time we received broadcast from this server, just add it to foundServers list
            Log.d(javaClass.name, "Received first broadcast packet from ${entry.serverID}, starting heartbeat thread...")

            ServerData.foundServers[entry.serverID] = entry
            ServerData.foundServersTimestamps[entry.serverID] = Date().time

            thread(start = true) {
                // Heartbeat thread
                while (true) {
                    val curTime = Date().time
                    val lastPingTime = ServerData.foundServersTimestamps[entry.serverID] ?: break

                    if (curTime - lastPingTime > 30000 || broadcastThread == null) {
                        Log.d(javaClass.name, "${entry.serverID} did not send broadcast in more than 30 seconds, assuming it's down...")
                        ServerData.foundServersTimestamps.remove(entry.serverID)
                        ServerData.foundServers.remove(entry.serverID)

                        refreshUI(ctx)
                        break
                    }

                    refreshUI(ctx)
                    Thread.sleep(1000)
                }
            }
        } else {
            // We already have this server in foundServers list, update its timestamp
            Log.d(javaClass.name, "Received another broadcast packet from ${entry.serverID}, updating timestamp...")
            ServerData.foundServersTimestamps[entry.serverID] = Date().time

            refreshUI(ctx)
        }
    }

    fun startBluetoothScan(ctx: Activity)
    {
        if (bluetoothAdapter != null)
            return

        val bluetoothManager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.w(javaClass.name, "startBluetoothScan: No BLUETOOTH_SCAN permission, aborting...")
            changeConnectionStatus(ctx, ConnectionStatus.NOT_CONNECTED)
            return
        }

        val success = bluetoothAdapter?.startDiscovery()

        if (!success!!)
        {
            Log.w(javaClass.name, "startBluetoothScan: Failed to start discovery, aborting...")
            changeConnectionStatus(ctx, ConnectionStatus.NOT_CONNECTED)
            Toast.makeText(ctx, ctx.getString(R.string.bt_connector_failed_to_start_discovery), Toast.LENGTH_SHORT).show()
        }

        Log.d(javaClass.name, "startBluetoothScan: Started discovery...")
    }

    fun stopBluetoothScan(ctx: Activity)
    {
        if (bluetoothAdapter == null)
            return

        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.w(javaClass.name, "stopBluetoothScan: No BLUETOOTH_SCAN permission, aborting...")
            return
        }

        bluetoothAdapter?.cancelDiscovery()
        bluetoothAdapter = null

        Log.d(javaClass.name, "stopBluetoothScan: Stopped discovery...")
    }
}
