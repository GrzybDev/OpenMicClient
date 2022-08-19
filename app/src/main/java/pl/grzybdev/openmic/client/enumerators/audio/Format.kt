package pl.grzybdev.openmic.client.enumerators.audio

enum class Format(val id: Int) {
    INVALID(-1),
    PCM_8BIT(0),
    PCM_16BIT(1),
    PCM_FLOAT(2)
}
