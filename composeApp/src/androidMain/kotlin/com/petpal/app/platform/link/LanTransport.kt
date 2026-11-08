package com.petpal.app.platform.link

import android.content.Context
import android.net.wifi.WifiManager
import com.petpal.app.domain.platform.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.*

/**
 * Battles over a phone hotspot or shared Wi-Fi. Fully local: TCP for the match,
 * UDP broadcast beacons so guests can find the host. No internet is used.
 */
class LanTransport(context: Context) : BattleTransport {
    private val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?

    override val kind = LinkKind.HOTSPOT
    override val isAvailable = true

    override suspend fun host(name: String): BattleConnection = coroutineScope {
        val server = ServerSocket().apply {
            reuseAddress = true
            bind(InetSocketAddress(GAME_PORT))
        }
        val beacon = launch(Dispatchers.IO) { broadcastBeacon(name) }
        try {
            val socket = blockingIo(server) { server.accept() }
            socket.tcpNoDelay = true
            StreamConnection(socket.getInputStream(), socket.getOutputStream(), socket)
        } finally {
            beacon.cancel()
            runCatching { server.close() }
        }
    }

    private suspend fun broadcastBeacon(name: String) {
        DatagramSocket().use { udp ->
            udp.broadcast = true
            val payload = "$BEACON_TAG|${name.replace('|', ' ')}|$GAME_PORT".toByteArray()
            while (currentCoroutineContext().isActive) {
                broadcastAddresses().forEach { addr ->
                    runCatching { udp.send(DatagramPacket(payload, payload.size, addr, BEACON_PORT)) }
                }
                delay(1000)
            }
        }
    }

    override fun discover(): Flow<List<Peer>> = channelFlow {
        val lock = wifi?.createMulticastLock("petpal-discovery")?.apply { setReferenceCounted(false); acquire() }
        val seen = LinkedHashMap<String, Pair<Peer, Long>>()
        gatewayPeer()?.let { seen[it.id] = it to Long.MAX_VALUE }
        send(seen.values.map { it.first })
        val udp = DatagramSocket(null).apply {
            reuseAddress = true
            broadcast = true
            soTimeout = 1000
            bind(InetSocketAddress(BEACON_PORT))
        }
        try {
            withContext(Dispatchers.IO) {
                val buf = ByteArray(256)
                while (isActive) {
                    val packet = DatagramPacket(buf, buf.size)
                    try {
                        udp.receive(packet)
                        val parts = String(packet.data, 0, packet.length).split('|')
                        if (parts.size == 3 && parts[0] == BEACON_TAG) {
                            val ip = packet.address.hostAddress ?: continue
                            val id = "$ip:${parts[2]}"
                            seen.remove("$ip:$GAME_PORT")?.takeIf { it.second == Long.MAX_VALUE } // replace gateway guess
                            seen[id] = Peer(id, "${parts[1]} ($ip)") to System.currentTimeMillis()
                        }
                    } catch (_: SocketTimeoutException) {
                    }
                    val now = System.currentTimeMillis()
                    seen.entries.removeAll { now - it.value.second > 5000 && it.value.second != Long.MAX_VALUE }
                    send(seen.values.map { it.first })
                }
            }
        } finally {
            udp.close()
            lock?.release()
        }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    override suspend fun connect(peer: Peer): BattleConnection {
        val (ip, port) = peer.id.split(':').let { it[0] to it[1].toInt() }
        val socket = Socket()
        blockingIo(socket) { socket.connect(InetSocketAddress(ip, port), 6000) }
        socket.tcpNoDelay = true
        return StreamConnection(socket.getInputStream(), socket.getOutputStream(), socket)
    }

    /** When joined to someone's hotspot, the hotspot phone is the gateway. */
    @Suppress("DEPRECATION")
    private fun gatewayPeer(): Peer? {
        val gw = runCatching { wifi?.dhcpInfo?.gateway ?: 0 }.getOrDefault(0)
        if (gw == 0) return null
        val ip = listOf(gw and 0xff, gw shr 8 and 0xff, gw shr 16 and 0xff, gw shr 24 and 0xff).joinToString(".")
        return Peer("$ip:$GAME_PORT", "Hotspot owner ($ip)")
    }

    private fun broadcastAddresses(): Set<InetAddress> {
        val out = mutableSetOf<InetAddress>(InetAddress.getByName("255.255.255.255"))
        runCatching {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.interfaceAddresses }
                .mapNotNullTo(out) { it.broadcast }
        }
        return out
    }

    private companion object {
        const val GAME_PORT = 47_771
        const val BEACON_PORT = 47_772
        const val BEACON_TAG = "PETPAL1"
    }
}
