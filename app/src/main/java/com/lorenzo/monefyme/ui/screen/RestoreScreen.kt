package com.lorenzo.monefyme.ui.screen

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lorenzo.monefyme.domain.CsvImportResult
import com.lorenzo.monefyme.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun RestoreScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importResult by viewModel.lastImportResult.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val content = loadContent(context, uri)
            if (content == null) {
                viewModel.postMessage("Could not read selected CSV file.")
                return@launch
            }
            viewModel.importCsv(content)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = "Manual CSV restore",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select a Monefy CSV export to import historical transactions. " +
                "Re-import is idempotent using duplicate detection (date + category + amount + description + account).",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                launcher.launch(arrayOf("text/*", "text/csv", "application/csv"))
            },
            enabled = !isImporting,
        ) {
            Text("Select CSV file")
        }

        if (isImporting) {
            Spacer(modifier = Modifier.height(12.dp))
            CircularProgressIndicator()
        }

        if (importResult != null) {
            Spacer(modifier = Modifier.height(16.dp))
            ImportSummaryCard(importResult = importResult!!)
        }
    }
}

@Composable
private fun ImportSummaryCard(importResult: CsvImportResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Last import summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text("Processed: ${importResult.processedRows}")
            Text("Inserted: ${importResult.insertedRows}")
            Text("Duplicates: ${importResult.duplicateRows}")
            Text("Invalid: ${importResult.invalidRows}")
        }
    }
}

private fun loadContent(context: Context, uri: Uri): ByteArray? {
    return context.contentResolver.openInputStream(uri)?.use { input ->
        input.readBytes()
    }
}
