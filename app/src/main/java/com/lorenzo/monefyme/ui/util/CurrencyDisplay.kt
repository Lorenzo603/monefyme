package com.lorenzo.monefyme.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit

/** Default ratio between the cents font size and the size of the main amount. */
const val currencyCentsFontSizeScale: Float = 0.6f

/**
 * Builds the text of a currency value with its decimal part (cents) in a smaller font than the
 * main part, e.g. "€123.45" renders "45" much smaller than "123".
 *
 * Only the font size of the fractional part is overridden, so colours, weights and the rest of the
 * [androidx.compose.material3.Text] style still apply.
 *
 * @param text Value as displayed (including any sign and currency symbol), as produced by
 * [centsToDisplay].
 * @param baseFontSize Font size used for the whole part; the cents use [centsScale] of it.
 * @param centsScale Ratio between the cents font size and [baseFontSize].
 */
fun currencyAnnotatedString(
    text: String,
    baseFontSize: TextUnit,
    centsScale: Float = currencyCentsFontSizeScale,
): AnnotatedString {
    val decimalIdx = text.lastIndexOf('.')
    if (decimalIdx < 0 || baseFontSize == TextUnit.Unspecified) return AnnotatedString(text)

    return buildAnnotatedString {
        append(text.substring(0, decimalIdx))
        withStyle(SpanStyle(fontSize = baseFontSize * centsScale)) {
            append(text.substring(decimalIdx))
        }
    }
}
