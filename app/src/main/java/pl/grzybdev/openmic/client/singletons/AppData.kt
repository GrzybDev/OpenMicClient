package pl.grzybdev.openmic.client.singletons

import android.content.res.Resources
import pl.grzybdev.openmic.client.OpenMic
import pl.grzybdev.openmic.client.enumerators.ConnectionStatus
import pl.grzybdev.openmic.client.enumerators.ConnectorState
import pl.grzybdev.openmic.client.interfaces.IConnection
import pl.grzybdev.openmic.client.interfaces.IConnector
import java.util.*

object AppData {
    var openmic: OpenMic = OpenMic()
    var resources: Resources? = null

    var connectorListeners: MutableList<IConnector> = mutableListOf()
    var connectionListeners: MutableList<IConnection> = mutableListOf()

    var connectionStatus = ConnectionStatus.NOT_CONNECTED

    var usbState: ConnectorState = ConnectorState.UNKNOWN
    var usbTimer: TimerTask? = null

    var communicationPort = 10000
    var deviceID: String = ""
}
