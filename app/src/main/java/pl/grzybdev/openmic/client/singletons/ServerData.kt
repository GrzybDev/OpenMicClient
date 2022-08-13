package pl.grzybdev.openmic.client.singletons

import pl.grzybdev.openmic.client.enumerators.ServerVersion

object ServerData {
    var name: String = ""
    var id: String = ""
    var os: String = ""
    var version: ServerVersion = ServerVersion.UNKNOWN
}
