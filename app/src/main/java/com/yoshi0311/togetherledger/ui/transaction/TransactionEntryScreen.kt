package com.yoshi0311.togetherledger.ui.transaction
import android.app.DatePickerDialog
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yoshi0311.togetherledger.LedgerTopAppBar
import com.yoshi0311.togetherledger.R
import com.yoshi0311.togetherledger.data.Category
import com.yoshi0311.togetherledger.ui.AppViewModelProvider
import com.yoshi0311.togetherledger.ui.navigation.NavigationDestination
import com.yoshi0311.togetherledger.ui.theme.TogetherLedgerTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.collectAsState

object TransactionEntryDestination : NavigationDestination {
    override val route = "transaction_entry"
    override val titleRes = R.string.transaction_entry_title
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntryScreen(
    navigateBack: () -> Unit,
    onNavigateUp: () -> Unit,
    canNavigateBack: Boolean = true,
    viewModel: TransactionEntryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val categories by viewModel.categoriesUiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            LedgerTopAppBar(
                title = stringResource(TransactionEntryDestination.titleRes),
                canNavigateBack = canNavigateBack,
                navigateUp = onNavigateUp,
            )
        }
    ) { innerPadding ->
        TransactionEntryBody(
            transactionUiState = viewModel.transactionUiState,
            onTransactionValueChange = viewModel::updateUiState,
            onSaveClick = {
                coroutineScope.launch {
                    viewModel.saveTransaction()
                    navigateBack()
                }
            },
            categories = categories,
            onAddCategory = { name -> viewModel.addCategory(name, isIncome = false) },
            onDeleteCategory = viewModel::deleteCategory,
            onUpdateCategory = viewModel::updateCategory,
            modifier = Modifier
                .padding(
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    top = innerPadding.calculateTopPadding(),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                )
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
        )
    }
}

@Composable
fun TransactionEntryBody(
    transactionUiState: TransactionUiState,
    onTransactionValueChange: (TransactionDetails) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    categories: List<Category>,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onUpdateCategory: (Category, String) -> Unit,
) {
    Column(
        modifier = modifier
            .padding(dimensionResource(id = R.dimen.padding_medium)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_large)),
    ) {
        TransactionInputForm(
            transactionDetails = transactionUiState.transactionDetails,
            onValueChange = onTransactionValueChange,
            modifier = Modifier.fillMaxWidth(),
            categories = categories,
            onAddCategory = onAddCategory,
            onDeleteCategory = onDeleteCategory,
            onUpdateCategory = onUpdateCategory,
        )
        Button(
            onClick = onSaveClick,
            enabled = transactionUiState.isEntryValid,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.save_action))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionInputForm(
    transactionDetails: TransactionDetails,
    modifier: Modifier = Modifier,
    onValueChange: (TransactionDetails) -> Unit = {},
    enabled: Boolean = true,
    categories: List<Category>,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onUpdateCategory: (Category, String) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_medium))
    ) {

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = stringResource(R.string.transaction_is_income_req), // "수입/지출"
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !transactionDetails.isIncome,      // 지출
                        onClick = { onValueChange(transactionDetails.copy(isIncome = false)) },
                        enabled = enabled
                    )
                    Text("지출")

                    RadioButton(
                        selected = transactionDetails.isIncome,       // 수입
                        onClick = { onValueChange(transactionDetails.copy(isIncome = true)) },
                        enabled = enabled
                    )
                    Text("수입")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp) // 간격 조절
        )
        {
            var showDateModal by remember { mutableStateOf(false) }
            var selectedDate by remember { mutableStateOf<Long?>(null) }

            OutlinedTextField(
                value = transactionDetails.timeStamp.takeIf { it.isNotBlank() }
                    ?.substringBefore(" ") ?: "",
                onValueChange = {
                    onValueChange(transactionDetails.copy(timeStamp = it))
                },
                label = { Text(stringResource(R.string.transaction_timestamp_date_req)) },
                placeholder = { Text("yyyy-MM-dd") },
                trailingIcon = {
                    Icon(Icons.Default.DateRange, contentDescription = "Select date")
                },
                modifier = modifier
                    .weight(1f)
                    .pointerInput(selectedDate) {
                        awaitEachGesture {
                            // Modifier.clickable doesn't work for text fields, so we use Modifier.pointerInput
                            // in the Initial pass to observe events before the text field consumes them
                            // in the Main pass.
                            awaitFirstDown(pass = PointerEventPass.Initial)
                            val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                            if (upEvent != null) {
                                showDateModal = true
                            }
                        }
                    },
                readOnly = true,
            )
            if (showDateModal) {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val initialDateMillis = try {
                    LocalDate.parse(transactionDetails.timeStamp.substringBefore(" "), formatter)
                        .atStartOfDay(ZoneOffset.UTC)   // UTC 기준으로 변환
                        .toInstant()
                        .toEpochMilli()
                } catch (e: Exception) {
                    null
                }
                DatePickerModal(
                    onDateSelected = { millis ->
                        selectedDate = millis
                        millis?.let {
                            val pickedDate = Instant.ofEpochMilli(it)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            val currentTime = transactionDetails.timeStamp.substringAfter(" ", "")
                                .ifBlank {
                                    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                                }
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            val updatedDate = pickedDate.format(formatter)
                            val updated = if (currentTime.isNotBlank()) {
                                "$updatedDate $currentTime"
                            } else {
                                updatedDate
                            }
                            onValueChange(transactionDetails.copy(timeStamp = updated))
                        }
                    },
                    onDismiss = { showDateModal = false },
                    initialDateMillis = initialDateMillis,
                )
            }

            var showTimePicker by remember { mutableStateOf(false) }
            var selectedTime by remember { mutableStateOf<Long?>(null) }
            val currentTime = Calendar.getInstance()
            val timePickerState = rememberTimePickerState(
                    initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
                    initialMinute = currentTime.get(Calendar.MINUTE),
                    is24Hour = true,
                )

            OutlinedTextField(
                value = transactionDetails.timeStamp.takeIf { it.isNotBlank() }
                    ?.substringAfter(" ") ?: "",
                onValueChange = {
                    onValueChange(transactionDetails.copy(timeStamp = it))
                },
                label = { Text(stringResource(R.string.transaction_timestamp_time_req)) },
                placeholder = { Text("HH:mm") },
                trailingIcon = {
                    Icon(Icons.Outlined.Create, contentDescription = "Select time")
                },
                modifier = modifier
                    .weight(1f)
                    .pointerInput(selectedTime) {
                        awaitEachGesture {
                            // Modifier.clickable doesn't work for text fields, so we use Modifier.pointerInput
                            // in the Initial pass to observe events before the text field consumes them
                            // in the Main pass.
                            awaitFirstDown(pass = PointerEventPass.Initial)
                            val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                            if (upEvent != null) {
                                showTimePicker = true
                            }
                        }
                    },
                readOnly = true,
            )
            if (showTimePicker) {
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                val initialTime = try {
                    LocalTime.parse(transactionDetails.timeStamp.substringAfter(" "), formatter)
                } catch (e: Exception) {
                    LocalTime.now()
                }
                TimePickerDialog(
                    onDismiss = { showTimePicker = false },
                    onConfirm = { timePickerState ->
                        val pickedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        val currentDate = transactionDetails.timeStamp.substringBefore(" ", "")
                            .ifBlank {
                                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            }
                        val formatter = DateTimeFormatter.ofPattern("HH:mm")
                        val updatedTime = pickedTime.format(formatter)
                        val updated = if (currentDate.isNotBlank()) {
                            "$currentDate $updatedTime"
                        } else {
                            updatedTime
                        }
                        onValueChange(transactionDetails.copy(timeStamp = updated))
                        showTimePicker = false
                    },
                    initialHour = initialTime.hour,
                    initialMinute = initialTime.minute,
                ) {
                    TimePicker(
                        state = timePickerState,
                    )
                }
            }
        }

        OutlinedTextField(
            value = transactionDetails.amount,
            onValueChange = { onValueChange(transactionDetails.copy(amount = it)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            label = { Text(stringResource(R.string.transaction_amount_req)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            leadingIcon = { Text(Currency.getInstance(Locale.getDefault()).symbol) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true
        )

        OutlinedTextField(
            value = transactionDetails.content,
            onValueChange = { onValueChange(transactionDetails.copy(content = it)) },
            label = { Text(stringResource(R.string.transaction_content_req)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true
        )

        CategoryComboBox(
            categories = categories,
            selectedCategoryId = transactionDetails.categoryId,
            onSelected = {
                onValueChange(transactionDetails.copy(categoryId = it))
            },
            fieldName = stringResource(R.string.transaction_category_req),
            enabled = true,
            onAdd = onAddCategory,
            onDelete = onDeleteCategory,
            onUpdate = onUpdateCategory,
        )

        // 자산 구분은 추후 업데이트 하는 것으로...
//        CategoryComboBox(
//            categories = listOf("현금", "하나카드", "국민은행"),
//            selected = transactionDetails.assetType,
//            onSelected = {
//                onValueChange(transactionDetails.copy(assetType = it))
//            },
//            fieldName = stringResource(R.string.transaction_asset_type_req),
//            enabled = true,
//        )

        if (enabled) {
            Text(
                text = stringResource(R.string.required_fields),
                modifier = Modifier.padding(start = dimensionResource(id = R.dimen.padding_medium))
            )
        }
    }
}

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    initialDateMillis: Long? = null,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (timePickerState: TimePickerState) -> Unit,
    initialHour: Int = LocalTime.now().hour,
    initialMinute: Int = LocalTime.now().minute,
    content: @Composable () -> Unit,
) {

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Dismiss")
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState) }) {
                Text("OK")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }

    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryComboBox(
    categories: List<Category>,
    selectedCategoryId: Int?,
    onSelected: (Int) -> Unit,
    fieldName: String,
    enabled: Boolean,
    onAdd: (String) -> Unit,
    onDelete: (Category) -> Unit,
    onUpdate: (Category, String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    val selectedCategoryName = categories.find { it.id == selectedCategoryId }?.name ?: "카테고리 선택"

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (enabled) expanded = !expanded },
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = selectedCategoryName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(fieldName) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .clickable(enabled = enabled) { expanded = true },
                    enabled = enabled,
                    singleLine = true
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.name) },
                            onClick = {
                                onSelected(item.id)
                                expanded = false
                            }
                        )
                    }
                }
            }

            // 우측 편집 버튼
            IconButton(
                onClick = { showEditSheet = true },
                enabled = enabled,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "카테고리 편집")
            }

        }
    }

    // 편집 모달 토글
    if (showEditSheet) {
        CategoryManagementSheet(
            categories = categories,
            onDismiss = { showEditSheet = false },
            onAdd = onAdd,
            onDelete = onDelete,
            onUpdate = onUpdate,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementSheet(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onDelete: (Category) -> Unit,
    onUpdate: (Category, String) -> Unit
) {
    // 삭제 확인 다이얼로그를 위한 상태
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Text("카테고리 편집", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            // 1. 새 카테고리 추가 입력창
            var newCategoryName by remember { mutableStateOf("") }
            OutlinedTextField(
                value = newCategoryName,
                onValueChange = { newCategoryName = it },
                label = { Text("새 카테고리 추가") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                onAdd(newCategoryName.trim())
                                newCategoryName = ""
                            }
                        },
                        enabled = newCategoryName.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "추가")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 2. 카테고리 리스트 (인라인 편집 적용)
            LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 300.dp)) {
                items(
                    items = categories,
                    key = { it.id } // DB ID를 키로 사용하여 성능 최적화
                ) { category ->
                    CategoryItemRow(
                        category = category,
                        onDeleteClick = { categoryToDelete = category },
                        onUpdate = onUpdate
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("닫기")
            }
        }
    }

    // 3. 삭제 확인 다이얼로그
    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("카테고리 삭제") },
            text = { Text("'${categoryToDelete?.name}' 카테고리를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    categoryToDelete?.let { onDelete(it) }
                    categoryToDelete = null
                }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun CategoryItemRow(
    category: Category,
    onDeleteClick: () -> Unit,
    onUpdate: (Category, String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(category.name) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEditing) {
            // [수정 모드]
            OutlinedTextField(
                value = editName,
                onValueChange = { editName = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                trailingIcon = {
                    Row {
                        IconButton(onClick = {
                            if (editName.isNotBlank() && editName != category.name) {
                                onUpdate(category, editName.trim())
                            }
                            isEditing = false
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "저장", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            isEditing = false
                            editName = category.name // 초기화
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "취소")
                        }
                    }
                }
            )
        } else {
            // [일반 모드]
            Text(
                text = category.name,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodyLarge
            )
            IconButton(onClick = { isEditing = true }) {
                Icon(Icons.Default.Edit, contentDescription = "수정", tint = MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}