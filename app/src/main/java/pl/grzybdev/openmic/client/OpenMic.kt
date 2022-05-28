package pl.grzybdev.openmic.client

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import pl.grzybdev.openmic.client.activities.MainActivity
import pl.grzybdev.openmic.client.network.Client
import java.util.concurrent.TimeUnit

class OpenMic(private val parent: MainActivity) {

    private val client: Client = Client()
    private lateinit var webSocket: WebSocket

    init {
        RestartClient();
    }

    private fun RestartClient() {
        if (this::webSocket.isInitialized && client.isListening)
            webSocket.close(1001, null)

        val httpClient = OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val webRequest = Request.Builder()
            .url("ws://localhost:10000")
            .build()

        webSocket = httpClient.newWebSocket(webRequest, client)
        httpClient.dispatcher.executorService.shutdown()
    }
}
