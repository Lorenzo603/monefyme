package com.lorenzo.monefyme.ui.util

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyDisplayTest {
    @Test
    fun fractionalPartIsRenderedInSmallerFont() {
        val baseFontSize = 24.sp
        val annotated = currencyAnnotatedString("€123.45", baseFontSize)

        assertEquals("€123.45", annotated.text)
        assertEquals(1, annotated.spanStyles.size)

        val span = annotated.spanStyles.single()
        assertEquals(4, span.start)
        assertEquals(annotated.text.length, span.end)
        assertEquals(baseFontSize * currencyCentsFontSizeScale, span.item.fontSize)
    }

    @Test
    fun centsScaleIsConfigurable() {
        val annotated = currencyAnnotatedString("12.34", 20.sp, centsScale = 0.5f)

        assertEquals(0.5f, annotated.spanStyles.single().item.fontSize.value / 20f, 0.0001f)
    }

    @Test
    fun textWithoutDecimalPartStaysPlain() {
        val annotated = currencyAnnotatedString("€100", 24.sp)

        assertEquals("€100", annotated.text)
        assertEquals(0, annotated.spanStyles.size)
    }

    @Test
    fun unspecifiedBaseSizeKeepsEverythingAtDefaultStyle() {
        val annotated = currencyAnnotatedString("€123.45", TextUnit.Unspecified)

        assertEquals("€123.45", annotated.text)
        assertEquals(0, annotated.spanStyles.size)
    }
}
