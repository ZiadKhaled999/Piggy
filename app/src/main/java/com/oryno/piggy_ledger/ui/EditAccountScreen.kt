package com.oryno.piggy_ledger.ui

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.R
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.data.AccountType
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountScreen(
    accountId: Long,
    viewModel: PiggyLedgerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val account = remember { viewModel.allAccounts.value.find { it.id == accountId } }
    
    var name by remember { mutableStateOf("") }
    var accountLabel by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.BANK) }
    var currency by remember { mutableStateOf("EGP") }
    var startingBalance by remember { mutableStateOf("") }
    var currentBalance by remember { mutableStateOf("") }
    var excludeFromAll by remember { mutableStateOf(false) }
    
    // Card specific
    var creditLimit by remember { mutableStateOf("") }
    var availableCredit by remember { mutableStateOf("") }
    var paymentDueDay by remember { mutableStateOf("") }
    var cardNumbers by remember { mutableStateOf("") }
    
    // Bank specific
    var bankAccountNo by remember { mutableStateOf("") }
    
    // Wallet specific
    var provider by remember { mutableStateOf("") }
    
    // Fees
    var instaPayFee by remember { mutableStateOf(true) }

    // Color & Icon Picker State
    var selectedColorHex by remember { mutableStateOf("#3B82F6") }
    var selectedIconName by remember { mutableStateOf("AccountBalance") }
    var userCustomizedColor by remember { mutableStateOf(false) }

    // Bottom Sheet State
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    var bottomSheetType by remember { mutableStateOf(BottomSheetType.NONE) }
    var currencySearchQuery by remember { mutableStateOf("") }
    var bankSearchQuery by remember { mutableStateOf("") }
    
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val availableCurrencies = listOf(
        CurrencyInfo("USD", "US Dollar", "🇺🇸", "$"),
        CurrencyInfo("EUR", "Euro", "🇪🇺", "€"),
        CurrencyInfo("EGP", "Egyptian Pound", "🇪🇬", "EGP"),
        CurrencyInfo("GBP", "British Pound", "🇬🇧", "£"),
        CurrencyInfo("AED", "UAE Dirham", "🇦🇪", "AED"),
        CurrencyInfo("SAR", "Saudi Riyal", "🇸🇦", "SAR"),
        CurrencyInfo("OMR", "Omani Rial", "🇴🇲", "OMR"),
        CurrencyInfo("JPY", "Japanese Yen", "🇯🇵", "¥"),
        CurrencyInfo("CNY", "Chinese Yuan", "🇨🇳", "CN¥")
    )

    val availableProviders = listOf(
        ProviderInfo("Vodafone Cash", Color(0xFFE11D48), "#E11D48"),
        ProviderInfo("Orange Cash", Color(0xFFEA580C), "#EA580C"),
        ProviderInfo("e& Cash", Color(0xFF16A34A), "#16A34A"),
        ProviderInfo("WE Pay", Color(0xFF7C3AED), "#7C3AED")
    )

    val availableBanks = listOf(
        "Commercial International Bank (CIB)",
        "National Bank of Egypt (NBE)",
        "Banque Misr",
        "QNB Alahli",
        "Banque Du Caire",
        "Arab African International Bank (AAIB)",
        "HSBC Egypt",
        "Alex Bank",
        "Faisal Islamic Bank",
        "Abu Dhabi Islamic Bank (ADIB)",
        "Credit Agricole Egypt",
        "Egyptian Gulf Bank (EG Bank)",
        "SAIB Bank",
        "Al Baraka Bank",
        "Housing and Development Bank (HDB)",
        "Attijariwafa Bank Egypt",
        "Arab Bank",
        "Abu Dhabi Commercial Bank (ADCB)",
        "EBank (Export Development Bank of Egypt)",
        "The United Bank of Egypt",
        "Suez Canal Bank",
        "Mashreq Bank",
        "Emirates NBD Egypt",
        "Citibank Egypt"
    )

    val customColors = listOf(
        "#3B82F6" to "Blue",
        "#8B5CF6" to "Purple",
        "#10B981" to "Emerald",
        "#F59E0B" to "Amber",
        "#EF4444" to "Red",
        "#EC4899" to "Pink",
        "#06B6D4" to "Cyan",
        "#14B8A6" to "Teal",
        "#6366F1" to "Indigo",
        "#22C55E" to "Green",
        "#F43F5E" to "Rose",
        "#1E293B" to "Slate"
    )

    LaunchedEffect(account) {
        account?.let {
            name = it.name
            type = it.type
            currency = it.currency
            startingBalance = it.starting_balance.toString()
            currentBalance = it.current_balance.toString()
            excludeFromAll = it.exclude_from_all
            creditLimit = it.credit_limit?.toString() ?: ""
            availableCredit = it.available_credit?.toString() ?: ""
            paymentDueDay = it.payment_due_day?.toString() ?: ""
            cardNumbers = it.card_numbers ?: ""
            bankAccountNo = it.bank_account_no ?: ""
            provider = it.provider ?: ""
            instaPayFee = it.insta_pay_fee
            selectedColorHex = it.icon_color
            selectedIconName = it.icon_name
            accountLabel = it.label ?: ""
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PinkPrimary,
        unfocusedBorderColor = Color(0xFFE2E8F0),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedLabelColor = PinkPrimary,
        unfocusedLabelColor = TextLight
    )

    fun openSheet(type: BottomSheetType) {
        bottomSheetType = type
        showBottomSheet = true
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.edit_account), fontWeight = FontWeight.Black, fontSize = 20.sp, color = NavyDark) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NavyDark)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (name.isNotBlank() && account != null) {
                                val limit = creditLimit.toDoubleOrNull()
                                val available = availableCredit.toDoubleOrNull() ?: limit
                                val initialBalance = if (type == AccountType.CARD) {
                                    (available ?: 0.0) - (limit ?: 0.0)
                                } else {
                                    startingBalance.toDoubleOrNull() ?: 0.0
                                }
                                val currentBalanceVal = if (type == AccountType.CARD) {
                                    (available ?: 0.0) - (limit ?: 0.0)
                                } else {
                                    currentBalance.toDoubleOrNull() ?: initialBalance
                                }

                                viewModel.updateAccount(
                                    account.copy(
                                        name = name,
                                        type = type,
                                        icon_color = selectedColorHex,
                                        icon_name = selectedIconName,
                                        currency = currency,
                                        starting_balance = initialBalance,
                                        current_balance = currentBalanceVal,
                                        exclude_from_all = excludeFromAll,
                                        credit_limit = limit,
                                        available_credit = available,
                                        payment_due_day = paymentDueDay.toIntOrNull(),
                                        card_numbers = cardNumbers.takeIf { it.isNotBlank() },
                                        bank_account_no = bankAccountNo.takeIf { it.isNotBlank() },
                                        provider = provider.takeIf { it.isNotBlank() },
                                        insta_pay_fee = instaPayFee,
                                        label = accountLabel.takeIf { it.isNotBlank() }
                                    )
                                )
                                Toast.makeText(context, context.getString(R.string.toast_account_updated), Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, disabledContainerColor = PinkPrimary.copy(alpha = 0.5f)),
                        enabled = name.isNotBlank()
                    ) {
                        Text(stringResource(R.string.save), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // SECTION: General Info
            SectionCard(title = stringResource(R.string.general_info)) {
                // Account Name
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.account_name), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text(stringResource(R.string.account_name_placeholder), color = TextLight) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Medium, color = NavyDark, fontSize = 16.sp),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )
                }

                // Account Label
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.account_label), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    OutlinedTextField(
                        value = accountLabel,
                        onValueChange = { accountLabel = it },
                        placeholder = { Text(stringResource(R.string.account_label_placeholder), color = TextLight) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Medium, color = NavyDark, fontSize = 16.sp),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )
                }

                // Account Type
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.account_type), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val types = listOf<Triple<AccountType, String, androidx.compose.ui.graphics.vector.ImageVector>>(
                            Triple(AccountType.BANK, stringResource(R.string.bank), Icons.Default.AccountBalance),
                            Triple(AccountType.CARD, stringResource(R.string.card), Icons.Default.CreditCard),
                            Triple(AccountType.CASH, stringResource(R.string.cash), Icons.Default.Payments),
                            Triple(AccountType.WALLET, stringResource(R.string.e_wallet), Icons.Default.AccountBalanceWallet)
                        )
                        types.forEach { (accType, label, icon) ->
                            val isSelected = type == accType
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) PinkPrimary.copy(alpha = 0.1f) else Color(0xFFF8FAFC))
                                    .clickable { 
                                        type = accType 
                                        provider = ""
                                        if (!userCustomizedColor) {
                                            selectedColorHex = when (accType) {
                                                AccountType.BANK -> "#3B82F6"
                                                AccountType.CARD -> "#8B5CF6"
                                                AccountType.CASH -> "#10B981"
                                                AccountType.WALLET -> "#F59E0B"
                                            }
                                            selectedIconName = when (accType) {
                                                AccountType.BANK -> "AccountBalance"
                                                AccountType.CARD -> "CreditCard"
                                                AccountType.CASH -> "Payments"
                                                AccountType.WALLET -> "AccountBalanceWallet"
                                            }
                                        }
                                    }
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) PinkPrimary else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) PinkPrimary else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) PinkPrimary else Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bank selection for BANK/CARD
                if (type == AccountType.BANK || type == AccountType.CARD) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.bank_card_issuer), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .clickable { openSheet(BottomSheetType.BANK_SELECT) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = PinkPrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = provider.ifBlank { stringResource(R.string.select_bank) },
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp,
                                    color = if (provider.isBlank()) TextLight else NavyDark
                                )
                            }
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = TextLight)
                        }
                    }
                }

                // Wallet provider selection for WALLET
                if (type == AccountType.WALLET) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.wallet_provider), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .clickable { openSheet(BottomSheetType.PROVIDER_SELECT) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PinkPrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = provider.ifBlank { stringResource(R.string.select_wallet_provider) },
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp,
                                    color = if (provider.isBlank()) TextLight else NavyDark
                                )
                            }
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = TextLight)
                        }
                    }
                }

                // Balance
                if (type != AccountType.CARD) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.current_balance), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                        OutlinedTextField(
                            value = currentBalance,
                            onValueChange = { input -> 
                                if (input.isEmpty() || input.toDoubleOrNull() != null || input == "-" || input == "-." || input == ".") {
                                    currentBalance = input 
                                }
                            },
                            placeholder = { Text("0.00", color = TextLight) },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Medium, color = NavyDark, fontSize = 16.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            leadingIcon = {
                                Text(
                                    text = availableCurrencies.find { it.code == currency }?.symbol ?: "$",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PinkPrimary,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        )
                    }
                }
            }

            // SECTION: Advanced Settings
            if (type == AccountType.CARD) {
                SectionCard(title = stringResource(R.string.credit_card_details)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.credit_limit), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                            OutlinedTextField(
                                value = creditLimit,
                                onValueChange = { creditLimit = it },
                                placeholder = { Text("0.00", color = TextLight) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.available), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                            OutlinedTextField(
                                value = availableCredit,
                                onValueChange = { availableCredit = it },
                                placeholder = { Text("0.00", color = TextLight) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.payment_due_day), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                            OutlinedTextField(
                                value = paymentDueDay,
                                onValueChange = { paymentDueDay = it },
                                placeholder = { Text("1 - 28", color = TextLight) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.card_numbers_optional), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                            OutlinedTextField(
                                value = cardNumbers,
                                onValueChange = { cardNumbers = it },
                                placeholder = { Text("•••• 4092", color = TextLight) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors
                            )
                        }
                    }
                }
            }

            if (type == AccountType.BANK) {
                SectionCard(title = stringResource(R.string.bank_account_details)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.bank_account_number_optional), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                        OutlinedTextField(
                            value = bankAccountNo,
                            onValueChange = { bankAccountNo = it },
                            placeholder = { Text(stringResource(R.string.card_number_placeholder), color = TextLight) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                    }
                }
            }

            // SECTION: Style & Customization
            SectionCard(title = stringResource(R.string.visual_style)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.card_theme_color), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(48.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(customColors) { (hex, label) ->
                            val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .clickable { 
                                        selectedColorHex = hex 
                                        userCustomizedColor = true
                                    }
                                    .border(
                                        width = 3.dp,
                                        color = if (isSelected) PinkPrimary else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // SECTION: Delete Account
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDeleteConfirmDialog = true }
                    .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.delete_account), tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.delete_account), fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    // Confirmation dialog for deletion
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.delete_account_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_account_confirm_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteAccount(accountId)
                        Toast.makeText(context, context.getString(R.string.toast_account_deleted), Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                ) {
                    Text(stringResource(R.string.delete), color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel), color = NavyDark)
                }
            }
        )
    }

    // Modal Bottom Sheet
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            when (bottomSheetType) {
                BottomSheetType.CURRENCY_SELECT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.8f)
                            .padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.select_currency),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = NavyDark,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                        OutlinedTextField(
                            value = currencySearchQuery,
                            onValueChange = { currencySearchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_ellipsis), color = TextLight) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextLight) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                        val filteredCurrencies = availableCurrencies.filter {
                            it.code.contains(currencySearchQuery, ignoreCase = true) ||
                            it.name.contains(currencySearchQuery, ignoreCase = true)
                        }
                        LazyColumn {
                            items(filteredCurrencies) { item ->
                                val isSelected = currency == item.code
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currency = item.code
                                            showBottomSheet = false
                                        }
                                        .background(if (isSelected) PinkPrimary.copy(alpha = 0.05f) else Color.Transparent)
                                        .padding(horizontal = 24.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = item.flag, fontSize = 28.sp, modifier = Modifier.padding(end = 16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = item.code, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NavyDark)
                                        Text(text = item.name, fontSize = 13.sp, color = TextLight)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = PinkPrimary)
                                    } else {
                                        Text(text = item.symbol, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = TextLight)
                                    }
                                }
                            }
                        }
                    }
                }
                BottomSheetType.BANK_SELECT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.8f)
                            .padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.select_bank),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = NavyDark,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                        OutlinedTextField(
                            value = bankSearchQuery,
                            onValueChange = { bankSearchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_banks_ellipsis), color = TextLight) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextLight) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                        val filteredBanks = availableBanks.filter {
                            it.contains(bankSearchQuery, ignoreCase = true)
                        }
                        LazyColumn {
                            items(filteredBanks) { bankName ->
                                val isSelected = provider == bankName
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            provider = bankName
                                            showBottomSheet = false
                                        }
                                        .background(if (isSelected) PinkPrimary.copy(alpha = 0.05f) else Color.Transparent)
                                        .padding(horizontal = 24.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BrandLogo(
                                        provider = bankName,
                                        accountType = AccountType.BANK,
                                        iconColorHex = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = bankName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = NavyDark,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = PinkPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
                BottomSheetType.PROVIDER_SELECT -> {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.select_wallet_provider), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NavyDark, modifier = Modifier.padding(bottom = 16.dp))
                        availableProviders.forEach { info ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        provider = info.name
                                        selectedColorHex = info.iconColorHex
                                        showBottomSheet = false
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(info.color))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(info.name, fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                if (provider == info.name) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = PinkPrimary)
                                }
                            }
                        }
                    }
                }
                BottomSheetType.COLOR_SELECT -> {}
                BottomSheetType.NONE -> {}
            }
        }
    }
}
