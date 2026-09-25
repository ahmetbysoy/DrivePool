package com.drivepool.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val selectFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            DrivePoolTheme {
                DrivePoolApp(this)
            }
        }
    }

    private fun selectFile(uri: Uri) {
        val workRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(
                "file_uri" to uri.toString(),
                "upload_id" to UUID.randomUUID().toString(),
                "timestamp" to System.currentTimeMillis()
            ))
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()
            )
            .build()
            
        WorkManager.getInstance(this).enqueue(workRequest)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrivePoolApp(activity: ComponentActivity) {
    var accounts by remember { mutableStateOf(emptyList<Account>()) }
    var selectedAccount by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    activity.startActivity(Intent(Intent.ACTION.GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        setType("*/*")
                    })
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add file")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "DRIVE POOL",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "10 Google hesabı tek havuz gibi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Quick Stats
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                enabled = false
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TOPLAM KULLANILABILIR", style = MaterialTheme.typography.labelSmall)
                    Text("71.8 GB", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
            }

            // Account Status
            Text(
                "HESAPLAR",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium
            )
            
            LazyColumn {
                items(accounts) { account ->
                    AccountItem(account)
                }
                
                // Add account button
                item {
                    TextButton(onClick = { /* Show add account dialog */ }) {
                        Text("Hesap Ekle")
                    }
                }
            }

            // Upload History
            Text(
                "SON İŞLEMLER",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun AccountItem(account: Account) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(account.email, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${account.usedBytes / (1024*1024*1024)} GB / ${(account.totalBytes / (1024*1024*1024))} GB",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${account.freeBytes / (1024*1024*1024)} GB",
                style = MaterialTheme.typography.titleMedium,
                color = if (account.freeBytes > 5L * 1024*1024*1024) 
                    MaterialTheme.colorScheme.primary else 
                    MaterialTheme.colorScheme.error
            )
        }
    }
}

data class Account(
    val email: String,
    val totalBytes: Long = 15L * 1024*1024*1024,
    val usedBytes: Long = 7L * 1024*1024*1024,
    val enabled: Boolean = true
)
