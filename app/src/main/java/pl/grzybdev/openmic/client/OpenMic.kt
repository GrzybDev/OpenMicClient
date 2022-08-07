package pl.grzybdev.openmic.client

import android.content.Context
import android.util.Log
import androidx.navigation.NavController
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocketListener
import pl.grzybdev.openmic.client.enumerators.ConnectionStatus
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ConnectorStatus
import pl.grzybdev.openmic.client.network.Listener
import pl.grzybdev.openmic.client.network.USBCheckListener
import java.util.concurrent.TimeUnit


class OpenMic {

    private val httpClient = OkHttpClient.Builder()
        .readTimeout(20, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var listener: WebSocketListener? = null

    fun connectTo(connector: Connector, address: String) {
        if (AppData.connectionStatus >= ConnectionStatus.CONNECTING && AppData.connectionStatus <= ConnectionStatus.CONNECTED) {
            // It should never execute as user can connect only on start screen, and when user is connected or connecting start screen is inaccessible
            Log.wtf(javaClass.name, "Can't connect because connection status is ${AppData.connectionStatus} which is illegal at this stage. How did you get here?")
            return
        }

        AppData.connectionStatus = ConnectionStatus.CONNECTING

        forceDisconnect(null)

        Log.d(javaClass.name, "connectTo: Trying to connect to $address, via $connector...")

        if (connector != Connector.Bluetooth) {
            listener = Listener(connector)

            val webRequest = Request.Builder()
                .url("ws://$address:${AppData.communicationPort}")
                .build()

            httpClient.newWebSocket(webRequest, listener as Listener)
            httpClient.dispatcher.executorService.shutdown()
        } else {
        // val client = Client(connector)
        }
    }

    fun usbCheck(ctx: Context) {
        Log.d(javaClass.name, "usbCheck: Checking if USB device has OpenMic Server running...")

        if (AppData.usbStatus != ConnectorStatus.READY
            && AppData.usbStatus != ConnectorStatus.USB_CONNECTED_NO_SERVER)
            // Don't update status if it's already set to READY or USB_CONNECTED_NO_SERVER
            AppData.connectSignal.dispatcher.onEvent(Connector.USB, ConnectorStatus.USB_CHECKING)

        listener = USBCheckListener(ctx)

        val httpClient = OkHttpClient.Builder()
            .readTimeout(20, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()

        val webRequest = Request.Builder()
            .url("ws://${ctx.getString(R.string.INTERNAL_USB_ADDRESS)}:${AppData.communicationPort}")
            .build()

        httpClient.newWebSocket(webRequest, listener as USBCheckListener)
        httpClient.dispatcher.executorService.shutdown()
    }

    fun forceDisconnect(navController: NavController?) {
        Log.d(javaClass.name, "forceDisconnect: Disconnecting...")

        navController?.navigate(R.id.action_cancel_connection)

        if (listener != null)
        {
            if (listener is USBCheckListener)
                (listener as USBCheckListener).forceClose()
            else
                (listener as Listener).forceClose()
        }
        else
        {
            Log.w(javaClass.name, "forceDisconnect: No listener to close...")
        }

        navController?.navigate(R.id.action_disconnected)
    }
}
