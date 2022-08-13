package pl.grzybdev.openmic.client.receivers.signals

import android.content.BroadcastReceiver
import android.util.Log
import pl.grzybdev.openmic.client.enumerators.ConnectionStatus
import pl.grzybdev.openmic.client.interfaces.IConnection
import pl.grzybdev.openmic.client.singletons.AppData

class ConnectionSignalReceiver : BroadcastReceiver() {

    override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
        if (intent.action == "UpdateStatus") {
            val statusInt: Int = intent.getIntExtra("status", 0)
            val status: ConnectionStatus = ConnectionStatus.values()[statusInt]
            Log.d(javaClass.name, "New Connection Status: $status")

            AppData.connectionStatus = status
            notifyAllListeners(status)
        }
    }

    private fun notifyAllListeners(status: ConnectionStatus)
    {
        for (listener in AppData.connectionListeners)
            listener.onConnectionStateChange(status)
    }

    fun addListener(listener: IConnection)
    {
        AppData.connectionListeners.add(listener)
    }

    fun removeListener(listener: IConnection)
    {
        AppData.connectionListeners.remove(listener)
    }

}
