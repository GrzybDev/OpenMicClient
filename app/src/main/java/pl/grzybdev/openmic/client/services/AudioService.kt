package pl.grzybdev.openmic.client.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import pl.grzybdev.openmic.client.BuildConfig
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.activities.MainActivity
import pl.grzybdev.openmic.client.enumerators.audio.Action
import pl.grzybdev.openmic.client.singletons.AppData
import pl.grzybdev.openmic.client.singletons.StreamData
import kotlin.concurrent.thread


class AudioService : Service() {

    lateinit var audioThread: Thread
    lateinit var recorder: AudioRecord

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
            when (Action.values().find {it.code == actionInt}) {
                Action.START -> run { startStream() }
                else -> {}
            }

            return START_STICKY
        }

        return START_STICKY
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun startStream()
    {
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
                StreamData.sampleRate,
                StreamData.channelsInt,
                StreamData.formatInt,
                StreamData.bufferSize
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            stopSelf()
        }

        startAudioThread()
    }

    private fun startAudioThread() {
        if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING)
        {
            Log.w(javaClass.name, "Cannot start audio recorder because it's already running!")
            return
        }

        recorder.startRecording()

        val buffer = ByteArray(StreamData.bufferSize)

        audioThread = thread(start = true) {
            while (!Thread.interrupted()) {
                StreamData.bufferSize = recorder.read(buffer, 0, buffer.size)
                AppData.openmic.client.sendPacketDirect(buffer)
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