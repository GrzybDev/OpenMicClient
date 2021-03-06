package pl.grzybdev.openmic.client.network

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import pl.grzybdev.openmic.client.enumerators.Connector
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

        fun handlePacket(socket: Any, connector: Connector, type: Message, data: String) {
            when (type) {
                Message.SYSTEM_HELLO, Message.SYSTEM_GOODBYE -> SystemPacket.handle(socket, connector, type, data)
                Message.AUTH_CODE_VERIFY -> AuthPacket.handle(socket, connector, type, data)
            }
        }
    }
}
