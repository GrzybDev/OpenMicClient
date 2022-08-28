package pl.grzybdev.openmic.client.receivers.connectors

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.enumerators.network.Connector
import pl.grzybdev.openmic.client.enumerators.network.ConnectorState

class BTStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            BluetoothDevice.ACTION_FOUND -> run {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                if (context?.let {
                        ActivityCompat.checkSelfPermission(
                            it,
                            Manifest.permission.BLUETOOTH_CONNECT
                        )
                    } != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d(javaClass.name, "App doesn't have BLUETOOTH_CONNECT permission, ignoring onReceive signal!")
                    return
                }

                if (device?.name != null) {
                    Log.d(javaClass.name, "Found Bluetooth device: ${device.name}")
                }
            }

            BluetoothAdapter.ACTION_STATE_CHANGED -> run {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                    BluetoothAdapter.STATE_OFF -> OpenMic.changeConnectorStatus(context!!, Connector.Bluetooth, ConnectorState.NOT_READY)
                    BluetoothAdapter.STATE_ON -> OpenMic.changeConnectorStatus(context!!, Connector.Bluetooth, ConnectorState.READY)
                }
            }
        }
    }

}
