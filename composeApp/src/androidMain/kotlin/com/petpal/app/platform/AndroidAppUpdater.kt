package com.petpal.app.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.petpal.app.domain.platform.AppUpdater
import com.petpal.app.domain.platform.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Updates from the public GitHub releases of JasperRosales/petpal. */
class AndroidAppUpdater(private val context: Context) : AppUpdater {
    override val currentVersion: String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "0"

    override suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        val json = JSONObject(open(LATEST).inputStream.bufferedReader().use { it.readText() })
        val version = json.getString("tag_name")
        if (!AppUpdater.isNewer(version, currentVersion)) return@withContext null
        val assets = json.getJSONArray("assets")
        val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
            .firstOrNull { it.getString("name").endsWith(".apk") } ?: return@withContext null
        UpdateInfo(version.removePrefix("v"), json.optString("body"), apk.getString("browser_download_url"))
    }

    override suspend fun install(update: UpdateInfo, onProgress: (Float) -> Unit) {
        val file = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { deleteRecursively(); mkdirs() }
            val out = File(dir, "petpal-${update.version}.apk")
            val conn = open(update.apkUrl)
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        done += n
                        if (total > 0) onProgress(done.toFloat() / total)
                    }
                }
            }
            out
        }
        // Android 8+: the user must allow PetPal to install apps once.
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            throw IOException("Allow PetPal to install apps, then tap Update again")
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun open(url: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 20_000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "PetPal-Android")
        if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
        return conn
    }

    private companion object {
        const val LATEST = "https://api.github.com/repos/JasperRosales/petpal/releases/latest"
    }
}
