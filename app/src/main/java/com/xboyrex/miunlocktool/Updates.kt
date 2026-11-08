package com.xboyrex.miunlocktool

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

@Immutable
data class UpdateInfo(
    val version: String,
    val tag: String,
    val notes: String,
    val apkUrl: String,
    val sizeBytes: Long,
)

internal val RELEASE_NOTES = """
    • Pure AMOLED Dark + Onyx Yellow (#FFD600) theme with Liquid Glass UI shaders.
    • High-precision Beijing midnight (00:00:00 CST) auto-sniper with SNTP & HTTP/TCP latency compensation.
    • Built-in Xiaomi Account QR Login & WebView Cookie Extractor for automatic new_bbs_serviceToken & ssecurity retrieval.
    • Full USB OTG Fastboot & Xiaomi ahaUnlock encryptData(token) generator for PC-less bootloader unlocking.
    • HyperOS Spotlight & Dynamic Island live countdown notification support.
""".trimIndent()

@Stable
class UpdateState(private val scope: CoroutineScope) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    var info by mutableStateOf<UpdateInfo?>(null)
        private set
    var visible by mutableStateOf(false)
        private set
    var checking by mutableStateOf(false)
        private set
    var downloading by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var downloaded by mutableStateOf<File?>(null)
        private set
    var message by mutableStateOf<String?>(null)

    private var downloadJob: Job? = null

    fun check(manual: Boolean = false) {
        if (checking) return
        checking = true
        scope.launch {
            val latest = withContext(Dispatchers.IO) { runCatching { fetchLatest() }.getOrNull() }
            checking = false
            if (latest == null) {
                if (manual) message = "You're on the latest version (${BuildConfig.VERSION_NAME})."
                return@launch
            }
            if (isNewerVersion(latest.version, BuildConfig.VERSION_NAME)) {
                info = latest
                error = null
                visible = true
            } else if (manual) {
                message = "You're on the latest version (${BuildConfig.VERSION_NAME})."
            }
        }
    }

    fun download(context: Context) {
        val update = info ?: return
        if (downloading) return
        downloading = true
        error = null
        progress = 0f
        downloadJob = scope.launch {
            val file = withContext(Dispatchers.IO) {
                runCatching { downloadApk(context, update) { p -> progress = p } }.getOrNull()
            }
            downloading = false
            downloadJob = null
            if (file != null) {
                downloaded = file
                install(context)
            } else {
                error = "Download failed. Check your connection and try again."
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        downloading = false
        progress = 0f
    }

    fun install(context: Context) {
        val file = downloaded ?: return
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            error = "Allow installs from Mi Unlock Tool, then tap Install again."
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            return
        }
        error = null
        runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            error = "Couldn't open the installer. Try again."
        }
    }

    fun dismiss() {
        if (!downloading) visible = false
    }

    private fun fetchLatest(): UpdateInfo? {
        val request = Request.Builder()
            .url("https://api.github.com/repos/xboyrex/miunlocktool/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "MiUnlockTool/${BuildConfig.VERSION_NAME}")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val json = JSONObject(response.body?.string().orEmpty())
            val tag = json.optString("tag_name").ifBlank { return null }
            val version = tag.removePrefix("v").removePrefix("V").trim()
            if (version.isBlank()) return null
            val assets = json.optJSONArray("assets") ?: return null
            var apkUrl: String? = null
            var size = 0L
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name")
                if (!name.endsWith(".apk", ignoreCase = true)) continue
                val candidate = asset.optString("browser_download_url")
                if (candidate.isBlank()) continue
                if (apkUrl == null || name.contains("MiUnlockTool", ignoreCase = true)) {
                    apkUrl = candidate
                    size = asset.optLong("size")
                }
            }
            val url = apkUrl ?: return null
            return UpdateInfo(
                version = version,
                tag = tag,
                notes = json.optString("body").orEmpty(),
                apkUrl = url,
                sizeBytes = size,
            )
        }
    }

    private suspend fun downloadApk(
        context: Context,
        update: UpdateInfo,
        onProgress: (Float) -> Unit,
    ): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(dir, "MiUnlockTool.apk")
        if (target.exists()) target.delete()
        val request = Request.Builder()
            .url(update.apkUrl)
            .header("User-Agent", "MiUnlockTool/${BuildConfig.VERSION_NAME}")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body ?: error("Empty download")
            val total = body.contentLength().takeIf { it > 0 } ?: update.sizeBytes
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var done = 0L
                    var lastTick = 0L
                    while (input.read(buffer).also { read = it } > 0) {
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) {
                            val p = (done.toFloat() / total).coerceIn(0f, 1f)
                            val now = System.currentTimeMillis()
                            if (now - lastTick > 80 || p >= 1f) {
                                lastTick = now
                                withContext(Dispatchers.Main) { onProgress(p) }
                            }
                        }
                    }
                }
            }
        }
        if (target.length() <= 0L) error("Empty download")
        return target
    }
}

fun isNewerVersion(remote: String, current: String): Boolean {
    fun parts(value: String) = value.trim()
        .removePrefix("v")
        .removePrefix("V")
        .substringBefore('-')
        .split('.')
        .map { it.toIntOrNull() ?: 0 }
    val r = parts(remote)
    val c = parts(current)
    for (i in 0 until maxOf(r.size, c.size)) {
        val a = r.getOrElse(i) { 0 }
        val b = c.getOrElse(i) { 0 }
        if (a != b) return a > b
    }
    return false
}

private fun formatUpdateSize(bytes: Long): String = when {
    bytes <= 0L -> ""
    bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024f * 1024f))
    bytes >= 1024L -> "${bytes / 1024L} KB"
    else -> "$bytes B"
}

@Composable
fun UpdateOverlay(state: UpdateState, backdrop: Backdrop) {
    val context = LocalContext.current
    val update = state.info
    val c = MaterialTheme.colorScheme

    AnimatedVisibility(
        visible = state.visible && update != null,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(150)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { state.dismiss() },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = state.visible,
                enter = scaleIn(
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
                    initialScale = 0.86f,
                ) + fadeIn(tween(220)),
                exit = scaleOut(tween(140), targetScale = 0.94f) + fadeOut(tween(140)),
            ) {
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .widthIn(max = 410.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .liquidGlass(
                            backdrop,
                            glassSpec(
                                shape = RoundedCornerShape(30.dp),
                                tint = glassFill(0.92f),
                                blur = 26.dp,
                                refractionHeight = 16.dp,
                                refractionAmount = 24.dp,
                            ),
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { }
                        .padding(22.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(c.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = c.primary,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Update available",
                                fontFamily = RoundedHeavy,
                                color = c.onSurface,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Mi Unlock Tool ${update?.version.orEmpty()}" +
                                    formatUpdateSize(update?.sizeBytes ?: 0L).let { if (it.isBlank()) "" else " · $it" },
                                color = c.onSurfaceVariant,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .pressable(onClick = { state.dismiss() }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Close update dialog",
                                tint = c.onSurfaceVariant,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "What's new",
                        color = c.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        update?.notes?.trim()?.ifBlank { RELEASE_NOTES } ?: RELEASE_NOTES,
                        color = c.onSurface.copy(alpha = 0.92f),
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        maxLines = 9,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 172.dp)
                            .verticalScroll(rememberScrollState()),
                    )

                    state.error?.let { message ->
                        Spacer(Modifier.height(12.dp))
                        Text(
                            message,
                            color = c.error,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    if (state.downloading) {
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = c.primary,
                            trackColor = c.onSurface.copy(alpha = 0.12f),
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            "${(state.progress * 100).toInt()}% · downloading MiUnlockTool.apk",
                            color = c.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                if (state.downloading) state.cancelDownload() else state.dismiss()
                            },
                        ) {
                            Text(
                                "Cancel",
                                color = c.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (state.downloaded != null) state.install(context) else state.download(context)
                            },
                            enabled = !state.downloading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = c.primary,
                                contentColor = c.onPrimary,
                            ),
                        ) {
                            if (state.checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = c.onPrimary,
                                )
                            } else {
                                Text(
                                    "Install",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Downloaded from GitHub Releases · xboyrex/miunlocktool",
                        color = c.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
