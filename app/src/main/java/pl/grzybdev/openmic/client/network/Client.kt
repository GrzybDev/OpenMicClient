package pl.grzybdev.openmic.client.network

import android.util.Log
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import pl.grzybdev.openmic.client.network.messages.client.ClientMessage
import pl.grzybdev.openmic.client.network.messages.server.MessageType
import pl.grzybdev.openmic.client.network.messages.server.ServerMessage
import pl.grzybdev.openmic.client.network.messages.server.packets.BasePacket
import pl.grzybdev.openmic.client.network.messages.server.packets.ErrorPacket
import pl.grzybdev.openmic.client.network.messages.server.packets.ServerPacket

class Client : WebSocketListener() {

    var isListening: Boolean = false

    override fun onOpen(webSocket: WebSocket, response: Response) {
        isListening = true

        webSocket.send(Handler.GetPacket(ClientMessage.SYSTEM_HELLO))
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val json = Json { ignoreUnknownKeys = true }
        val pBase: ServerPacket = json.decodeFromString(Handler.Companion.PacketSerializer, text)

        if (pBase is BasePacket) {
            val mType: ServerMessage = MessageType.fromString(pBase.type)
            Handler.HandlePacket(webSocket, mType, text)
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
