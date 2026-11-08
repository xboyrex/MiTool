package com.xboyrex.miunlocktool

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRuntimeShaderSupported
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

object GlassQuality {
    @Volatile
    private var deviceSupports = isRuntimeShaderSupported()

    var effectsSupported by mutableStateOf(deviceSupports)
        private set

    fun init(context: Context) {
        val am = context.getSystemService(ActivityManager::class.java)
        deviceSupports = isRuntimeShaderSupported() && am?.isLowRamDevice != true
        effectsSupported = deviceSupports && GlassSettings.enabled
    }

    fun setEnabled(enabled: Boolean) {
        effectsSupported = deviceSupports && enabled
    }
}

object GlassSettings {
    private const val PREFS = "appearance_xboyrex"
    private const val KEY = "liquid_glass"

    private var prefs: SharedPreferences? = null

    var enabled by mutableStateOf(true)
        private set

    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).also { prefs = it }
        enabled = p.getBoolean(KEY, true)
        GlassQuality.setEnabled(enabled)
    }

    fun updateEnabled(value: Boolean) {
        enabled = value
        prefs?.edit()?.putBoolean(KEY, value)?.apply()
        GlassQuality.setEnabled(value)
    }
}

@Composable
fun glassFill(alpha: Float = 1f): Color {
    if (!GlassQuality.effectsSupported) {
        return MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val dark = isDarkTheme()
    return if (dark) Color(0xFF101010).copy(alpha = 0.44f * alpha) else
        Color(0xFFFAFAFA).copy(alpha = 0.36f * alpha)
}

val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

@Immutable
data class GlassSpec(
    val shape: Shape,
    val tint: Color,
    val blur: Dp,
    val refractionHeight: Dp,
    val refractionAmount: Dp,
    val shadow: Shadow?,
    val highlight: Highlight?,
    val innerShadow: InnerShadow?,
    val chromaticAberration: Boolean,
)

@Composable
fun glassSpec(
    shape: Shape = RoundedCornerShape(26.dp),
    tint: Color = glassFill(),
    blur: Dp = 18.dp,
    refractionHeight: Dp = 14.dp,
    refractionAmount: Dp = 22.dp,
    shadow: Shadow? = Shadow.Default,
    highlight: Highlight? = Highlight.Default,
    innerShadow: InnerShadow? = InnerShadow.Default.copy(radius = 10.dp, alpha = 0.5f),
    chromaticAberration: Boolean = true,
): GlassSpec = GlassSpec(
    shape = shape,
    tint = if (GlassQuality.effectsSupported) tint else tint.copy(alpha = 1f),
    blur = blur,
    refractionHeight = refractionHeight,
    refractionAmount = refractionAmount,
    shadow = shadow,
    highlight = highlight,
    innerShadow = innerShadow,
    chromaticAberration = chromaticAberration,
)

fun Modifier.liquidGlass(backdrop: Backdrop, spec: GlassSpec): Modifier =
    if (GlassQuality.effectsSupported) {
        drawBackdrop(
            backdrop = backdrop,
            shape = { spec.shape },
            effects = {
                vibrancy()
                blur(spec.blur.toPx())
                lens(
                    spec.refractionHeight.toPx(),
                    spec.refractionAmount.toPx(),
                    chromaticAberration = spec.chromaticAberration,
                )
            },
            highlight = spec.highlight?.let { h -> { h } },
            shadow = spec.shadow?.let { s -> { s } },
            innerShadow = spec.innerShadow?.let { s -> { s } },
            onDrawSurface = {
                if (spec.tint.isSpecified) drawRect(spec.tint)
            },
        )
    } else {
        fallbackGlass(spec)
    }

private fun Modifier.fallbackGlass(spec: GlassSpec): Modifier =
    if (!GlassQuality.effectsSupported) {
        this
            .clip(spec.shape)
            .background(spec.tint.copy(alpha = 1f))
    } else {
        this
            .clip(spec.shape)
            .background(spec.tint)
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.14f),
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.05f),
                    ),
                )
            }
            .border(1.dp, Color.White.copy(alpha = 0.16f), spec.shape)
    }
