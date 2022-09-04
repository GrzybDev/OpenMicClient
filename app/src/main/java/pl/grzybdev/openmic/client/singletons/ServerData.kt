package pl.grzybdev.openmic.client.singletons

import pl.grzybdev.openmic.client.adapters.DeviceListAdapter
import pl.grzybdev.openmic.client.adapters.ServerListAdapter
import pl.grzybdev.openmic.client.dataclasses.ServerEntry
import pl.grzybdev.openmic.client.enumerators.ServerVersion

object ServerData {
    var name: String = ""
    var id: String = ""
    var os: String = ""
    var version: ServerVersion = ServerVersion.UNKNOWN

    val foundServers: MutableMap<String, ServerEntry> = mutableMapOf()
    val foundServersTimestamps: MutableMap<String, Long> = mutableMapOf()

    var srvListAdapter: ServerListAdapter = ServerListAdapter()
    var devListAdapter: DeviceListAdapter = DeviceListAdapter()
}
