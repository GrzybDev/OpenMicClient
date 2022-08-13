package pl.grzybdev.openmic.client.services

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import okhttp3.WebSocket
import okio.ByteString.Companion.toByteString
import pl.grzybdev.openmic.client.BuildConfig
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.activities.MainActivity
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.network.Audio
import java.io.IOException
import kotlin.concurrent.thread


class AudioService : Service() {

    lateinit var audioThread: Thread
    lateinit var recorder: AudioRecord

    //private val audioSignal = Signals.signal(IAudio::class)


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                BuildConfig.APPLICATION_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager = getSystemService(
                NotificationManager::class.java
            )

            manager.createNotificationChannel(serviceChannel)
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val actionInt = intent.getIntExtra("action", -1)

        if (actionInt != -1) {
            /*
            when (Action.values().find { it.code == actionInt }) {
                Action.MUTE -> run {
                    Log.d(javaClass.name, "MUTE")
                    audioThread.interrupt()
                    audioThread.join()

                    // AppData.isMuted = true
                }

                Action.UNMUTE -> run {
                    Log.d(javaClass.name, "UNMUTE")
                    startAudioThread()

                    // AppData.isMuted = false
                }

                Action.GET_MUTE_STATUS -> run {
                    Log.d(javaClass.name, "GET_MUTE_STATUS")
                    // AppData.isMuted = recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING
                }

                else -> {
                    // Invalid command?
                }
            }
             */

            // audioSignal.dispatcher.onAudioStateChanged()
            return START_NOT_STICKY
        }

        val notificationIntent = Intent(this, MainActivity::class.java)

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannel()

        val notification: Notification =
            NotificationCompat.Builder(this, BuildConfig.APPLICATION_ID)
                .setContentTitle(getString(R.string.connected_action_title))
                .setContentText(getString(R.string.connected_action_desc, "SERVER_NAME"))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build()

        startForeground(1, notification)

        try {
            recorder = AudioRecord(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaRecorder.AudioSource.VOICE_PERFORMANCE else MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                Audio.Data.sampleRate,
                Audio.Data.channelConfig,
                Audio.Data.audioFormat,
                Audio.Data.minBufSize
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            stopSelf()
            return START_NOT_STICKY
        }

        startAudioThread()
        return START_NOT_STICKY
    }

    private fun startAudioThread() {
        if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING)
        {
            Log.w(javaClass.name, "Cannot start audio recorder because it's already running!")
            return
        }

        recorder.startRecording()

        val buffer = ByteArray(Audio.Data.minBufSize)

        audioThread = thread(start = true) {
            while (!Thread.interrupted()) {
                Audio.Data.minBufSize = recorder.read(buffer, 0, buffer.size)

                if (Audio.Data.connector != Connector.Bluetooth) {
                    val webSocket = Audio.Data.socket as WebSocket
                    webSocket.send(buffer.toByteString())
                } else {
                    val btSocket = Audio.Data.socket as BluetoothSocket

                    try {
                        btSocket.outputStream.write(buffer)
                    } catch (e: IOException) {
                        break
                    }
                }
            }

            recorder.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        audioThread.interrupt()
        audioThread.join()

        recorder.stop()
    }
}