package com.oryno.piggy_ledger.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryWidgetProviderTest {

    @Test
    fun formatWidgetNumber_formatsWithoutCurrencySymbols() {
        val zero = WidgetUtils.formatWidgetNumber(0.0)
        assertEquals("0", zero)

        val small = WidgetUtils.formatWidgetNumber(450.0)
        assertEquals("450", small)

        val thousand = WidgetUtils.formatWidgetNumber(1250.0)
        assertEquals("1,250", thousand)

        val large = WidgetUtils.formatWidgetNumber(1500000.0)
        assertEquals("1,500,000", large)

        // Ensure no currency symbols appear
        assertFalse(zero.contains("$"))
        assertFalse(zero.contains("EGP"))
        assertFalse(thousand.contains("$"))
        assertFalse(thousand.contains("EGP"))
        assertFalse(large.contains("$"))
        assertFalse(large.contains("EGP"))
    }

    @Test
    fun getAdaptiveTextSize_decreasesAsNumberLengthGrows() {
        // Short numbers get full size (20sp)
        val size1 = SummaryWidgetProvider.getAdaptiveTextSize("0".length)
        val size4 = SummaryWidgetProvider.getAdaptiveTextSize("9999".length)
        assertEquals(20f, size1)
        assertEquals(20f, size4)

        // Mid-sized numbers scale down smoothly
        val size6 = SummaryWidgetProvider.getAdaptiveTextSize("15,000".length)
        assertEquals(17.5f, size6)
        assertTrue(size6 < size4)

        val size8 = SummaryWidgetProvider.getAdaptiveTextSize("150,000".length)
        assertEquals(15f, size8)
        assertTrue(size8 < size6)

        val size10 = SummaryWidgetProvider.getAdaptiveTextSize("-1,500,000".length)
        assertEquals(13f, size10)
        assertTrue(size10 < size8)

        val size12 = SummaryWidgetProvider.getAdaptiveTextSize("-150,000,000".length)
        assertEquals(11.5f, size12)
        assertTrue(size12 < size10)

        // Very long numbers scale to minimum readable size
        val size14 = SummaryWidgetProvider.getAdaptiveTextSize("1,000,000,000".length)
        assertEquals(10f, size14)
        assertTrue(size14 < size12)
    }
}
