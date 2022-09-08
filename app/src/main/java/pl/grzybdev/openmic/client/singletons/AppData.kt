package pl.grzybdev.openmic.client.singletons

import android.content.SharedPreferences
import android.content.res.Resources
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.enumerators.network.ConnectionStatus
import pl.grzybdev.openmic.client.enumerators.network.ConnectorState
import pl.grzybdev.openmic.client.interfaces.*
import pl.grzybdev.openmic.client.network.Audio
import java.util.*

object AppData {
    val audio = Audio()
    val openmic = OpenMic()

    var sharedPrefs: SharedPreferences? = null
    var resources: Resources? = null

    var connectorListeners: MutableList<IConnector> = mutableListOf()
    var connectionListeners: MutableList<IConnection> = mutableListOf()
    var dialogListeners: MutableList<IDialog> = mutableListOf()
    var refreshListeners: MutableList<IRefresh> = mutableListOf()

    var connectionStatus = ConnectionStatus.NOT_CONNECTED

    var usbState: ConnectorState = ConnectorState.UNKNOWN
    var usbTimer: TimerTask? = null

    var communicationPort = -1
    var deviceID: String = ""
}
