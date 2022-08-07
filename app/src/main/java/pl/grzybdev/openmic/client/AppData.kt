package pl.grzybdev.openmic.client

import com.gazman.signals.Signals
import pl.grzybdev.openmic.client.enumerators.ConnectionStatus
import pl.grzybdev.openmic.client.enumerators.ConnectorStatus
import pl.grzybdev.openmic.client.interfaces.IConnector
import java.util.TimerTask

object AppData {
    val openmic: OpenMic = OpenMic()

    var connectSignal = Signals.signal(IConnector::class)
    var connectionStatus = ConnectionStatus.UNKNOWN

    var usbStatus: ConnectorStatus = ConnectorStatus.UNKNOWN
    var usbTimer: TimerTask? = null

    var communicationPort = 10000
    var deviceID: String = ""
}
