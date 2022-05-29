package pl.grzybdev.openmic.client.network.messages.client.packets

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("System_Hello")
class SystemHello(
    val clientApp: String,
    val clientVersion: String,
    val clientID: String
) : ClientPacket()
