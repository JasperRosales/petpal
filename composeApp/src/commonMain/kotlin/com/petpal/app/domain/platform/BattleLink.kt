package com.petpal.app.domain.platform

import kotlinx.coroutines.flow.Flow

/** A connected opponent. Lines in, lines out. */
interface BattleConnection {
    /** Hot stream of received lines; completes when the link drops. */
    val incoming: Flow<String>
    suspend fun send(line: String)
    fun close()
}

data class Peer(val id: String, val name: String)

enum class LinkKind(val label: String) { BLUETOOTH("Bluetooth"), HOTSPOT("Hotspot / Wi-Fi") }

/** Offline local link (Bluetooth RFCOMM or LAN sockets over a phone hotspot). */
interface BattleTransport {
    val kind: LinkKind
    val isAvailable: Boolean
    /** Advertise and wait for one guest. Cancel to stop hosting. */
    suspend fun host(name: String): BattleConnection
    /** Nearby hosts, updated while collected. */
    fun discover(): Flow<List<Peer>>
    suspend fun connect(peer: Peer): BattleConnection
}

class BattleLinks(private val all: List<BattleTransport>) {
    operator fun get(kind: LinkKind) = all.first { it.kind == kind }
}
