package pl.grzybdev.openmic.client.network.messages.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("System_Hello")
class SystemHello(
    val clientApp: String,
    val clientVersion: String,
    val clientID: String
    ) : ClientPacket()

@Serializable
@SerialName("System_Goodbye")
class SystemGoodbye(
    val exitCode: Int,
    ) : ClientPacket()

@Serializable
@SerialName("System_IsAlive")
class SystemIsAlive : ClientPacket()
