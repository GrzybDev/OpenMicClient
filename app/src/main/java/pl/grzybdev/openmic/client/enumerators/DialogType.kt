package pl.grzybdev.openmic.client.enumerators

enum class DialogType {
    SERVER_ERROR,
    SERVER_DISCONNECT,
    AUTH,
    CLIENT_CONFIG_NOT_COMPATIBLE,
    CLIENT_CONFIG_INVALID,
    SERVER_CONFIG_NOT_COMPATIBLE,
    CLIENT_INTERNAL_ERROR
}
