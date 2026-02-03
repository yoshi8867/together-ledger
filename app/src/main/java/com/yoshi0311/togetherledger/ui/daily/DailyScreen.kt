package com.yoshi0311.togetherledger.ui.daily

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yoshi0311.togetherledger.LedgerTopAppBar
import com.yoshi0311.togetherledger.R
import com.yoshi0311.togetherledger.data.Transaction
import com.yoshi0311.togetherledger.ui.AppViewModelProvider
import com.yoshi0311.togetherledger.ui.navigation.NavigationDestination
import com.yoshi0311.togetherledger.ui.theme.TogetherLedgerTheme
import java.text.NumberFormat
import java.util.Locale

object DailyDestination : NavigationDestination {
    override val route = "daily"
    override val titleRes = R.string.daily_screen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyScreen(
    navigateToTransactionEntry: () -> Unit,
    navigateToTransactionUpdate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DailyViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val dailyUiState by viewModel.dailyUiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LedgerTopAppBar(
                title = stringResource(DailyDestination.titleRes),
                canNavigateBack = false,
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = navigateToTransactionEntry,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .padding(
                        end = WindowInsets.safeDrawing.asPaddingValues()
                            .calculateEndPadding(LocalLayoutDirection.current)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.transaction_entry_title)
                )
            }
        },
    ) { innerPadding ->
        DailyBody(
            transactionList = dailyUiState.transactionList,
            onItemClick = navigateToTransactionUpdate,
            modifier = modifier.fillMaxSize(),
            contentPadding = innerPadding,
        )
    }
}

@Composable
private fun DailyBody(
    transactionList: List<Transaction>,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        if (transactionList.isEmpty()) {
            Text(
                text = stringResource(R.string.no_item_description),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(contentPadding),
            )
        } else {
            DailyList(
                transactionList = transactionList,
                onItemClick = { onItemClick(it.id) },
                contentPadding = contentPadding,
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_small)),
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DailyList(
    transactionList: List<Transaction>,
    onItemClick: (Transaction) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding
    ) {
        items(items = transactionList, key = { it.id }) { transaction ->
            DailyItem(transaction = transaction,
                modifier = Modifier
                    .padding(dimensionResource(id = R.dimen.padding_small))
                    .clickable { onItemClick(transaction) })
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DailyItem(
    transaction: Transaction,
    modifier: Modifier = Modifier,
) {
//    val dateTime = LocalDateTime.parse(transaction.timeStamp)
    Card(
        modifier = modifier, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 좌측: 분류
            Text(
                text = transaction.category,
                modifier = Modifier.weight(1f), // 좌측 정렬
                fontSize = 14.sp
            )

            // 중앙: 내용 + 시각
            Column(
                modifier = Modifier.weight(3f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = transaction.content,
                    fontSize = 16.sp
                )
                Text(
//                    text = dateTime.format(DateTimeFormatter.ofPattern("a hh:mm:ss")),
                    text = transaction.timeStamp,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // 우측: 금액
            Box(
                modifier = Modifier
                    .weight(1f)  // 좌/중/우 비율 유지
                    .fillMaxWidth(), // Box가 전체 weight 공간을 차지
                contentAlignment = Alignment.CenterEnd // 우측 정렬
            ) {
                Text(
                    // text = "${item.amount}원",
                    text = "${NumberFormat.getNumberInstance(Locale.KOREA).format(transaction.amount)}원",
                    fontSize = 16.sp,
                    color = if (transaction.isIncome) Color.Red else Color.Blue
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun HomeBodyEmptyListPreview() {
    TogetherLedgerTheme {
        DailyItem(
            Transaction(
                id = 1,
                category = "🍔식비",
                content = "마켓컬리 주문",
                timeStamp = "2026-02-02 15:42:00",
                amount = 15800,
                assetType = "국민은행",
                isIncome = false,
            )
        )
    }
}