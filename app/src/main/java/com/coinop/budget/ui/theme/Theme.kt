package com.coinop.budget.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.coinop.budget.R

// ---------------------------------------------------------------- locals

val LocalPalette = staticCompositionLocalOf { Themes.byId("arcadia").dark }
val LocalDark = staticCompositionLocalOf { true }
val LocalUiStyle = staticCompositionLocalOf { UiStyle.ARCADIA }
val LocalFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

/** Shorthand for the active palette and flags. */
object Retro {
    val p: ThemeColors @Composable @ReadOnlyComposable get() = LocalPalette.current
    val dark: Boolean @Composable @ReadOnlyComposable get() = LocalDark.current
    val cyber: Boolean @Composable @ReadOnlyComposable get() = LocalUiStyle.current == UiStyle.CYBERPUNK
    val primary: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.primary(LocalDark.current)
    val text: Color @Composable @ReadOnlyComposable get() = if (LocalDark.current) LocalPalette.current.cream else LocalPalette.current.ink
    val family: FontFamily @Composable @ReadOnlyComposable get() = LocalFontFamily.current
}

// ---------------------------------------------------------------- fonts

private val Silkscreen = FontFamily(
    Font(R.font.silkscreen_regular, FontWeight.Normal),
    Font(R.font.silkscreen_bold, FontWeight.Bold),
)
private val PressStart = FontFamily(Font(R.font.press_start_2p, FontWeight.Normal))
private val Vt323 = FontFamily(Font(R.font.vt323, FontWeight.Normal))
private val Rajdhani = FontFamily(
    Font(R.font.rajdhani_medium, FontWeight.Normal),
    Font(R.font.rajdhani_bold, FontWeight.Bold),
)

/**
 * Personal-use display fonts are looked up by resource name so the app still builds and runs when a
 * file is missing; Rajdhani stands in. One cut serves every weight.
 */
private fun optionalFont(context: Context, resName: String, fallback: FontFamily): FontFamily {
    val id = context.resources.getIdentifier(resName, "font", context.packageName)
    return if (id == 0) fallback else FontFamily(Font(id, FontWeight.Normal), Font(id, FontWeight.Bold))
}

fun FontChoice.family(cyber: Boolean, context: Context): FontFamily = when (this) {
    FontChoice.CYBER_HORIZON -> optionalFont(context, "cyber_horizon", Rajdhani)
    FontChoice.SD_DYSTOPIAN -> optionalFont(context, "sd_dystopian", Rajdhani)
    FontChoice.DEFAULT -> if (cyber) Rajdhani else Silkscreen
    FontChoice.SILKSCREEN -> Silkscreen
    FontChoice.PRESS_START -> PressStart
    FontChoice.VT323 -> Vt323
    FontChoice.RAJDHANI -> Rajdhani
    FontChoice.SYSTEM -> FontFamily.SansSerif
}

private fun typographyFor(f: FontFamily): Typography {
    val d = Typography()
    fun TextStyle.t(size: Int): TextStyle = copy(fontFamily = f, fontSize = size.sp, lineHeight = (size + 6).sp)
    return Typography(
        displayLarge = d.displayLarge.copy(fontFamily = f),
        displayMedium = d.displayMedium.copy(fontFamily = f),
        displaySmall = d.displaySmall.copy(fontFamily = f),
        headlineLarge = d.headlineLarge.copy(fontFamily = f),
        headlineMedium = d.headlineMedium.copy(fontFamily = f),
        headlineSmall = d.headlineSmall.copy(fontFamily = f),
        titleLarge = d.titleLarge.t(20),
        titleMedium = d.titleMedium.t(16),
        titleSmall = d.titleSmall.t(14),
        bodyLarge = d.bodyLarge.t(15),
        bodyMedium = d.bodyMedium.t(14),
        bodySmall = d.bodySmall.t(12),
        labelLarge = d.labelLarge.t(14),
        labelMedium = d.labelMedium.t(12),
        labelSmall = d.labelSmall.t(12),
    )
}

// ---------------------------------------------------------------- shapes

/** One helper for every corner: rounded in Arcadia, two cut corners in Cyberpunk. */
fun styledShape(cyber: Boolean, radius: Dp): CornerBasedShape =
    if (!cyber) {
        RoundedCornerShape(radius)
    } else {
        val cut = (radius * 1.1f).coerceIn(3.dp, 16.dp)
        CutCornerShape(topStart = 0.dp, topEnd = cut, bottomEnd = 0.dp, bottomStart = cut)
    }

@Composable
@ReadOnlyComposable
fun rShape(radius: Dp = 12.dp): CornerBasedShape = styledShape(Retro.cyber, radius)

/** Sheet / tab shapes: only the top corners are styled. */
@Composable
@ReadOnlyComposable
fun topShape(radius: Dp = 14.dp): CornerBasedShape =
    if (!Retro.cyber) {
        RoundedCornerShape(topStart = radius, topEnd = radius)
    } else {
        CutCornerShape(topStart = 0.dp, topEnd = (radius * 1.1f).coerceIn(3.dp, 16.dp), bottomEnd = 0.dp, bottomStart = 0.dp)
    }

private fun shapesFor(cyber: Boolean): Shapes = Shapes(
    extraSmall = styledShape(cyber, 4.dp),
    small = styledShape(cyber, 8.dp),
    medium = styledShape(cyber, 12.dp),
    large = styledShape(cyber, 14.dp),
    extraLarge = styledShape(cyber, 16.dp),
)

// ---------------------------------------------------------------- the sticker surface

/**
 * The one surface recipe: hard shadow (3 right and down, no blur), flat fill,
 * 2 wide outline. Leaves 3 of room right and bottom for the shadow.
 */
@Composable
fun Modifier.sticker(
    fill: Color = Retro.p.surface,
    radius: Dp = 12.dp,
    shadow: Boolean = true,
    selected: Boolean = false,
    shape: Shape = rShape(radius),
): Modifier {
    val p = Retro.p
    val dark = Retro.dark
    val shadowColor = if (Retro.cyber && dark) p.red else p.ink
    val offset = 3.dp
    val borderWidth = if (selected) 4.dp else 2.dp
    val borderColor = if (selected) p.primary(dark) else p.outline
    val base = if (shadow) {
        this.padding(end = offset, bottom = offset).drawBehind {
            val o = shape.createOutline(size, layoutDirection, this)
            translate(offset.toPx(), offset.toPx()) { drawOutline(o, shadowColor) }
        }
    } else {
        this
    }
    return base.clip(shape).background(fill, shape).border(borderWidth, borderColor, shape)
}

// ---------------------------------------------------------------- backdrops

@Composable
fun Modifier.backdrop(style: BackdropStyle, strength: Float, forceAlpha: Float? = null): Modifier {
    if (style == BackdropStyle.PLAIN) return this
    val dark = Retro.dark
    val p = Retro.p
    val base = if (dark) p.cream else p.ink
    val alpha = forceAlpha ?: ((if (dark) 0.20f else 0.30f) * strength)
    val color = base.copy(alpha = alpha)
    return this.drawBehind { drawPattern(style, color) }
}

private fun DrawScope.drawPattern(style: BackdropStyle, color: Color) {
    val u = 14.dp.toPx()
    val w = size.width
    val h = size.height
    when (style) {
        BackdropStyle.DOTS -> {
            val s = 2.dp.toPx()
            var y = u / 2
            while (y < h) {
                var x = u / 2
                while (x < w) {
                    drawRect(color, Offset(x - s / 2, y - s / 2), Size(s, s))
                    x += u
                }
                y += u
            }
        }
        BackdropStyle.GRID -> {
            val t = 1.dp.toPx()
            var x = 0f
            while (x < w) { drawRect(color, Offset(x, 0f), Size(t, h)); x += u }
            var y = 0f
            while (y < h) { drawRect(color, Offset(0f, y), Size(w, t)); y += u }
        }
        BackdropStyle.SCAN -> {
            val t = 1.dp.toPx()
            val step = u / 3.5f
            var y = 0f
            while (y < h) { drawRect(color, Offset(0f, y), Size(w, t)); y += step }
        }
        BackdropStyle.CHECK -> {
            val s = u * 1.2f
            val c = color.copy(alpha = color.alpha * 0.45f)
            var row = 0
            var y = 0f
            while (y < h) {
                var col = 0
                var x = 0f
                while (x < w) {
                    if ((row + col) % 2 == 0) drawRect(c, Offset(x, y), Size(s, s))
                    x += s; col++
                }
                y += s; row++
            }
        }
        BackdropStyle.STRIPE -> {
            val t = 1.dp.toPx()
            var k = 0f
            while (k < w + h) {
                drawLine(color, Offset(0f, k), Offset(k, 0f), strokeWidth = t)
                k += u
            }
        }
        BackdropStyle.PLAIN -> Unit
    }
}

// ---------------------------------------------------------------- the theme itself

private fun schemeFor(p: ThemeColors, dark: Boolean): ColorScheme {
    val text = if (dark) p.cream else p.ink
    val primary = p.primary(dark)
    val tertiary = p.tertiary(dark)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = p.onFace(primary),
        secondary = p.green,
        onSecondary = p.onFace(p.green),
        tertiary = tertiary,
        onTertiary = p.onFace(tertiary),
        background = p.bg,
        onBackground = text,
        surface = p.surface,
        onSurface = text,
        surfaceVariant = p.variant,
        onSurfaceVariant = p.onVariant,
        surfaceTint = Color.Transparent,
        outline = p.outline,
        outlineVariant = p.outline,
        error = p.red,
        onError = p.onFace(p.red),
        scrim = Color.Black,
        surfaceContainer = p.surface,
        surfaceContainerHigh = p.surface,
        surfaceContainerHighest = p.surface,
        surfaceContainerLow = p.surface,
        surfaceContainerLowest = p.surface,
    )
}

@Composable
fun CoinopTheme(appearance: Appearance, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (appearance.face) {
        Face.SYSTEM -> systemDark
        Face.LIGHT -> false
        Face.DARK -> true
    }
    val def = Themes.byId(appearance.themeId)
    val colors = if (dark) def.dark else def.light
    val cyber = appearance.uiStyle == UiStyle.CYBERPUNK
    val family = appearance.font.family(cyber, LocalContext.current)
    val density = LocalDensity.current

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val c = WindowCompat.getInsetsController(window, view)
                c.isAppearanceLightStatusBars = !dark
                c.isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(
        LocalPalette provides colors,
        LocalDark provides dark,
        LocalUiStyle provides appearance.uiStyle,
        LocalFontFamily provides family,
        // text size multiplier applied once at the root
        LocalDensity provides Density(density.density, density.fontScale * appearance.textScale),
    ) {
        MaterialTheme(
            colorScheme = schemeFor(colors, dark),
            typography = typographyFor(family),
            shapes = shapesFor(cyber),
            content = content,
        )
    }
}
