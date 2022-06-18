package pl.grzybdev.openmic.client.network

import android.bluetooth.BluetoothSocket
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import com.gazman.signals.Signals
import okhttp3.WebSocket
import okio.ByteString.Companion.toByteString
import pl.grzybdev.openmic.client.AppData
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ConnectorEvent
import pl.grzybdev.openmic.client.interfaces.IConnector
import java.io.IOException
import kotlin.concurrent.thread


class Audio {

    object Data {
        var socket: Any? = null
        var connector: Connector? = null

        var sampleRate = 8000
        var channelConfig = AudioFormat.CHANNEL_IN_MONO
        var audioFormat = AudioFormat.ENCODING_PCM_16BIT
        var minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    }

    companion object {

        fun initAudio(socket: Any, connector: Connector) {
            Data.socket = socket
            Data.connector = connector

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

                        if (Data.connector != Connector.Bluetooth) {
                            val webSocket = Data.socket as WebSocket
                            webSocket.send(buffer.toByteString())
                        } else {
                            val btSocket = Data.socket as BluetoothSocket

                            try {
                                btSocket.outputStream.write(buffer)
                            } catch (e: IOException) {
                                break
                            }
                        }
                    }
                }
            }
        }

    }
}