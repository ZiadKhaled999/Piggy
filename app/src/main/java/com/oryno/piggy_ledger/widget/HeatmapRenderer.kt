package com.oryno.piggy_ledger.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

object HeatmapRenderer {

    // Black widget theme: dark charcoal empty tile + Piggy Ledger PinkPrimary (#F43F5E) gradient
    const val EMPTY_TILE: Int = 0xFF1B1B1F.toInt()
    const val ACCENT_PINK: Int = 0xFFF43F5E.toInt()
    private const val MAX_BITMAP_BYTES: Long = 350_000L

    data class Geometry(
        val columns: Int,
        val rows: Int,
        val cell: Float,
        val gap: Float
    )

    fun computeGeometry(widthPx: Int, heightPx: Int, density: Float): Geometry {
        val safeDensity = density.coerceAtLeast(0.5f)
        val gapDp = 3f
        val gap = gapDp * safeDensity
        val rows = 7 // GitHub-style: one row per weekday

        // Drive cell size from available height so boxes fill the card height cleanly
        // while horizontal resizing adds or removes week columns smoothly.
        val cellFromHeight = ((heightPx - gap * (rows - 1)) / rows)
            .coerceAtLeast(6f * safeDensity)

        val columns = ((widthPx + gap) / (cellFromHeight + gap))
            .toInt()
            .coerceAtLeast(1)

        val cell = minOf(
            (widthPx - gap * (columns - 1)) / columns,
            (heightPx - gap * (rows - 1)) / rows
        ).coerceAtLeast(1f)

        return Geometry(columns = columns, rows = rows, cell = cell, gap = gap)
    }

    fun levelFor(count: Int, allCounts: List<Int>): Int {
        if (count <= 0) return 0
        val max = (allCounts.maxOrNull() ?: 0).coerceAtLeast(1)
        val ratio = ln(1.0 + count) / ln(1.0 + max)
        return (ratio * 4).roundToInt().coerceIn(1, 4)
    }

    fun colorForLevel(
        level: Int,
        accent: Int = ACCENT_PINK,
        emptyTile: Int = EMPTY_TILE
    ): Int {
        if (level <= 0) return emptyTile
        val from = argbToHsv(emptyTile)
        val to = argbToHsv(accent)
        val f = (level.coerceIn(0, 4)) / 4f
        return hsvToArgb(
            hue = to[0],                              // keep Piggy Ledger pink hue
            saturation = from[1] + (to[1] - from[1]) * f,
            value = from[2] + (to[2] - from[2]) * f
        )
    }

    internal fun argbToHsv(color: Int): FloatArray {
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f

        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        val hue = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta) % 6f)
            max == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }.let { if (it < 0f) it + 360f else it }

        val saturation = if (max == 0f) 0f else delta / max
        return floatArrayOf(hue, saturation, max)
    }

    internal fun hsvToArgb(hue: Float, saturation: Float, value: Float): Int {
        val h = ((hue % 360f) + 360f) % 360f
        val s = saturation.coerceIn(0f, 1f)
        val v = value.coerceIn(0f, 1f)

        val c = v * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = v - c

        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val r = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun renderHeatmap(
        density: Float,
        days: List<Int>,
        widthPx: Int,
        heightPx: Int,
        accent: Int = ACCENT_PINK,
        emptyTile: Int = EMPTY_TILE
    ): Bitmap {
        val safeWidth = widthPx.coerceAtLeast(16)
        val safeHeight = heightPx.coerceAtLeast(16)

        // Keep bitmap under Binder/RemoteViews IPC limit while preserving aspect ratio & square cells
        val requestedBytes = safeWidth.toLong() * safeHeight.toLong() * 4L
        val scale = if (requestedBytes > MAX_BITMAP_BYTES) {
            sqrt(MAX_BITMAP_BYTES.toDouble() / requestedBytes.toDouble()).toFloat()
        } else {
            1f
        }

        val bitmapWidth = (safeWidth * scale).roundToInt().coerceAtLeast(16)
        val bitmapHeight = (safeHeight * scale).roundToInt().coerceAtLeast(16)
        val effectiveDensity = density.coerceAtLeast(0.5f) * scale

        val geo = computeGeometry(bitmapWidth, bitmapHeight, effectiveDensity)
        val total = geo.columns * geo.rows
        val visible = days.takeLast(total)       // oldest -> newest, today last
        val startIndex = total - visible.size    // partial first week stays blank

        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val corner = geo.cell * 0.25f

        val gridWidth = geo.columns * geo.cell + (geo.columns - 1) * geo.gap
        val gridHeight = geo.rows * geo.cell + (geo.rows - 1) * geo.gap
        val leftOffset = ((bitmapWidth - gridWidth) / 2f).coerceAtLeast(0f)
        val topOffset = ((bitmapHeight - gridHeight) / 2f).coerceAtLeast(0f)

        // Draw any leading partial-week cells as empty tiles so grid alignment stays clean
        for (idx in 0 until startIndex) {
            val col = idx / geo.rows
            val row = idx % geo.rows
            val left = leftOffset + col * (geo.cell + geo.gap)
            val top = topOffset + row * (geo.cell + geo.gap)
            paint.color = emptyTile
            canvas.drawRoundRect(
                left,
                top,
                left + geo.cell,
                top + geo.cell,
                corner,
                corner,
                paint
            )
        }

        for (i in visible.indices) {
            val idx = startIndex + i
            val col = idx / geo.rows
            val row = idx % geo.rows
            val left = leftOffset + col * (geo.cell + geo.gap)
            val top = topOffset + row * (geo.cell + geo.gap)
            paint.color = colorForLevel(levelFor(visible[i], visible), accent, emptyTile)
            canvas.drawRoundRect(
                left,
                top,
                left + geo.cell,
                top + geo.cell,
                corner,
                corner,
                paint
            )
        }

        return bitmap
    }

    fun renderLegend(
        density: Float,
        accent: Int = ACCENT_PINK,
        emptyTile: Int = EMPTY_TILE
    ): Bitmap {
        val safeDensity = density.coerceAtLeast(0.5f).coerceAtMost(2.5f)
        val cell = 7f * safeDensity
        val gap = 2.5f * safeDensity
        val count = 5
        val width = (count * cell + (count - 1) * gap).roundToInt().coerceAtLeast(10)
        val height = cell.roundToInt().coerceAtLeast(4)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val corner = cell * 0.25f

        for (level in 0 until count) {
            val left = level * (cell + gap)
            paint.color = colorForLevel(level, accent, emptyTile)
            canvas.drawRoundRect(left, 0f, left + cell, cell, corner, corner, paint)
        }
        return bitmap
    }
}
