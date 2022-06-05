package pl.grzybdev.openmic.client.network

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import okhttp3.WebSocket
import okio.ByteString.Companion.toByteString
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