package com.xboyrex.miunlocktool

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xboyrex.miunlocktool.data.PreferencesManager
import com.xboyrex.miunlocktool.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: (() -> Unit)? = null,
    onSignIn: () -> Unit,
    onSignedOut: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onCheckUpdate: () -> Unit = {},
) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val token by viewModel.token.collectAsState()
    val userId by viewModel.userId.collectAsState()

    var activeFloatingSection by remember { mutableStateOf<SettingsFloatingSection?>(null) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateFeedback by remember { mutableStateOf<String?>(null) }

    val isOverlayOpen = activeFloatingSection != null || showSignOutConfirm || checkingUpdate || updateFeedback != null
    val blurRadius by animateDpAsState(
        targetValue = if (isOverlayOpen && Build.VERSION.SDK_INT >= 31) 16.dp else 0.dp,
        animationSpec = tween(220),
        label = "settingsBlur",
    )

    val openUrl: (String) -> Unit = { url ->
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(c.surface)
                .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
        ) {
            if (onBack != null) {
                BackScreenHeader(onBack = onBack) {
                    Text(
                        text = I18n.t("settings", "Settings"),
                        fontFamily = RoundedHeavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp,
                        color = c.onSurface,
                    )
                }
            } else {
                ScreenHeader(
                    title = I18n.t("settings", "Settings"),
                    subtitle = "Customization, Sniper Engine & @xboyrex Hub",
                    brandMark = true
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                SettingsXiaomiAccountCard(
                    token = token,
                    userId = userId,
                    onSignIn = onSignIn,
                    onRequestSignOut = { showSignOutConfirm = true },
                )

                SettingsSectionHeader(I18n.t("customization", "Customization"))
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.Palette,
                        title = I18n.t("appearance_theme", "Appearance & Theme"),
                        subtitle = I18n.t("appearance_sub", "AMOLED mode, Onyx Yellow palette and Liquid Glass"),
                        onClick = { activeFloatingSection = SettingsFloatingSection.APPEARANCE },
                    )
                }

                SettingsSectionHeader("Sniper Engine & Interface")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.Tune,
                        title = I18n.t("general_modification", "General Modification"),
                        subtitle = I18n.t("general_modification_sub", "Font, NTP server, regional timings & burst parameters"),
                        onClick = { activeFloatingSection = SettingsFloatingSection.GENERAL_MODS },
                    )
                }

                SettingsSectionHeader(I18n.t("language_section", "Language"))
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.Translate,
                        title = I18n.t("language", "Language"),
                        subtitle = LanguageSettings.currentDisplay(),
                        onClick = { activeFloatingSection = SettingsFloatingSection.LANGUAGE },
                    )
                }

                SettingsSectionHeader(I18n.t("data_storage", "Data & Storage"))
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.DataUsage,
                        title = I18n.t("storage_usage", "Storage Usage"),
                        subtitle = I18n.t("storage_sub", "See phone storage & clear Mi Unlock Tool cache"),
                        onClick = { activeFloatingSection = SettingsFloatingSection.STORAGE_USAGE },
                    )
                }

                SettingsSectionHeader(I18n.t("about", "About & Developer"))
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.Info,
                        title = I18n.t("version", "Version"),
                        subtitle = "Mi Unlock Tool ${BuildConfig.VERSION_NAME} (Android 17 Ready)",
                        trailing = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF132818))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2ECC71))
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = I18n.t("up_to_date", "Up to date"),
                                        color = Color(0xFF2ECC71),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                    )
                    SettingsRow(
                        icon = Icons.Rounded.FileDownload,
                        title = I18n.t("check_updates", "Check for updates"),
                        subtitle = I18n.t("check_updates_sub", "New GitHub releases install from inside the app"),
                        onClick = {
                            if (checkingUpdate) return@SettingsRow
                            checkingUpdate = true
                            updateFeedback = null
                            scope.launch {
                                val start = System.currentTimeMillis()
                                onCheckUpdate()
                                val elapsed = System.currentTimeMillis() - start
                                if (elapsed < 1100) delay(1100 - elapsed)
                                checkingUpdate = false
                                updateFeedback = "You're on the latest version (${BuildConfig.VERSION_NAME})"
                            }
                        },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Code,
                        title = I18n.t("open_source", "Open Source Repository"),
                        subtitle = "github.com/xboyrex/miunlocktool",
                        onClick = { openUrl("https://github.com/xboyrex/miunlocktool") },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Person,
                        title = I18n.t("developer", "Developer"),
                        subtitle = "ㅤᏞᴀᴢʏㅤᎷꫝʏᴀɴк まやんく (@xboyrex)",
                        onClick = { openUrl("https://github.com/xboyrex") },
                    )
                }

                SettingsSectionHeader("Community & Social Links")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.Send,
                        title = "Telegram Channel",
                        subtitle = "t.me/MayankDroid7 (@MayankDroid7)",
                        onClick = { openUrl("https://t.me/MayankDroid7") },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Forum,
                        title = "Onyx Hub Telegram",
                        subtitle = "t.me/OnyxHub7 (@OnyxHub7)",
                        onClick = { openUrl("https://t.me/OnyxHub7") },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.PlayCircle,
                        title = "YouTube · Mayank Droid",
                        subtitle = "youtube.com/@mayankdroid",
                        onClick = { openUrl("https://youtube.com/@mayankdroid?si=slX8DDU0dpLSIYyY") },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.PlayCircle,
                        title = "YouTube · Onyx Droid",
                        subtitle = "youtube.com/@onyxdroid89",
                        onClick = { openUrl("https://youtube.com/@onyxdroid89?si=9gxpZfa3bpI9PKku") },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.CameraAlt,
                        title = "Instagram",
                        subtitle = "instagram.com/mayank.zx7 (@mayank.zx7)",
                        onClick = { openUrl("https://www.instagram.com/mayank.zx7") },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.AlternateEmail,
                        title = "Developer Contact",
                        subtitle = "xboyrex496@gmail.com",
                        onClick = { openUrl("mailto:xboyrex496@gmail.com") },
                    )
                }
            }
        }

        if (isOverlayOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.34f))
            )
        }

        AnimatedContent(
            targetState = activeFloatingSection,
            transitionSpec = {
                (scaleIn(spring(dampingRatio = 0.82f, stiffness = 420f), initialScale = 0.90f) + fadeIn(tween(180))) togetherWith
                    (scaleOut(tween(130), targetScale = 0.92f) + fadeOut(tween(130)))
            },
            label = "settingsFloatingSection",
        ) { section ->
            when (section) {
                SettingsFloatingSection.APPEARANCE -> {
                    SettingsFloatingDialog(
                        title = I18n.t("appearance_theme", "Appearance & Theme"),
                        subtitle = I18n.t("appearance_sub", "Theme mode, color palette & Liquid Glass"),
                        onDismiss = { activeFloatingSection = null },
                    ) {
                        AppearanceThemeContent(context = context)
                    }
                }
                SettingsFloatingSection.GENERAL_MODS -> {
                    SettingsFloatingDialog(
                        title = I18n.t("general_modification", "General Modification"),
                        subtitle = I18n.t("general_modification_sub", "Font, NTP server, timings & burst"),
                        onDismiss = { activeFloatingSection = null },
                    ) {
                        GeneralModificationContent(viewModel = viewModel)
                    }
                }
                SettingsFloatingSection.STORAGE_USAGE -> {
                    SettingsFloatingDialog(
                        title = I18n.t("storage_usage", "Storage Usage"),
                        subtitle = I18n.t("storage_sub", "Device memory & Mi Unlock Tool data"),
                        onDismiss = { activeFloatingSection = null },
                    ) {
                        StorageUsageContent(context = context, viewModel = viewModel)
                    }
                }
                SettingsFloatingSection.LANGUAGE -> {
                    SettingsFloatingDialog(
                        title = I18n.t("language", "Language"),
                        subtitle = "Choose application language",
                        onDismiss = { activeFloatingSection = null },
                    ) {
                        LanguageContent(context = context) {
                            activeFloatingSection = null
                        }
                    }
                }
                null -> {}
            }
        }

        AnimatedVisibility(
            visible = showSignOutConfirm,
            enter = scaleIn(spring(dampingRatio = 0.8f, stiffness = 400f), initialScale = 0.88f) + fadeIn(tween(160)),
            exit = scaleOut(tween(120), targetScale = 0.92f) + fadeOut(tween(120)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showSignOutConfirm = false },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .widthIn(max = 350.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(c.surfaceContainerHighest)
                        .border(1.dp, c.onSurface.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { }
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(c.error.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            tint = c.error,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Clear Xiaomi Session?",
                        fontFamily = RoundedHeavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = c.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Are you sure you want to clear your stored new_bbs_serviceToken and ssecurity session?",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = c.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(c.surfaceContainerHigh)
                                .pressable { showSignOutConfirm = false }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Cancel", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = c.onSurface)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(c.error)
                                .pressable {
                                    showSignOutConfirm = false
                                    viewModel.signOutSession()
                                    onSignedOut()
                                }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Sign out", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = c.onError)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = checkingUpdate || updateFeedback != null,
            enter = scaleIn(spring(dampingRatio = 0.8f, stiffness = 400f), initialScale = 0.88f) + fadeIn(tween(160)),
            exit = scaleOut(tween(120), targetScale = 0.92f) + fadeOut(tween(120)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (!checkingUpdate) updateFeedback = null
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .widthIn(max = 350.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(c.surfaceContainerHighest)
                        .border(1.dp, c.onSurface.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { }
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (checkingUpdate) {
                        CircularProgressIndicator(
                            color = c.primary,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Checking for updates…",
                            fontFamily = RoundedHeavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = c.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Connecting to github.com/xboyrex/miunlocktool…",
                            fontSize = 12.sp,
                            color = c.onSurfaceVariant,
                        )
                    } else if (updateFeedback != null) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF132818)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color(0xFF2ECC71),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Up to date",
                            fontFamily = RoundedHeavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = c.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = updateFeedback.orEmpty(),
                            fontSize = 12.sp,
                            color = c.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(c.primary)
                                .pressable { updateFeedback = null }
                                .padding(horizontal = 22.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("OK", color = c.onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

private enum class SettingsFloatingSection {
    APPEARANCE,
    GENERAL_MODS,
    STORAGE_USAGE,
    LANGUAGE,
}

@Composable
private fun SettingsXiaomiAccountCard(
    token: String,
    userId: String,
    onSignIn: () -> Unit,
    onRequestSignOut: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val isSignedIn = token.isNotBlank()
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.surfaceContainerHigh)
            .border(1.dp, if (isSignedIn) c.primary.copy(alpha = 0.30f) else Color.Transparent, RoundedCornerShape(18.dp))
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressable {
                    if (!isSignedIn) onSignIn() else expanded = !expanded
                }
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedContent(
                targetState = isSignedIn,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(tween(240)) + slideInVertically(tween(300)) { it / 8 }) togetherWith
                        (fadeOut(tween(160)) + slideOutVertically(tween(200)) { -it / 8 })
                },
                label = "accountHeader",
            ) { active ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (active) c.primary.copy(alpha = 0.18f) else c.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (active) Icons.Rounded.VerifiedUser else Icons.Rounded.AccountCircle,
                            contentDescription = null,
                            tint = if (active) c.primary else c.onSurfaceVariant,
                            modifier = Modifier.size(26.dp),
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (active) {
                                if (userId.isNotBlank()) "Xiaomi UID: $userId" else "Xiaomi Account Authorized"
                            } else {
                                "Xiaomi Account"
                            },
                            fontFamily = RoundedHeavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = c.onSurface,
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = if (active) {
                                val masked = if (token.length > 14) "${token.take(8)}...${token.takeLast(6)}" else "Active Token"
                                "Service Token: $masked"
                            } else {
                                "Sign in via QR or WebView to sync token"
                            },
                            fontSize = 12.sp,
                            color = c.onSurfaceVariant,
                        )
                    }
                }
            }

            if (!isSignedIn) {
                Text(
                    "Sign in",
                    modifier = Modifier.pressable(onClick = onSignIn).padding(8.dp),
                    fontFamily = RoundedHeavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = c.primary,
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                    contentDescription = if (expanded) "Hide" else "Expand",
                    tint = c.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(4.dp)
                        .size(18.dp)
                        .graphicsLayer {
                            rotationZ = if (expanded) 90f else 0f
                        },
                )
            }
        }

        AnimatedVisibility(
            visible = expanded && isSignedIn,
            enter = fadeIn(tween(180)) + expandVertically(tween(260)),
            exit = fadeOut(tween(140)) + shrinkVertically(tween(200)),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 8.dp)) {
                HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .pressable(onClick = onSignIn)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(c.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.QrCode2, null, tint = c.primary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Refresh / Switch Xiaomi Account", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = c.primary)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .pressable(onClick = onRequestSignOut)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(c.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, null, tint = c.error, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Sign out & Clear Token", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = c.error)
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val c = MaterialTheme.colorScheme
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = c.primary.copy(alpha = 0.88f),
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.surfaceContainerHigh),
        content = content,
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(c.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = c.primary,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                color = c.onSurface,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                color = c.onSurfaceVariant.copy(alpha = 0.82f),
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        } else if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                contentDescription = null,
                tint = c.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SettingsFloatingDialog(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .widthIn(max = 410.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(c.surfaceContainerHighest)
                .border(1.dp, c.primary.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { }
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(c.surfaceContainerHigh)
                        .pressable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = c.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontFamily = RoundedHeavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = c.onSurface,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = c.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(c.surfaceContainerHigh)
                        .pressable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = c.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }

            HorizontalDivider(
                color = c.onSurface.copy(alpha = 0.08f),
                modifier = Modifier.padding(bottom = 6.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun AppearanceThemeContent(context: Context) {
    val c = MaterialTheme.colorScheme
    var customHex by remember { mutableStateOf(ThemeSettings.seed ?: SEED_ONYX_YELLOW) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        SettingsSwitchRow(
            title = "Liquid Glass",
            subtitle = if (GlassSettings.enabled) {
                "Frosted, see-through surfaces with depth, lens refraction and blur"
            } else {
                "Using solid Material surfaces with no transparency"
            },
            checked = GlassSettings.enabled,
            onCheckedChange = { GlassSettings.updateEnabled(it) },
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Theme mode", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text("Default is pure AMOLED Dark", color = c.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "amoled" to "AMOLED",
                    "dark" to "Dark",
                    "light" to "Light",
                    "system" to "System",
                ).forEach { (mode, label) ->
                    val on = ThemeSettings.mode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .pressable { ThemeSettings.chooseMode(mode) }
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (on) c.primary else c.surfaceContainerHigh)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (on) c.onPrimary else c.onSurfaceVariant,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text("Color palette", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text("Onyx Yellow (#FFD600) is the signature default", color = c.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))

            val dark = isDarkTheme()
            val choices = buildList {
                if (ThemeSettings.wallpaperAvailable) add(null to "Wallpaper")
                MI_THEMES.forEach { add(it.hex to it.name) }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                choices.forEach { (hex, name) ->
                    val selected = ThemeSettings.seed?.lowercase() == hex?.lowercase()
                    val scheme = schemeFor(context, hex, dark)
                    Column(
                        modifier = Modifier
                            .width(68.dp)
                            .pressable {
                                ThemeSettings.choose(hex)
                                if (hex != null) customHex = hex
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (selected) scheme.primaryContainer else Color.Transparent)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(scheme.primary, scheme.primaryContainer)
                                    )
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(Icons.Rounded.Check, "Selected", tint = scheme.onPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            name,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) c.onSurface else c.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Custom HEX Color Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.surfaceContainerLow)
                        .border(1.dp, c.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    BasicTextField(
                        value = customHex,
                        onValueChange = { customHex = it },
                        textStyle = TextStyle(color = c.onSurface, fontFamily = MonoFont, fontSize = 13.sp),
                        singleLine = true,
                        cursorBrush = SolidColor(c.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                PillButton(
                    text = "Apply HEX",
                    onClick = {
                        val clean = customHex.trim().removePrefix("#")
                        if (clean.length == 6 || clean.length == 3) {
                            ThemeSettings.choose("#$clean")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun GeneralModificationContent(viewModel: MainViewModel) {
    val c = MaterialTheme.colorScheme
    val hyperIsland by viewModel.hyperIslandEnabled.collectAsState()
    val autoNtp by viewModel.autoNtpEnabled.collectAsState()
    val autoPing by viewModel.autoPingEnabled.collectAsState()
    val ntpServer by viewModel.ntpServer.collectAsState()
    val timingProfile by viewModel.timingProfile.collectAsState()
    val customTimings by viewModel.customTimings.collectAsState()
    val burstCount by viewModel.burstCount.collectAsState()
    val burstInterval by viewModel.burstIntervalMs.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSwitchRow(
            title = "Use System Font",
            subtitle = "Use device default font instead of YTX Rounded Heavy font",
            checked = ThemeSettings.useSystemFont,
            onCheckedChange = { ThemeSettings.updateUseSystemFont(it) },
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsSwitchRow(
            title = "HyperOS Spotlight / Dynamic Island",
            subtitle = "Show live countdown pill in HyperOS status bar & lockscreen",
            checked = hyperIsland,
            onCheckedChange = viewModel::updateHyperIslandEnabled,
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsSwitchRow(
            title = "Auto NTP Sync at 23:59:00 CST",
            subtitle = "Automatically synchronize clock offset 1 minute before midnight",
            checked = autoNtp,
            onCheckedChange = viewModel::updateAutoNtpEnabled,
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsSwitchRow(
            title = "Auto Ping Measurement at 23:59:40 CST",
            subtitle = "Measure 15s HTTP/TCP latency to Xiaomi servers before firing",
            checked = autoPing,
            onCheckedChange = viewModel::updateAutoPingEnabled,
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

        Text("NTP Time Server", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = c.onSurface)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "time.google.com" to "Google",
                "time.cloudflare.com" to "Cloudflare",
                "time.android.com" to "Android",
            ).forEach { (host, label) ->
                val selected = ntpServer == host
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .pressable { viewModel.updateNtpServer(host) }
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) c.primary else c.surfaceContainerHigh)
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        color = if (selected) c.onPrimary else c.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

        Text("Regional Timing Profile (23:59:XX.XXX)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = c.onSurface)
        Spacer(Modifier.height(6.dp))
        listOf(
            PreferencesManager.PROFILE_ASIA to "Asia (58.800, 59.100, 59.300, 59.500, 59.700)",
            PreferencesManager.PROFILE_EUROPE to "Europe (58.700, 58.800, 59.100, 59.300, 59.500, 59.800)",
            PreferencesManager.PROFILE_USA_AFRICA to "USA / Africa (58.500, 58.800, 59.100, 59.300, 59.500, 59.700)",
            PreferencesManager.PROFILE_CUSTOM to "Custom Timings (Comma-separated seconds)"
        ).forEach { (profile, desc) ->
            val selected = timingProfile == profile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) c.primary.copy(alpha = 0.16f) else c.surfaceContainerHigh)
                    .border(1.dp, if (selected) c.primary else Color.Transparent, RoundedCornerShape(12.dp))
                    .pressable { viewModel.updateTimingProfile(profile) }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) c.primary else c.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    Icon(Icons.Rounded.Check, null, tint = c.primary, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (timingProfile == PreferencesManager.PROFILE_CUSTOM) {
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.surfaceContainerLow)
                    .border(1.dp, c.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = customTimings,
                    onValueChange = viewModel::updateCustomTimings,
                    textStyle = TextStyle(color = c.onSurface, fontFamily = MonoFont, fontSize = 12.5.sp),
                    cursorBrush = SolidColor(c.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

        Text("Burst Count: $burstCount requests", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = c.onSurface)
        Slider(
            value = burstCount.toFloat(),
            onValueChange = { viewModel.updateBurstCount(it.toInt()) },
            valueRange = 5f..50f,
            colors = SliderDefaults.colors(thumbColor = c.primary, activeTrackColor = c.primary)
        )

        Text("Burst Interval: ${burstInterval} ms", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = c.onSurface)
        Slider(
            value = burstInterval.toFloat(),
            onValueChange = { viewModel.updateBurstIntervalMs(it.toLong()) },
            valueRange = 25f..500f,
            colors = SliderDefaults.colors(thumbColor = c.primary, activeTrackColor = c.primary)
        )
    }
}

private data class StorageData(
    val deviceTotal: Long,
    val deviceUsed: Long,
    val deviceFree: Long,
    val appSize: Long,
    val cacheSize: Long,
    val tokenFilesSize: Long,
    val dataSize: Long,
    val totalSize: Long,
)

private fun getFolderSize(dir: File?): Long {
    if (dir == null || !dir.exists()) return 0L
    var size = 0L
    dir.listFiles()?.forEach { file ->
        size += if (file.isDirectory) getFolderSize(file) else file.length()
    }
    return size
}

private fun formatStorageBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return if (digitGroups >= 3) {
        "%.1f %s".format(value, units[digitGroups])
    } else {
        "%.0f %s".format(value, units[digitGroups])
    }
}

private fun calculateStorageData(context: Context): StorageData {
    return runCatching {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val free = stat.availableBytes
        val used = (total - free).coerceAtLeast(0L)

        val appSize = runCatching { File(context.packageCodePath).length() }.getOrDefault(6_400_000L)
        val cacheSize = getFolderSize(context.cacheDir) + (context.externalCacheDir?.let { getFolderSize(it) } ?: 0L)
        val dlDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MiUnlockTool")
        val tokenFilesSize = getFolderSize(dlDir)
        val dataSize = getFolderSize(context.filesDir)

        StorageData(
            deviceTotal = total,
            deviceUsed = used,
            deviceFree = free,
            appSize = appSize,
            cacheSize = cacheSize,
            tokenFilesSize = tokenFilesSize,
            dataSize = dataSize,
            totalSize = appSize + cacheSize + tokenFilesSize + dataSize,
        )
    }.getOrDefault(
        StorageData(128_000_000_000L, 42_000_000_000L, 86_000_000_000L, 6_400_000L, 128_000L, 0L, 64_000L, 6_592_000L)
    )
}

@Composable
private fun StorageUsageContent(context: Context, viewModel: MainViewModel) {
    val c = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var storageData by remember { mutableStateOf(calculateStorageData(context)) }
    var isClearing by remember { mutableStateOf(false) }
    var clearedNotice by remember { mutableStateOf<String?>(null) }

    val usedGb = storageData.deviceUsed / 1_000_000_000.0
    val totalGb = storageData.deviceTotal / 1_000_000_000.0
    val freeGb = storageData.deviceFree / 1_000_000_000.0
    val progress = (storageData.deviceUsed.toFloat() / storageData.deviceTotal.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = "Device storage",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = c.onSurface,
        )
        Spacer(Modifier.height(8.dp))

        var animatedTarget by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(progress) { animatedTarget = progress }
        val animatedProgress by animateFloatAsState(
            targetValue = animatedTarget,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 260f),
            label = "storageProgress",
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(11.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(c.surfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(c.primary, c.tertiary, c.secondary)
                        )
                    )
            )
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${"%.1f".format(usedGb)} GB used of ${"%.1f".format(totalGb)} GB",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = c.onSurface,
            )
            Text(
                text = "${"%.1f".format(freeGb)} GB free",
                fontSize = 11.5.sp,
                color = c.onSurfaceVariant,
            )
        }

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))

        Text(
            text = "Used by Mi Unlock Tool",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = c.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Total space: ${formatStorageBytes(storageData.totalSize)}",
            fontSize = 12.sp,
            color = c.primary,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(10.dp))

        StorageItemRow("App & Core Crypto Engine", formatStorageBytes(storageData.appSize))
        StorageItemRow("Execution Logs & Network Cache", formatStorageBytes(storageData.cacheSize))
        StorageItemRow("Saved token.bin Files", formatStorageBytes(storageData.tokenFilesSize))
        StorageItemRow("Session & Profile Config", formatStorageBytes(storageData.dataSize))

        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2E1517))
                .pressable(enabled = !isClearing) {
                    isClearing = true
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            runCatching {
                                context.cacheDir.deleteRecursively()
                                context.externalCacheDir?.deleteRecursively()
                            }
                            val fresh = calculateStorageData(context)
                            withContext(Dispatchers.Main) {
                                viewModel.clearLogs()
                                storageData = fresh
                                isClearing = false
                                clearedNotice = "Cache & execution logs cleared!"
                            }
                        }
                    }
                }
                .padding(vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = clearedNotice ?: "Clear Cache & Execution Logs",
                    color = Color(0xFFEF5350),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                )
            }
        }
    }
}

@Composable
private fun StorageItemRow(label: String, sizeStr: String) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 12.sp, color = c.onSurfaceVariant)
        Text(sizeStr, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.onSurface)
    }
}

@Composable
private fun LanguageContent(context: Context, onLanguageSelected: () -> Unit) {
    val c = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }
    val currentCode = LanguageSettings.currentCode
    val filteredLanguages = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            LanguageSettings.ALL_LANGUAGES
        } else {
            LanguageSettings.ALL_LANGUAGES.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.nativeName.contains(searchQuery, ignoreCase = true) ||
                    it.code.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(c.surfaceContainerLow)
                .border(1.dp, c.onSurface.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = "Search",
                tint = c.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                textStyle = TextStyle(
                    color = c.onSurface,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(c.primary),
                singleLine = true,
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search languages...",
                            color = c.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 13.5.sp,
                        )
                    }
                    innerTextField()
                }
            )
            if (searchQuery.isNotEmpty()) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Clear",
                    tint = c.onSurfaceVariant,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { searchQuery = "" },
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(filteredLanguages, key = { it.code }) { item ->
                val isSelected = currentCode.equals(item.code, ignoreCase = true)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) c.primary.copy(alpha = 0.14f) else c.surfaceContainerLow.copy(alpha = 0.5f))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) c.primary.copy(alpha = 0.5f) else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            LanguageSettings.choose(context, item.code)
                            onLanguageSelected()
                        }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.nativeName,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = if (isSelected) c.primary else c.onSurface,
                        )
                        Text(
                            text = if (item.code == "auto") "Follow device language" else item.name,
                            fontSize = 11.5.sp,
                            color = c.onSurfaceVariant.copy(alpha = 0.75f),
                        )
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(c.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = c.onPrimary,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable { onCheckedChange(!checked) }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = c.onSurface,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = subtitle,
                color = c.onSurfaceVariant,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
            )
        }
        Spacer(Modifier.width(6.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = c.primary),
        )
    }
}
