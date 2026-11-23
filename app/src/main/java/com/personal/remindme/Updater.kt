package com.personal.remindme

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Checks GitHub for a newer build, shows what's new, downloads it and
 * opens Android's installer. Android always asks you to tap Install.
 */
object Updater {
    data class Release(val code: Int, val name: String, val notes: String, val apkUrl: String)

    sealed class State {
        object Idle : State()
        data class Downloading(val percent: Int) : State()
        data class Failed(val message: String) : State()
    }

    private const val LATEST = "https://api.github.com/repos/ajitgitthub/geddydun/releases/latest"

    val available = MutableStateFlow<Release?>(null)
    val state = MutableStateFlow<State>(State.Idle)
    @Volatile private var checking = false
    private var lastCheck = 0L

    fun currentCode(ctx: Context): Int =
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).longVersionCode.toInt()

    fun currentName(ctx: Context): String =
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""

    /** Look for a newer version (at most every 10 minutes). */
    fun check(ctx: Context, force: Boolean = false, done: ((Boolean) -> Unit)? = null) {
        val now = System.currentTimeMillis()
        if (checking || (!force && now - lastCheck < 10 * 60_000L)) return
        checking = true
        val app = ctx.applicationContext
        thread(name = "update-check") {
            var found = false
            try {
                val c = URL(LATEST).openConnection() as HttpURLConnection
                c.setRequestProperty("Accept", "application/vnd.github+json")
                c.setRequestProperty("User-Agent", "Geddydun-app")
                c.connectTimeout = 10_000
                c.readTimeout = 10_000
                if (c.responseCode == 200) {
                    val o = JSONObject(c.inputStream.bufferedReader().readText())
                    val tag = o.optString("tag_name")
                    val code = tag.substringAfterLast('.').toIntOrNull() ?: 0
                    val assets = o.optJSONArray("assets")
                    var url = ""
                    if (assets != null) for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        if (a.optString("name").endsWith(".apk")) url = a.optString("browser_download_url")
                    }
                    if (code > currentCode(app) && url.isNotBlank()) {
                        found = true
                        available.value = Release(code, tag.removePrefix("v"), o.optString("body").trim(), url)
                    } else {
                        available.value = null
                    }
                    lastCheck = System.currentTimeMillis()
                }
                c.disconnect()
            } catch (_: Exception) {
            } finally {
                checking = false
                done?.invoke(found)
            }
        }
    }

    private fun apkFile(ctx: Context, r: Release) = File(File(ctx.cacheDir, "updates"), "Geddydun-${r.code}.apk")

    /** Download (if needed) and open the installer. */
    fun install(ctx: Context, r: Release) {
        val app = ctx.applicationContext
        if (!app.packageManager.canRequestPackageInstalls()) {
            // One-time: allow Geddydun to install updates.
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            state.value = State.Failed("Allow Geddydun to install updates, then come back and tap Update again.")
            return
        }
        val file = apkFile(app, r)
        if (file.exists() && file.length() > 0) return openInstaller(app, file)
        if (state.value is State.Downloading) return
        state.value = State.Downloading(0)
        thread(name = "update-download") {
            try {
                file.parentFile?.listFiles()?.forEach { it.delete() }
                file.parentFile?.mkdirs()
                val tmp = File(file.parentFile, file.name + ".part")
                var url = URL(r.apkUrl)
                var c: HttpURLConnection
                var hops = 0
                while (true) {
                    c = url.openConnection() as HttpURLConnection
                    c.instanceFollowRedirects = false
                    c.setRequestProperty("User-Agent", "Geddydun-app")
                    c.connectTimeout = 15_000
                    c.readTimeout = 30_000
                    val code = c.responseCode
                    if (code in 300..399 && hops < 5) {
                        url = URL(url, c.getHeaderField("Location")); c.disconnect(); hops++
                    } else break
                }
                if (c.responseCode != 200) throw Exception("Download failed (${c.responseCode})")
                val total = c.contentLengthLong
                var read = 0L
                c.inputStream.use { input ->
                    tmp.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            read += n
                            if (total > 0) state.value = State.Downloading((read * 100 / total).toInt())
                        }
                    }
                }
                c.disconnect()
                tmp.renameTo(file)
                state.value = State.Idle
                openInstaller(app, file)
            } catch (e: Exception) {
                state.value = State.Failed("Couldn't download the update. Check your internet and try again.")
            }
        }
    }

    private fun openInstaller(ctx: Context, file: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
