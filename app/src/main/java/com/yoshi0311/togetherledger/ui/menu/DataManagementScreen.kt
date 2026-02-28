package com.yoshi0311.togetherledger.ui.menu

import android.R.attr.onClick
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yoshi0311.togetherledger.LedgerTopAppBar
import com.yoshi0311.togetherledger.R
import com.yoshi0311.togetherledger.ui.AppViewModelProvider
import com.yoshi0311.togetherledger.ui.navigation.NavigationDestination
import com.yoshi0311.togetherledger.ui.transaction.TransactionEditDestination
import com.yoshi0311.togetherledger.ui.transaction.TransactionEditViewModel

object DataManagementDestination : NavigationDestination {
    override val route = "data_management"
    override val titleRes = R.string.management_data_title
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    navigateBack: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DataManagementViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
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
            Button(
                onClick = { /* TODO: 문자 메시지 로직 */ },
                modifier = Modifier.fillMaxWidth(0.7f).padding(vertical = 8.dp)
            ) {
                Text(stringResource(R.string.import_from_sms))
            }

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "text/*"
//                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "application/csv", "application/octet-stream"))
                    }
                    launcher.launch(intent)
                },
                modifier = Modifier.fillMaxWidth(0.7f).padding(vertical = 8.dp)
            ) {
                Text(stringResource(R.string.import_from_excel))
            }

            Button(
                onClick = {
                    viewModel.exportTransactionsToExcel(context = context)
                },
                modifier = Modifier.fillMaxWidth(0.7f).padding(vertical = 8.dp)
            ) {
                Text(stringResource(R.string.export_to_excel))
            }
        }
    }
}