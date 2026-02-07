package com.yoshi0311.togetherledger.ui.menu

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.material3.OutlinedTextFieldDefaults.contentPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

object DailyDestination : NavigationDestination {
    override val route = "daily"
    override val titleRes = R.string.daily_screen
}

enum class ScreenType {
    LIST,       // 목록 조회
    CALENDAR,   // 캘린더 조회
    STATISTICS  // 통계 그래프
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyScreen(
    navigateToTransactionEntry: () -> Unit,
    navigateToTransactionUpdate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DailyViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val listUiState by viewModel.listUiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val screenType = remember { mutableStateOf(ScreenType.LIST) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column() {
                LedgerTopAppBar(
                    title = stringResource(DailyDestination.titleRes),
                    canNavigateBack = false,
                    scrollBehavior = scrollBehavior,
                )
                val options = listOf(ScreenType.LIST, ScreenType.CALENDAR, ScreenType.STATISTICS)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    options.forEach { type ->
                        SegmentedButton(
                            selected = screenType.value == type,
                            onClick = { screenType.value = type },
                            label = {
                                when(type) {
                                    ScreenType.LIST -> Text("일별")
                                    ScreenType.CALENDAR -> Text("캘린더")
                                    ScreenType.STATISTICS -> Text("통계")
                                }
                            },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding()
            ) {
                Column {
                    SelectMonthButton(
                        modifier = modifier,
                        listUiState = listUiState,
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
            transactionList = listUiState.transactionList,
            onItemClick = navigateToTransactionUpdate,
            modifier = modifier.fillMaxSize(),
            contentPadding = innerPadding,
            listUiState = listUiState,
            screenType = screenType,
        )
    }
}

@Composable
private fun DailyBody(
    transactionList: List<Transaction>,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    listUiState: ListUiState,
    screenType: MutableState<ScreenType>,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        when (screenType.value) {
            ScreenType.CALENDAR -> {
                CalendarView(
                    transactionList = transactionList,
                    year = listUiState.selectedYear,
                    month = listUiState.selectedMonth,
                    startDayOfWeek = StartDayOfWeek.Sunday, // TODO: 사용자가 직접 설정하도록 변경할 것
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_small)),
                )
            }
            ScreenType.STATISTICS -> {
            }
            // ScreenType.LIST
            else -> {
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
    }
}

@Composable
private fun DailyList(
    transactionList: List<Transaction>,
    onItemClick: (Transaction) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
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
private fun CalendarList(
    transactionList: List<Transaction>,
    onItemClick: (Transaction) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {

}

enum class StartDayOfWeek {
    Sunday, Monday
}

@Composable
fun CalendarView(
    transactionList: List<Transaction>,
    year: Int,
    month: Int,
    startDayOfWeek: StartDayOfWeek = StartDayOfWeek.Sunday,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // 날짜별 그룹핑
    val grouped = transactionList.groupBy { transaction ->
        LocalDateTime.parse(transaction.timeStamp, formatter).toLocalDate()
    }

    // 날짜별 합산 문자열 맵
    val totals: Map<LocalDate, Pair<String?, String?>> = grouped.mapValues { (_, transactions) ->
        val incomeTotal = transactions.filter { it.isIncome }.sumOf { it.amount }
        val expenseTotal = transactions.filter { !it.isIncome }.sumOf { it.amount }

        val incomeTotalString = if (incomeTotal > 0)
            NumberFormat.getNumberInstance(Locale.KOREA).format(incomeTotal) else null
        val expenseTotalString = if (expenseTotal > 0)
            NumberFormat.getNumberInstance(Locale.KOREA).format(expenseTotal) else null

        expenseTotalString to incomeTotalString
    }

    val firstDayOfMonth = LocalDate.of(year, month, 1)
    val lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth())

    // 시작 요일 인덱스 계산
    val startDayIndex = when (startDayOfWeek) {
        StartDayOfWeek.Sunday -> firstDayOfMonth.dayOfWeek.value % 7 // Sunday=0
        StartDayOfWeek.Monday -> firstDayOfMonth.dayOfWeek.value - 1 // Monday=0
    }

    val totalCells = startDayIndex + lastDayOfMonth.dayOfMonth
    val rows = ceil(totalCells / 7.0).toInt()

    val days: List<Pair<Int?, LocalDate?>> = (0 until rows * 7).map { index ->
        val dayNumber = index - startDayIndex + 1
        if (dayNumber in 1..lastDayOfMonth.dayOfMonth) {
            val date = LocalDate.of(year, month, dayNumber)
            dayNumber to date
        } else {
            null to null
        }
    }

    Column(
        modifier = modifier
            .padding(top = 160.dp),
    ) {
        Text(
            text = (year%2000).toString() + stringResource(R.string.year) + " "
                    + month.toString() + stringResource(R.string.month),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.padding_small))
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
        ) {
            items(days.size) { index ->
                val (day, date) = days[index]

                val (expense, income) = if (date != null) {
                    totals[date] ?: (null to null)
                } else (null to null)

                // 색상 계산
                val dayColor = if (day != null && date != null) {
                    when (startDayOfWeek) {
                        StartDayOfWeek.Sunday -> when (date.dayOfWeek) {
                            DayOfWeek.SUNDAY -> Color.Red
                            DayOfWeek.SATURDAY -> Color.Blue
                            else -> Color.Black
                        }
                        StartDayOfWeek.Monday -> when (date.dayOfWeek) {
                            DayOfWeek.SATURDAY -> Color.Blue
                            DayOfWeek.SUNDAY -> Color.Red
                            else -> Color.Black
                        }
                    }
                } else null

                CalendarItem(
                    day = day,
                    dayColor = dayColor,
                    income = income,
                    expense = expense
                )
            }
        }
    }

}


@Composable
private fun CalendarItem(
    day: Int?,       // 날짜 (빈 셀일 경우 null)
    dayColor: Color? = null,       // 날짜 색상
    income: String? = null,        // 수입 합산
    expense: String? = null,       // 지출 합산
) {
    Box(
        modifier = Modifier
            .border(1.dp, Color.LightGray)
            .fillMaxWidth()
            .height(80.dp)
            .padding(2.dp)
    ) {
        // 좌측 상단 날짜 표시
        if (day != null && dayColor != null) {
            Text(
                text = day.toString(),
                color = dayColor,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }

        // 우측 하단 금액 표시
        Column(
            modifier = Modifier.align(Alignment.BottomEnd),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy((-7).dp),
        ) {
            if (!income.isNullOrEmpty()) {
                val fontSize = if (income.length > 10) 5.sp else 9.sp
                Text(
                    text = income,
                    color = Color.Red,
                    fontSize = fontSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!expense.isNullOrEmpty()) {
                val fontSize = if (expense.length > 10) 5.sp else 9.sp
                Text(
                    text = expense,
                    color = Color.Blue,
                    fontSize = fontSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
    val list: List<Transaction> = listOf(
        Transaction(
            id = 1,
            category = "🍔식비",
            content = "마켓컬리 주문",
            timeStamp = "2026-02-02 15:42:00",
            amount = 15800,
            assetType = "국민은행",
            isIncome = false,
        ),
        Transaction(
            id = 2,
            category = "🍪간식비",
            content = "CU편의점",
            timeStamp = "2026-02-03 12:15:00",
            amount = 4200,
            assetType = "하나카드",
            isIncome = false,
        ),
        Transaction(
            id = 3,
            category = "🍟간식비",
            content = "명랑핫도그",
            timeStamp = "2026-02-03 17:50:15",
            amount = 3800,
            assetType = "국민은행",
            isIncome = false,
        ),
        Transaction(
            id = 4,
            category = "🚌교통비",
            content = "기후동행 충전",
            timeStamp = "2026-02-04 09:42:00",
            amount = 55000,
            assetType = "카카오뱅크",
            isIncome = false,
        ),
        Transaction(
            id = 5,
            category = "부수입",
            content = "방과후 수입",
            timeStamp = "2026-02-04 14:20:00",
            amount = 75000,
            assetType = "국민카드",
            isIncome = true,
        ),
        Transaction(
            id = 6,
            category = "💡공과금",
            content = "전기요금 납부",
            timeStamp = "2026-02-04 20:05:00",
            amount = 72000,
            assetType = "우리카드",
            isIncome = false,
        ),
        Transaction(
            id = 7,
            category = "🍷데이트",
            content = "레스토랑 저녁식사",
            timeStamp = "2026-02-06 19:30:00",
            amount = 68000,
            assetType = "현금",
            isIncome = false,
        ),
        Transaction(
            id = 8,
            category = "💼업무",
            content = "카페 회의",
            timeStamp = "2026-02-07 10:45:00",
            amount = 12000,
            assetType = "하나카드",
            isIncome = false,
        ),
        Transaction(
            id = 9,
            category = "🍔식비",
            content = "점심 도시락",
            timeStamp = "2026-02-07 13:10:00",
            amount = 8500,
            assetType = "국민은행",
            isIncome = false,
        ),
    )

    val incomeTotal = list.filter { it.isIncome }.sumOf { it.amount }
    val expenseTotal = list.filter { !it.isIncome }.sumOf { it.amount }
    val incomeTotalString = "${NumberFormat.getNumberInstance(Locale.KOREA).format(incomeTotal)}"
    val expenseTotalString = "${NumberFormat.getNumberInstance(Locale.KOREA).format(expenseTotal)}"
    TogetherLedgerTheme {
//        CalendarItem(
//            day = 5,
//            dayOfWeek = 3,
//            income = if (incomeTotal > 0) incomeTotalString else null,
//            expense = if (expenseTotal > 0) expenseTotalString else null,
//        )
        CalendarView(
            transactionList = list,
            year = 2026,
            month = 2,
            startDayOfWeek = StartDayOfWeek.Monday,
        )
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