package com.lorenzo.monefyme.domain

data class CsvImportResult(
    val processedRows: Int,
    val insertedRows: Int,
    val duplicateRows: Int,
    val invalidRows: Int,
)
