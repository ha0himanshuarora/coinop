package com.coinop.budget.ui

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coinop.budget.ui.theme.AppIcon
import com.coinop.budget.ui.theme.AppIconView
import com.coinop.budget.ui.theme.Retro
import com.coinop.budget.ui.theme.onFace
import com.coinop.budget.ui.theme.primary
import com.coinop.budget.ui.theme.tertiary
import com.coinop.budget.ui.theme.rShape
import com.coinop.budget.ui.theme.sticker
import com.coinop.budget.ui.theme.topShape
import kotlin.math.floor
import kotlin.math.roundToInt

// ------------------------------------------------------------------ text

@Composable
fun RText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 14.sp,
    color: Color = Color.Unspecified,
    bold: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        color = if (color == Color.Unspecified) Retro.text else color,
        fontSize = size,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontFamily = Retro.family,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        lineHeight = size * 1.35f,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    RText(text.uppercase(), modifier.padding(top = 16.dp, bottom = 8.dp), size = 12.sp, color = Retro.p.onVariant, bold = true)
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        RText(text, color = Retro.p.onVariant, textAlign = TextAlign.Center)
    }
}

// ------------------------------------------------------------------ buttons

@Composable
fun RetroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    face: Color? = null,
    icon: AppIcon? = null,
) {
    val p = Retro.p
    // Disabled = opaque variant fill, readable label, no shadow.
    val fill = if (enabled) (face ?: Retro.primary) else p.variant
    val content = if (enabled) p.onFace(fill) else p.onVariant
    Row(
        modifier
            .sticker(fill, 12.dp, shadow = enabled)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            AppIconView(icon, content, 16.dp)
            Spacer(Modifier.width(8.dp))
        }
        RText(text.uppercase(), color = content, bold = true, size = 13.sp, maxLines = 1)
    }
}

@Composable
fun IconBox(
    icon: AppIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    face: Color? = null,
    size: Dp = 40.dp,
) {
    val p = Retro.p
    val fill = face ?: p.surface
    Box(
        modifier.size(size + 3.dp).sticker(fill, 6.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIconView(icon, p.onFace(fill), 20.dp)
    }
}

/** Small selectable chip. Selected = mustard face. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = Retro.p
    val fill = if (selected) p.mustard else p.variant
    Box(
        modifier.sticker(fill, 6.dp, shadow = false).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        RText(text, color = p.onFace(fill), size = 13.sp, bold = selected, maxLines = 1)
    }
}

@Composable
fun Tag(text: String, face: Color, modifier: Modifier = Modifier) {
    val p = Retro.p
    Box(modifier.sticker(face, 4.dp, shadow = false).padding(horizontal = 6.dp, vertical = 2.dp)) {
        RText(text.uppercase(), color = p.onFace(face), size = 12.sp, bold = true, maxLines = 1)
    }
}

// ------------------------------------------------------------------ cards

@Composable
fun RetroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    fill: Color? = null,
    selected: Boolean = false,
    padding: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = Retro.p
    var m = Modifier.fillMaxWidth().then(modifier).sticker(fill ?: p.surface, 12.dp, selected = selected)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(padding), content = content)
}

// ------------------------------------------------------------------ tabs

@Composable
fun <T> RetroTabs(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = Retro.p
    val cyber = Retro.cyber
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (cyber) 8.dp else 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        options.forEach { (value, label) ->
            val sel = value == selected
            val fill = if (sel) p.mustard else p.variant
            val shape = if (cyber) rShape(8.dp) else topShape(8.dp)
            Box(
                Modifier
                    .weight(1f)
                    .sticker(fill, shadow = false, shape = shape)
                    .clickable { onSelect(value) }
                    .padding(vertical = if (sel) 12.dp else 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                RText(label.uppercase(), color = p.onFace(fill), bold = sel, size = 12.sp, maxLines = 1)
            }
        }
    }
}

// ------------------------------------------------------------------ switch, field

/**
 * A slider drawn as chunky blocks in an outlined track: lit blocks in the accent, unlit ones in the
 * card colour. Tap or drag; the value snaps to whole blocks (value runs 0..1).
 */
@Composable
fun RetroSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    segments: Int = 10,
) {
    val p = Retro.p
    var width by remember { mutableIntStateOf(0) }
    val lit = (value.coerceIn(0f, 1f) * segments).roundToInt()

    fun pick(x: Float) {
        if (width <= 0) return
        val steps = ((x / width).coerceIn(0f, 1f) * segments).roundToInt()
        onValueChange(steps / segments.toFloat())
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(22.dp)
            .sticker(p.variant, 4.dp, shadow = false)
            .onSizeChanged { width = it.width }
            .pointerInput(segments) { detectTapGestures { pick(it.x) } }
            .pointerInput(segments) {
                detectHorizontalDragGestures(onDragStart = { pick(it.x) }) { change, _ ->
                    change.consume()
                    pick(change.position.x)
                }
            }
            .padding(3.dp),
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(segments) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (i < lit) Retro.primary else p.surface),
                )
            }
        }
    }
}

@Composable
fun RetroSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = Retro.p
    val track = if (checked) p.green else p.variant
    Box(
        Modifier
            .width(52.dp)
            .height(28.dp)
            .sticker(track, 4.dp, shadow = false)
            .clickable { onChange(!checked) }
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .size(18.dp)
                .sticker(p.mustard, 2.dp, shadow = false),
        )
    }
}

@Composable
fun RetroField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String = "",
    password: Boolean = false,
) {
    val p = Retro.p
    Column(modifier.fillMaxWidth()) {
        RText(label.uppercase(), size = 12.sp, color = p.onVariant, bold = true)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.replace("\n", "")) },
            singleLine = true,
            textStyle = TextStyle(color = Retro.text, fontFamily = Retro.family, fontSize = 15.sp),
            cursorBrush = SolidColor(Retro.primary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (password) {
                androidx.compose.ui.text.input.PasswordVisualTransformation()
            } else {
                androidx.compose.ui.text.input.VisualTransformation.None
            },
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .sticker(p.variant, 8.dp, shadow = false)
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        RText(placeholder, color = p.onVariant, size = 14.sp)
                    }
                    inner()
                }
            },
        )
    }
}

// ------------------------------------------------------------------ dialogs and sheets

@Composable
fun RetroDialog(
    title: String,
    onDismiss: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = Retro.p
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .sticker(p.surface, 14.dp)
                .padding(16.dp),
        ) {
            RText(title.uppercase(), size = 16.sp, bold = true)
            Spacer(Modifier.height(8.dp))
            DashedDivider()
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    RetroDialog(
        title = title,
        onDismiss = onDismiss,
        actions = {
            RetroButton("Cancel", onDismiss, face = Retro.p.cream)
            RetroButton(confirmLabel, onConfirm, face = Retro.p.red)
        },
    ) {
        RText(text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetroSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val p = Retro.p
    val sheetShape = topShape(14.dp)
    val outline = p.outline
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = p.surface,
        contentColor = Retro.text,
        tonalElevation = 0.dp,
        shape = sheetShape,
        // The 2 wide outline is drawn from the handle slot as a very tall shape. The sheet clips it to
        // its own corners, so it runs down both sides without drawing a second frame around the sheet.
        dragHandle = {
            Box(
                Modifier.fillMaxWidth().drawBehind {
                    val tall = sheetShape.createOutline(Size(size.width, size.height + 4000f), layoutDirection, this)
                    drawOutline(tall, outline, style = Stroke(width = 4.dp.toPx()))
                },
                contentAlignment = Alignment.TopCenter,
            ) { Grip() }
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/**
 * A bottom sheet that picks one of a few options: title, hint, a dashed rule, then one row per
 * option with a tick on the current one. [row] draws the option itself (so a font can show in its own face).
 */
@Composable
fun <T> OptionSheet(
    title: String,
    subtitle: String,
    options: List<T>,
    selected: T,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
    row: @Composable (T) -> Unit,
) {
    val p = Retro.p
    RetroSheet(onDismiss) {
        Column {
            RText(title, size = 18.sp, bold = true)
            RText(subtitle, size = 12.sp, color = p.onVariant)
        }
        DashedDivider()
        Column {
            options.forEach { o ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(o) }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) { row(o) }
                    if (o == selected) AppIconView(AppIcon.CHECK, p.green, 22.dp)
                }
            }
        }
    }
}

/** Three small mustard squares: the sheet drag handle. */
@Composable
private fun Grip() {
    val p = Retro.p
    Row(
        Modifier.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(3) { Box(Modifier.size(8.dp).background(p.mustard)) }
    }
}

@Composable
fun DashedDivider(modifier: Modifier = Modifier) {
    val c = Retro.p.outline
    Canvas(modifier.fillMaxWidth().height(2.dp)) {
        drawLine(
            color = c,
            start = Offset(0f, 1f),
            end = Offset(size.width, 1f),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
        )
    }
}

// ------------------------------------------------------------------ progress

/** Chunky segmented bar. No gradients. */
@Composable
fun BlockBar(fraction: Float, color: Color, modifier: Modifier = Modifier, blocks: Int = 20) {
    val p = Retro.p
    val filled = (fraction.coerceIn(0f, 1f) * blocks).roundToInt()
    Box(modifier.fillMaxWidth().height(18.dp).sticker(p.variant, 4.dp, shadow = false).padding(3.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val gap = 2.dp.toPx()
            val w = (size.width - gap * (blocks - 1)) / blocks
            for (i in 0 until filled) {
                drawRect(color, Offset(i * (w + gap), 0f), Size(w, size.height))
            }
        }
    }
}

// ------------------------------------------------------------------ skeleton

private val StepEasing = Easing { f -> floor(f * 24f) / 24f }

/** Loading placeholder: flat fill with a highlight band that hops in 24 steps. */
@Composable
fun Skeleton(modifier: Modifier = Modifier, height: Dp = 64.dp) {
    val p = Retro.p
    val t = rememberInfiniteTransition(label = "skeleton")
    val step by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = StepEasing), RepeatMode.Restart),
        label = "skeletonStep",
    )
    Box(modifier.fillMaxWidth().height(height).sticker(p.variant, 8.dp, shadow = false)) {
        Canvas(Modifier.fillMaxSize()) {
            val band = size.width * 0.25f
            val x = (size.width + band) * step - band
            drawRect(p.onVariant.copy(alpha = 0.2f), Offset(x, 0f), Size(band, size.height))
        }
    }
}

// ------------------------------------------------------------------ layout helpers

@Composable
fun ScreenTitle(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconBox(AppIcon.BACK, onBack)
            Spacer(Modifier.width(12.dp))
        }
        RText(title.uppercase(), Modifier.weight(1f), size = 18.sp, bold = true, maxLines = 1)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipFlow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

/** Square colour swatch for a category (hex string like #F59E0B). */
@Composable
fun ColorDot(hex: String?, modifier: Modifier = Modifier, size: Dp = 14.dp) {
    val c = parseHex(hex, Retro.p.onVariant)
    Box(modifier.size(size).sticker(c, 2.dp, shadow = false))
}

fun colorToHex(c: Color): String = String.format("#%06X", c.toArgb() and 0xFFFFFF)

/** Twelve colours drawn from the active theme (its accents and blends of them), for category swatches. */
@Composable
fun themeSwatches(): List<String> {
    val p = Retro.p
    val dark = Retro.dark
    val a = listOf(p.red, p.green, p.mustard, p.primary(dark), p.tertiary(dark))
    val mixes = listOf(
        lerp(p.red, p.mustard, 0.5f), lerp(p.green, p.mustard, 0.5f), lerp(p.red, p.green, 0.5f),
        lerp(p.red, p.cream, 0.4f), lerp(p.green, p.cream, 0.4f), lerp(p.mustard, p.cream, 0.4f),
        lerp(p.red, p.ink, 0.3f), lerp(p.green, p.ink, 0.3f),
    )
    return (a + mixes).map { colorToHex(it) }.distinct().take(12)
}

fun parseHex(hex: String?, fallback: Color): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
