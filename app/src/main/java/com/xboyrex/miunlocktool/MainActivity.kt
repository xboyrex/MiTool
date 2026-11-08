package com.xboyrex.miunlocktool

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.xboyrex.miunlocktool.fastboot.FastbootUsbTransport
import com.xboyrex.miunlocktool.ui.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private val viewModel: MainViewModel by viewModels()

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                FastbootUsbTransport.ACTION_USB_PERMISSION -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    viewModel.onUsbPermissionResult(granted, device)
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    context?.let { viewModel.checkAndConnectFastboot(it) }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    viewModel.disconnectFastboot()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setHighestRefreshRate()

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MiUnlockTheme {
                MiUnlockApp(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(FastbootUsbTransport.ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(usbReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        runCatching { unregisterReceiver(usbReceiver) }
    }

    override fun onResume() {
        super.onResume()
        setHighestRefreshRate()
    }

    private fun setHighestRefreshRate() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val disp = display ?: return
                val currentMode = disp.mode
                val modes = disp.supportedModes
                val bestMode = modes
                    .filter { it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight }
                    .maxByOrNull { it.refreshRate }
                    ?: modes.maxByOrNull { it.refreshRate }
                if (bestMode != null) {
                    val lp = window.attributes
                    lp.preferredDisplayModeId = bestMode.modeId
                    lp.preferredRefreshRate = bestMode.refreshRate
                    window.attributes = lp
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val disp = windowManager.defaultDisplay ?: return
                val currentMode = disp.mode
                val modes = disp.supportedModes
                val bestMode = modes
                    .filter { it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight }
                    .maxByOrNull { it.refreshRate }
                    ?: modes.maxByOrNull { it.refreshRate }
                if (bestMode != null) {
                    val lp = window.attributes
                    lp.preferredDisplayModeId = bestMode.modeId
                    window.attributes = lp
                }
            }
        }
    }
}

enum class AppTab(val defaultLabel: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Apply("Apply", Icons.Outlined.Bolt, Icons.Rounded.Bolt),
    Fastboot("Fastboot", Icons.Outlined.Usb, Icons.Rounded.Usb),
    Guide("Guide", Icons.Outlined.MenuBook, Icons.Rounded.MenuBook),
    Settings("Settings", Icons.Outlined.Settings, Icons.Rounded.Settings);

    val label: String
        get() = when (this) {
            Apply -> I18n.t("tab_apply", defaultLabel)
            Fastboot -> I18n.t("tab_fastboot", defaultLabel)
            Guide -> I18n.t("tab_guide", defaultLabel)
            Settings -> I18n.t("tab_settings", defaultLabel)
        }
}

private val NavHeight = 64.dp
private val NavWidth = 376.dp
private val NavLift = 10.dp

@Composable
fun MiUnlockApp(viewModel: MainViewModel) {
    val c = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val appContext = LocalContext.current
    val reduced = rememberReducedMotion()

    var tab by rememberSaveable { mutableStateOf(AppTab.Apply) }
    var showAuthModal by remember { mutableStateOf(false) }

    val isSniperRunning by viewModel.scheduler.isRunning.collectAsState()
    val applyDialogResult by viewModel.lastApplyDialogResult.collectAsState()

    val updateState = remember { UpdateState(scope) }
    val pageBackdrop = rememberLayerBackdrop()

    // Smooth open / entrance animation on app launch
    val entranceScale = remember { Animatable(0.95f) }
    val entranceAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (reduced) {
            entranceScale.snapTo(1f)
            entranceAlpha.snapTo(1f)
        } else {
            launch { entranceScale.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 360f)) }
            launch { entranceAlpha.animateTo(1f, tween(220, easing = Motion.Emphasized)) }
        }
        kotlinx.coroutines.delay(1400)
        updateState.check(manual = false)
    }

    LaunchedEffect(updateState.message) {
        updateState.message?.let { msg ->
            android.widget.Toast.makeText(appContext, msg, android.widget.Toast.LENGTH_SHORT).show()
            updateState.message = null
        }
    }

    val rootView = LocalView.current
    DisposableEffect(isSniperRunning) {
        rootView.keepScreenOn = isSniperRunning
        onDispose { rootView.keepScreenOn = false }
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val stackBottom = navBottom.coerceAtLeast(8.dp) +
        if (WindowInsets.tappableElement.asPaddingValues().calculateBottomPadding() > 0.dp) 10.dp else 0.dp
    val navTop = stackBottom + NavLift + NavHeight

    val contentPadding = PaddingValues(bottom = navTop + 16.dp)

    BackHandler(enabled = showAuthModal) { showAuthModal = false }
    BackHandler(enabled = !showAuthModal && applyDialogResult != null) { viewModel.dismissApplyResultDialog() }
    BackHandler(enabled = !showAuthModal && applyDialogResult == null && tab != AppTab.Apply) {
        tab = AppTab.Apply
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.surface)
            .graphicsLayer {
                scaleX = entranceScale.value
                scaleY = entranceScale.value
                alpha = entranceAlpha.value
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(pageBackdrop)
                .background(c.surface)
        ) {
            AnimatedContent(
                targetState = tab,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxHeight()
                    .fillMaxWidth(),
                transitionSpec = {
                    if (reduced) {
                        fadeIn(snap()) togetherWith fadeOut(snap())
                    } else {
                        val forward = targetState.ordinal >= initialState.ordinal
                        val slideDir = if (forward) 1 else -1
                        (fadeIn(tween(Motion.PageEnterMs, easing = Motion.Emphasized)) +
                            slideInHorizontally(tween(Motion.PageRiseMs, easing = Motion.Emphasized)) { (it / 14) * slideDir } +
                            slideInVertically(tween(Motion.PageRiseMs, easing = Motion.Emphasized)) { (it / 40).coerceIn(6, 20) }) togetherWith
                            (fadeOut(tween(Motion.PageExitMs)) +
                                slideOutHorizontally(tween(Motion.PageExitMs)) { -(it / 22) * slideDir })
                    }
                },
                label = "mainTabs"
            ) { currentTab ->
                when (currentTab) {
                    AppTab.Apply -> ApplyScreen(
                        viewModel = viewModel,
                        contentPadding = contentPadding,
                        onOpenAuth = { showAuthModal = true },
                        onOpenSettings = { tab = AppTab.Settings }
                    )
                    AppTab.Fastboot -> FastbootScreen(
                        viewModel = viewModel,
                        contentPadding = contentPadding,
                        onOpenAuth = { showAuthModal = true }
                    )
                    AppTab.Guide -> GuideScreen(
                        contentPadding = contentPadding
                    )
                    AppTab.Settings -> SettingsScreen(
                        viewModel = viewModel,
                        onBack = null,
                        onSignIn = { showAuthModal = true },
                        onSignedOut = {},
                        contentPadding = contentPadding,
                        onCheckUpdate = { updateState.check(manual = true) }
                    )
                }
            }

            BottomGestureGuard(Modifier.align(Alignment.BottomCenter))
        }

        // Super Smooth YTX Liquid Glass Bottom Navigation Bar
        AnimatedVisibility(
            visible = !showAuthModal,
            enter = slideInVertically(tween(260, easing = Motion.Emphasized)) { it } + fadeIn(tween(160)),
            exit = slideOutVertically(tween(200, easing = Motion.Emphasized)) { it } + fadeOut(tween(140)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = stackBottom + NavLift)
                .widthIn(max = NavWidth)
                .fillMaxWidth(),
        ) {
            LiquidBottomBar(
                selectedTabIndex = { AppTab.entries.indexOf(tab) },
                onTabSelected = { index -> AppTab.entries.getOrNull(index)?.let { tab = it } },
                backdrop = pageBackdrop,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AppTab.entries.forEach { t ->
                    LiquidNavTab(
                        tab = t,
                        selected = t == tab,
                        isSniperActive = isSniperRunning,
                        onClick = { tab = t }
                    )
                }
            }
        }

        // Xiaomi Account Auth Modal (QR + Embedded WebView)
        XiaomiAuthModal(
            visible = showAuthModal,
            onDismiss = { showAuthModal = false },
            onCredentialsExtracted = { creds ->
                viewModel.applySessionCredentials(creds)
            }
        )

        // Apply Result Modal
        ApplyResultModal(
            result = applyDialogResult,
            onDismiss = { viewModel.dismissApplyResultDialog() }
        )

        // In-App GitHub Update Overlay
        UpdateOverlay(state = updateState, backdrop = pageBackdrop)
    }
}
