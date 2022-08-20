package pl.grzybdev.openmic.client.network.messages.server

import android.content.Context
import android.media.AudioRecord
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.enumerators.DialogType
import pl.grzybdev.openmic.client.enumerators.audio.Channels
import pl.grzybdev.openmic.client.enumerators.audio.Format
import pl.grzybdev.openmic.client.network.messages.Message
import pl.grzybdev.openmic.client.singletons.AppData
import pl.grzybdev.openmic.client.singletons.StreamData

@Serializable
data class StreamStart(
    override val type: String,
    val sampleRate: Int? = null,
    val channelConfig: Int? = null,
    val audioFormat: Int? = null,
) : ServerPacket()

class StreamPacket {
    companion object {
        fun handle(
            context: Context,
            type: Message,
            data: String
        ) {
            when (type) {
                Message.STREAM_START -> handleStart(context, data)
                else -> {}
            }
        }

        private fun handleStart(context: Context, data: String) {
            val packet: StreamStart = Json.decodeFromString(data)
            val changedByServer: Boolean = packet.sampleRate != null || packet.channelConfig != null || packet.audioFormat != null

            if (packet.sampleRate != null) StreamData.sampleRate = packet.sampleRate
            if (packet.channelConfig != null) StreamData.channels = Channels.values()[packet.channelConfig]
            if (packet.audioFormat != null) StreamData.format = Format.values()[packet.audioFormat]

            // Final check to make sure we have a valid buffer size (the configs are correct)
            if (StreamData.bufferSize == AudioRecord.ERROR || StreamData.bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.w(StreamPacket::class.java.name, "Invalid audio recording settings")

                if (changedByServer)
                    OpenMic.showDialog(context, DialogType.SERVER_CONFIG_NOT_COMPATIBLE, null)
                else
                    OpenMic.showDialog(context, DialogType.CLIENT_CONFIG_NOT_COMPATIBLE, null)
            }

            AppData.audio.start(context)
        }
    }
}
