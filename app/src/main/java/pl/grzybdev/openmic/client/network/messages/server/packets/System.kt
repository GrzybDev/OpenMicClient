package pl.grzybdev.openmic.client.network.messages.server.packets

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.WebSocket
import pl.grzybdev.openmic.client.network.messages.server.ServerMessage

@Serializable
data class SystemHello(override val type: String,
                       val serverApp: String,
                       val serverVersion: String,
                       val serverOS: String,
                       val serverName: String): ServerPacket()

class SystemPacket {
    companion object {
        fun handle(type: ServerMessage, data: String, socket: WebSocket) {
            when (type) {
                ServerMessage.SYSTEM_HELLO -> handleHello(data, socket)
                else -> {}
            }
        }

        private fun handleHello(data: String, socket: WebSocket) {
            val packet: SystemHello = Json.decodeFromString(data)

            Log.d(javaClass.name, "Connected to: " + packet.serverName)

        }
    }
}