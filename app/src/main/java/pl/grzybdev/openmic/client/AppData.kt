package pl.grzybdev.openmic.client

import pl.grzybdev.openmic.client.enumerators.ServerOS

object AppData {

    var deviceID: String = ""

    // Server data
    var serverName: String = ""
    var serverID: String = ""
    var serverOS: ServerOS = ServerOS.OTHER
}
