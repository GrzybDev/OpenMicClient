package pl.grzybdev.openmic.client

import com.gazman.signals.Signals
import pl.grzybdev.openmic.client.enumerators.ConnectionStatus
import pl.grzybdev.openmic.client.enumerators.ConnectorStatus
import pl.grzybdev.openmic.client.interfaces.IConnector
import pl.grzybdev.openmic.client.network.Client
import java.util.TimerTask

object AppData {
    var openmic: OpenMic? = null
    var client: Client? = null

    var connectSignal = Signals.signal(IConnector::class)
    var connectionStatus = ConnectionStatus.UNKNOWN

    var usbStatus: ConnectorStatus = ConnectorStatus.UNKNOWN
    var usbTimer: TimerTask? = null

    var communicationPort = 10000
    var deviceID: String = ""
}
