package pl.grzybdev.openmic.client.receivers.signals

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import pl.grzybdev.openmic.client.enumerators.DialogType
import pl.grzybdev.openmic.client.interfaces.IDialog
import pl.grzybdev.openmic.client.singletons.AppData

class DialogSignalReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "ShowDialog") {
            val dialogTypeInt: Int = intent.getIntExtra("type", -1)
            val dialogType: DialogType = DialogType.values()[dialogTypeInt]

            val additionalData: String? = intent.getStringExtra("data")

            notifyAllListeners(dialogType, additionalData)
        }
    }

    private fun notifyAllListeners(dialogType: DialogType, additionalData: String?)
    {
        for (listener in AppData.dialogListeners)
            listener.showDialog(dialogType, additionalData)
    }

    fun addListener(listener: IDialog)
    {
        AppData.dialogListeners.add(listener)
    }

    fun removeListener(listener: IDialog)
    {
        AppData.dialogListeners.remove(listener)
    }

}
