package com.yoshi0311.togetherledger.ui.menu

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
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
import androidx.navigation.NavController
import com.yoshi0311.togetherledger.LedgerTopAppBar
import com.yoshi0311.togetherledger.R
import com.yoshi0311.togetherledger.data.Transaction
import com.yoshi0311.togetherledger.ui.AppViewModelProvider
import com.yoshi0311.togetherledger.ui.navigation.NavigationDestination
import com.yoshi0311.togetherledger.ui.theme.TogetherLedgerTheme
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
    val dailyUiState by viewModel.listUiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LedgerTopAppBar(
                title = stringResource(DailyDestination.titleRes),
                canNavigateBack = false,
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding()
            ) {
                Column {
                    SelectMonthButton(
                        modifier = modifier,
                        listUiState = dailyUiState,
                        onMonthSelected = viewModel::selectMonth,
                    )
                }
            }
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

@Composable
private fun DailyList(
    transactionList: List<Transaction>,
    onItemClick: (Transaction) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val grouped = transactionList.groupBy { transaction ->
        // String → LocalDateTime → LocalDate
        val dateTime = LocalDateTime.parse(transaction.timeStamp, formatter)
        dateTime.toLocalDate()
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding
    ) {
//        items(items = transactionList, key = { it.id }) { transaction ->
//            DailyItem(transaction = transaction,
//                modifier = Modifier
//                    .padding(dimensionResource(id = R.dimen.padding_small))
//                    .clickable { onItemClick(transaction) })
//        }

        grouped.forEach { (date, transactions) ->

            val incomeTotal = transactions.filter { it.isIncome }.sumOf { it.amount }
            val expenseTotal = transactions.filter { !it.isIncome }.sumOf { it.amount }
            val incomeTotalString = "${NumberFormat.getNumberInstance(Locale.KOREA).format(incomeTotal)}원"
            val expenseTotalString = "${NumberFormat.getNumberInstance(Locale.KOREA).format(expenseTotal)}원"

            item {
                Text(
                    text = date.format(DateTimeFormatter.ofPattern(
                        "M.d.(E)", Locale.KOREA)),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(dimensionResource(id = R.dimen.padding_small))
                )
            }
            items(transactions, key = { it.id }) { transaction ->
                DailyItem(
                    transaction = transaction,
                    modifier = Modifier
                        .padding(dimensionResource(id = R.dimen.padding_small))
                        .clickable { onItemClick(transaction) }
                )
            }
        }

    }
}

@Composable
private fun DailyItem(
    transaction: Transaction,
    modifier: Modifier = Modifier,
) {
//    val dateTime = LocalDateTime.parse(transaction.timeStamp)
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                modifier = Modifier.weight(2f), // 좌측 정렬
                fontSize = 14.sp
            )

            // 중앙: 내용 + 시각
            Column(
                modifier = Modifier.weight(5f),
                horizontalAlignment = Alignment.Start,
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
                    .weight(2f)  // 좌/중/우 비율 유지
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

@Composable
fun SelectMonthButton(
    modifier: Modifier = Modifier,
    listUiState: ListUiState,
    onMonthSelected: (year: Int, month: Int) -> Unit,
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 30.dp, ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                modifier = modifier.weight(2f),
                onClick = {
                    val currentYear = listUiState.selectedYear
                    val currentMonth = listUiState.selectedMonth
                    val newMonth = (currentMonth - 1 + 12) % 12
                    val newYear = if (newMonth == 0) currentYear - 1 else currentYear
                    onMonthSelected(
                        newYear,
                        if (newMonth == 0) 12 else newMonth,
                    )
                },
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.prev),
                )
            }
            Button(
                modifier = modifier.weight(4f).padding(horizontal = 5.dp),
                onClick = {

                },
            ) {
                Text(
                    text = listUiState.selectedYear.toString() + stringResource(R.string.year)
                            + listUiState.selectedMonth.toString() + stringResource(R.string.month),
                    fontSize = 18.sp,
                )
            }
            Button(
                modifier = modifier.weight(2f),
                onClick = {
                    val currentYear = listUiState.selectedYear
                    val currentMonth = listUiState.selectedMonth
                    val newMonth = (currentMonth + 1) % 12
                    val newYear = if (newMonth == 1) currentYear + 1 else currentYear
                    onMonthSelected(
                        newYear,
                        if (newMonth == 0) 12 else newMonth,
                    )
                },
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.next),
                )
            }
        }
    }
}

@Composable
fun MonthPicker(
    modifier: Modifier = Modifier,
    listUiState: ListUiState,
    onMonthSelected: (year: Int, month: Int) -> Unit,
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 30.dp, ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    modifier = modifier.weight(2f),
                    onClick = {

                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.prev),
                    )
                }
                Button(
                    modifier = modifier.weight(4f).padding(horizontal = 5.dp),
                    onClick = {
                    },
                ) {
                    Text(
                        text = listUiState.selectedYear.toString() + stringResource(R.string.year),
                        fontSize = 15.sp,
                    )
                }
                Button(
                    modifier = modifier.weight(2f),
                    onClick = {
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.next),
                    )
                }
                Button(
                    modifier = modifier.weight(3f).padding(horizontal = 5.dp),
                    onClick = {
                        // MonthPicker on/off
                    },
                ) {
                    Text(
                        text = stringResource(R.string.this_month),
                        fontSize = 15.sp,
                    )
                }
            }

            for (i in 0..2) {
                Row {
                    for (j in 1..4) {
                        val x = i*4 + j
                        Button(
                            modifier = modifier.weight(2f).padding(horizontal = 2.dp),
                            onClick = { },
                        ) {
                            Text(
                                text = x.toString() + stringResource(R.string.month),
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }

        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun TestPreview() {
    TogetherLedgerTheme {
        // MonthPicker()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
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