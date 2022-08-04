package pl.grzybdev.openmic.client.network

import android.content.Context
import android.util.Log
import com.gazman.signals.Signals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import pl.grzybdev.openmic.client.AppData
import pl.grzybdev.openmic.client.BuildConfig
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ConnectorStatus
import pl.grzybdev.openmic.client.interfaces.IConnector
import pl.grzybdev.openmic.client.network.messages.client.ClientPacket
import pl.grzybdev.openmic.client.network.messages.client.SystemHello

class USBCheckListener: WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        // TODO: Replace with is alive packet
        Log.d(javaClass.name, "usbCheck [onOpen]: Successfully opened websocket connection, sending IsAlive packet...")

        val packet: ClientPacket = SystemHello(BuildConfig.APPLICATION_ID, BuildConfig.VERSION_NAME, AppData.deviceID)
        webSocket.send(Json.encodeToString(packet))
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d(javaClass.name, "usbCheck [onMessage]: Received IsAlive packet, closing websocket connection and setting USB Status as Ready...")

        // If we receive anything back, close connection and allow user to connect via USB
        webSocket.close(1000, null)
        AppData.connectSignal.dispatcher.onEvent(Connector.USB, ConnectorStatus.READY)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(javaClass.name, "usbCheck [onClosing]: Websocket connection closed, reason: $code")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.d(javaClass.name, "usbCheck [onFailure]: Websocket connection failed, reason: ${t.message}. Marking USB Status as Connected but not Ready...")

        AppData.connectSignal.dispatcher.onEvent(Connector.USB, ConnectorStatus.USB_CONNECTED_NO_SERVER)
    }
}
