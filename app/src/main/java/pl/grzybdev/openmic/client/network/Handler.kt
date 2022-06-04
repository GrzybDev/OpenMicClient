package pl.grzybdev.openmic.client.network

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import okhttp3.WebSocket
import pl.grzybdev.openmic.client.network.messages.Message
import pl.grzybdev.openmic.client.network.messages.server.*


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

        fun handlePacket(webSocket: WebSocket, type: Message, data: String) {
            when (type) {
                Message.SYSTEM_HELLO, Message.SYSTEM_GOODBYE -> SystemPacket.handle(type, data, webSocket)
                Message.AUTH_CODE_VERIFY -> AuthPacket.handle(type, data, webSocket)
            }
        }
    }
}
