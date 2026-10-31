package com.lorenzo.monefyme.data.repository

import android.database.SQLException
import com.lorenzo.monefyme.data.importer.MonefyCsvParser
import com.lorenzo.monefyme.data.local.dao.CategoryDao
import com.lorenzo.monefyme.data.local.dao.TransactionDao
import com.lorenzo.monefyme.data.local.entity.CategoryEntity
import com.lorenzo.monefyme.data.local.entity.TransactionEntity
import com.lorenzo.monefyme.data.model.CategoryItem
import com.lorenzo.monefyme.data.model.ExpenseSlice
import com.lorenzo.monefyme.data.model.TransactionListItem
import com.lorenzo.monefyme.domain.CsvImportResult
import com.lorenzo.monefyme.domain.TransactionDraft
import com.lorenzo.monefyme.domain.TransactionType
import java.io.InputStream
import java.math.RoundingMode
import java.time.YearMonth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class BudgetRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val csvParser: MonefyCsvParser = MonefyCsvParser(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BudgetRepository {

    override fun observeCategoryNames(): Flow<List<String>> {
        return categoryDao.observeAll().map { categories ->
            categories.map { it.displayName }
        }
    }

    override fun observeCategories(): Flow<List<CategoryItem>> {
        return categoryDao.observeAll().map { entities ->
            entities.map { entity ->
                CategoryItem(
                    id = entity.id,
                    displayName = entity.displayName,
                    iconName = entity.iconName,
                    colorArgb = entity.colorArgb,
                )
            }
        }
    }

    override fun observeMonthTransactions(month: YearMonth): Flow<List<TransactionListItem>> {
        val (start, end) = month.toEpochRange()
        return transactionDao.observeByDateRange(start, end)
    }

    override fun observeMonthExpenseSlices(month: YearMonth): Flow<List<ExpenseSlice>> {
        val (start, end) = month.toEpochRange()
        return transactionDao.observeExpenseSlicesByDateRange(start, end)
    }

    override suspend fun saveCategory(displayName: String, iconName: String, colorArgb: Int): Boolean {
        return withContext(ioDispatcher) {
            val normalized = normalizeCategoryLookupName(displayName)
            val insertedId = categoryDao.insert(
                CategoryEntity(
                    normalizedName = normalized,
                    displayName = normalizeCategoryDisplayName(displayName),
                    iconName = iconName,
                    colorArgb = colorArgb,
                ),
            )
            insertedId != -1L
        }
    }

    override suspend fun saveTransaction(transactionId: Long?, draft: TransactionDraft): Boolean {
        if (draft.amountCents <= 0) {
            throw IllegalArgumentException("Amount must be greater than zero.")
        }
        if (draft.categoryName.isBlank()) {
            throw IllegalArgumentException("Category cannot be blank.")
        }

        return withContext(ioDispatcher) {
            val categoryId = resolveCategoryId(draft.categoryName)
            val entity = TransactionEntity(
                id = transactionId ?: 0L,
                dateEpochDay = draft.date.toEpochDay(),
                categoryId = categoryId,
                amountCents = draft.amountCents,
                type = draft.type.name,
                description = draft.description.trim(),
                account = draft.account.trim(),
                currency = draft.currency.trim().ifBlank { "EUR" },
            )

            if (transactionId == null) {
                transactionDao.insert(entity) != -1L
            } else {
                val existing = transactionDao.findById(transactionId) ?: return@withContext false
                transactionDao.update(entity.copy(id = existing.id))
                true
            }
        }
    }

    override suspend fun deleteTransaction(transactionId: Long) {
        withContext(ioDispatcher) {
            val existing = transactionDao.findById(transactionId) ?: return@withContext
            transactionDao.delete(existing)
        }
    }

    override suspend fun importCsv(inputStream: InputStream): CsvImportResult {
        return withContext(ioDispatcher) {
            val parseResult = csvParser.parse(inputStream)
            val categoryCache = mutableMapOf<String, Long>()
            var insertedRows = 0
            var duplicateRows = 0
            var invalidRows = parseResult.invalidRows

            parseResult.rows.forEach { row ->
                val sign = row.amount.signum()
                if (sign == 0) {
                    invalidRows += 1
                    return@forEach
                }

                val amountCents = runCatching {
                    row.amount
                        .abs()
                        .movePointRight(2)
                        .setScale(0, RoundingMode.HALF_UP)
                        .longValueExact()
                }.getOrElse {
                    invalidRows += 1
                    return@forEach
                }

                val transactionType = if (sign < 0) TransactionType.EXPENSE else TransactionType.INCOME
                val categoryId = resolveCategoryId(row.category, categoryCache)
                val insertedId = transactionDao.insert(
                    TransactionEntity(
                        dateEpochDay = row.date.toEpochDay(),
                        categoryId = categoryId,
                        amountCents = amountCents,
                        type = transactionType.name,
                        description = row.description.trim(),
                        account = row.account.trim(),
                        currency = row.currency.trim().ifBlank { "EUR" },
                    ),
                )

                if (insertedId == -1L) {
                    duplicateRows += 1
                } else {
                    insertedRows += 1
                }
            }

            CsvImportResult(
                processedRows = parseResult.rows.size + parseResult.invalidRows,
                insertedRows = insertedRows,
                duplicateRows = duplicateRows,
                invalidRows = invalidRows,
            )
        }
    }

    private suspend fun resolveCategoryId(
        rawCategoryName: String,
        cache: MutableMap<String, Long>? = null,
    ): Long {
        val displayName = normalizeCategoryDisplayName(rawCategoryName)
        val normalizedName = normalizeCategoryLookupName(displayName)

        cache?.get(normalizedName)?.let { return it }

        val existing = categoryDao.findByNormalizedName(normalizedName)
        if (existing != null) {
            cache?.set(normalizedName, existing.id)
            return existing.id
        }

        val insertedId = categoryDao.insert(
            CategoryEntity(
                normalizedName = normalizedName,
                displayName = displayName,
            ),
        )
        val resolvedId = if (insertedId == -1L) {
            categoryDao.findByNormalizedName(normalizedName)?.id
                ?: throw SQLException("Could not resolve category after insert conflict.")
        } else {
            insertedId
        }

        cache?.set(normalizedName, resolvedId)
        return resolvedId
    }

    private fun normalizeCategoryDisplayName(value: String): String {
        return value.trim().replace(Regex("\\s+"), " ")
    }

    private fun normalizeCategoryLookupName(value: String): String {
        return normalizeCategoryDisplayName(value).lowercase()
    }

    private fun YearMonth.toEpochRange(): Pair<Long, Long> {
        val start = atDay(1).toEpochDay()
        val end = atEndOfMonth().toEpochDay()
        return start to end
    }
}
