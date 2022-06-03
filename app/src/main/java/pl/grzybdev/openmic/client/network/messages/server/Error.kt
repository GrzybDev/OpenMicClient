package pl.grzybdev.openmic.client.network.messages.server

import kotlinx.serialization.Serializable

@Serializable
data class ErrorPacket(override val type: String,
                       val error: Int,
                       val message: String): ServerPacket()
