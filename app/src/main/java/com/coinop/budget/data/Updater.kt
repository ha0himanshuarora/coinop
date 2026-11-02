package com.coinop.budget.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One published GitHub release that carries an APK. */
data class AppRelease(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val apkName: String,
    val size: Long,
    val shaUrl: String?,
)

/** What the "Check for updates" sheet is showing. */
sealed interface UpdateUi {
    data object Idle : UpdateUi
    data object Checking : UpdateUi
    data object UpToDate : UpdateUi
    data class Available(val release: AppRelease) : UpdateUi
    data class Downloading(val release: AppRelease, val progress: Float) : UpdateUi
    data class Ready(val release: AppRelease, val file: File) : UpdateUi
    data class Failed(val message: String) : UpdateUi
}

/** Checks the repo's latest GitHub release, downloads its APK and hands it to the system installer. */
object Updater {
    const val REPO = "ha0himanshuarora/coinop"

    @Serializable
    private class GhAsset(
        val name: String,
        @SerialName("browser_download_url") val url: String,
        val size: Long = 0,
    )

    @Serializable
    private class GhRelease(
        @SerialName("tag_name") val tag: String,
        val body: String? = null,
        val assets: List<GhAsset> = emptyList(),
    )

    private val json = Json { ignoreUnknownKeys = true }

    private fun parts(v: String): List<Int> =
        v.removePrefix("v").substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }

    /** 1.0.1 > 1.0.0; a local "1.0.0-dev" build counts as 1.0.0. */
    fun isNewer(remote: String, local: String): Boolean {
        val r = parts(remote)
        val l = parts(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "Coinop-Android")
            setRequestProperty("Accept", "application/vnd.github+json")
        }

    /** The latest release, or null when the repo has no release with an APK yet. */
    suspend fun latest(): AppRelease? = withContext(Dispatchers.IO) {
        val c = open("https://api.github.com/repos/$REPO/releases/latest")
        try {
            when (c.responseCode) {
                404 -> return@withContext null
                200 -> Unit
                else -> error("GitHub answered ${c.responseCode}")
            }
            val rel = json.decodeFromString<GhRelease>(c.inputStream.bufferedReader().use { it.readText() })
            val apk = rel.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) } ?: return@withContext null
            val sha = rel.assets.firstOrNull { it.name == apk.name + ".sha256" }
            AppRelease(rel.tag.removePrefix("v"), rel.body.orEmpty().trim(), apk.url, apk.name, apk.size, sha?.url)
        } finally {
            c.disconnect()
        }
    }

    /** Downloads the APK into the app cache, checking its SHA-256 when the release publishes one. */
    suspend fun download(context: Context, release: AppRelease, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val out = File(dir, release.apkName)
            val digest = MessageDigest.getInstance("SHA-256")
            val c = open(release.apkUrl)
            try {
                if (c.responseCode != 200) error("Download failed (${c.responseCode})")
                val total = c.contentLengthLong.takeIf { it > 0 } ?: release.size
                var done = 0L
                c.inputStream.use { input ->
                    out.outputStream().use { sink ->
                        val buf = ByteArray(32 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            sink.write(buf, 0, n)
                            digest.update(buf, 0, n)
                            done += n
                            if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            } catch (e: Exception) {
                out.delete()
                throw e
            } finally {
                c.disconnect()
            }
            release.shaUrl?.let { url ->
                val expected = open(url).let { sc ->
                    try {
                        sc.inputStream.bufferedReader().use { it.readText() }
                    } finally {
                        sc.disconnect()
                    }
                }.trim().substringBefore(" ").lowercase()
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                if (expected.isNotEmpty() && expected != actual) {
                    out.delete()
                    error("The downloaded file is damaged (checksum mismatch). Try again.")
                }
            }
            out
        }

    /**
     * Opens the system installer. Returns false when Android first needs the "install unknown apps"
     * permission for Coinop; its settings screen is opened and the user taps Install again afterwards.
     */
    fun install(context: Context, file: File): Boolean {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return true
    }
}
