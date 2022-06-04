package pl.grzybdev.openmic.client.enumerators

enum class ServerOS(val kernelType: String) {
    WINDOWS("winnt"),
    LINUX("linux"),
    OTHER("unknown")
}