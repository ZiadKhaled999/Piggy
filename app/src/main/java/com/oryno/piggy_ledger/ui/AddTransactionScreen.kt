package com.oryno.piggy_ledger.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

data class TransactionCategory(
    val key: String,
    val nameRes: Int? = null,
    val customName: String? = null,
    val icon: ImageVector,
    val descRes: Int? = null
)

val categoriesList = listOf(
    TransactionCategory("atm_withdrawal", R.string.cat_atm_withdrawal, null, Icons.Default.LocalAtm),
    TransactionCategory("bonus", R.string.cat_bonus, null, Icons.Default.CardMembership),
    TransactionCategory("cashback", R.string.cat_cashback, null, Icons.Default.LocalOffer),
    TransactionCategory("credit_card_settlement", R.string.cat_credit_card_settlement, null, Icons.Default.CreditCard, R.string.cat_credit_card_settlement_desc),
    TransactionCategory("education", R.string.cat_education, null, Icons.Default.School),
    TransactionCategory("entertainment", R.string.cat_entertainment, null, Icons.Default.Movie),
    TransactionCategory("food", R.string.cat_food, null, Icons.Default.Restaurant),
    TransactionCategory("freelance", R.string.cat_freelance, null, Icons.Default.Computer),
    TransactionCategory("fuel", R.string.cat_fuel, null, Icons.Default.LocalGasStation),
    TransactionCategory("health", R.string.cat_health, null, Icons.Default.Favorite),
    TransactionCategory("home", R.string.cat_home, null, Icons.Default.Home),
    TransactionCategory("investment", R.string.cat_investment, null, Icons.Default.TrendingUp),
    TransactionCategory("other", R.string.cat_other, null, Icons.Default.Payments),
    TransactionCategory("personal_care", R.string.cat_personal_care, null, Icons.Default.Spa),
    TransactionCategory("refund", R.string.cat_refund, null, Icons.Default.Undo),
    TransactionCategory("salary", R.string.cat_salary, null, Icons.Default.AccountBalanceWallet),
    TransactionCategory("shopping", R.string.cat_shopping, null, Icons.Default.ShoppingCart),
    TransactionCategory("subscriptions", R.string.cat_subscriptions, null, Icons.Default.Subscriptions),
    TransactionCategory("transfer", R.string.cat_transfer, null, Icons.Default.SwapHoriz),
    TransactionCategory("transportation", R.string.cat_transportation, null, Icons.Default.DirectionsCar),
    TransactionCategory("travel", R.string.cat_travel, null, Icons.Default.Flight),
    TransactionCategory("utilities", R.string.cat_utilities, null, Icons.Default.Lightbulb),
    TransactionCategory("work", R.string.cat_work, null, Icons.Default.Business)
)

fun cleanCategoryKey(rawKey: String): String {
    var cleaned = rawKey.trim()
    while (cleaned.startsWith("cat_", ignoreCase = true) ||
           cleaned.startsWith("cat ", ignoreCase = true) ||
           cleaned.startsWith("cat-", ignoreCase = true) ||
           cleaned.startsWith("custom_", ignoreCase = true) ||
           cleaned.startsWith("custom ", ignoreCase = true)) {
        cleaned = when {
            cleaned.startsWith("cat_", ignoreCase = true) -> cleaned.substring(4)
            cleaned.startsWith("cat ", ignoreCase = true) -> cleaned.substring(4)
            cleaned.startsWith("cat-", ignoreCase = true) -> cleaned.substring(4)
            cleaned.startsWith("custom_", ignoreCase = true) -> cleaned.substring(7)
            cleaned.startsWith("custom ", ignoreCase = true) -> cleaned.substring(7)
            else -> cleaned
        }.trim()
    }
    return cleaned
}

@Composable
fun resolveCategoryDisplayName(rawKey: String): String {
    if (rawKey == "Other" || rawKey == "category_other_marker" || rawKey.equals("cat_other", ignoreCase = true)) {
        return stringResource(R.string.category_other)
    }
    val cleaned = cleanCategoryKey(rawKey)
    val catItem = categoriesList.find { 
        it.key.equals(rawKey, ignoreCase = true) || 
        it.key.equals("cat_$cleaned", ignoreCase = true) ||
        cleanCategoryKey(it.key).equals(cleaned, ignoreCase = true)
    }
    return if (catItem != null && catItem.nameRes != null) {
        stringResource(catItem.nameRes)
    } else {
        cleaned.replace("_", " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
    }
}

fun showNativeDatePicker(context: Context, initialTimestamp: Long, onDateSelected: (Long) -> Unit) {
    try {
        val cal = Calendar.getInstance().apply { timeInMillis = initialTimestamp }
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                onDateSelected(newCal.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    } catch (e: Exception) {
        android.util.Log.e("DatePicker", "Error showing date picker", e)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: PiggyLedgerViewModel,
    selectedAccountId: String?,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onNavigateToAddAccount: () -> Unit = {},
    onNavigateToSettingsPro: () -> Unit = {},
    onNavigateToTransfer: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isPremium by viewModel.isPremium.collectAsState()

    // 0: Amount & Type (Poster), 1: Category (Arc Wheel), 2: Account, 3: Description, 4: Date & Fee
    var currentStep by remember { mutableIntStateOf(0) }

    // Core Data
    var isExpense by remember { mutableStateOf(true) }
    var txAmountStr by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<TransactionCategory?>(categoriesList.find { it.key == "food" } ?: categoriesList.first()) }
    var sourceAccountId by remember { mutableStateOf(selectedAccountId ?: accounts.firstOrNull()?.id ?: "") }
    var txDescription by remember { mutableStateOf("") }
    var txHint by remember { mutableStateOf("") }
    var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var applyInstaPayFee by remember { mutableStateOf(false) }

    val allCategories = remember { mutableStateListOf<TransactionCategory>().apply { addAll(categoriesList.filter { it.key != "cat_transfer" }) } }
    var showCreateCategorySheet by remember { mutableStateOf(false) }

    val activeSourceAccount = accounts.find { it.id == sourceAccountId }
    val currencySymbol = activeSourceAccount?.currency?.let { getCurrencySymbol(it) } ?: "EGP"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // TOP HEADER WITH PROGRESS INDICATOR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            if (currentStep > 0) {
                                currentStep--
                            } else {
                                onDismiss()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (currentStep > 0) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                            contentDescription = stringResource(R.string.back_icon),
                            tint = NavyDark
                        )
                    }

                    // Step Indicator Dots
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in 0..4) {
                            val isActive = i == currentStep
                            val isPassed = i < currentStep
                            Box(
                                modifier = Modifier
                                    .size(if (isActive) 10.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isActive -> PinkPrimary
                                            isPassed -> PinkPrimary.copy(alpha = 0.4f)
                                            else -> Color(0xFFCBD5E1)
                                        }
                                    )
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color(0xFF94A3B8))
                    }
                }

                // SCREEN SLIDE CONTENT WITH ANIMATED CONTENT TRANSITION
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn() togetherWith
                                        slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut()
                            } else {
                                slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn() togetherWith
                                        slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut()
                            }
                        },
                        label = "SlideTransition"
                    ) { step ->
                        when (step) {
                            0 -> Step0AmountScreen(
                                isExpense = isExpense,
                                onTypeChanged = { isExpense = it },
                                amountStr = txAmountStr,
                                onAmountChanged = { txAmountStr = it },
                                currencySymbol = currencySymbol,
                                onNavigateToTransfer = if (onNavigateToTransfer != null) {
                                    {
                                        onDismiss()
                                        onNavigateToTransfer.invoke()
                                    }
                                } else null,
                                onNext = { if (txAmountStr.toDoubleOrNull() != null && txAmountStr.toDoubleOrNull()!! > 0) currentStep = 1 }
                            )
                            1 -> Step1CategoryArcWheelScreen(
                                isExpense = isExpense,
                                categories = allCategories,
                                selectedCategory = selectedCategory,
                                onCategorySelected = { selectedCategory = it },
                                onCreateCategoryClicked = { showCreateCategorySheet = true },
                                onNext = { if (selectedCategory != null) currentStep = 2 }
                            )
                            2 -> Step2AccountSelectionScreen(
                                accounts = accounts,
                                selectedAccountId = sourceAccountId,
                                onAccountSelected = { sourceAccountId = it },
                                onNavigateToAddAccount = onNavigateToAddAccount,
                                onNext = { if (sourceAccountId.isNotEmpty()) currentStep = 3 }
                            )
                            3 -> Step3DescriptionHintScreen(
                                description = txDescription,
                                onDescriptionChanged = { txDescription = it },
                                hint = txHint,
                                onHintChanged = { txHint = it },
                                isExpense = isExpense,
                                onNext = { currentStep = 4 }
                            )
                            4 -> Step4ConfirmationScreen(
                                isExpense = isExpense,
                                amountStr = txAmountStr,
                                currencySymbol = currencySymbol,
                                selectedCategory = selectedCategory,
                                selectedAccount = activeSourceAccount,
                                description = txDescription,
                                hint = txHint,
                                timestamp = selectedTimestamp,
                                onPickDateClicked = {
                                    showNativeDatePicker(context, selectedTimestamp) { newTs ->
                                        selectedTimestamp = newTs
                                    }
                                },
                                applyInstaPayFee = applyInstaPayFee,
                                onInstaPayFeeToggled = { applyInstaPayFee = it },
                                onConfirm = {
                                    val amt = txAmountStr.toDoubleOrNull()
                                    if (amt != null && amt > 0.0 && sourceAccountId.isNotEmpty()) {
                                        var finalAmt = if (isExpense) -amt else amt
                                        if (isExpense && applyInstaPayFee && activeSourceAccount?.insta_pay_fee == true) {
                                            val feeAmt = minOf(amt * 0.001, 20.0)
                                            finalAmt -= feeAmt
                                        }
                                        val categoryKey = selectedCategory?.key ?: "cat_other"
                                        val descText = txDescription.trim()
                                        val finalMerchant = "$categoryKey|$descText"

                                        viewModel.addAccountTransaction(
                                            sourceAccountId,
                                            finalAmt,
                                            finalMerchant,
                                            "MANUAL",
                                            selectedTimestamp
                                        )
                                        com.oryno.piggy_ledger.ui.ToastUtil.show(
                                            context,
                                            context.getString(R.string.toast_transaction_added),
                                            Toast.LENGTH_SHORT
                                        )
                                        onDismiss()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // CREATE CUSTOM CATEGORY 2-SLIDE BOTTOM SHEET
    if (showCreateCategorySheet) {
        CreateCustomCategoryBottomSheet(
            onDismiss = { showCreateCategorySheet = false },
            onCategoryCreated = { newCat ->
                allCategories.add(0, newCat)
                selectedCategory = newCat
                showCreateCategorySheet = false
            }
        )
    }
}

// SLIDE 0: AMOUNT & TYPE (POS/POSTER NUMERIC KEYPAD)
@Composable
fun Step0AmountScreen(
    isExpense: Boolean,
    onTypeChanged: (Boolean) -> Unit,
    amountStr: String,
    onAmountChanged: (String) -> Unit,
    currencySymbol: String,
    onNavigateToTransfer: (() -> Unit)? = null,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Expense vs Income Segmented Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFE2E8F0))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { onTypeChanged(true) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isExpense) PinkPrimary else Color.Transparent,
                    contentColor = if (isExpense) Color.White else Color(0xFF64748B)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isExpense) 2.dp else 0.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.expense), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Button(
                onClick = { onTypeChanged(false) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isExpense) Color(0xFF10B981) else Color.Transparent,
                    contentColor = if (!isExpense) Color.White else Color(0xFF64748B)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = if (!isExpense) 2.dp else 0.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.income), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        if (onNavigateToTransfer != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                onClick = onNavigateToTransfer,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = PinkPrimary.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, PinkPrimary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.transfer_between_accounts),
                        fontWeight = FontWeight.Bold,
                        color = PinkPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Large Display Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isExpense) stringResource(R.string.enter_expense_amount) else stringResource(R.string.enter_income_amount),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currencySymbol,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isExpense) PinkPrimary else Color(0xFF10B981),
                    modifier = Modifier.padding(end = 8.dp)
                )

                Text(
                    text = amountStr.ifEmpty { "0" },
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = NavyDark,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Addition Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(50, 100, 200, 500).forEach { addVal ->
                SuggestionChip(
                    onClick = {
                        val curr = amountStr.toDoubleOrNull() ?: 0.0
                        onAmountChanged(String.format(Locale.US, "%.0f", curr + addVal))
                    },
                    label = { Text("+$addVal", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color.White,
                        labelColor = NavyDark
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // POS Style Numeric Keypad (0-9, dot, backspace)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val keyRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(".", "0", "⌫")
            )

            keyRows.forEach { rowKeys ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowKeys.forEach { key ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    when (key) {
                                        "⌫" -> if (amountStr.isNotEmpty()) onAmountChanged(amountStr.dropLast(1))
                                        "." -> if (!amountStr.contains(".")) onAmountChanged(if (amountStr.isEmpty()) "0." else "$amountStr.")
                                        else -> {
                                            if (amountStr == "0") onAmountChanged(key)
                                            else onAmountChanged(amountStr + key)
                                        }
                                    }
                                },
                            color = Color.White,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 1.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (key == "⌫") {
                                    Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = NavyDark)
                                } else {
                                    Text(
                                        text = key,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Continue Button
        val isAmountValid = amountStr.toDoubleOrNull() != null && amountStr.toDoubleOrNull()!! > 0
        Button(
            onClick = onNext,
            enabled = isAmountValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isExpense) PinkPrimary else Color(0xFF10B981)
            )
        ) {
            Text(stringResource(R.string.next_select_category), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

// SLIDE 1: CATEGORY SELECTION (simple vertical scrollable list, tap to choose)
@Composable
fun Step1CategoryArcWheelScreen(
    isExpense: Boolean,
    categories: List<TransactionCategory>,
    selectedCategory: TransactionCategory?,
    onCategorySelected: (TransactionCategory) -> Unit,
    onCreateCategoryClicked: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.select_category_caps),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.2.sp
        )
        Text(
            text = "Tap a category to select it",
            fontSize = 13.sp,
            color = TextLight,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory?.key == cat.key
                val catDisplayName = if (cat.customName != null) {
                    cat.customName
                } else if (cat.nameRes != null) {
                    stringResource(cat.nameRes)
                } else {
                    resolveCategoryDisplayName(cat.key)
                }
                
                val highlightColor = if (isExpense) PinkPrimary else Color(0xFF10B981)
                val selectedBg = Color(0xFFE2E8F0)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onCategorySelected(cat) },
                    color = if (isSelected) Color(0xFFE2E8F0) else Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = if (isSelected) BorderStroke(1.5.dp, highlightColor) else BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) Color(0xFFE2E8F0) else Color.White)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color.Transparent else Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = catDisplayName,
                                tint = if (isSelected) highlightColor else Color(0xFF94A3B8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = catDisplayName,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) NavyDark else Color(0xFF475569),
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = highlightColor, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onCreateCategoryClicked() },
                    color = PinkPrimary.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, PinkPrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.new_category_plus),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = PinkPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNext,
            enabled = selectedCategory != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
        ) {
            Text(stringResource(R.string.next_select_account), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}
// SLIDE 2: ACCOUNT SELECTION
@Composable
fun Step2AccountSelectionScreen(
    accounts: List<Account>,
    selectedAccountId: String,
    onAccountSelected: (String) -> Unit,
    onNavigateToAddAccount: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.target_account_caps),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.2.sp
        )
        Text(
            text = stringResource(R.string.choose_account_applies),
            fontSize = 14.sp,
            color = TextLight,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(accounts) { acc ->
                val isSelected = selectedAccountId == acc.id
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onAccountSelected(acc.id) },
                    color = if (isSelected) Color(0xFFE2E8F0) else Color.White,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) PinkPrimary else Color(0xFFE2E8F0)),
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) Color(0xFFE2E8F0) else Color.White)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BrandLogo(
                            provider = acc.provider ?: acc.name,
                            accountType = acc.type,
                            iconColorHex = acc.icon_color,
                            logoUrl = acc.logo_url,
                            modifier = Modifier.size(44.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            val nameWithLabel = if (!acc.label.isNullOrBlank()) "${acc.name} (${acc.label})" else acc.name
                            Text(text = nameWithLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NavyDark)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${getCurrencySymbol(acc.currency)} ${String.format(Locale.getDefault(), "%,.2f", acc.current_balance)}",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { onAccountSelected(acc.id) },
                            colors = RadioButtonDefaults.colors(selectedColor = PinkPrimary)
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onNavigateToAddAccount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, PinkPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = PinkPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_new_account_btn), color = PinkPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNext,
            enabled = selectedAccountId.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
        ) {
            Text(stringResource(R.string.next_add_description), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

// SLIDE 3: DESCRIPTION & HINT
@Composable
fun Step3DescriptionHintScreen(
    description: String,
    onDescriptionChanged: (String) -> Unit,
    hint: String,
    onHintChanged: (String) -> Unit,
    isExpense: Boolean,
    onNext: () -> Unit
) {
    val quickSuggestions = if (isExpense) listOf("Coffee ☕", "Groceries 🛒", "Uber 🚗", "Dinner 🍽️", "Rent 🏠", "Shopping 🛍️")
    else listOf("Salary 💰", "Freelance 💻", "Transfer 🔄", "Refund ↩️", "Bonus 🎁")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = "DESCRIPTION & NOTES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.2.sp
        )
        Text(
            text = stringResource(R.string.review_details_before_logging),
            fontSize = 14.sp,
            color = TextLight,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        // Description Input
        Text(stringResource(R.string.description_merchant), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyDark)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChanged,
            placeholder = { Text(if (isExpense) stringResource(R.string.where_did_you_spend_placeholder) else stringResource(R.string.source_of_income_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = PinkPrimary,
                unfocusedBorderColor = Color(0xFFE2E8F0)
            ),
            singleLine = true
        )

        // Quick Suggestion Chips
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickSuggestions.take(3).forEach { sug ->
                SuggestionChip(
                    onClick = { onDescriptionChanged(sug) },
                    label = { Text(sug, fontSize = 12.sp) },
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hint/Note Input
        Text(stringResource(R.string.notes_extra_hint_optional), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyDark)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = hint,
            onValueChange = onHintChanged,
            placeholder = { Text(stringResource(R.string.add_extra_note_memo)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = PinkPrimary,
                unfocusedBorderColor = Color(0xFFE2E8F0)
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
        ) {
            Text(stringResource(R.string.next_date_fees), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

// SLIDE 4: DATE, INSTAPAY FEE & CONFIRMATION
@Composable
fun Step4ConfirmationScreen(
    isExpense: Boolean,
    amountStr: String,
    currencySymbol: String,
    selectedCategory: TransactionCategory?,
    selectedAccount: Account?,
    description: String,
    hint: String,
    timestamp: Long,
    onPickDateClicked: () -> Unit,
    applyInstaPayFee: Boolean,
    onInstaPayFeeToggled: (Boolean) -> Unit,
    onConfirm: () -> Unit
) {
    val formattedDate = remember(timestamp) {
        SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.confirm_and_save_caps),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.2.sp
        )
        Text(
            text = stringResource(R.string.review_details_before_logging),
            fontSize = 14.sp,
            color = TextLight,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // DATE PICKER ROW
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onPickDateClicked() },
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PinkPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.date_label_caps), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyDark)
                }

                Text(formattedDate, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = PinkPrimary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // INSTAPAY FEE OPTION (IF APPLICABLE)
        if (isExpense && selectedAccount?.insta_pay_fee == true) {
            val rawAmt = amountStr.toDoubleOrNull() ?: 0.0
            val feeAmt = minOf(rawAmt * 0.001, 20.0)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onInstaPayFeeToggled(!applyInstaPayFee) },
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = applyInstaPayFee,
                        onCheckedChange = onInstaPayFeeToggled,
                        colors = CheckboxDefaults.colors(checkedColor = PinkPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.transfer_between_accounts), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyDark)
                        if (applyInstaPayFee && feeAmt > 0) {
                            Text(stringResource(R.string.fee_format, currencySymbol, String.format(Locale.getDefault(), "%,.2f", feeAmt)), fontSize = 12.sp, color = PinkPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // TRANSACTION SUMMARY CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.total_amount), fontSize = 13.sp, color = TextLight, fontWeight = FontWeight.Medium)
                    Text(
                        text = "${if (isExpense) "-" else "+"}$currencySymbol $amountStr",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isExpense) PinkPrimary else Color(0xFF10B981)
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Summary Rows
                SummaryDetailRow(label = stringResource(R.string.category_title), value = selectedCategory?.let { resolveCategoryDisplayName(it.key) } ?: stringResource(R.string.cat_other))
                SummaryDetailRow(label = stringResource(R.string.select_account), value = selectedAccount?.name ?: stringResource(R.string.select_account))
                if (description.isNotBlank()) {
                    SummaryDetailRow(label = stringResource(R.string.merchant_description), value = description)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // CONFIRM BUTTON
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isExpense) PinkPrimary else Color(0xFF10B981))
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.confirm_and_add_transaction), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun SummaryDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = TextLight)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
    }
}

// 2-SLIDE MODAL BOTTOM SHEET TO CREATE CUSTOM CATEGORY
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCustomCategoryBottomSheet(
    onDismiss: () -> Unit,
    onCategoryCreated: (TransactionCategory) -> Unit
) {
    var sheetStep by remember { mutableIntStateOf(0) } // 0: Name, 1: Icons
    var categoryName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf<ImageVector>(Icons.Default.Star) }

    val iconList = listOf(
        Icons.Default.ShoppingCart, Icons.Default.Store, Icons.Default.ShoppingBag, Icons.Default.LocalOffer,
        Icons.Default.Restaurant, Icons.Default.LocalCafe, Icons.Default.Fastfood, Icons.Default.Liquor, Icons.Default.BakeryDining,
        Icons.Default.DirectionsCar, Icons.Default.Flight, Icons.Default.DirectionsBus, Icons.Default.Train, Icons.Default.Commute,
        Icons.Default.Movie, Icons.Default.SportsEsports, Icons.Default.MusicNote, Icons.Default.Brush, Icons.Default.CameraAlt, Icons.Default.FitnessCenter,
        Icons.Default.Favorite, Icons.Default.Spa, Icons.Default.MedicalServices, Icons.Default.LocalPharmacy, Icons.Default.Pets,
        Icons.Default.Home, Icons.Default.Lightbulb, Icons.Default.Computer, Icons.Default.Smartphone, Icons.Default.Tv, Icons.Default.Wifi,
        Icons.Default.AccountBalance, Icons.Default.CreditCard, Icons.Default.Work, Icons.Default.School, Icons.Default.TrendingUp, Icons.Default.Payments
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFCBD5E1)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Progress / Back
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (sheetStep == 1) {
                    IconButton(onClick = { sheetStep = 0 }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NavyDark)
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Text(
                    text = if (sheetStep == 0) stringResource(R.string.step_format, 1, 2) + ": " + stringResource(R.string.new_category_name_title) else stringResource(R.string.step_format, 2, 2) + ": " + stringResource(R.string.next_choose_icon),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = PinkPrimary
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(targetState = sheetStep, label = "SheetStepAnimation") { step ->
                if (step == 0) {
                    // SLIDE 1: CATEGORY NAME
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.new_category_name_title),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyDark
                        )
                        Text(
                            text = stringResource(R.string.enter_title_custom_cat),
                            fontSize = 13.sp,
                            color = TextLight,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                        )

                        OutlinedTextField(
                            value = categoryName,
                            onValueChange = { categoryName = it },
                            placeholder = { Text(stringResource(R.string.custom_cat_placeholder)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PinkPrimary,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                cursorColor = PinkPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = { if (categoryName.isNotBlank()) sheetStep = 1 },
                            enabled = categoryName.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                        ) {
                            Text(stringResource(R.string.next_choose_icon), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                } else {
                    // SLIDE 2: RICH ICON SELECTOR
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.choose_icon_for_format, categoryName),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyDark
                        )
                        Text(
                            text = stringResource(R.string.select_from_symbols_desc),
                            fontSize = 13.sp,
                            color = TextLight,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        // Rich Icon Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(6),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            items(iconList) { ic ->
                                val isSelected = selectedIcon == ic
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) PinkPrimary else Color(0xFFF1F5F9))
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) PinkPrimary else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedIcon = ic },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = ic,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else NavyDark,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val cleanKey = "custom_${categoryName.trim().lowercase().replace(" ", "_")}"
                                val newCat = TransactionCategory(
                                    key = cleanKey,
                                    customName = categoryName.trim(),
                                    icon = selectedIcon
                                )
                                onCategoryCreated(newCat)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.create_category), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
