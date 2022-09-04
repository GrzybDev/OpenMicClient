package pl.grzybdev.openmic.client.enumerators

enum class ServerOS(val kernelType: String) {
    WINDOWS("windows"),
    LINUX("linux"),
    LINUX_ARCH("arch"),
    LINUX_DEBIAN("debian"),
    LINUX_FEDORA("fedora"),
    LINUX_MANJARO("manjaro"),
    LINUX_MINT("mint"),
    LINUX_POP_OS("pop"),
    LINUX_UBUNTU("ubuntu"),
    MACOS("macos"),
    UNKNOWN("unknown")
}
