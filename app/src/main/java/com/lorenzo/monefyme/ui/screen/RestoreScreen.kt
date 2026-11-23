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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lorenzo.monefyme.domain.CsvImportResult
import com.lorenzo.monefyme.ui.viewmodel.MainViewModel
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importResult by viewModel.lastImportResult.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val exportRowCount by viewModel.lastExportRowCount.collectAsStateWithLifecycle()
    val exportError by viewModel.lastExportError.collectAsStateWithLifecycle()
    val isBackingUpToDrive by viewModel.isBackingUpToDrive.collectAsStateWithLifecycle()
    val driveAccount by viewModel.driveAccount.collectAsStateWithLifecycle()
    val driveConsentRequest by viewModel.drivePendingConsent.collectAsStateWithLifecycle()
    var writeError by remember { mutableStateOf<String?>(null) }
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
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            writeError = null
            val content = viewModel.exportCsv() ?: return@launch
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(content)
                } ?: throw IOException("Could not open the selected file for writing.")
            } catch (ioException: IOException) {
                val failure = "Could not write the exported file: ${ioException.message ?: "unknown error"}."
                writeError = failure
                viewModel.postMessage(failure)
            }
        }
    }
    val driveConsentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { activityResult ->
        // A null result data means the user dismissed the Google sign-in prompt.
        viewModel.resolveDriveConsent(activityResult.data)
    }
    LaunchedEffect(driveConsentRequest) {
        driveConsentRequest?.let { intentSenderRequest ->
            driveConsentLauncher.launch(intentSenderRequest)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Restore") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF74C792),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        launcher.launch(arrayOf("text/*", "text/csv", "application/csv"))
                    },
                    enabled = !isImporting,
                ) {
                    Text("Select CSV file")
                }
                Button(
                    onClick = {
                        exportLauncher.launch(defaultExportFileName())
                    },
                ) {
                    Text("Export to CSV")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Google Drive backup",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Backs up every stored transaction to the root of your Google Drive " +
                    "(My Drive) as Monefy.Backup.yyyy-MM-dd.csv, using the same CSV layout as the export.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        scope.launch { viewModel.backupToGoogleDrive() }
                    },
                    enabled = !isBackingUpToDrive,
                ) {
                    Text("Back up to Google Drive")
                }
                if (isBackingUpToDrive) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            val rememberedDriveAccount = driveAccount
            Text(
                text = if (rememberedDriveAccount != null) {
                    "Signed in as $rememberedDriveAccount for Google Drive backups."
                } else {
                    "Signs in with your Google account when needed and uploads the CSV to My Drive."
                },
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(12.dp))
            val exportFailure = writeError ?: exportError
            Text(
                text = when {
                    exportFailure != null -> exportFailure
                    exportRowCount != null -> "Last export: $exportRowCount rows written."
                    else -> "Export writes every stored transaction, across all months, to a CSV file."
                },
                style = MaterialTheme.typography.bodyMedium,
            )

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

private fun defaultExportFileName(): String {
    val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd.HH-mm-ss").format(LocalDateTime.now())
    return "Monefy.Export.$stamp.csv"
}

private fun loadContent(context: Context, uri: Uri): ByteArray? {
    return context.contentResolver.openInputStream(uri)?.use { input ->
        input.readBytes()
    }
}
