package pl.grzybdev.openmic.client

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import pl.grzybdev.openmic.client.activities.MainActivity
import pl.grzybdev.openmic.client.dialogs.AuthDialog
import pl.grzybdev.openmic.client.network.Client
import java.util.*
import java.util.concurrent.TimeUnit

class OpenMic(context: Context) {

    private val client: Client = Client()
    private lateinit var webSocket: WebSocket

    private val deviceID: String

    init {
        val deviceIDKey = context.getString(R.string.PREFERENCE_APP_DEVICE_ID)

        if (!App.appPreferences?.contains(deviceIDKey)!!) {
            val newID = UUID.randomUUID()
            Log.d(javaClass.name, "Device ID was not set, generated new one: $newID")

            with (App.appPreferences!!.edit()) {
                putString(deviceIDKey, newID.toString())
                apply()
            }
        }

        deviceID = App.appPreferences!!.getString(deviceIDKey, "").toString()
        Log.d(javaClass.name, "Device ID: $deviceID")

        AppData.deviceID = deviceID

        RestartClient();
    }

    object App {
        var appPreferences: SharedPreferences? = null
        var mainActivity: MainActivity? = null
    }

    private fun RestartClient() {
        if (this::webSocket.isInitialized && client.isListening)
            webSocket.close(1001, null)

        val httpClient = OkHttpClient.Builder()
            .readTimeout(20, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()

        val webRequest = Request.Builder()
            .url("ws://localhost:10000")
            .build()

        webSocket = httpClient.newWebSocket(webRequest, client)
        httpClient.dispatcher.executorService.shutdown()
    }
}
