package com.yoshi0311.togetherledger.ui.menu

import android.Manifest
import android.R.attr.enabled
import android.R.id.message
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yoshi0311.togetherledger.LedgerTopAppBar
import com.yoshi0311.togetherledger.R
import com.yoshi0311.togetherledger.ui.AppViewModelProvider
import com.yoshi0311.togetherledger.ui.navigation.NavigationDestination
import com.yoshi0311.togetherledger.ui.transaction.TransactionDetails
import com.yoshi0311.togetherledger.ui.transaction.TransactionEditDestination
import com.yoshi0311.togetherledger.ui.transaction.TransactionInputFormSmall
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataManagementDestination : NavigationDestination {
    override val route = "data_management"
    override val titleRes = R.string.management_data_title
}

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    navigateBack: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DataManagementViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val context = LocalContext.current
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->

                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                viewModel.importFromCsv(context, uri)
            }
        }
    }
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadSmsMessages(context)
        } else {
            // 권한 거부 처리 (Toast 등)
        }
    }
    val categories by viewModel.categoriesUiState.collectAsState()

    Scaffold(
        topBar = {
            LedgerTopAppBar(
                title = stringResource(TransactionEditDestination.titleRes),
                canNavigateBack = true,
                navigateUp = onNavigateUp,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(0.dp))

            val transactionList by viewModel.transactionList.collectAsStateWithLifecycle()
            if (viewModel.isImporting && transactionList.isNotEmpty()) {
                LazyColumn(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)) {
                    items(transactionList) { transactionDetails ->
                        Card() {
                            Column(modifier = Modifier.padding(bottom = 40.dp)) {
                                TransactionInputFormSmall(
                                    transactionDetails = transactionDetails,
                                    onValueChange = { updatedDetails ->
                                        viewModel.updateTransactionItem(updatedDetails)
                                    },
                                    enabled = true,
                                    categories = categories,
                                    onAddCategory = { name -> viewModel.addCategory(name, isIncome = false) },
                                    onDeleteCategory = viewModel::deleteCategory,
                                    onUpdateCategory = viewModel::updateCategory,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    // 삭제 버튼 (목록에서만 제거)
                                    TextButton(onClick = {
                                        viewModel.deleteTransactionItem(transactionDetails.timeStamp)
                                    }) {
                                        Text("삭제")
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // 저장 버튼 (DB 저장 후 목록에서 제거)
                                    Button(onClick = {
                                        viewModel.saveIndividualTransaction(transactionDetails)
                                    }) {
                                        Text("저장")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Button(
                    onClick = {
                        val permission = Manifest.permission.READ_SMS
                        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                            viewModel.loadSmsMessages(context)
                        } else {
                            smsPermissionLauncher.launch(permission)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(vertical = 8.dp)
                ) {
                    Text(stringResource(R.string.import_from_sms))
                }
            }

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "text/*"
//                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "application/csv", "application/octet-stream"))
                    }
                    csvLauncher.launch(intent)
                },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(vertical = 8.dp)
            ) {
                Text(stringResource(R.string.import_from_excel))
            }

            Button(
                onClick = {
                    viewModel.exportTransactionsToExcel(context = context)
                },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(vertical = 8.dp)
            ) {
                Text(stringResource(R.string.export_to_excel))
            }
        }
    }
}