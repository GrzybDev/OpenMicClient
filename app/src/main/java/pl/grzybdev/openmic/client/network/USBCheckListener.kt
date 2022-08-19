package pl.grzybdev.openmic.client.network

import android.content.Context
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.enumerators.network.ConnectionStatus
import pl.grzybdev.openmic.client.enumerators.network.Connector
import pl.grzybdev.openmic.client.enumerators.network.ConnectorState
import pl.grzybdev.openmic.client.network.messages.client.ClientPacket
import pl.grzybdev.openmic.client.network.messages.client.SystemIsAlive
import pl.grzybdev.openmic.client.singletons.AppData
import java.util.*
import kotlin.concurrent.schedule

class USBCheckListener(val context: Context): WebSocketListener() {

    private var socket: WebSocket? = null

    override fun onOpen(webSocket: WebSocket, response: Response) {
        socket = webSocket

        Log.d(javaClass.name, "usbCheck [onOpen]: Successfully opened websocket connection, sending IsAlive packet...")

        val packet: ClientPacket = SystemIsAlive()
        webSocket.send(Json.encodeToString(packet))
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        socket = webSocket

        Log.d(javaClass.name, "usbCheck [onMessage]: Received IsAlive packet, closing websocket connection and setting USB Status as Ready...")

        // If we receive anything back, close connection and allow user to connect via USB
        webSocket.close(1000, null)

        OpenMic.changeConnectorStatus(context, Connector.USB, ConnectorState.READY)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        socket = webSocket

        Log.d(javaClass.name, "usbCheck [onClosing]: Websocket connection closed, reason: $code")

        if (AppData.usbTimer != null) {
            AppData.usbTimer!!.cancel()
            AppData.usbTimer = null
        }

        AppData.usbTimer = Timer("USBCheckTimer", false).schedule(1000){
            if (AppData.usbState == ConnectorState.READY && AppData.connectionStatus == ConnectionStatus.NOT_CONNECTED) {
                Log.d(javaClass.name, "usbCheck [onOpen]: Re-checking...")
                AppData.openmic.usbCheck(context)
            }
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        socket = webSocket

        Log.d(javaClass.name, "usbCheck [onFailure]: Websocket connection failed, reason: ${t.message}. Marking USB Status as Connected but not Ready...")

        OpenMic.changeConnectorStatus(context, Connector.USB, ConnectorState.USB_CONNECTED_NO_SERVER)

        if (AppData.usbTimer != null) {
            AppData.usbTimer!!.cancel()
            AppData.usbTimer = null
        }

        AppData.usbTimer = Timer("USBCheckTimer", false).schedule(1000){
            if (AppData.usbState == ConnectorState.USB_CONNECTED_NO_SERVER && AppData.connectionStatus == ConnectionStatus.NOT_CONNECTED) {
                Log.d(javaClass.name, "usbCheck [onFailure]: Retrying...")
                AppData.openmic.usbCheck(context)
            }
        }
    }

    fun forceClose()
    {
        socket?.close(1000, null)
    }
}
