package com.yoshi0311.togetherledger.ui.menu

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yoshi0311.togetherledger.LedgerTopAppBar
import com.yoshi0311.togetherledger.R
import com.yoshi0311.togetherledger.ui.AppViewModelProvider
import com.yoshi0311.togetherledger.ui.navigation.NavigationDestination
import com.yoshi0311.togetherledger.ui.transaction.TransactionDetails
import com.yoshi0311.togetherledger.ui.transaction.TransactionInputFormSmall

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
                title = stringResource(DataManagementDestination.titleRes),
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

            var isPushMessageListOpen by remember { mutableStateOf<Boolean>(false) }
            if (isPushMessageListOpen) {
                PushMessageListScreen(
                    viewModel = viewModel,
                    onBack = { isPushMessageListOpen = false }
                )
            } else {
                Button(
                    onClick = {
                        if (viewModel.hasNotificationPermission(context)) {
                            isPushMessageListOpen = true
                        } else {
                            // 권한이 없다면: 시스템 설정창으로 이동
                            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)

                            Toast.makeText(context, "알림 접근 권한을 켜주세요.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(vertical = 8.dp)
                ) {
                    Text(stringResource(R.string.import_from_app_push))
                }
            }


            var isAppPushAllowListOpen by remember { mutableStateOf<Boolean>(false) }
            if (isAppPushAllowListOpen) {
                AppSelectionScreen(
                    viewModel = viewModel,
                    onBack = { isAppPushAllowListOpen = false }
                )
            } else {
                Button(
                    onClick = {
                        if (viewModel.hasNotificationPermission(context)) {
                            viewModel.loadFinancialApps(context)
                            isAppPushAllowListOpen = true
                        } else {
                            // 권한이 없다면: 시스템 설정창으로 이동
                            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)

                            Toast.makeText(context, "알림 접근 권한을 켜주세요.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(vertical = 8.dp)
                ) {
                    Text(stringResource(R.string.app_push_setting))
                }
            }

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "text/*"
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

@Composable
fun PushMessageListScreen(
    viewModel: DataManagementViewModel,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "뒤로") }

        val transactionList by viewModel.notificationToTransactionList.collectAsStateWithLifecycle()
        val categories by viewModel.categoriesUiState.collectAsState()
        if (transactionList.isNotEmpty()) {
            LazyColumn(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)) {
                items(
                    items = transactionList,
                    key = { transaction -> transaction.notificationId }
                ) { transactionDetails ->
                    Card(modifier = Modifier.padding(bottom = 10.dp)) {
                        Column(modifier = Modifier.padding(3.dp)) {
                            TransactionInputFormSmall(
                                transactionDetails = transactionDetails,
                                onValueChange = { updatedDetails ->
                                    viewModel.updateNotificationItem(updatedDetails)
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
                                    viewModel.markNotificationAsProcessed(transactionDetails.notificationId)
                                }) {
                                    Text("삭제")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // 저장 버튼 (DB 저장 후 목록에서 제거)
                                Button(onClick = {
                                    viewModel.saveIndividualTransaction(transactionDetails)
                                    viewModel.markNotificationAsProcessed(transactionDetails.notificationId)
                                }) {
                                    Text("저장")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Text("푸시 알림 없음")
        }
    }
}


@Composable
fun AppSelectionScreen(viewModel: DataManagementViewModel, onBack: () -> Unit) {
    // 뷰모델의 uiState를 구독 (로딩 완료 전까지는 emptyList 상태)
    val apps by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "뒤로") }

        if (apps.isEmpty()) {
            // 로딩 중일 때 표시
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // 로딩 완료 시 리스트 표시
            LazyColumn {
                items(apps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        onToggle = { viewModel.toggleAppSelection(app.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
fun AppRow(app: AppInfoData, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아이콘 표시 (Glide나 Coil 사용 추천)
        Image(
            bitmap = app.icon.toBitmap().asImageBitmap(), // 예시: 간단한 변환
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(text = app.appName, modifier = Modifier.weight(1f))

        Switch(
            checked = app.isSelected,
            onCheckedChange = { onToggle() }
        )
    }
}