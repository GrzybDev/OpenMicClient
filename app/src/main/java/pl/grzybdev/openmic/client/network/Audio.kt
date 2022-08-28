package pl.grzybdev.openmic.client.network

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Build
import androidx.core.content.ContextCompat
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.enumerators.DialogType
import pl.grzybdev.openmic.client.enumerators.audio.Action
import pl.grzybdev.openmic.client.enumerators.audio.Channels
import pl.grzybdev.openmic.client.enumerators.audio.Format
import pl.grzybdev.openmic.client.network.messages.client.StreamStart
import pl.grzybdev.openmic.client.services.AudioService
import pl.grzybdev.openmic.client.singletons.AppData
import pl.grzybdev.openmic.client.singletons.StreamData


class Audio {

    fun initialize() {
        StreamData.sampleRate = AppData.sharedPrefs?.getInt(AppData.resources?.getString(R.string.PREFERENCE_APP_AUDIO_SAMPLE_RATE), 44100)!!
        val channels = AppData.sharedPrefs?.getInt(AppData.resources?.getString(R.string.PREFERENCE_APP_AUDIO_CHANNELS), AudioFormat.CHANNEL_IN_MONO)!!
        val format = AppData.sharedPrefs?.getInt(AppData.resources?.getString(R.string.PREFERENCE_APP_AUDIO_FORMAT), AudioFormat.ENCODING_PCM_16BIT)!!

        StreamData.bufferSize = AudioRecord.getMinBufferSize(StreamData.sampleRate, channels, format)

        StreamData.channels = if (channels == AudioFormat.CHANNEL_IN_MONO) Channels.MONO else Channels.STEREO

        StreamData.format = when (format) {
            AudioFormat.ENCODING_PCM_8BIT -> Format.PCM_8BIT
            AudioFormat.ENCODING_PCM_16BIT -> Format.PCM_16BIT
            AudioFormat.ENCODING_PCM_32BIT -> Format.PCM_32BIT
            AudioFormat.ENCODING_PCM_FLOAT -> Format.PCM_FLOAT
            else -> Format.INVALID
        }

        AppData.openmic.client.sendPacket(StreamStart(StreamData.sampleRate, StreamData.channels.count, StreamData.format.id))
    }

    fun start(context: Context) {
        StreamData.channelsInt = if (StreamData.channels == Channels.MONO) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO
        StreamData.formatInt = when (StreamData.format) {
            Format.PCM_8BIT -> AudioFormat.ENCODING_PCM_8BIT
            Format.PCM_16BIT -> AudioFormat.ENCODING_PCM_16BIT
            Format.PCM_32BIT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AudioFormat.ENCODING_PCM_32BIT
            } else {
                OpenMic.showDialog(context, DialogType.CLIENT_CONFIG_INVALID, null)
                return
            }
            Format.PCM_FLOAT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioFormat.ENCODING_PCM_FLOAT
            } else {
                OpenMic.showDialog(context, DialogType.CLIENT_CONFIG_INVALID, null)
                return
            }
            else -> {
                OpenMic.showDialog(context, DialogType.CLIENT_CONFIG_INVALID, null)
                return
            }
        }

        StreamData.bufferSize = AudioRecord.getMinBufferSize(StreamData.sampleRate,
            StreamData.channelsInt, StreamData.formatInt)

        StreamData.intent = Intent(context, AudioService::class.java)
        StreamData.intent.putExtra("action", Action.START.code)
        StreamData.intentActive = true

        ContextCompat.startForegroundService(context, StreamData.intent)
    }

    fun toggleMute(context: Context)
    {
        StreamData.muted = !StreamData.muted

        val intent = StreamData.intent
        intent.putExtra("action", Action.TOGGLE_MUTE.code)
        ContextCompat.startForegroundService(context, intent)

        OpenMic.refreshUI(context)
    }

}
