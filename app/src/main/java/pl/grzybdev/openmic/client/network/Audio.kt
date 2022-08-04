package pl.grzybdev.openmic.client.network

import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import androidx.core.content.ContextCompat
import com.gazman.signals.Signals
import pl.grzybdev.openmic.client.AppData
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ConnectorStatus
import pl.grzybdev.openmic.client.interfaces.IConnector
import pl.grzybdev.openmic.client.services.AudioService


class Audio {

    object Data {
        var socket: Any? = null
        var connector: Connector? = null

        var sampleRate = 8000
        var channelConfig = AudioFormat.CHANNEL_IN_MONO
        var audioFormat = AudioFormat.ENCODING_PCM_16BIT
        var minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        //var intent = Intent(OpenMic.App.mainActivity, AudioService::class.java)
    }

    companion object {

        fun start(socket: Any?, connector: Connector?) {
            if (socket != null && connector != null) {
                Data.socket = socket
                Data.connector = connector
            }

            val signal = Signals.signal(IConnector::class)
            //AppData.currentConn?.let { signal.dispatcher.onEvent(it, ConnectorStatus.CONNECTED_OR_READY) }

            //ContextCompat.startForegroundService(OpenMic.App.mainActivity!!, Data.intent)
        }

        fun stop() {
            //OpenMic.App.mainActivity?.stopService(Data.intent)
        }

    }
}