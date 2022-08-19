package pl.grzybdev.openmic.client.singletons

import pl.grzybdev.openmic.client.enumerators.audio.Channels
import pl.grzybdev.openmic.client.enumerators.audio.Format

object StreamData {
    var sampleRate: Int = -1
    var channels: Channels = Channels.INVALID
    var format: Format = Format.INVALID
    var bufferSize: Int = -1
}
