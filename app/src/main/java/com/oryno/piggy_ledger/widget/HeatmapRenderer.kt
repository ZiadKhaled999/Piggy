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
    private const val ROWS: Int = 7
    private const val REFERENCE_COLUMNS: Int = 15
    private const val GAP_RATIO: Float = 0.28f
    private const val MIN_CELL_DP: Float = 11.2f
    private const val WIDE_MIN_CELL_DP: Float = 17.5f
    private const val MAX_CELL_TARGET_DP: Float = 24f
    private const val MAX_COLUMNS: Int = 20

    data class Geometry(
        val columns: Int,
        val rows: Int,
        val cell: Float,
        val gap: Float
    )

    fun computeGeometry(widthPx: Int, heightPx: Int, density: Float): Geometry {
        val safeDensity = density.coerceAtLeast(0.5f)
        val safeWidthPx = widthPx.coerceAtLeast(16).toFloat()
        val safeHeightPx = heightPx.coerceAtLeast(16).toFloat()

        val widthDp = safeWidthPx / safeDensity
        val heightDp = safeHeightPx / safeDensity

        // Enforce a minimum reference pitch so shorter widgets don't cram tiny pixels horizontally,
        // and keep 15x7 proportions across standard 4x2 phone widget sizes (260dp-360dp wide).
        val rowUnits = ROWS + (ROWS - 1) * GAP_RATIO
        val maxCellFromHeightDp = (heightDp / rowUnits).coerceAtMost(MAX_CELL_TARGET_DP)
        val basePitchCellDp = maxOf(maxCellFromHeightDp, MIN_CELL_DP)
        val rawColumns = ((widthDp / basePitchCellDp + GAP_RATIO) / (1f + GAP_RATIO))
            .toInt()
            .coerceAtLeast(1)

        // Only expand past 15 columns when the widget is genuinely wide enough (e.g. tablets)
        // to sustain spacious tiles without looking cluttered.
        val columns = if (rawColumns <= REFERENCE_COLUMNS) {
            rawColumns
        } else {
            val widePitchCellDp = maxOf(basePitchCellDp, WIDE_MIN_CELL_DP)
            ((widthDp / widePitchCellDp + GAP_RATIO) / (1f + GAP_RATIO))
                .toInt()
                .coerceIn(REFERENCE_COLUMNS, MAX_COLUMNS)
        }

        val cellByWidth = safeWidthPx / (columns + (columns - 1) * GAP_RATIO)
        val cellByHeight = safeHeightPx / rowUnits
        val cell = minOf(cellByWidth, cellByHeight).coerceAtLeast(1f)
        val gap = cell * GAP_RATIO

        return Geometry(columns = columns, rows = ROWS, cell = cell, gap = gap)
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
        emptyTile: Int = EMPTY_TILE,
        maxBitmapBytes: Long = MAX_BITMAP_BYTES
    ): Bitmap {
        val safeWidth = widthPx.coerceAtLeast(16)
        val safeHeight = heightPx.coerceAtLeast(16)
        val safeMaxBytes = maxBitmapBytes.coerceAtLeast(64_000L)

        // Keep bitmap under Binder/RemoteViews IPC limit while preserving aspect ratio & square cells
        val requestedBytes = safeWidth.toLong() * safeHeight.toLong() * 4L
        val scale = if (requestedBytes > safeMaxBytes) {
            sqrt(safeMaxBytes.toDouble() / requestedBytes.toDouble()).toFloat()
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
        val corner = geo.cell * 0.22f

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
