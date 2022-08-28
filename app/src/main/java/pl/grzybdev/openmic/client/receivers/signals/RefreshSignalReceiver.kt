package pl.grzybdev.openmic.client.receivers.signals

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import pl.grzybdev.openmic.client.enumerators.network.ConnectionStatus
import pl.grzybdev.openmic.client.interfaces.IConnection
import pl.grzybdev.openmic.client.interfaces.IRefresh
import pl.grzybdev.openmic.client.singletons.AppData

class RefreshSignalReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("RefreshSignalReceiver", "onReceive (${intent.action}")

        if (intent.action == "RefreshUI") {
            Log.d(javaClass.name, "Refreshing UI...")
            notifyAllListeners()
        }
    }

    private fun notifyAllListeners()
    {
        for (listener in AppData.refreshListeners)
            listener.onRefresh()
    }

    fun addListener(listener: IRefresh)
    {
        AppData.refreshListeners.add(listener)
    }

    fun removeListener(listener: IRefresh)
    {
        AppData.refreshListeners.remove(listener)
    }

}
