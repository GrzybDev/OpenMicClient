package pl.grzybdev.openmic.client.enumerators

enum class ConnectorStatus {
    UNKNOWN,
    USB_NOT_CONNECTED,
    USB_CONNECTED,
    USB_CHECKING,
    USB_CONNECTED_NO_SERVER,
    CONNECTING,
    READY,
    DISABLED,
    NEED_MANUAL_LAUNCH,
    CONNECTED_OR_READY
}
