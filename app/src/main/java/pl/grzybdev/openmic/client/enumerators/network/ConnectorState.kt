package pl.grzybdev.openmic.client.enumerators.network

enum class ConnectorState {
    UNKNOWN,
    NOT_READY,
    USB_CONNECTED,
    USB_CHECKING,
    USB_CONNECTED_NO_SERVER,
    READY,
}
