package com.oryno.piggy_ledger.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapRendererTest {

    @Test
    fun computeGeometry_horizontalResizeScalesColumnsWhileKeepingSquareCells() {
        val density = 2f
        val fixedHeightPx = (108 * density).toInt()
        val narrowGeo = HeatmapRenderer.computeGeometry(
            widthPx = (120 * density).toInt(),
            heightPx = fixedHeightPx,
            density = density
        )
        val mediumGeo = HeatmapRenderer.computeGeometry(
            widthPx = (240 * density).toInt(),
            heightPx = fixedHeightPx,
            density = density
        )
        val wideGeo = HeatmapRenderer.computeGeometry(
            widthPx = (360 * density).toInt(),
            heightPx = fixedHeightPx,
            density = density
        )
        assertEquals(7, narrowGeo.rows)
        assertEquals(7, mediumGeo.rows)
        assertEquals(7, wideGeo.rows)
        assertTrue(narrowGeo.columns < mediumGeo.columns)
        assertTrue(mediumGeo.columns < wideGeo.columns)
        assertTrue(narrowGeo.cell > 8f && mediumGeo.cell > 8f && wideGeo.cell > 8f)
        // Cells stay crisp & uniform across horizontal resize
        assertEquals(mediumGeo.cell, wideGeo.cell, 0.01f)
    }

    @Test
    fun levelFor_usesLogarithmicScalingWithoutSingleDaySaturation() {
        val counts = listOf(0, 1, 2, 5, 10)
        assertEquals(0, HeatmapRenderer.levelFor(0, counts))
        assertEquals(1, HeatmapRenderer.levelFor(1, counts))
        assertEquals(2, HeatmapRenderer.levelFor(2, counts))
        assertEquals(3, HeatmapRenderer.levelFor(5, counts))
        assertEquals(4, HeatmapRenderer.levelFor(10, counts))
    }

    @Test
    fun colorForLevel_producesBlackThemePinkGradient() {
        val level0 = HeatmapRenderer.colorForLevel(0)
        val level1 = HeatmapRenderer.colorForLevel(1)
        val level2 = HeatmapRenderer.colorForLevel(2)
        val level3 = HeatmapRenderer.colorForLevel(3)
        val level4 = HeatmapRenderer.colorForLevel(4)

        assertEquals(HeatmapRenderer.EMPTY_TILE, level0)
        assertEquals(HeatmapRenderer.ACCENT_PINK, level4)

        val hsv1 = HeatmapRenderer.argbToHsv(level1)
        val hsv2 = HeatmapRenderer.argbToHsv(level2)
        val hsv3 = HeatmapRenderer.argbToHsv(level3)
        val hsv4 = HeatmapRenderer.argbToHsv(level4)
        val accentHsv = HeatmapRenderer.argbToHsv(HeatmapRenderer.ACCENT_PINK)

        // Keep Piggy Ledger pink hue across levels 1..4 while monotonically increasing saturation and value
        assertEquals(accentHsv[0], hsv1[0], 3.5f)
        assertEquals(accentHsv[0], hsv4[0], 1.5f)
        assertTrue(hsv1[1] < hsv2[1] && hsv2[1] < hsv3[1] && hsv3[1] < hsv4[1])
        assertTrue(hsv1[2] < hsv2[2] && hsv2[2] < hsv3[2] && hsv3[2] < hsv4[2])
    }
}
