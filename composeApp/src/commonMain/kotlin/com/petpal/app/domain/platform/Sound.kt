package com.petpal.app.domain.platform

/** Built-in sound effects (synthesized on the device, no audio files). */
enum class Sfx {
    HIT, HIT_BIG, CRY, KO, EVADE, CHARGE,
    SLASH, PUNCH, BITE, BOOM, BEAM, WHOOSH,
    FIRE, WATER, LEAF, ICE, ZAP, WIND, ROCK,
    BUFF, HEAL, SHIELD,
}

interface SoundPlayer {
    var enabled: Boolean
    /** [pitch] 0.5-2 (playback rate), [volume] 0-1. */
    fun play(sfx: Sfx, pitch: Float = 1f, volume: Float = 1f)
}
