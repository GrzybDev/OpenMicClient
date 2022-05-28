package pl.grzybdev.openmic.client.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import pl.grzybdev.openmic.client.BuildConfig
import pl.grzybdev.openmic.client.network.messages.client.Message
import pl.grzybdev.openmic.client.network.messages.client.packets.ClientPacket
import pl.grzybdev.openmic.client.network.messages.client.packets.SystemHello


class Command {

    companion object {
        fun Get(type: Message) : String {
            val messageContent: ClientPacket = when (type) {
                Message.SYSTEM_HELLO -> SystemHello(BuildConfig.APPLICATION_ID, BuildConfig.VERSION_NAME)
            }

            return Json.encodeToString(messageContent)
        }
    }
}
