package pl.grzybdev.openmic.client.network.messages.server

import kotlinx.serialization.Serializable

@Serializable
sealed class ServerPacket {
    abstract val type: String
}

@Serializable
data class BasePacket(override val type: String): ServerPacket()
