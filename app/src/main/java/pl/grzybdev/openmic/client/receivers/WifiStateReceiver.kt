@file:Suppress("DEPRECATION")

package pl.grzybdev.openmic.client.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.gazman.signals.Signals
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ConnectorEvent
import pl.grzybdev.openmic.client.interfaces.IConnector


class WifiStateReceiver : BroadcastReceiver() {

    private val connectSignal = Signals.signal(IConnector::class)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ConnectivityManager.CONNECTIVITY_ACTION)
        {
            var isConnected = false

            val connectivityManager = OpenMic.App.mainActivity?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val networkCapabilities = connectivityManager.activeNetwork
                val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities)

                if (actNw != null) {
                    isConnected = actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                }
            } else {
                connectivityManager.run {
                    connectivityManager.activeNetworkInfo?.run {
                        isConnected = type == ConnectivityManager.TYPE_WIFI
                    }
                }
            }

            connectSignal.dispatcher.onEvent(Connector.WiFi, if (isConnected) ConnectorEvent.CONNECTED_OR_READY else ConnectorEvent.DISABLED)
        }
    }

}