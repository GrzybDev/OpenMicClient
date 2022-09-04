package pl.grzybdev.openmic.client.enumerators.network

enum class ConnectionStatus {
    NOT_CONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    DISCONNECTED,
    SELECTING_SERVER_WIFI,
    SELECTING_DEVICE_BT,
}
