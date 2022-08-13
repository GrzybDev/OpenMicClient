package pl.grzybdev.openmic.client

import android.content.Context
import android.content.Intent
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocketListener
import pl.grzybdev.openmic.client.enumerators.ConnectionStatus
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ConnectorState
import pl.grzybdev.openmic.client.enumerators.ServerVersion
import pl.grzybdev.openmic.client.network.Client
import pl.grzybdev.openmic.client.network.Listener
import pl.grzybdev.openmic.client.network.USBCheckListener
import pl.grzybdev.openmic.client.receivers.signals.ConnectionSignalReceiver
import pl.grzybdev.openmic.client.receivers.signals.ConnectorSignalReceiver
import pl.grzybdev.openmic.client.singletons.AppData
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule


class OpenMic {

    var wsClient = Client(null, null)

    private val httpClient = OkHttpClient.Builder()
        .readTimeout(20, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var listener: WebSocketListener? = null

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

        fun getServerVersion(serverApp: String, serverVersion: String): ServerVersion {
            if (serverApp == AppData.resources?.getString(R.string.SERVER_APP_NAME)) {
                // It's official app, check if versions match
                if (serverVersion != BuildConfig.VERSION_NAME)
                    return ServerVersion.MISMATCH

                return ServerVersion.MATCH
            }

            return ServerVersion.UNOFFICIAL
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

        forceDisconnect()

        wsClient = Client(ctx, connector)
        Log.d(javaClass.name, "connectTo: Trying to connect to $address, via $connector...")

        if (connector != Connector.Bluetooth) {
            listener = Listener()

            val webRequest = Request.Builder()
                .url("ws://$address:${AppData.communicationPort}")
                .build()

            httpClient.newWebSocket(webRequest, listener as Listener)
        } else {
        // val client = Client(connector)
        }
    }

    fun usbCheck(ctx: Context, uiDelay: Boolean = false) {
        Log.d(javaClass.name, "usbCheck: Checking if USB device has OpenMic Server running...")

        changeConnectorStatus(ctx, Connector.USB, ConnectorState.USB_CHECKING)

        listener = USBCheckListener(ctx)

        val webRequest = Request.Builder()
            .url("ws://${AppData.resources?.getString(R.string.INTERNAL_USB_ADDRESS)}:${AppData.communicationPort}")
            .build()

        if (uiDelay) {
            Timer("USBCheckDelay", false).schedule(5000) {
                httpClient.newWebSocket(webRequest, listener as USBCheckListener)
            }
        } else {
            httpClient.newWebSocket(webRequest, listener as USBCheckListener)
        }
    }

    fun forceDisconnect() {
        Log.d(javaClass.name, "forceDisconnect: Disconnecting...")

        if (listener != null)
        {
            if (listener is USBCheckListener)
                (listener as USBCheckListener).forceClose()
            else
                (listener as Listener).handleDisconnect()
        }
        else
            Log.w(javaClass.name, "forceDisconnect: No listener to close...")
    }
}
