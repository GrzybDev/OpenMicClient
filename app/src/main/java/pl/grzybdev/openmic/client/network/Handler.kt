package pl.grzybdev.openmic.client.network

import android.content.Context
import android.util.Log
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.enumerators.DialogType
import pl.grzybdev.openmic.client.enumerators.network.Connector
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

        fun handlePacket(context: Context, socket: Any, connector: Connector, type: Message, data: String) {
            try {
                when (type) {
                    Message.SYSTEM_HELLO, Message.SYSTEM_GOODBYE, Message.SYSTEM_IS_ALIVE -> SystemPacket.handle(context, socket, connector, type, data)
                    Message.AUTH_CLIENT, Message.AUTH_CODE_VERIFY -> AuthPacket.handle(context, socket, type, data)
                    Message.STREAM_START, Message.STREAM_VOLUME -> StreamPacket.handle(context, type, data)
                }
            }
            catch (e: Exception)
            {
                Log.e(Handler::class.java.name, e.message.toString())
                OpenMic.showDialog(context, DialogType.CLIENT_INTERNAL_ERROR, null)
            }
        }
    }
}
