package pl.grzybdev.openmic.client.enumerators.audio

enum class Channels(val count: Int) {
    INVALID(0),
    MONO(1),
    STEREO(2),
}
