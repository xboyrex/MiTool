package com.xboyrex.miunlocktool

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.xboyrex.miunlocktool.data.AccountStatus
import com.xboyrex.miunlocktool.data.ApplyResult
import com.xboyrex.miunlocktool.data.AuthPollResult
import com.xboyrex.miunlocktool.data.AuthSessionInfo
import com.xboyrex.miunlocktool.data.ExecutionMode
import com.xboyrex.miunlocktool.data.LogColorType
import com.xboyrex.miunlocktool.data.LogEntry
import com.xboyrex.miunlocktool.data.PreferencesManager
import com.xboyrex.miunlocktool.data.TranslationManager
import com.xboyrex.miunlocktool.data.XiaomiAuthManager
import com.xboyrex.miunlocktool.data.XiaomiSessionCredentials
import com.xboyrex.miunlocktool.fastboot.FastbootConnectionState
import com.xboyrex.miunlocktool.ui.MainViewModel
import com.xboyrex.miunlocktool.unlock.MiUnlockApiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ApplyScreen(
    viewModel: MainViewModel,
    contentPadding: PaddingValues,
    onOpenAuth: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val clockBeijing by viewModel.scheduler.clockBeijing.collectAsState()
    val countdown by viewModel.scheduler.countdown.collectAsState()
    val pingDetailed by viewModel.scheduler.pingDetailed.collectAsState()
    val pingMs by viewModel.scheduler.pingMs.collectAsState()
    val isRunning by viewModel.scheduler.isRunning.collectAsState()

    val token by viewModel.token.collectAsState()
    val accountStatus by viewModel.accountStatus.collectAsState()
    val isCheckingStatus by viewModel.isCheckingStatus.collectAsState()
    val executionMode by viewModel.executionMode.collectAsState()
    val timingProfile by viewModel.timingProfile.collectAsState()
    val logs by viewModel.logs.collectAsState()

    CollapsingHeaderLayout(
        header = {
            ScreenHeader(
                title = "Mi Unlock Tool",
                subtitle = "HyperOS Midnight Sniper & Token Studio · @xboyrex",
                brandMark = true,
                actions = {
                    CircleAction(
                        icon = Icons.Rounded.QrCode2,
                        contentDescription = "Xiaomi Account Login",
                        onClick = onOpenAuth,
                        tint = if (token.isNotEmpty()) Color(0xFF2ECC71) else c.primary,
                        activeBadge = token.isNotEmpty()
                    )
                    CircleAction(
                        icon = Icons.Rounded.Sync,
                        contentDescription = "Sync NTP & Ping",
                        onClick = {
                            viewModel.syncNtp()
                            viewModel.measurePing()
                        }
                    )
                }
            )
        }
    ) { topPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = topPadding + 6.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Beijing Time & Countdown Hero Card
            BeijingClockHeroCard(
                clockBeijing = clockBeijing,
                countdown = countdown,
                pingHttp = pingDetailed?.httpPingMs ?: -1L,
                pingTcp = pingDetailed?.tcpPingMs ?: -1L,
                pingAvg = if (pingDetailed != null && pingDetailed!!.averageMs > 0) pingDetailed!!.averageMs else pingMs,
                timingProfile = timingProfile,
                activeTimings = viewModel.prefs.getActiveTimings(),
                onSyncNtp = { viewModel.syncNtp() },
                onMeasurePing = { viewModel.measurePing() }
            )

            // 2. Xiaomi Service Token & Eligibility Card
            TokenAndEligibilityCard(
                token = token,
                onTokenChange = viewModel::updateToken,
                accountStatus = accountStatus?.status ?: AccountStatus.NOT_CHECKED,
                statusMessage = accountStatus?.message ?: TranslationManager.tr("status_not_checked"),
                statusDeadline = accountStatus?.deadline,
                isChecking = isCheckingStatus,
                onOpenAuth = onOpenAuth,
                onPaste = {
                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val pasted = cb.primaryClip?.getItemAt(0)?.text?.toString()?.trim().orEmpty()
                    if (pasted.isNotEmpty()) {
                        viewModel.updateToken(pasted)
                    } else {
                        Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                    }
                },
                onClear = { viewModel.updateToken("") },
                onCheckStatus = { viewModel.checkAccountStatus() }
            )

            // 3. Execution Mode & Sniper Control Card
            ExecutionControlCard(
                executionMode = executionMode,
                isRunning = isRunning,
                onSelectMode = viewModel::updateExecutionMode,
                onToggleStartStop = {
                    if (!isRunning && token.isBlank()) {
                        Toast.makeText(context, TranslationManager.tr("toast_token_required"), Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.toggleScheduler()
                    }
                }
            )

            // 4. Live Execution Console Log Card
            ExecutionConsoleCard(
                logs = logs,
                onCopyLogs = {
                    val all = viewModel.getAllLogsText()
                    if (all.isNotEmpty()) {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("MiUnlockTool Logs", all))
                        Toast.makeText(context, TranslationManager.tr("log_copied"), Toast.LENGTH_SHORT).show()
                    }
                },
                onClearLogs = { viewModel.clearLogs() }
            )
        }
    }
}

@Composable
private fun BeijingClockHeroCard(
    clockBeijing: String,
    countdown: String,
    pingHttp: Long,
    pingTcp: Long,
    pingAvg: Long,
    timingProfile: String,
    activeTimings: List<Double>,
    onSyncNtp: () -> Unit,
    onMeasurePing: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val sendOffsetStr = remember(pingAvg) {
        if (pingAvg > 0) {
            val sendSec = (60.0 - (pingAvg / 1000.0)).coerceIn(56.0, 59.95)
            String.format(Locale.US, "23:59:%.3f", sendSec)
        } else {
            "23:59:58.800"
        }
    }
    val profileLabel = when (timingProfile) {
        PreferencesManager.PROFILE_EUROPE -> "Europe"
        PreferencesManager.PROFILE_USA_AFRICA -> "USA / Africa"
        PreferencesManager.PROFILE_CUSTOM -> "Custom"
        else -> "Asia"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        c.surfaceContainerHigh,
                        c.surfaceContainer
                    )
                )
            )
            .border(1.dp, c.primary.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(c.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.AccessTime, null, tint = c.primary, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = TranslationManager.tr("beijing_time_title"),
                        fontFamily = RoundedHeavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = c.onSurface
                    )
                    Text(
                        text = "Quota resets daily at 00:00:00.000 CST",
                        fontSize = 11.sp,
                        color = c.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallActionChip(
                    label = "NTP",
                    icon = Icons.Rounded.Sync,
                    onClick = onSyncNtp
                )
                SmallActionChip(
                    label = "Ping",
                    icon = Icons.Rounded.NetworkCheck,
                    onClick = onMeasurePing
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Big Beijing Time Readout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(c.surfaceContainerLowest)
                .border(1.dp, c.onSurface.copy(alpha = 0.07f), RoundedCornerShape(18.dp))
                .padding(vertical = 14.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = clockBeijing,
                    fontFamily = MonoFont,
                    fontWeight = FontWeight.Black,
                    fontSize = 31.sp,
                    letterSpacing = 1.sp,
                    color = c.primary
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "T-MINUS",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = c.onSurfaceVariant
                    )
                    Text(
                        text = countdown,
                        fontFamily = MonoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = c.onSurface
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Ping & Trigger Time Metrics Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricMiniTile(
                modifier = Modifier.weight(1f),
                title = "API Ping",
                value = if (pingAvg > 0) "${pingAvg} ms" else "Measuring…",
                subtitle = if (pingHttp > 0) "HTTP ${pingHttp}ms · TCP ${pingTcp}ms" else "sgp-api.buy.mi.com",
                accent = c.primary
            )
            MetricMiniTile(
                modifier = Modifier.weight(1f),
                title = "Trigger Window",
                value = sendOffsetStr,
                subtitle = "$profileLabel (${activeTimings.size} bursts)",
                accent = Color(0xFF2ECC71)
            )
        }
    }
}

@Composable
private fun MetricMiniTile(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(c.surfaceContainerHighest.copy(alpha = 0.65f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontFamily = MonoFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = subtitle,
            fontSize = 10.5.sp,
            color = c.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SmallActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .pressable(onClick = onClick)
            .clip(CircleShape)
            .background(c.surfaceContainerHighest)
            .border(1.dp, c.primary.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = c.primary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            fontFamily = RoundedHeavy,
            fontWeight = FontWeight.Bold,
            fontSize = 11.5.sp,
            color = c.onSurface
        )
    }
}

@Composable
private fun TokenAndEligibilityCard(
    token: String,
    onTokenChange: (String) -> Unit,
    accountStatus: AccountStatus,
    statusMessage: String,
    statusDeadline: String?,
    isChecking: Boolean,
    onOpenAuth: () -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    onCheckStatus: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(c.surfaceContainerHigh)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(c.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Key, null, tint = c.primary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = TranslationManager.tr("section_token"),
                        fontFamily = RoundedHeavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = c.onSurface
                    )
                    Text(
                        text = "Auto-extract via QR / Web Login or paste cookie",
                        fontSize = 11.5.sp,
                        color = c.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .pressable(onClick = onOpenAuth)
                    .clip(CircleShape)
                    .background(c.primary)
                    .padding(horizontal = 13.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.QrCode2, null, tint = c.onPrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "Account Login",
                    fontFamily = RoundedHeavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = c.onPrimary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Token Input Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(c.surfaceContainerLowest)
                .border(
                    width = 1.dp,
                    color = if (token.isNotEmpty()) c.primary.copy(alpha = 0.55f) else c.onSurface.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = token,
                onValueChange = onTokenChange,
                textStyle = TextStyle(
                    color = c.onSurface,
                    fontSize = 13.sp,
                    fontFamily = MonoFont,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(c.primary),
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (token.isEmpty()) {
                        Text(
                            text = "Paste new_bbs_serviceToken or tap 'Account Login'...",
                            color = c.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 12.5.sp,
                            fontFamily = MonoFont
                        )
                    }
                    inner()
                }
            )
        }

        Spacer(Modifier.height(10.dp))

        // Paste / Clear / Check Status Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionPillSecondary(
                text = TranslationManager.tr("token_paste"),
                icon = Icons.Rounded.ContentPaste,
                onClick = onPaste,
                modifier = Modifier.weight(1f)
            )
            ActionPillSecondary(
                text = TranslationManager.tr("token_clear"),
                icon = Icons.Rounded.Clear,
                onClick = onClear,
                modifier = Modifier.weight(1f)
            )
            Row(
                modifier = Modifier
                    .weight(1.35f)
                    .height(40.dp)
                    .pressable(enabled = !isChecking, onClick = onCheckStatus)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.primary)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        color = c.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(15.dp)
                    )
                } else {
                    Icon(Icons.Rounded.VerifiedUser, null, tint = c.onPrimary, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = TranslationManager.tr("token_check_status"),
                    color = c.onPrimary,
                    fontFamily = RoundedHeavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Account Eligibility Status Banner
        val (badgeBg, badgeFg, badgeIcon) = when (accountStatus) {
            AccountStatus.READY -> Triple(Color(0xFF132A1C), Color(0xFF2ECC71), Icons.Rounded.CheckCircle)
            AccountStatus.APPROVED -> Triple(Color(0xFF12283A), Color(0xFF3EA6FF), Icons.Rounded.VerifiedUser)
            AccountStatus.BLOCKED, AccountStatus.TOO_NEW -> Triple(Color(0xFF2E2412), Color(0xFFFFB300), Icons.Rounded.ErrorOutline)
            AccountStatus.EXPIRED, AccountStatus.ERROR -> Triple(Color(0xFF2E1416), Color(0xFFFF6B6B), Icons.Rounded.ErrorOutline)
            AccountStatus.CHECKING -> Triple(c.surfaceContainerHighest, c.primary, Icons.Rounded.Sync)
            else -> Triple(c.surfaceContainerHighest, c.onSurfaceVariant, Icons.Rounded.Security)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(badgeBg)
                .border(1.dp, badgeFg.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlourishIcon(
                icon = badgeIcon,
                description = null,
                active = accountStatus == AccountStatus.READY || accountStatus == AccountStatus.APPROVED,
                kind = Flourish.Pop,
                tint = badgeFg,
                size = 20.dp
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusMessage,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = badgeFg
                )
                if (!statusDeadline.isNullOrBlank()) {
                    Text(
                        text = "Deadline / Window: $statusDeadline",
                        fontSize = 11.sp,
                        color = badgeFg.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionPillSecondary(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .height(40.dp)
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceContainerHighest)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = c.onSurface, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            color = c.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun ExecutionControlCard(
    executionMode: ExecutionMode,
    isRunning: Boolean,
    onSelectMode: (ExecutionMode) -> Unit,
    onToggleStartStop: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(c.surfaceContainerHigh)
            .padding(16.dp)
    ) {
        Text(
            text = TranslationManager.tr("modes_title"),
            fontFamily = RoundedHeavy,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = c.onSurface
        )
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                ExecutionMode.AUTO to ("Auto Midnight Sniper" to "Fires multi-timing burst at 23:59:58 CST"),
                ExecutionMode.MANUAL to ("Manual Burst" to "Send immediate unlock request now")
            ).forEach { (mode, labels) ->
                val selected = executionMode == mode
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .pressable { onSelectMode(mode) }
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) c.primary.copy(alpha = 0.16f) else c.surfaceContainerHighest)
                        .border(
                            width = 1.dp,
                            color = if (selected) c.primary else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (mode == ExecutionMode.AUTO) Icons.Rounded.Bolt else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = if (selected) c.primary else c.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = labels.first,
                            fontFamily = RoundedHeavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selected) c.primary else c.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = labels.second,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = c.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        val btnBg by animateColorAsState(
            targetValue = if (isRunning) Color(0xFFD32F2F) else c.primary,
            animationSpec = tween(220),
            label = "startStopBg"
        )
        val btnFg = if (isRunning) Color.White else c.onPrimary

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .squishOn(isRunning)
                .pressable(onClick = onToggleStartStop)
                .clip(RoundedCornerShape(16.dp))
                .background(btnBg)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Rounded.Stop else Icons.Rounded.Bolt,
                contentDescription = null,
                tint = btnFg,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isRunning) {
                    TranslationManager.tr("btn_stop_auto")
                } else if (executionMode == ExecutionMode.AUTO) {
                    TranslationManager.tr("btn_start_auto")
                } else {
                    TranslationManager.tr("btn_send_now")
                },
                fontFamily = RoundedHeavy,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.5.sp,
                color = btnFg
            )
        }
    }
}

@Composable
private fun ExecutionConsoleCard(
    logs: List<LogEntry>,
    onCopyLogs: () -> Unit,
    onClearLogs: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(c.surfaceContainerHigh)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Terminal, null, tint = c.primary, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = TranslationManager.tr("log_title"),
                    fontFamily = RoundedHeavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = c.onSurface
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallActionChip(
                    label = TranslationManager.tr("log_copy"),
                    icon = Icons.Rounded.ContentCopy,
                    onClick = onCopyLogs
                )
                SmallActionChip(
                    label = TranslationManager.tr("log_clear"),
                    icon = Icons.Rounded.DeleteOutline,
                    onClick = onClearLogs
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF050505))
                .border(1.dp, c.onSurface.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            if (logs.isEmpty()) {
                Text(
                    text = TranslationManager.tr("log_empty"),
                    fontFamily = MonoFont,
                    fontSize = 12.sp,
                    color = Color(0xFF707078),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(logs, key = { it.id }) { entry ->
                        val lineColor = when (entry.colorType) {
                            LogColorType.GREEN -> Color(0xFF2ECC71)
                            LogColorType.CYAN -> Color(0xFF00E5FF)
                            LogColorType.YELLOW -> Color(0xFFFFD600)
                            LogColorType.RED -> Color(0xFFFF5252)
                            LogColorType.PURPLE -> Color(0xFFE040FB)
                            LogColorType.DEFAULT -> Color(0xFFE0E0E6)
                        }
                        Text(
                            text = "[${entry.timestamp}] ${entry.text}",
                            fontFamily = MonoFont,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp,
                            color = lineColor
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 2: FASTBOOT USB OTG & XIAOMI ENCRYPTDATA(TOKEN) GENERATOR
// ============================================================================

@Composable
fun FastbootScreen(
    viewModel: MainViewModel,
    contentPadding: PaddingValues,
    onOpenAuth: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val fbState by viewModel.fastbootDeviceState.collectAsState()
    val fbDeviceText by viewModel.fastbootDeviceInfoText.collectAsState()
    val fbProduct by viewModel.fastbootProduct.collectAsState()
    val fbToken by viewModel.fastbootToken.collectAsState()

    val unlockZone by viewModel.unlockZone.collectAsState()
    val manualProduct by viewModel.manualProduct.collectAsState()
    val manualDeviceToken by viewModel.manualDeviceToken.collectAsState()
    val userId by viewModel.userId.collectAsState()
    val ssecurity by viewModel.ssecurity.collectAsState()
    val deviceId by viewModel.deviceId.collectAsState()
    val encryptDataHex by viewModel.encryptDataHex.collectAsState()
    val isRequestingEncryptData by viewModel.isRequestingEncryptData.collectAsState()
    val clearNotice by viewModel.clearWarningNotice.collectAsState()

    CollapsingHeaderLayout(
        header = {
            ScreenHeader(
                title = "Fastboot & Token Studio",
                subtitle = "USB OTG Fastboot + ahaUnlock encryptData(token)",
                brandMark = true,
                actions = {
                    CircleAction(
                        icon = Icons.Rounded.Usb,
                        contentDescription = "Scan USB OTG",
                        onClick = { viewModel.checkAndConnectFastboot(context) },
                        activeBadge = fbState == FastbootConnectionState.CONNECTED
                    )
                }
            )
        }
    ) { topPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = topPadding + 6.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. USB OTG Fastboot Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(c.surfaceContainerHigh)
                    .border(1.dp, c.primary.copy(alpha = 0.22f), RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Usb, null, tint = c.primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Fastboot (USB OTG)",
                                fontFamily = RoundedHeavy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                                color = c.onSurface
                            )
                            Text(
                                text = fbDeviceText,
                                fontSize = 11.5.sp,
                                color = c.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val (statusText, statusColor) = when (fbState) {
                        FastbootConnectionState.CONNECTED -> "CONNECTED" to Color(0xFF2ECC71)
                        FastbootConnectionState.PERMISSION_REQUIRED, FastbootConnectionState.DEVICE_ATTACHED -> "PERMISSION" to Color(0xFFFFB300)
                        FastbootConnectionState.CONNECTING -> "CONNECTING" to c.primary
                        FastbootConnectionState.ERROR -> "ERROR" to Color(0xFFFF5252)
                        else -> "DISCONNECTED" to c.onSurfaceVariant
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(statusColor.copy(alpha = 0.16f))
                            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusText,
                            fontFamily = MonoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            color = statusColor
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricMiniTile(
                        modifier = Modifier.weight(1f),
                        title = "Product Codename",
                        value = fbProduct ?: "--",
                        subtitle = "getvar:product",
                        accent = c.primary
                    )
                    MetricMiniTile(
                        modifier = Modifier.weight(1f),
                        title = "Bootloader Token",
                        value = fbToken?.let { "${it.take(10)}…" } ?: "--",
                        subtitle = if (fbToken != null) "${fbToken!!.length} hex chars" else "oem get_token",
                        accent = Color(0xFF00E5FF)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PillButton(
                        text = "Connect OTG",
                        icon = Icons.Rounded.Usb,
                        onClick = { viewModel.checkAndConnectFastboot(context) },
                        modifier = Modifier.weight(1f)
                    )
                    ActionPillSecondary(
                        text = "Get Product",
                        icon = Icons.Rounded.Memory,
                        onClick = { viewModel.fetchFastbootProduct() },
                        modifier = Modifier.weight(1f)
                    )
                    ActionPillSecondary(
                        text = "Get Token",
                        icon = Icons.Rounded.Key,
                        onClick = { viewModel.fetchFastbootToken() },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionPillSecondary(
                        text = "Reboot System",
                        icon = Icons.Rounded.RestartAlt,
                        onClick = { viewModel.rebootFastbootDevice("") },
                        modifier = Modifier.weight(1f)
                    )
                    ActionPillSecondary(
                        text = "Bootloader",
                        icon = Icons.Rounded.DeveloperMode,
                        onClick = { viewModel.rebootFastbootDevice("bootloader") },
                        modifier = Modifier.weight(1f)
                    )
                    ActionPillSecondary(
                        text = "Disconnect",
                        icon = Icons.Rounded.Close,
                        onClick = { viewModel.disconnectFastboot() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Xiaomi ahaUnlock encryptData(token) Generator Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(c.surfaceContainerHigh)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.LockOpen, null, tint = c.primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "encryptData(token) Generator",
                                fontFamily = RoundedHeavy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                                color = c.onSurface
                            )
                            Text(
                                text = "AES-CBC + HMAC-SHA1 /api/v3/ahaUnlock client",
                                fontSize = 11.5.sp,
                                color = c.onSurfaceVariant
                            )
                        }
                    }

                    SmallActionChip(
                        label = "Sync Auth",
                        icon = Icons.Rounded.QrCode2,
                        onClick = onOpenAuth
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Server Region Zone",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiUnlockApiClient.DOMAINS.keys.forEach { zone ->
                        val selected = unlockZone.equals(zone, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .pressable { viewModel.updateUnlockZone(zone) }
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) c.primary else c.surfaceContainerHighest)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = zone,
                                fontFamily = RoundedHeavy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (selected) c.onPrimary else c.onSurface
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                LabeledMonoField(
                    label = "Product Codename (e.g. marble, redwood, sweet)",
                    value = manualProduct,
                    placeholder = "Auto-filled from OTG or type codename...",
                    onValueChange = viewModel::updateManualProduct
                )

                Spacer(Modifier.height(8.dp))

                LabeledMonoField(
                    label = "Fastboot Device Token (from 'fastboot oem get_token')",
                    value = manualDeviceToken,
                    placeholder = "Paste device unlock token hex...",
                    onValueChange = viewModel::updateManualDeviceToken,
                    maxLines = 2
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LabeledMonoField(
                        modifier = Modifier.weight(1f),
                        label = "Xiaomi User ID",
                        value = userId,
                        placeholder = "userId",
                        onValueChange = viewModel::updateUserId
                    )
                    LabeledMonoField(
                        modifier = Modifier.weight(1.4f),
                        label = "Session ssecurity (Base64)",
                        value = ssecurity,
                        placeholder = "ssecurity key",
                        onValueChange = viewModel::updateSsecurity
                    )
                }

                clearNotice?.let { notice ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = notice,
                        fontSize = 11.5.sp,
                        color = c.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .pressable(enabled = !isRequestingEncryptData, onClick = { viewModel.requestEncryptDataToken() })
                        .clip(RoundedCornerShape(15.dp))
                        .background(c.primary)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isRequestingEncryptData) {
                        CircularProgressIndicator(
                            color = c.onPrimary,
                            strokeWidth = 2.2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Rounded.Key, null, tint = c.onPrimary, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = "Retrieve encryptData(token)",
                        fontFamily = RoundedHeavy,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp,
                        color = c.onPrimary
                    )
                }

                if (!encryptDataHex.isNullOrEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF050505))
                            .border(1.dp, Color(0xFF2ECC71).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Generated encryptData (${encryptDataHex!!.length / 2} bytes)",
                                fontFamily = RoundedHeavy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF2ECC71)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = encryptDataHex.orEmpty(),
                                fontFamily = MonoFont,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = Color(0xFFE0E0E6),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionPillSecondary(
                            text = "Copy Hex",
                            icon = Icons.Rounded.ContentCopy,
                            onClick = {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cb.setPrimaryClip(ClipData.newPlainText("encryptData", encryptDataHex))
                                Toast.makeText(context, "encryptData hex copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ActionPillSecondary(
                            text = "Save token.bin",
                            icon = Icons.Rounded.SaveAlt,
                            onClick = {
                                viewModel.saveEncryptDataToBin(context) { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ActionPillSecondary(
                            text = "OTG Unlock",
                            icon = Icons.Rounded.LockOpen,
                            onClick = { viewModel.stageAndOemUnlockViaOtg() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledMonoField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    maxLines: Int = 1,
) {
    val c = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.surfaceContainerLowest)
                .border(1.dp, c.onSurface.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = c.onSurface,
                    fontSize = 12.5.sp,
                    fontFamily = MonoFont,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(c.primary),
                maxLines = maxLines,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = c.onSurfaceVariant.copy(alpha = 0.45f),
                            fontSize = 12.sp,
                            fontFamily = MonoFont
                        )
                    }
                    inner()
                }
            )
        }
    }
}

// ============================================================================
// TAB 3: HYPEROS & FASTBOOT UNLOCK GUIDE
// ============================================================================

@Composable
fun GuideScreen(
    contentPadding: PaddingValues,
) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val steps = listOf(
        Triple(
            TranslationManager.tr("guide_step1_title"),
            TranslationManager.tr("guide_step1_body"),
            "Tip: You can also use the built-in QR / Web Login button inside the Apply tab to extract new_bbs_serviceToken automatically."
        ),
        Triple(
            TranslationManager.tr("guide_step2_title"),
            TranslationManager.tr("guide_step2_body"),
            "Sniper Mode automatically synchronizes NTP at 23:59:00 CST and measures HTTP/TCP latency at 23:59:40 CST."
        ),
        Triple(
            TranslationManager.tr("guide_step3_title"),
            TranslationManager.tr("guide_step3_body"),
            "Important: Always turn OFF Wi-Fi and use SIM Mobile Data when tapping 'Add account and device'."
        ),
        Triple(
            "4. USB OTG Fastboot & encryptData(token) Unlock",
            "• Boot your target Xiaomi device into Fastboot mode (Volume Down + Power).\n• Connect it to this phone via a USB OTG cable and open the Fastboot tab.\n• Tap 'Connect OTG', read Product & Token, generate encryptData(token), and tap 'OTG Unlock' (or save token.bin for PC/Termux).",
            "fastboot stage token.bin && fastboot oem unlock"
        ),
        Triple(
            TranslationManager.tr("guide_step4_title"),
            TranslationManager.tr("guide_step4_body"),
            "Code 100003 means your request hit the queue window — check Developer Options binding immediately!"
        )
    )

    CollapsingHeaderLayout(
        header = {
            ScreenHeader(
                title = TranslationManager.tr("guide_title"),
                subtitle = "Complete HyperOS Quota Sniper & Fastboot Unlock Manual",
                brandMark = true
            )
        }
    ) { topPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = topPadding + 6.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            steps.forEachIndexed { idx, (title, body, tip) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(c.surfaceContainerHigh)
                        .border(1.dp, c.primary.copy(alpha = if (idx == 0) 0.32f else 0.12f), RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = title,
                        fontFamily = RoundedHeavy,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.5.sp,
                        color = c.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = body,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = c.onSurface
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.surfaceContainerLowest)
                            .clickable {
                                if (tip.startsWith("fastboot")) {
                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cb.setPrimaryClip(ClipData.newPlainText("fastboot", tip))
                                    Toast.makeText(context, "Command copied!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tip,
                            fontFamily = if (tip.startsWith("fastboot")) MonoFont else FontFamily.Default,
                            fontSize = 11.5.sp,
                            color = c.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (tip.startsWith("fastboot")) {
                            Icon(Icons.Rounded.ContentCopy, null, tint = c.primary, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// XIAOMI ACCOUNT AUTH MODAL (QR CODE + EMBEDDED WEBVIEW COOKIE EXTRACTOR)
// ============================================================================

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun XiaomiAuthModal(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCredentialsExtracted: (XiaomiSessionCredentials) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var authTab by remember { mutableIntStateOf(0) } // 0 = QR Code, 1 = WebView Login
    val authManager = remember { XiaomiAuthManager() }
    var session by remember { mutableStateOf<AuthSessionInfo?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var statusText by remember { mutableStateOf(TranslationManager.tr("login_loading_qr")) }
    var isLoadingQr by remember { mutableStateOf(true) }

    fun loadQrSession() {
        isLoadingQr = true
        qrBitmap = null
        statusText = TranslationManager.tr("login_loading_qr")
        scope.launch {
            val res = authManager.initiateAuth()
            if (res.isSuccess) {
                val info = res.getOrThrow()
                session = info
                val bmp = authManager.fetchQrBitmap(info.qrUrl)
                qrBitmap = bmp
                isLoadingQr = false
                statusText = if (bmp != null) TranslationManager.tr("login_waiting") else "Failed to load QR image"
            } else {
                isLoadingQr = false
                statusText = TranslationManager.tr("login_qr_fetch_error", res.exceptionOrNull()?.message ?: "Error")
            }
        }
    }

    LaunchedEffect(visible, authTab) {
        if (visible && authTab == 0) {
            loadQrSession()
        }
    }

    LaunchedEffect(visible, session, authTab) {
        val currentSession = session ?: return@LaunchedEffect
        if (!visible || authTab != 0) return@LaunchedEffect
        var retries = 0
        while (isActive && retries < 100) {
            when (val poll = authManager.pollOnce(currentSession.lpUrl)) {
                is AuthPollResult.Success -> {
                    statusText = TranslationManager.tr("login_success")
                    onCredentialsExtracted(poll.credentials)
                    onDismiss()
                    return@LaunchedEffect
                }
                is AuthPollResult.Status -> {
                    retries++
                }
                is AuthPollResult.Error -> {
                    retries++
                    delay(1800)
                }
                is AuthPollResult.Expired -> {
                    statusText = TranslationManager.tr("login_qr_expired")
                    return@LaunchedEffect
                }
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(spring(dampingRatio = 0.82f, stiffness = 420f), initialScale = 0.90f) + fadeIn(tween(180)),
        exit = scaleOut(tween(130), targetScale = 0.92f) + fadeOut(tween(130)),
    ) {
        BackHandler(onBack = onDismiss)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 24.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(c.surfaceContainerHighest)
                    .border(1.dp, c.primary.copy(alpha = 0.30f), RoundedCornerShape(26.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(c.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Security, null, tint = c.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = TranslationManager.tr("login_title"),
                            fontFamily = RoundedHeavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = c.onSurface
                        )
                        Text(
                            text = TranslationManager.tr("login_subtitle"),
                            fontSize = 11.5.sp,
                            color = c.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(c.surfaceContainerHigh)
                            .pressable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Close, "Close", tint = c.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Mode Switcher: QR Code vs Embedded WebView
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.surfaceContainerHigh)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0 to "QR Code Scan", 1 to "In-App Web Login").forEach { (idx, label) ->
                        val active = authTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .pressable { authTab = idx }
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (active) c.primary else Color.Transparent)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontFamily = RoundedHeavy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (active) c.onPrimary else c.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (authTab == 0) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingQr) {
                            CircularProgressIndicator(color = Color.Black, strokeWidth = 3.dp)
                        } else if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap!!.asImageBitmap(),
                                contentDescription = "Xiaomi Auth QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Rounded.QrCode2, null, tint = Color.Black, modifier = Modifier.size(64.dp))
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = statusText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = c.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = TranslationManager.tr("login_qr_scan_tip"),
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        color = c.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionPillSecondary(
                            text = "Refresh QR",
                            icon = Icons.Rounded.Refresh,
                            onClick = { loadQrSession() },
                            modifier = Modifier.weight(1f)
                        )
                        PillButton(
                            text = "Open Browser",
                            icon = Icons.Rounded.OpenInBrowser,
                            onClick = {
                                session?.loginUrl?.takeIf { it.isNotEmpty() }?.let { url ->
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                }
                            },
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                } else {
                    // Embedded WebView Login that auto-captures new_bbs_serviceToken & cookies
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, c.onSurface.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                val cm = CookieManager.getInstance()
                                cm.setAcceptCookie(true)
                                WebView(ctx).apply {
                                    cm.setAcceptThirdPartyCookies(this, true)
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.userAgentString = WebSettings.getDefaultUserAgent(ctx).replace("; wv", "")

                                    fun inspectCookies(url: String) {
                                        val c1 = cm.getCookie("https://c.mi.com").orEmpty()
                                        val c2 = cm.getCookie("https://sgp-api.buy.mi.com").orEmpty()
                                        val c3 = cm.getCookie("https://account.xiaomi.com").orEmpty()
                                        val c4 = if (url.isNotEmpty()) cm.getCookie(url).orEmpty() else ""
                                        val merged = listOf(c1, c2, c3, c4).filter { it.isNotBlank() }.joinToString("; ")

                                        val bbs = Regex("""new_bbs_serviceToken=([^;\s]+)""").find(merged)?.groupValues?.get(1)
                                            ?: Regex("""serviceToken=([^;\s]+)""").find(merged)?.groupValues?.get(1)
                                        if (!bbs.isNullOrBlank()) {
                                            val uid = Regex("""userId=([^;\s]+)""").find(merged)?.groupValues?.get(1).orEmpty()
                                            val pass = Regex("""passToken=([^;\s]+)""").find(merged)?.groupValues?.get(1).orEmpty()
                                            val dev = Regex("""deviceId=([^;\s]+)""").find(merged)?.groupValues?.get(1).orEmpty()
                                            cm.flush()
                                            onCredentialsExtracted(
                                                XiaomiSessionCredentials(
                                                    token = bbs,
                                                    userId = uid,
                                                    passToken = pass,
                                                    deviceId = dev
                                                )
                                            )
                                            onDismiss()
                                        }
                                    }

                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView, url: String) {
                                            inspectCookies(url)
                                        }
                                    }
                                    loadUrl("https://account.xiaomi.com/pass/serviceLogin?sid=18n_bbs_global&_locale=en_US")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ApplyResultModal(
    result: ApplyResult?,
    onDismiss: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    AnimatedVisibility(
        visible = result != null,
        enter = scaleIn(spring(dampingRatio = 0.8f, stiffness = 400f), initialScale = 0.88f) + fadeIn(tween(160)),
        exit = scaleOut(tween(120), targetScale = 0.92f) + fadeOut(tween(120)),
    ) {
        val r = result ?: return@AnimatedVisibility
        val isSuccess = r.isSuccess || (r.code == 0 && r.applyResult == 3)
        val title = when {
            r.isSuccess -> TranslationManager.tr("dialog_success_title")
            r.isStatusUnavailable -> TranslationManager.tr("dialog_status_unavailable_title")
            isSuccess -> TranslationManager.tr("dialog_quota_title")
            else -> TranslationManager.tr("dialog_error_title")
        }
        val body = when {
            r.isSuccess -> TranslationManager.tr("dialog_success_msg")
            r.isStatusUnavailable -> TranslationManager.tr("dialog_status_unavailable_msg")
            isSuccess -> TranslationManager.tr("dialog_quota_msg")
            r.code == 100004 || r.code == 10004 -> TranslationManager.tr("status_expired")
            else -> r.message
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(c.surfaceContainerHighest)
                    .border(1.dp, c.primary.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isSuccess) Color(0xFF132818) else c.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Rounded.Check else Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = if (isSuccess) Color(0xFF2ECC71) else c.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = title,
                    fontFamily = RoundedHeavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.5.sp,
                    color = c.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = body,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = c.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(18.dp))
                PillButton(
                    text = TranslationManager.tr("dialog_ok"),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
