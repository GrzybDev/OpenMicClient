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
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.R
import pl.grzybdev.openmic.client.activities.MainActivity
import pl.grzybdev.openmic.client.enumerators.audio.Action
import pl.grzybdev.openmic.client.singletons.AppData
import pl.grzybdev.openmic.client.singletons.ServerData
import pl.grzybdev.openmic.client.singletons.StreamData
import kotlin.concurrent.thread


class AudioService : Service() {

    lateinit var audioThread: Thread
    lateinit var recorder: AudioRecord

    private var notificationInitialized = false

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

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        var actionInt = intent.getIntExtra("action", -1)

        if (actionInt == -1)
            actionInt = intent.getIntExtra("command", -1)

        if (actionInt != -1) {
            when (Action.values().find {it.code == actionInt}) {
                Action.START -> run { startStream() }
                Action.TOGGLE_MUTE -> run { toggleMute(false) }
                Action.TOGGLE_MUTE_SELF -> run { toggleMute(true) }
                Action.DISCONNECT -> run { AppData.openmic.forceDisconnect() }
                else -> {}
            }

            return START_STICKY
        }

        return START_STICKY
    }

    private fun startStream()
    {
        showNotification()

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

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun showNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        if (!notificationInitialized) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                createNotificationChannel()

            notificationInitialized = true
        }

        val intent = Intent(this, AudioService::class.java)

        val disconnectIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intent.putExtra("command", Action.DISCONNECT.code)
            PendingIntent.getService(this, Action.DISCONNECT.code, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            intent.putExtra("command", Action.DISCONNECT.code)
            PendingIntent.getService(this, Action.DISCONNECT.code, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val toggleMuteIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intent.putExtra("command", Action.TOGGLE_MUTE_SELF.code)
            PendingIntent.getService(this, Action.TOGGLE_MUTE_SELF.code, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            intent.putExtra("command", Action.TOGGLE_MUTE_SELF.code)
            PendingIntent.getService(this, Action.TOGGLE_MUTE_SELF.code, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val toggleMuteDrawable = if (StreamData.muted) R.drawable.ic_baseline_volume_up_24 else R.drawable.ic_baseline_volume_off_24
        val toggleMuteString = if (StreamData.muted) getString(R.string.audio_service_notification_btn_unmute) else getString(R.string.audio_service_notification_btn_mute)

        val notification: Notification =
            NotificationCompat.Builder(this, BuildConfig.APPLICATION_ID)
                .setContentTitle(getString(R.string.audio_service_notification_title))
                .setContentText(getString(R.string.audio_service_notification_desc, ServerData.name))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_baseline_phone_disabled_24, getString(R.string.audio_service_notification_btn_disconnect), disconnectIntent)
                .addAction(toggleMuteDrawable, toggleMuteString, toggleMuteIntent)
                .build()

        startForeground(1, notification)
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

    private fun stopStream() {
        showNotification()

        audioThread.interrupt()
        audioThread.join()

        recorder.stop()
    }

    private fun toggleMute(change: Boolean) {
        if (change)
        {
            StreamData.muted = !StreamData.muted
            OpenMic.refreshUI(this)
        }

        if (StreamData.muted) {
            stopStream()
        } else {
            startStream()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        stopStream()
    }
}