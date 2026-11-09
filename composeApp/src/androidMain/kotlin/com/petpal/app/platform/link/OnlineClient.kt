package com.petpal.app.platform.link

import android.content.Context
import com.petpal.app.domain.battle.FighterInfo
import com.petpal.app.domain.battle.Protocol
import com.petpal.app.domain.platform.BattleConnection
import com.petpal.app.domain.platform.Leaderboard
import com.petpal.app.domain.platform.OnlineMatch
import com.petpal.app.domain.platform.OnlineService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** PetPal's matchmaking server. The free Render plan sleeps when idle; the first call wakes it (up to ~1 min). */
const val ONLINE_URL = "https://petpal-online.onrender.com"

class OnlineClient(context: Context) : OnlineService {
    private val prefs = context.getSharedPreferences("petpal_online", Context.MODE_PRIVATE)

    /** Anonymous player id, made once per install. Your rating is tied to it. */
    private val playerId: String
        get() = prefs.getString("id", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("id", it).apply() }

    private val http = OkHttpClient.Builder()
        .connectTimeout(70, TimeUnit.SECONDS)
        .readTimeout(70, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    override suspend fun findMatch(me: FighterInfo): OnlineMatch {
        val url = ONLINE_URL.toHttpUrl().newBuilder().addPathSegment("play")
            .addQueryParameter("id", playerId)
            .addQueryParameter("name", me.name)
            .addQueryParameter("species", me.species.name)
            .addQueryParameter("stage", me.stage.name)
            .addQueryParameter("v", Protocol.VERSION.toString())
            .build()
        val conn = WebSocketConnection(http, Request.Builder().url(url).build())
        try {
            // Everything before the match notice is ignored; after it, lines belong to the battle.
            for (line in conn.lines) {
                if (!line.startsWith("~M|")) continue
                val p = line.split('|')
                return OnlineMatch(host = p[1] == "host", opponent = p.getOrElse(2) { "Opponent" }, conn = conn)
            }
            throw IOException(conn.failure ?: "server closed the connection")
        } catch (e: Throwable) {
            conn.close()
            throw e
        }
    }

    override suspend fun leaderboard(): Leaderboard = withContext(Dispatchers.IO) {
        val url = ONLINE_URL.toHttpUrl().newBuilder().addPathSegment("leaderboard").addQueryParameter("id", playerId).build()
        http.newCall(Request.Builder().url(url).build()).await().use { r ->
            if (!r.isSuccessful) throw IOException("server error ${r.code}")
            Leaderboard.parse(r.body?.string().orEmpty())
        }
    }
}

private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = cont.resumeWithException(e)
        override fun onResponse(call: Call, response: Response) = cont.resume(response)
    })
}

/** Battle link over a WebSocket: frames may carry several lines (the host batches them). */
private class WebSocketConnection(http: OkHttpClient, request: Request) : BattleConnection {
    val lines = Channel<String>(Channel.UNLIMITED)
    @Volatile var failure: String? = null
    private val ws: WebSocket = http.newWebSocket(request, object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            text.split('\n').forEach { if (it.isNotEmpty()) lines.trySend(it) }
        }
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
            lines.close()
        }
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { lines.close() }
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            failure = t.message ?: t::class.simpleName
            lines.close()
        }
    })

    override val incoming: Flow<String> = lines.receiveAsFlow()

    override suspend fun send(line: String) {
        if (!ws.send(line)) throw IOException("connection closed")
    }

    override fun close() {
        ws.close(1000, null)
        lines.close()
    }
}
