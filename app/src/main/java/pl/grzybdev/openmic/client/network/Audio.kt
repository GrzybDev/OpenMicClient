package pl.grzybdev.openmic.client.network

import android.media.AudioFormat
import android.media.AudioRecord
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.enumerators.audio.Channels
import pl.grzybdev.openmic.client.enumerators.audio.Format
import pl.grzybdev.openmic.client.network.messages.client.StreamStart
import pl.grzybdev.openmic.client.singletons.AppData
import pl.grzybdev.openmic.client.singletons.StreamData


class Audio {
    private lateinit var socket: Any

    fun start(socket: Any) {
        this.socket = socket

        StreamData.sampleRate = AppData.sharedPrefs?.getInt(AppData.resources?.getString(R.string.PREFERENCE_APP_AUDIO_SAMPLE_RATE), 8000)!!
        val channels = AppData.sharedPrefs?.getInt(AppData.resources?.getString(R.string.PREFERENCE_APP_AUDIO_CHANNELS), AudioFormat.CHANNEL_IN_MONO)!!
        val format = AppData.sharedPrefs?.getInt(AppData.resources?.getString(R.string.PREFERENCE_APP_AUDIO_FORMAT), AudioFormat.ENCODING_PCM_8BIT)!!

        StreamData.bufferSize = AudioRecord.getMinBufferSize(StreamData.sampleRate, channels, format)

        if (StreamData.bufferSize == AudioRecord.ERROR || StreamData.bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            // 44100Hz is currently the only rate that is guaranteed to work on all devices
            StreamData.sampleRate = 44100
            StreamData.bufferSize = AudioRecord.getMinBufferSize(StreamData.sampleRate, channels, format)

            with (AppData.sharedPrefs?.edit()) {
                this?.putInt(AppData.resources?.getString(R.string.PREFERENCE_APP_AUDIO_SAMPLE_RATE), StreamData.sampleRate)
                this?.apply()
            }
        }

        StreamData.channels = if (channels == AudioFormat.CHANNEL_IN_MONO) Channels.MONO else Channels.STEREO

        StreamData.format = when (format) {
            AudioFormat.ENCODING_PCM_8BIT -> Format.PCM_8BIT
            AudioFormat.ENCODING_PCM_16BIT -> Format.PCM_16BIT
            AudioFormat.ENCODING_PCM_FLOAT -> Format.PCM_FLOAT
            else -> Format.INVALID
        }

        AppData.openmic.client.sendPacket(StreamStart(StreamData.sampleRate, StreamData.channels.ordinal, StreamData.format.ordinal))
    }

}
