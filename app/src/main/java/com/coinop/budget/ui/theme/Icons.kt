package com.coinop.budget.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.AccountBalance
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material.icons.sharp.Check
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material.icons.sharp.ExitToApp
import androidx.compose.material.icons.sharp.Home
import androidx.compose.material.icons.sharp.KeyboardArrowDown
import androidx.compose.material.icons.sharp.KeyboardArrowRight
import androidx.compose.material.icons.sharp.KeyboardArrowUp
import androidx.compose.material.icons.sharp.PieChart
import androidx.compose.material.icons.sharp.Refresh
import androidx.compose.material.icons.sharp.Remove
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material.icons.sharp.SwapHoriz
import androidx.compose.material.icons.sharp.Tune
import androidx.compose.material.icons.sharp.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

enum class AppIcon {
    HOME, BANK, INVEST, BUDGET, SETTINGS, PLUS, MINUS, SWAP, TRASH, EDIT, CHECK, CLOSE,
    BACK, CHEVRON, UP, DOWN, REFRESH, SEARCH, LOGOUT
}

/** Two icon families behind one name: 16x16 pixel grid (Arcadia), angular line icons (Cyberpunk). */
@Composable
fun AppIconView(icon: AppIcon, tint: Color, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    if (Retro.cyber) {
        Icon(imageVector = sharpFor(icon), contentDescription = null, tint = tint, modifier = modifier.size(size))
    } else {
        val grid = remember(icon) { PixelIcons.grid(icon) }
        // centre the lit pixels, not the 16x16 grid, so every icon sits in the middle of its box
        val bounds = remember(grid) { litBounds(grid) }
        Canvas(modifier.size(size)) {
            val cell = this.size.minDimension / 16f
            val dx = kotlin.math.round((16 - (bounds[0] + bounds[1] + 1)) / 2f * cell)
            val dy = kotlin.math.round((16 - (bounds[2] + bounds[3] + 1)) / 2f * cell)
            for (y in 0 until 16) for (x in 0 until 16) {
                if (grid[y][x]) {
                    // +0.5 avoids hairline seams between cells
                    drawRect(tint, Offset(x * cell + dx, y * cell + dy), Size(cell + 0.5f, cell + 0.5f))
                }
            }
        }
    }
}

private fun sharpFor(i: AppIcon): ImageVector = when (i) {
    AppIcon.HOME -> Icons.Sharp.Home
    AppIcon.BANK -> Icons.Sharp.AccountBalance
    AppIcon.INVEST -> Icons.Sharp.TrendingUp
    AppIcon.BUDGET -> Icons.Sharp.PieChart
    AppIcon.SETTINGS -> Icons.Sharp.Tune
    AppIcon.PLUS -> Icons.Sharp.Add
    AppIcon.MINUS -> Icons.Sharp.Remove
    AppIcon.SWAP -> Icons.Sharp.SwapHoriz
    AppIcon.TRASH -> Icons.Sharp.Delete
    AppIcon.EDIT -> Icons.Sharp.Edit
    AppIcon.CHECK -> Icons.Sharp.Check
    AppIcon.CLOSE -> Icons.Sharp.Close
    AppIcon.BACK -> Icons.Sharp.ArrowBack
    AppIcon.CHEVRON -> Icons.Sharp.KeyboardArrowRight
    AppIcon.UP -> Icons.Sharp.KeyboardArrowUp
    AppIcon.DOWN -> Icons.Sharp.KeyboardArrowDown
    AppIcon.REFRESH -> Icons.Sharp.Refresh
    AppIcon.SEARCH -> Icons.Sharp.Search
    AppIcon.LOGOUT -> Icons.Sharp.ExitToApp
}

/** minX, maxX, minY, maxY of the lit cells (all zero for an empty grid). */
private fun litBounds(grid: Array<BooleanArray>): IntArray {
    var minX = 16; var maxX = -1; var minY = 16; var maxY = -1
    for (y in 0 until 16) for (x in 0 until 16) {
        if (grid[y][x]) {
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
    }
    return if (maxX < 0) intArrayOf(0, 15, 0, 15) else intArrayOf(minX, maxX, minY, maxY)
}

/** Pixel art drawn from code: no image assets. */
private class Px {
    val g = Array(16) { BooleanArray(16) }

    fun set(x: Int, y: Int, v: Boolean = true) {
        if (x in 0..15 && y in 0..15) g[y][x] = v
    }

    fun rect(x0: Int, y0: Int, x1: Int, y1: Int, v: Boolean = true) {
        for (y in y0..y1) for (x in x0..x1) set(x, y, v)
    }

    /** 2 wide frame */
    fun frame(x0: Int, y0: Int, x1: Int, y1: Int) {
        rect(x0, y0, x1, y1)
        rect(x0 + 2, y0 + 2, x1 - 2, y1 - 2, false)
    }

    /** Bresenham line drawn with a 2x2 brush. */
    fun line(x0: Int, y0: Int, x1: Int, y1: Int) {
        var x = x0
        var y = y0
        val dx = abs(x1 - x0)
        val dy = -abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        while (true) {
            rect(x, y, x + 1, y + 1)
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 >= dy) { err += dy; x += sx }
            if (e2 <= dx) { err += dx; y += sy }
        }
    }
}

private object PixelIcons {
    private val cache = HashMap<AppIcon, Array<BooleanArray>>()

    fun grid(icon: AppIcon): Array<BooleanArray> = cache.getOrPut(icon) { build(icon).g }

    private fun build(icon: AppIcon): Px = Px().apply {
        when (icon) {
            AppIcon.HOME -> {
                line(7, 1, 1, 7); line(8, 1, 14, 7)
                rect(3, 8, 12, 14); rect(6, 10, 9, 14, false)
            }
            AppIcon.BANK -> {
                line(7, 1, 1, 6); line(8, 1, 14, 6)
                rect(1, 7, 14, 7)
                rect(3, 9, 4, 12); rect(7, 9, 8, 12); rect(11, 9, 12, 12)
                rect(1, 13, 14, 14)
            }
            AppIcon.INVEST -> {
                rect(1, 11, 3, 14); rect(5, 8, 7, 14); rect(9, 5, 11, 14); rect(13, 2, 15, 14)
            }
            AppIcon.BUDGET -> {
                frame(1, 1, 14, 14)
                rect(4, 4, 11, 5); rect(4, 7, 9, 8); rect(4, 10, 10, 11)
            }
            AppIcon.SETTINGS -> {
                rect(1, 3, 14, 4); rect(4, 1, 7, 6)
                rect(1, 8, 14, 9); rect(9, 6, 12, 11)
                rect(1, 13, 14, 14); rect(5, 11, 8, 15)
            }
            AppIcon.PLUS -> { rect(6, 2, 9, 13); rect(2, 6, 13, 9) }
            AppIcon.MINUS -> rect(2, 6, 13, 9)
            AppIcon.SWAP -> {
                rect(1, 4, 12, 5); line(9, 1, 12, 4); line(9, 8, 12, 5)
                rect(3, 10, 14, 11); line(6, 7, 3, 10); line(6, 14, 3, 11)
            }
            AppIcon.TRASH -> {
                rect(2, 3, 13, 4); rect(6, 1, 9, 2)
                rect(3, 6, 12, 14); rect(5, 8, 6, 13, false); rect(9, 8, 10, 13, false)
            }
            AppIcon.EDIT -> { line(3, 13, 12, 4); rect(12, 2, 14, 4) }
            AppIcon.CHECK -> { line(2, 8, 6, 12); line(6, 12, 13, 3) }
            AppIcon.CLOSE -> { line(3, 3, 12, 12); line(12, 3, 3, 12) }
            AppIcon.BACK -> { rect(4, 7, 14, 8); line(8, 3, 4, 7); line(8, 12, 4, 8) }
            AppIcon.CHEVRON -> { line(5, 2, 10, 7); line(10, 8, 5, 13) }
            AppIcon.UP -> { line(2, 10, 7, 5); line(8, 5, 13, 10) }
            AppIcon.DOWN -> { line(2, 5, 7, 10); line(8, 10, 13, 5) }
            AppIcon.REFRESH -> {
                rect(4, 2, 10, 3); rect(2, 4, 3, 11); rect(4, 12, 11, 13); rect(12, 8, 13, 11)
                line(9, 0, 12, 3); line(9, 6, 12, 3)
            }
            AppIcon.SEARCH -> { frame(1, 1, 9, 9); line(9, 9, 14, 14) }
            AppIcon.LOGOUT -> {
                rect(2, 2, 3, 13); rect(2, 2, 8, 3); rect(2, 12, 8, 13)
                rect(6, 7, 14, 8); line(11, 4, 14, 7); line(11, 11, 14, 8)
            }
        }
    }
}
