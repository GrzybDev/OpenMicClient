package pl.grzybdev.openmic.client.network

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.gazman.signals.Signals
import okhttp3.WebSocket
import okio.ByteString.Companion.toByteString
import pl.grzybdev.openmic.client.AppData
import pl.grzybdev.openmic.client.enumerators.ConnectorEvent
import pl.grzybdev.openmic.client.interfaces.IConnector
import kotlin.concurrent.thread


class Audio {

    object Data {
        var socket: WebSocket? = null

        var sampleRate = 44100
        var channelConfig = AudioFormat.CHANNEL_IN_MONO
        var audioFormat = AudioFormat.ENCODING_PCM_16BIT
        var minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    }

    companion object {

        fun initAudio(socket: WebSocket) {
            Data.socket = socket

            val signal = Signals.signal(IConnector::class)
            AppData.currentConn?.let { signal.dispatcher.onEvent(it, ConnectorEvent.CONNECTED_OR_READY) }

            audioLoop()
        }

        private fun audioLoop() {
            thread(start = true) {
                while (true) {
                    val buffer = ByteArray(Data.minBufSize)

                    val recorder = AudioRecord(
                        MediaRecorder.AudioSource.VOICE_PERFORMANCE,
                        Data.sampleRate,
                        Data.channelConfig,
                        Data.audioFormat,
                        Data.minBufSize
                    )

                    recorder.startRecording()

                    while (true) {
                        Data.minBufSize = recorder.read(buffer, 0, buffer.size)
                        Data.socket?.send(buffer.toByteString())
                    }
                }
            }
        }

    }
}