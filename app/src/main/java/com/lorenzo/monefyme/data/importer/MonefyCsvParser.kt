package com.lorenzo.monefyme.data.importer

import java.io.InputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

private val monefyDateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("dd/MM/uuuu", Locale.US)
    .withResolverStyle(ResolverStyle.STRICT)

data class ParsedCsvRow(
    val date: LocalDate,
    val account: String,
    val category: String,
    val amount: BigDecimal,
    val currency: String,
    val description: String,
)

data class CsvParseResult(
    val rows: List<ParsedCsvRow>,
    val invalidRows: Int,
)

class MonefyCsvParser {
    fun parse(inputStream: InputStream): CsvParseResult {
        var invalidRows = 0
        val parsedRows = buildList {
            inputStream.bufferedReader().useLines { lines ->
                lines.drop(1).forEach { line ->
                    if (line.isBlank()) {
                        return@forEach
                    }

                    val fields = parseCsvLine(line)
                    if (fields.size < 8) {
                        invalidRows += 1
                        return@forEach
                    }

                    val date = fields[0].trim().takeIf { it.isNotEmpty() }?.runCatching {
                        LocalDate.parse(this, monefyDateFormatter)
                    }?.getOrNull()

                    val category = fields[2].trim()
                    val amount = fields[3].trim().replace(',', '.').takeIf { it.isNotEmpty() }?.runCatching {
                        BigDecimal(this)
                    }?.getOrNull()
                    val currency = fields[4].trim().ifEmpty { "EUR" }
                    val account = fields[1].trim()
                    val description = fields[7].trim()

                    if (date == null || amount == null || category.isBlank()) {
                        invalidRows += 1
                        return@forEach
                    }

                    add(
                        ParsedCsvRow(
                            date = date,
                            account = account,
                            category = category,
                            amount = amount,
                            currency = currency,
                            description = description,
                        ),
                    )
                }
            }
        }

        return CsvParseResult(
            rows = parsedRows,
            invalidRows = invalidRows,
        )
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && inQuotes && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index += 1
                }

                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result += current.toString()
                    current.clear()
                }

                else -> current.append(char)
            }
            index += 1
        }

        result += current.toString()
        return result
    }
}
