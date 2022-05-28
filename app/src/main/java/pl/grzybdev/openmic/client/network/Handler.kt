package pl.grzybdev.openmic.client.network

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import okhttp3.WebSocket
import pl.grzybdev.openmic.client.BuildConfig
import pl.grzybdev.openmic.client.network.messages.client.ClientMessage
import pl.grzybdev.openmic.client.network.messages.client.packets.ClientPacket
import pl.grzybdev.openmic.client.network.messages.server.ServerMessage
import pl.grzybdev.openmic.client.network.messages.server.packets.BasePacket
import pl.grzybdev.openmic.client.network.messages.server.packets.ErrorPacket
import pl.grzybdev.openmic.client.network.messages.server.packets.ServerPacket
import pl.grzybdev.openmic.client.network.messages.server.packets.SystemPacket
import pl.grzybdev.openmic.client.network.messages.client.packets.SystemHello as cSystemHello


class Handler {

    companion object {

        object PacketSerializer : JsonContentPolymorphicSerializer<ServerPacket>(ServerPacket::class) {
            override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out ServerPacket> {
                if (element.jsonObject.containsKey("type")) {
                    return if (element.jsonObject.containsKey("error"))
                        ErrorPacket.serializer()
                    else
                        BasePacket.serializer()
                }

                throw Exception("Invalid packet: Key 'type' not found")
            }
        }

        fun GetPacket(type: ClientMessage) : String {
            val clientMessageContent: ClientPacket = when (type) {
                ClientMessage.SYSTEM_HELLO -> cSystemHello(BuildConfig.APPLICATION_ID, BuildConfig.VERSION_NAME)
            }

            return Json.encodeToString(clientMessageContent)
        }

        fun HandlePacket(webSocket: WebSocket, type: ServerMessage, data: String) {
            when (type) {
                ServerMessage.SYSTEM_HELLO -> SystemPacket.handle(type, data, webSocket)
                else -> {}
            }
        }
    }
}
