package pl.grzybdev.openmic.client.enumerators.network

enum class ConnectorState {
    UNKNOWN,
    USB_NOT_CONNECTED,
    USB_CONNECTED,
    USB_CHECKING,
    USB_CONNECTED_NO_SERVER,
    READY,
}
