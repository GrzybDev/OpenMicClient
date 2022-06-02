package pl.grzybdev.openmic.client.network

import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import pl.grzybdev.openmic.client.AppData
import pl.grzybdev.openmic.client.BuildConfig
import pl.grzybdev.openmic.client.network.messages.Message
import pl.grzybdev.openmic.client.network.messages.client.ClientPacket
import pl.grzybdev.openmic.client.network.messages.client.SystemHello
import pl.grzybdev.openmic.client.network.messages.server.BasePacket
import pl.grzybdev.openmic.client.network.messages.server.ServerPacket

class Client : WebSocketListener() {

    var isListening: Boolean = false

    override fun onOpen(webSocket: WebSocket, response: Response) {
        isListening = true

        val packet: ClientPacket = SystemHello(BuildConfig.APPLICATION_ID, BuildConfig.VERSION_NAME, AppData.deviceID)
        webSocket.send(Json.encodeToString(packet))
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val json = Json { ignoreUnknownKeys = true }
        val pBase: ServerPacket = json.decodeFromString(Handler.Companion.PacketSerializer, text)

        if (pBase is BasePacket) {
            val mType: Message? = Message.values().find { it.type == pBase.type }

            if (mType != null) {
                Handler.handlePacket(webSocket, mType, text)
            } else {
                Log.e(javaClass.name, "Unknown message type! ($mType) Disconnecting...")
                webSocket.close(1003, "Unknown message type")
            }
        } else {
            Log.e(javaClass.name, "Received error packet, disconnecting...")
            webSocket.close(1006, "Received error packet")
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        isListening = false

        Log.d(javaClass.name, "onClosing")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        isListening = false

        Log.d(javaClass.name, "onFailure")
        Log.d(javaClass.name, t.message.toString())
    }
}
