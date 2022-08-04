package pl.grzybdev.openmic.client

import com.gazman.signals.Signals
import pl.grzybdev.openmic.client.interfaces.IConnector

object AppData {

    var connectSignal = Signals.signal(IConnector::class)

    var communicationPort = 10000
    var deviceID: String = ""
}
