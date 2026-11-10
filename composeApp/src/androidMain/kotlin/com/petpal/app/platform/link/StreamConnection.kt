package com.petpal.app.platform.link

import com.petpal.app.domain.platform.BattleConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

/** Line-based connection over any socket's streams (TCP or Bluetooth RFCOMM). */
internal class StreamConnection(
    input: InputStream,
    output: OutputStream,
    private val socket: Closeable,
) : BattleConnection {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lines = Channel<String>(Channel.UNLIMITED)
    private val writer = output.bufferedWriter(Charsets.UTF_8)
    private val writeLock = Mutex()

    init {
        scope.launch {
            try {
                input.bufferedReader(Charsets.UTF_8).useLines { seq -> seq.forEach { lines.trySend(it) } }
            } catch (_: Exception) {
            } finally {
                lines.close()
            }
        }
    }

    override val incoming: Flow<String> = lines.receiveAsFlow()

    override suspend fun send(line: String) = withContext(Dispatchers.IO) {
        writeLock.withLock {
            writer.write(line)
            writer.write("\n")
            writer.flush()
        }
    }

    override fun close() {
        runCatching { socket.close() }
        scope.cancel()
    }
}

/** Runs a blocking socket call; closing [closeable] on cancellation unblocks it. */
internal suspend fun <T> blockingIo(closeable: Closeable, block: () -> T): T = withContext(Dispatchers.IO) {
    val done = AtomicBoolean(false)
    val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            awaitCancellation()
        } finally {
            if (!done.get()) runCatching { closeable.close() }
        }
    }
    try {
        block()
    } catch (e: Exception) {
        // We closed the socket ourselves to unblock it: that's a cancel, not a "Socket closed" failure.
        ensureActive()
        throw e
    } finally {
        done.set(true)
        watcher.cancel()
    }
}
