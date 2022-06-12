package pl.grzybdev.openmic.client

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.util.Log
import com.gazman.signals.Signals
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import pl.grzybdev.openmic.client.activities.MainActivity
import pl.grzybdev.openmic.client.dialogs.AuthDialog
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ConnectorEvent
import pl.grzybdev.openmic.client.interfaces.IConnector
import pl.grzybdev.openmic.client.network.Client
import pl.grzybdev.openmic.client.receivers.USBReceiver
import java.util.*
import java.util.concurrent.TimeUnit


class OpenMic(context: Context) {

    private lateinit var client: Client
    private lateinit var webSocket: WebSocket

    private val connectSignal = Signals.signal(IConnector::class)

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

        // Init dialogs
        AuthDialog()

        restartClient()
    }

    object App {
        var appPreferences: SharedPreferences? = null
        var mainActivity: MainActivity? = null
    }

    private fun restartClient() {
        if (this::webSocket.isInitialized && client.isConnected)
            webSocket.close(1012, App.mainActivity?.getString(R.string.WebSocket_Restart))

        connectSignal.addListener { connector, event -> run {
            if (connector == Connector.USB) {
                when (event) {
                    ConnectorEvent.DISABLED -> run {
                        Log.d(javaClass.name, "Disconnected from PC")
                    }

                    ConnectorEvent.CONNECTING -> run {
                        Log.d(javaClass.name, "Connected to PC")
                        connectTo(Connector.USB, "localhost")
                    }

                    else -> {}
                }
            }
        }}

        App.mainActivity?.registerReceiver(USBReceiver(), IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun connectTo(connector: Connector, address: String)
    {
        if (AppData.connectLock) {
            return
        }

        AppData.connectLock = true
        Log.d(javaClass.name, "Trying to connect to $address...")

        client = Client(connector)

        val httpClient = OkHttpClient.Builder()
            .readTimeout(20, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()

        val port = App.appPreferences?.getInt(
            App.mainActivity?.getString(R.string.PREFERENCE_APP_PORT),
            10000
        )

        val webRequest = Request.Builder()
            .url("ws://$address:$port")
            .build()

        webSocket = httpClient.newWebSocket(webRequest, client)
        httpClient.dispatcher.executorService.shutdown()
    }
}
