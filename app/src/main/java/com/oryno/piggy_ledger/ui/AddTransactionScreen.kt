package com.oryno.piggy_ledger.ui
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import java.text.SimpleDateFormat
import java.util.*

data class TransactionCategory(
    val key: String,
    val nameRes: Int,
    val icon: ImageVector,
    val descRes: Int? = null
)

val categoriesList = listOf(
    TransactionCategory("atm_withdrawal", R.string.cat_atm_withdrawal, Icons.Default.LocalAtm),
    TransactionCategory("bonus", R.string.cat_bonus, Icons.Default.CardMembership),
    TransactionCategory("cashback", R.string.cat_cashback, Icons.Default.LocalOffer),
    TransactionCategory("credit_card_settlement", R.string.cat_credit_card_settlement, Icons.Default.CreditCard, R.string.cat_credit_card_settlement_desc),
    TransactionCategory("education", R.string.cat_education, Icons.Default.School),
    TransactionCategory("entertainment", R.string.cat_entertainment, Icons.Default.Movie),
    TransactionCategory("food", R.string.cat_food, Icons.Default.Restaurant),
    TransactionCategory("freelance", R.string.cat_freelance, Icons.Default.Computer),
    TransactionCategory("fuel", R.string.cat_fuel, Icons.Default.LocalGasStation),
    TransactionCategory("health", R.string.cat_health, Icons.Default.Favorite),
    TransactionCategory("home", R.string.cat_home, Icons.Default.Home),
    TransactionCategory("investment", R.string.cat_investment, Icons.Default.TrendingUp),
    TransactionCategory("other", R.string.cat_other, Icons.Default.Payments),
    TransactionCategory("personal_care", R.string.cat_personal_care, Icons.Default.Spa),
    TransactionCategory("refund", R.string.cat_refund, Icons.Default.Undo),
    TransactionCategory("salary", R.string.cat_salary, Icons.Default.AccountBalanceWallet),
    TransactionCategory("shopping", R.string.cat_shopping, Icons.Default.ShoppingCart),
    TransactionCategory("subscriptions", R.string.cat_subscriptions, Icons.Default.Subscriptions),
    TransactionCategory("transfer", R.string.cat_transfer, Icons.Default.SwapHoriz),
    TransactionCategory("transportation", R.string.cat_transportation, Icons.Default.DirectionsCar),
    TransactionCategory("travel", R.string.cat_travel, Icons.Default.Flight),
    TransactionCategory("utilities", R.string.cat_utilities, Icons.Default.Lightbulb),
    TransactionCategory("work", R.string.cat_work, Icons.Default.Business)
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
    return if (catItem != null) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: PiggyLedgerViewModel,
    selectedAccountId: String?,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onNavigateToAddAccount: () -> Unit = {},
    onNavigateToSettingsPro: () -> Unit = {}
) {
    val context = LocalContext.current
    val isPremium by viewModel.isPremium.collectAsState()
    // Core Transaction Data
    var isExpense by remember { mutableStateOf(true) }
    
    var txAmountStr by remember { mutableStateOf("") }
    var txDescription by remember { mutableStateOf("") }
    var txHint by remember { mutableStateOf("") }
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Selected Account
    var sourceAccountId by remember { mutableStateOf(selectedAccountId ?: accounts.firstOrNull()?.id ?: "") }
    
    // Bottom Sheets State
    var showCategoryBottomSheet by remember { mutableStateOf(false) }
    var showSourceAccountPicker by remember { mutableStateOf(false) }
    var showCustomCategoryBottomSheet by remember { mutableStateOf(false) }
    var showProCategoryDialog by remember { mutableStateOf(false) }
    
    // Selected Category (stored as TransactionCategory)
    var selectedCategory by remember { mutableStateOf<TransactionCategory?>(null) }
    var customCategoryName by remember { mutableStateOf("") }

    val activeSourceAccount = accounts.find { it.id == sourceAccountId }

    // Date Format
    val formattedDate = remember(selectedTimestamp) {
        SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(Date(selectedTimestamp))
    }

    var showDatePicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAFC) // Beautiful light slate gray background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = NavyDark)
                        }
                        
                        Text(
                            text = stringResource(R.string.new_transaction),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                        
                        IconButton(
                            onClick = {
                                try {
                                    com.posthog.PostHog.capture(event = "button_clicked", properties = mapOf("button_name" to "Add Transaction", "screen" to "AddTransactionScreen"))
                                } catch (e: Exception) {}
                                val amt = txAmountStr.toDoubleOrNull()
                                if (amt != null && amt > 0.0 && sourceAccountId != "") {
                                    val finalAmt = if (isExpense) -amt else amt
                                    val categoryKey = if (selectedCategory != null) {
                                        selectedCategory!!.key
                                    } else {
                                        "cat_other"
                                    }
                                    // Combine Description and Hint
                                    val descText = txDescription.trim()
                                    val finalMerchant = "$categoryKey|$descText"
                                    
                                    viewModel.addAccountTransaction(sourceAccountId, finalAmt, finalMerchant, "MANUAL", selectedTimestamp)
                                    com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.toast_transaction_added), Toast.LENGTH_SHORT)
                                    onDismiss()
                                }
                            },
                            enabled = txAmountStr.toDoubleOrNull() != null && txAmountStr.toDoubleOrNull()!! > 0.0
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Save",
                                tint = if (txAmountStr.toDoubleOrNull() != null) PinkPrimary else Color.LightGray
                            )
                        }
                    }
                    
                    // Scrollable Form Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Custom Segmented Tab for Expense vs Income
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { isExpense = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isExpense) PinkPrimary else Color.Transparent,
                                    contentColor = if (isExpense) Color.White else Color(0xFF64748B)
                                ),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Text(stringResource(R.string.expense), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            
                            Button(
                                onClick = { isExpense = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isExpense) Color(0xFF10B981) else Color.Transparent,
                                    contentColor = if (!isExpense) Color.White else Color(0xFF64748B)
                                ),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Text(stringResource(R.string.income), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        // 2. Large Amount Input Display
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeSourceAccount?.currency ?: "EGP",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            
                            Box(modifier = Modifier.weight(1f)) {
                                if (txAmountStr.isEmpty()) {
                                    Text(
                                        text = "0",
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                                BasicTextField(
                                    value = txAmountStr,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() || it == '.' }) {
                                            txAmountStr = input
                                        }
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDark
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // 3. CATEGORY section
                        Column {
                            Text(
                                text = stringResource(R.string.category_label),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                    .clickable { showCategoryBottomSheet = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PinkPrimary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = selectedCategory?.icon ?: Icons.Default.Category,
                                        contentDescription = null,
                                        tint = PinkPrimary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Text(
                                    text = if (selectedCategory != null) {
                                        if (selectedCategory!!.key.startsWith("custom_")) {
                                            selectedCategory!!.key.substringAfter("custom_")
                                        } else {
                                            stringResource(selectedCategory!!.nameRes)
                                        }
                                    } else {
                                        stringResource(R.string.select_category_placeholder)
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NavyDark,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // 4. ACCOUNT section (Source Account)
                        Column {
                            Text(
                                text = stringResource(R.string.account_label_caps),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                    .clickable { showSourceAccountPicker = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (activeSourceAccount != null) {
                                    BrandLogo(
                                        provider = activeSourceAccount.provider,
                                        accountType = activeSourceAccount.type,
                                        iconColorHex = activeSourceAccount.icon_color,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activeSourceAccount?.name ?: "",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDark
                                    )
                                    Text(
                                        text = activeSourceAccount?.currency ?: "EGP",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // 5. DESCRIPTION Field
                        Column {
                            Text(
                                text = stringResource(R.string.description_optional_label),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            OutlinedTextField(
                                value = txDescription,
                                onValueChange = { txDescription = it },
                                placeholder = {
                                    Text(
                                        text = if (isExpense) {
                                            stringResource(R.string.spend_on_placeholder)
                                        } else {
                                            stringResource(R.string.earn_from_placeholder)
                                        },
                                        color = Color(0xFF94A3B8)
                                    )
                                },
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
                        }

                        // 6. HINT/NOTE Field
                        Column {
                            Text(
                                text = stringResource(R.string.hint_optional_label),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            OutlinedTextField(
                                value = txHint,
                                onValueChange = { txHint = it },
                                placeholder = { Text(stringResource(R.string.add_note_placeholder), color = Color(0xFF94A3B8)) },
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
                        }

                        // 7. DATE section
                        Column {
                            Text(
                                text = stringResource(R.string.date_label_caps),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                    .clickable { showDatePicker = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        stringResource(R.string.pick_date),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                
                                Text(
                                    text = formattedDate,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PinkPrimary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // CATEGORIES BOTTOM SHEET (Replacing full-screen and popup dialog)
                if (showCategoryBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showCategoryBottomSheet = false },
                        containerColor = Color.White,
                        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFCBD5E1)) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.category_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = NavyDark,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                            
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 400.dp)
                                    .padding(horizontal = 16.dp)
                            ) {
                                items(categoriesList.filter { it.key != "cat_transfer" }) { cat ->
                                    val isSelected = selectedCategory?.key == cat.key
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                selectedCategory = cat
                                                showCategoryBottomSheet = false
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) PinkPrimary.copy(alpha = 0.05f) else Color.Transparent
                                        ),
                                        border = if (isSelected) BorderStroke(1.5.dp, PinkPrimary) else null
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSelected) PinkPrimary.copy(alpha = 0.15f)
                                                        else Color(0xFFF1F5F9)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = cat.icon,
                                                    contentDescription = null,
                                                    tint = if (isSelected) PinkPrimary else Color(0xFF475569)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.width(16.dp))
                                            
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = stringResource(cat.nameRes),
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NavyDark
                                                )
                                                if (cat.descRes != null) {
                                                    Text(
                                                        text = stringResource(cat.descRes),
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                item {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .clickable {
                                                if (!isPremium) {
                                                    showCategoryBottomSheet = false
                                                    showProCategoryDialog = true
                                                } else {
                                                    showCategoryBottomSheet = false
                                                    showCustomCategoryBottomSheet = true
                                                }
                                            },
                                        color = Color(0xFFF8FAFC),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f, fill = false)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AddCircleOutline,
                                                    contentDescription = null,
                                                    tint = PinkPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = stringResource(R.string.cant_find_it_create_own),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PinkPrimary
                                                )
                                            }
                                            if (!isPremium) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = PinkPrimary.copy(alpha = 0.12f)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Lock,
                                                            contentDescription = "Pro Feature",
                                                            tint = PinkPrimary,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = "PRO",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = PinkPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SOURCE ACCOUNT PICKER BOTTOM SHEET
                if (showSourceAccountPicker) {
                    ModalBottomSheet(
                        onDismissRequest = { showSourceAccountPicker = false },
                        containerColor = Color.White,
                        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFCBD5E1)) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.target_account),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = NavyDark,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                            
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .padding(horizontal = 16.dp)
                            ) {
                                items(accounts) { acc ->
                                    val isSelected = sourceAccountId == acc.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) PinkPrimary.copy(alpha = 0.05f) else Color.Transparent)
                                            .clickable {
                                                sourceAccountId = acc.id
                                                showSourceAccountPicker = false
                                            }
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BrandLogo(
                                            provider = acc.provider,
                                            accountType = acc.type,
                                            iconColorHex = acc.icon_color,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(acc.name, fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 15.sp)
                                            Text("${acc.type.name} • ${acc.currency} ${acc.current_balance}", color = Color(0xFF64748B), fontSize = 12.sp)
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = PinkPrimary)
                                        }
                                    }
                                }

                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(Color(0xFFE2E8F0))
                                            .padding(vertical = 8.dp)
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                showSourceAccountPicker = false
                                                onDismiss()
                                                onNavigateToAddAccount()
                                            }
                                            .padding(horizontal = 14.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(PinkPrimary.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = PinkPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = "Add a new account",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = PinkPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // CUSTOM CATEGORY BOTTOM SHEET
                if (showCustomCategoryBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showCustomCategoryBottomSheet = false },
                        containerColor = Color.White,
                        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFCBD5E1)) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                                .padding(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.custom_category),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = NavyDark
                            )
                            
                            Text(
                                text = stringResource(R.string.enter_custom_category),
                                fontSize = 14.sp,
                                color = Color(0xFF64748B)
                            )
                            
                            OutlinedTextField(
                                value = customCategoryName,
                                onValueChange = { customCategoryName = it },
                                placeholder = { Text(stringResource(R.string.eg_subscription)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PinkPrimary,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                singleLine = true
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TextButton(
                                    onClick = { showCustomCategoryBottomSheet = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.cancel_btn), color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                }
                                                                Button(
                                    onClick = {
                                        if (!isPremium) {
                                            showCustomCategoryBottomSheet = false
                                            showProCategoryDialog = true
                                            return@Button
                                        }
                                        if (customCategoryName.isNotBlank()) {
                                            selectedCategory = TransactionCategory(
                                                key = "custom_${customCategoryName.trim()}",
                                                nameRes = 0,
                                                icon = Icons.Default.Category
                                            )
                                            showCustomCategoryBottomSheet = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.confirm_btn), fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // PRO CUSTOM CATEGORIES DIALOG
                if (showProCategoryDialog) {
                    AlertDialog(
                        onDismissRequest = { showProCategoryDialog = false },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = Color.White,
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(PinkPrimary.copy(alpha = 0.12f), RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = PinkPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        title = {
                            Text(
                                text = stringResource(R.string.custom_categories_pro_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = NavyDark,
                                textAlign = TextAlign.Center
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.custom_categories_pro_desc),
                                fontSize = 14.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showProCategoryDialog = false
                                    onDismiss()
                                    onNavigateToSettingsPro()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.ai_upgrade_to_pro),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showProCategoryDialog = false }) {
                                Text(
                                    text = stringResource(R.string.cancel_btn),
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    )
                }

                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedTimestamp)
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                val selectedDate = datePickerState.selectedDateMillis
                                if (selectedDate != null) {
                                    selectedTimestamp = selectedDate
                                }
                                showDatePicker = false
                            }) { Text(stringResource(R.string.confirm_btn), color = PinkPrimary, fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel_btn), color = Color(0xFF64748B)) }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
            }
        }
    }
}
