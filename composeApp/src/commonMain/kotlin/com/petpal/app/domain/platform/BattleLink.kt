package com.petpal.app.domain.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** A connected opponent. Lines in, lines out. */
interface BattleConnection {
    /** Hot stream of received lines; completes when the link drops. */
    val incoming: Flow<String>
    suspend fun send(line: String)
    fun close()
}

data class Peer(val id: String, val name: String)

enum class LinkKind(val label: String) { HOTSPOT("Hotspot / Wi-Fi") }

/** Offline local link (LAN sockets over a phone hotspot or shared Wi-Fi). */
interface BattleTransport {
    val kind: LinkKind
    val isAvailable: Boolean
    /** Advertise and accept up to [count] guests, emitting each as it joins. Cancel to stop hosting. */
    fun hostMany(name: String, count: Int): Flow<BattleConnection>
    /** Advertise and wait for one guest. Cancel to stop hosting. */
    suspend fun host(name: String): BattleConnection = hostMany(name, 1).first()
    /** Nearby hosts, updated while collected. */
    fun discover(): Flow<List<Peer>>
    suspend fun connect(peer: Peer): BattleConnection
}

class BattleLinks(private val all: List<BattleTransport>) {
    operator fun get(kind: LinkKind) = all.first { it.kind == kind }
}
