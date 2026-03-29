package com.yoshi0311.togetherledger.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yoshi0311.togetherledger.LedgerTopAppBar
import com.yoshi0311.togetherledger.R
import com.yoshi0311.togetherledger.ui.AppViewModelProvider
import com.yoshi0311.togetherledger.ui.navigation.NavigationDestination
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SyncSettingsDestination : NavigationDestination {
    override val route = "sync_settings"
    override val titleRes = R.string.sync_settings_title
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsScreen(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SyncSettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(Unit) {
        viewModel.checkServerHealth()
    }

    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LedgerTopAppBar(
                title = stringResource(SyncSettingsDestination.titleRes),
                canNavigateBack = true,
                navigateUp = navigateBack,
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── 서버 설정 ────────────────────────────────────
            SectionCard(title = "서버 설정") {
                OutlinedTextField(
                    value = viewModel.serverUrlInput,
                    onValueChange = { viewModel.serverUrlInput = it },
                    label = { Text("서버 주소") },
                    placeholder = { Text("http://10.0.2.2:3000") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            viewModel.saveServerUrl()
                            viewModel.checkServerHealth()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("저장")
                    }
                    when (viewModel.serverAlive) {
                        null -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        true -> Icon(Icons.Default.CheckCircle, contentDescription = "서버 정상", tint = Color(0xFF22C55E), modifier = Modifier.size(28.dp))
                        false -> Icon(Icons.Default.Warning, contentDescription = "서버 응답 없음", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
                    }
                }
            }

            if (!uiState.isAuthenticated) {
                // ── 새 계정 등록 ──────────────────────────────
                SectionCard(title = "새 계정 등록") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = viewModel.syncIdInput,
                            onValueChange = { viewModel.syncIdInput = it },
                            label = { Text("Sync ID") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        Button(onClick = { viewModel.generateSyncId() }) {
                            Text("생성")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.passwordInput,
                        onValueChange = { viewModel.passwordInput = it },
                        label = { Text("비밀번호") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.register() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isSyncing,
                    ) {
                        if (viewModel.isSyncing) CircularProgressIndicator()
                        else Text("계정 등록")
                    }
                }

                HorizontalDivider()

                // ── 기존 계정 연결 ────────────────────────────
                SectionCard(title = "기존 계정 연결 (다른 기기에서)") {
                    OutlinedTextField(
                        value = viewModel.connectSyncIdInput,
                        onValueChange = { viewModel.connectSyncIdInput = it },
                        label = { Text("Sync ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.connectPasswordInput,
                        onValueChange = { viewModel.connectPasswordInput = it },
                        label = { Text("비밀번호") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.login() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isSyncing,
                    ) {
                        if (viewModel.isSyncing) CircularProgressIndicator()
                        else Text("연결")
                    }
                }
            } else {
                // ── 동기화 (로그인 된 상태) ───────────────────
                SectionCard(title = "동기화") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Sync ID", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(uiState.syncId, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("마지막 동기화", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (uiState.lastSyncedAt == 0L) "없음"
                            else SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                                .format(Date(uiState.lastSyncedAt))
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.sync() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isSyncing,
                    ) {
                        if (viewModel.isSyncing) CircularProgressIndicator()
                        else Text("지금 동기화")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("로그아웃")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
