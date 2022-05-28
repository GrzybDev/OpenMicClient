package pl.grzybdev.openmic.client.network.messages.server.packets

import kotlinx.serialization.Serializable

@Serializable
data class ErrorPacket(override val type: String,
                       val error: String,
                       val message: String): ServerPacket()
