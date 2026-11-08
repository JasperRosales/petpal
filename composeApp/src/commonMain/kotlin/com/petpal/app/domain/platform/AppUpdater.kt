package com.petpal.app.domain.platform

/** A newer release published on GitHub. */
data class UpdateInfo(val version: String, val notes: String, val apkUrl: String)

/** Checks GitHub releases for a newer build and installs it in place. */
interface AppUpdater {
    val currentVersion: String
    /** The latest release if it's newer than this build, else null. Throws on network errors. */
    suspend fun check(): UpdateInfo?
    /** Downloads the APK ([onProgress] 0..1) and opens the system installer. */
    suspend fun install(update: UpdateInfo, onProgress: (Float) -> Unit)

    companion object {
        /** True if dotted version [a] is newer than [b] ("0.14.1" > "0.14.0"); ignores a leading "v". */
        fun isNewer(a: String, b: String): Boolean {
            fun parts(v: String) = v.trim().removePrefix("v").split('.', '-').map { it.toIntOrNull() ?: 0 }
            val x = parts(a); val y = parts(b)
            for (i in 0 until maxOf(x.size, y.size)) {
                val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
                if (d != 0) return d > 0
            }
            return false
        }
    }
}
