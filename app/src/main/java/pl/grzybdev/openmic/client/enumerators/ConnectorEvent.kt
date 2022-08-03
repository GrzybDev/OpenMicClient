package pl.grzybdev.openmic.client.enumerators

enum class ConnectorEvent {
    USB_NOT_CONNECTED,
    USB_CONNECTED,
    USB_CHECKING,
    CONNECTING,
    READY,
    DISABLED,
    NEED_MANUAL_LAUNCH,
    CONNECTED_OR_READY
}
