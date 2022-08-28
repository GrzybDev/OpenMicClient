package pl.grzybdev.openmic.client.network.messages.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Stream_Start")
class StreamStart(
    val sampleRate: Int,
    val channels: Int,
    val format: Int,
) : ClientPacket()

@Serializable
@SerialName("Stream_Volume")
class StreamVolume(
    val volume: Int,
) : ClientPacket()
