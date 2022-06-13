package pl.grzybdev.openmic.client

import pl.grzybdev.openmic.client.enumerators.Connector
import pl.grzybdev.openmic.client.enumerators.ServerOS

object AppData {

    var communicationPort = 10000
    var deviceID: String = ""

    var connectLock: Boolean = false
    var currentConn: Connector? = null

    // Server data
    var serverName: String = ""
    var serverID: String = ""
    var serverOS: ServerOS = ServerOS.OTHER
}
