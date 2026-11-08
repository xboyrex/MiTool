package com.xboyrex.miunlocktool

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlin.math.abs

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

val AppRoundedHeavy = FontFamily(
    Font(DeviceFontFamilyName("sans-serif-rounded"), weight = FontWeight.ExtraBold),
    Font(DeviceFontFamilyName("sans-serif-rounded"), weight = FontWeight.Bold),
    Font(DeviceFontFamilyName("sans-serif"), weight = FontWeight.ExtraBold),
    Font(DeviceFontFamilyName("sans-serif"), weight = FontWeight.Bold),
)

val RoundedHeavy: FontFamily
    get() = if (ThemeSettings.useSystemFont) FontFamily.Default else AppRoundedHeavy

val MonoFont: FontFamily = FontFamily.Monospace

private fun hexToHsl(hex: String): Triple<Float, Float, Float> {
    var h2 = hex.removePrefix("#").trim()
    if (h2.length == 3) h2 = h2.map { "$it$it" }.joinToString("")
    if (h2.length != 6) h2 = "FFD600"
    val r = h2.substring(0, 2).toIntOrNull(16)?.div(255f) ?: 1f
    val g = h2.substring(2, 4).toIntOrNull(16)?.div(255f) ?: 0.84f
    val b = h2.substring(4, 6).toIntOrNull(16)?.div(255f) ?: 0f
    val mx = maxOf(r, g, b); val mn = minOf(r, g, b); val l = (mx + mn) / 2f
    var h = 0f; var s = 0f; val d = mx - mn
    if (d != 0f) {
        s = d / (1 - abs(2 * l - 1))
        h = when (mx) {
            r -> (((g - b) / d) % 6)
            g -> ((b - r) / d + 2)
            else -> ((r - g) / d + 4)
        } * 60f
        if (h < 0) h += 360f
    }
    return Triple(h, s * 100f, l * 100f)
}

private fun hsl(h: Float, s: Float, l: Float): Color {
    val sN = s.coerceIn(0f, 100f) / 100f
    val lN = l.coerceIn(0f, 100f) / 100f
    val c = (1 - abs(2 * lN - 1)) * sN
    val x = c * (1 - abs(((h / 60f) % 2) - 1))
    val m = lN - c / 2
    val (r, g, b) = when {
        h < 60 -> Triple(c, x, 0f)
        h < 120 -> Triple(x, c, 0f)
        h < 180 -> Triple(0f, c, x)
        h < 240 -> Triple(0f, x, c)
        h < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color((r + m).coerceIn(0f, 1f), (g + m).coerceIn(0f, 1f), (b + m).coerceIn(0f, 1f))
}

data class ThemeChoice(val name: String, val hex: String, val isNew: Boolean = false)

const val SEED_ONYX_YELLOW = "#FFD600"
const val SEED_CYBER_GOLD  = "#FFEA00"
const val SEED_MI_ORANGE   = "#FF5500"
const val SEED_CREAM_ROSE  = "#833761"
const val SEED_AURORA      = "#6C2BD9"
const val SEED_LILAC_AIR   = "#7454CE"
const val SEED_JAI         = "#D6005C"
const val SEED_PUMPKIN     = "#B0714A"
const val SEED_MOONSTONE   = "#8A8F98"

val MI_THEMES = listOf(
    ThemeChoice("Onyx Yellow", SEED_ONYX_YELLOW, isNew = true),
    ThemeChoice("Cyber Gold",  SEED_CYBER_GOLD,  isNew = true),
    ThemeChoice("Mi Orange",   SEED_MI_ORANGE,   isNew = true),
    ThemeChoice("Cream Rose",  SEED_CREAM_ROSE),
    ThemeChoice("Erica",       SEED_AURORA),
    ThemeChoice("Lilac Air",   SEED_LILAC_AIR),
    ThemeChoice("Jai",         SEED_JAI),
    ThemeChoice("Pumpkin",     SEED_PUMPKIN),
    ThemeChoice("Moonstone",   SEED_MOONSTONE),
    ThemeChoice("Iris",        "#6750A4"),
    ThemeChoice("Harbor",      "#3B5BDB"),
)

private fun onyxYellowScheme(dark: Boolean, cyberGold: Boolean = false): ColorScheme {
    val primaryYellow = if (cyberGold) Color(0xFFFFEA00) else Color(0xFFFFD600)
    return if (!dark) lightColorScheme(
        primary = Color(0xFFB28900),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFF0B3),
        onPrimaryContainer = Color(0xFF332600),
        secondary = Color(0xFF6B5E3F),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF5E5BC),
        onSecondaryContainer = Color(0xFF241B04),
        tertiary = Color(0xFFFF5500),
        tertiaryContainer = Color(0xFFFFDBCC),
        onTertiaryContainer = Color(0xFF380D00),
        background = Color(0xFFFFFDF7),
        onBackground = Color(0xFF1A1812),
        surface = Color(0xFFFFFDF7),
        onSurface = Color(0xFF1A1812),
        surfaceVariant = Color(0xFFEDE6D4),
        onSurfaceVariant = Color(0xFF4D4739),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFFAF6EC),
        surfaceContainer = Color(0xFFF4EFE4),
        surfaceContainerHigh = Color(0xFFEEE9DD),
        surfaceContainerHighest = Color(0xFFE8E2D6),
        outline = Color(0xFF7F7767),
        outlineVariant = Color(0xFFD0C6B4),
    ) else darkColorScheme(
        primary = primaryYellow,
        onPrimary = Color(0xFF1A1400),
        primaryContainer = Color(0xFF3A3000),
        onPrimaryContainer = Color(0xFFFFF0A3),
        secondary = Color(0xFFE8C96B),
        onSecondary = Color(0xFF261E00),
        secondaryContainer = Color(0xFF292412),
        onSecondaryContainer = Color(0xFFF7E4A6),
        tertiary = Color(0xFFFF8A50),
        tertiaryContainer = Color(0xFF3B1A08),
        onTertiaryContainer = Color(0xFFFFDBCC),
        background = Color(0xFF0A0A08),
        onBackground = Color(0xFFF7F5EB),
        surface = Color(0xFF0A0A08),
        onSurface = Color(0xFFF7F5EB),
        surfaceVariant = Color(0xFF26241C),
        onSurfaceVariant = Color(0xFFC2BCA8),
        surfaceContainerLowest = Color(0xFF040403),
        surfaceContainerLow = Color(0xFF11100C),
        surfaceContainer = Color(0xFF171611),
        surfaceContainerHigh = Color(0xFF201E17),
        surfaceContainerHighest = Color(0xFF2B281F),
        outline = Color(0xFF8C8673),
        outlineVariant = Color(0xFF2D2A20),
        error = Color(0xFFFF6B6B),
        onError = Color(0xFF3B0000),
        errorContainer = Color(0xFF690005),
        onErrorContainer = Color(0xFFFFDAD6),
    )
}

private fun miOrangeScheme(dark: Boolean): ColorScheme = if (!dark) lightColorScheme(
    primary = Color(0xFFE64A00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCC),
    onPrimaryContainer = Color(0xFF380D00),
    secondary = Color(0xFF77574B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBCC),
    onSecondaryContainer = Color(0xFF2C160D),
    tertiary = Color(0xFFFFD600),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF231916),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF231916),
    surfaceVariant = Color(0xFFF5DED6),
    onSurfaceVariant = Color(0xFF53433E),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF1EC),
    surfaceContainer = Color(0xFFFCEAE3),
    surfaceContainerHigh = Color(0xFFF6E4DD),
    surfaceContainerHighest = Color(0xFFF0DFD8),
    outline = Color(0xFF85736D),
    outlineVariant = Color(0xFFD8C2BA),
) else darkColorScheme(
    primary = Color(0xFFFF6D24),
    onPrimary = Color(0xFF2B0900),
    primaryContainer = Color(0xFF541D00),
    onPrimaryContainer = Color(0xFFFFDBCC),
    secondary = Color(0xFFE7BDB0),
    onSecondary = Color(0xFF442A20),
    secondaryContainer = Color(0xFF36221A),
    onSecondaryContainer = Color(0xFFFFDBCC),
    tertiary = Color(0xFFFFD600),
    background = Color(0xFF0E0B0A),
    onBackground = Color(0xFFF5ECE8),
    surface = Color(0xFF0E0B0A),
    onSurface = Color(0xFFF5ECE8),
    surfaceVariant = Color(0xFF2B211E),
    onSurfaceVariant = Color(0xFFD8C2BA),
    surfaceContainerLowest = Color(0xFF080605),
    surfaceContainerLow = Color(0xFF16110F),
    surfaceContainer = Color(0xFF1D1714),
    surfaceContainerHigh = Color(0xFF271F1C),
    surfaceContainerHighest = Color(0xFF332925),
    outline = Color(0xFFA08C85),
    outlineVariant = Color(0xFF382D29),
)

fun seedScheme(seedHex: String, dark: Boolean): ColorScheme {
    if (seedHex.equals(SEED_ONYX_YELLOW, ignoreCase = true)) return onyxYellowScheme(dark, cyberGold = false)
    if (seedHex.equals(SEED_CYBER_GOLD, ignoreCase = true)) return onyxYellowScheme(dark, cyberGold = true)
    if (seedHex.equals(SEED_MI_ORANGE, ignoreCase = true)) return miOrangeScheme(dark)

    val (h, s, _) = hexToHsl(seedHex)
    val isMoonstone = seedHex.equals(SEED_MOONSTONE, ignoreCase = true)
    val isPumpkin = seedHex.equals(SEED_PUMPKIN, ignoreCase = true)
    val accent = when {
        isMoonstone -> 7f
        isPumpkin -> 37f
        else -> maxOf(s, 55f)
    }
    val surf = when {
        isMoonstone -> 3f
        isPumpkin -> 24f
        else -> if (dark) 12f else 14f
    }
    return if (!dark) lightColorScheme(
        primary = hsl(h, accent, 42f), onPrimary = Color.White,
        primaryContainer = hsl(h, minOf(accent, 68f), 90f), onPrimaryContainer = hsl(h, accent, 16f),
        secondary = hsl(h, 26f, 40f), onSecondary = Color.White,
        secondaryContainer = hsl(h, 22f, 88f), onSecondaryContainer = hsl(h, 26f, 20f),
        tertiary = hsl((h + 40f) % 360f, 40f, 45f),
        background = hsl(h, surf, 98f), onBackground = hsl(h, 10f, 12f),
        surface = hsl(h, surf, 98f), onSurface = hsl(h, 10f, 12f),
        surfaceVariant = hsl(h, surf, 90f), onSurfaceVariant = hsl(h, minOf(12f, surf), 36f),
        surfaceContainerLowest = hsl(h, surf, 100f),
        surfaceContainerLow = hsl(h, surf, 96f),
        surfaceContainer = hsl(h, surf, 95f),
        surfaceContainerHigh = hsl(h, surf, 93f),
        surfaceContainerHighest = hsl(h, surf, 90f),
        outline = hsl(h, minOf(10f, surf), 54f), outlineVariant = hsl(h, minOf(10f, surf), 84f),
        error = Color(0xFFB3261E), onError = Color.White, errorContainer = Color(0xFFFFDAD5), onErrorContainer = Color(0xFF410E0B)
    ) else darkColorScheme(
        primary = hsl(h, minOf(accent, 85f), 74f), onPrimary = hsl(h, accent, 14f),
        primaryContainer = hsl(h, minOf(accent, 65f), 28f), onPrimaryContainer = hsl(h, 70f, 92f),
        secondary = hsl(h, 22f, 80f), onSecondary = hsl(h, 20f, 18f),
        secondaryContainer = hsl(h, 18f, 26f), onSecondaryContainer = hsl(h, 18f, 88f),
        tertiary = hsl((h + 40f) % 360f, 45f, 78f),
        background = hsl(h, surf, 6f), onBackground = hsl(h, 8f, 93f),
        surface = hsl(h, surf, 6f), onSurface = hsl(h, 8f, 93f),
        surfaceVariant = hsl(h, surf, 24f), onSurfaceVariant = hsl(h, minOf(10f, surf), 78f),
        surfaceContainerLowest = hsl(h, surf, 3f),
        surfaceContainerLow = hsl(h, surf, 9f),
        surfaceContainer = hsl(h, surf, 12f),
        surfaceContainerHigh = hsl(h, surf, 16f),
        surfaceContainerHighest = hsl(h, surf, 21f),
        outline = hsl(h, minOf(8f, surf), 58f), outlineVariant = hsl(h, minOf(8f, surf), 28f),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005), errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6)
    )
}

object ThemeSettings {
    private var prefs: SharedPreferences? = null
    val wallpaperAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // Default to Onyx Yellow (#FFD600) and AMOLED Dark mode!
    var seed by mutableStateOf<String?>(SEED_ONYX_YELLOW)
        private set

    var mode by mutableStateOf("amoled")
        private set

    var useSystemFont by mutableStateOf(false)
        private set

    fun init(context: Context) {
        val p = context.getSharedPreferences("appearance_xboyrex", Context.MODE_PRIVATE).also { prefs = it }
        seed = if (p.contains("seed")) p.getString("seed", null) else SEED_ONYX_YELLOW
        mode = p.getString("mode", "amoled") ?: "amoled"
        useSystemFont = p.getBoolean("use_system_font", false)
    }

    fun choose(seedHex: String?) {
        seed = seedHex
        prefs?.edit()?.putString("seed", seedHex)?.apply()
    }

    fun chooseMode(value: String) {
        mode = value
        prefs?.edit()?.putString("mode", value)?.apply()
    }

    fun updateUseSystemFont(enabled: Boolean) {
        useSystemFont = enabled
        prefs?.edit()?.putBoolean("use_system_font", enabled)?.apply()
    }
}

fun schemeFor(context: Context, seedHex: String?, dark: Boolean): ColorScheme =
    if (seedHex == null && ThemeSettings.wallpaperAvailable) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val seedValue = seedHex ?: SEED_ONYX_YELLOW
        seedScheme(seedValue, dark)
    }

@Composable
fun isDarkTheme(): Boolean = when (ThemeSettings.mode) {
    "dark", "amoled" -> true
    "light" -> false
    else -> isSystemInDarkTheme()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiUnlockTheme(content: @Composable () -> Unit) {
    val dark = isDarkTheme()
    val isAmoled = ThemeSettings.mode == "amoled"
    val context = LocalContext.current
    val baseScheme = schemeFor(context, ThemeSettings.seed, dark)
    val scheme = if (isAmoled) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF141414),
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF060606),
            surfaceContainer = Color(0xFF0D0D0D),
            surfaceContainerHigh = Color(0xFF151515),
            surfaceContainerHighest = Color(0xFF202020),
            onBackground = Color(0xFFF7F7F7),
            onSurface = Color(0xFFF7F7F7),
            outlineVariant = Color(0xFF222222),
        )
    } else baseScheme

    SystemBars(dark, scheme.surface)
    MaterialTheme(colorScheme = scheme, shapes = ExpressiveShapes, typography = Typography()) {
        CompositionLocalProvider(
            LocalRippleConfiguration provides null,
            LocalContentColor provides scheme.onSurface,
            content = content,
        )
    }
}

@Composable
private fun SystemBars(dark: Boolean, windowBg: Color) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val activity = view.context as? ComponentActivity
    SideEffect {
        val window = activity?.window ?: (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
        if (activity != null) {
            val barStyle = if (dark) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            }
            runCatching {
                activity.enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }
    DisposableEffect(windowBg) {
        (view.context as? Activity)?.window?.setBackgroundDrawable(ColorDrawable(windowBg.toArgb()))
        onDispose { }
    }
}
