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

enum class BottomSheetType {
    NONE,
    PROVIDER_SELECT,
    CURRENCY_SELECT,
    COLOR_SELECT,
    BANK_SELECT
}

data class ProviderInfo(val name: String, val color: Color, val iconColorHex: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    viewModel: PiggyLedgerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var accountLabel by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.BANK) }
    var currency by remember { mutableStateOf("EGP") }
    var startingBalance by remember { mutableStateOf("") }
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

    val accounts by viewModel.allAccounts.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()

    // Bottom Sheet State
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    var bottomSheetType by remember { mutableStateOf(BottomSheetType.NONE) }
    var currencySearchQuery by remember { mutableStateOf("") }
    var bankSearchQuery by remember { mutableStateOf("") }


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

    val customIcons = listOf(
        "AccountBalance" to Icons.Default.AccountBalance,
        "CreditCard" to Icons.Default.CreditCard,
        "Payments" to Icons.Default.Payments,
        "AccountBalanceWallet" to Icons.Default.AccountBalanceWallet,
        "Star" to Icons.Default.Star,
        "Home" to Icons.Default.Home,
        "TrendingUp" to Icons.Default.TrendingUp
    )

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFFCBD5E1),
        unfocusedBorderColor = Color(0xFFE2E8F0),
        focusedLabelColor = NavyDark,
        unfocusedLabelColor = TextLight,
        cursorColor = PinkPrimary,
        focusedContainerColor = Color(0xFFF8FAFC),
        unfocusedContainerColor = Color(0xFFF8FAFC)
    )
    
    fun openSheet(type: BottomSheetType) {
        bottomSheetType = type
        showBottomSheet = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_account), fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NavyDark)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            try {
                                com.posthog.PostHog.capture(event = "button_clicked", properties = mapOf("button_name" to "Create Account", "screen" to "AddAccountScreen"))
                            } catch (e: Exception) {}
                            if (name.isNotBlank() && viewModel.canAddAccount(accounts.size)) {
                                val limit = creditLimit.toDoubleOrNull() ?: 0.0
                                val available = availableCredit.toDoubleOrNull() ?: limit
                                val initialBalance = if (type == AccountType.CARD) {
                                    available - limit
                                } else {
                                    startingBalance.toDoubleOrNull() ?: 0.0
                                }
                                viewModel.addAccount(
                                    Account(
                                        name = name,
                                        type = type,
                                        icon_color = selectedColorHex,
                                        icon_name = selectedIconName,
                                        currency = currency,
                                        starting_balance = initialBalance,
                                        exclude_from_all = excludeFromAll,
                                        credit_limit = creditLimit.toDoubleOrNull(),
                                        available_credit = availableCredit.toDoubleOrNull() ?: creditLimit.toDoubleOrNull(),
                                        payment_due_day = paymentDueDay.toIntOrNull(),
                                        card_numbers = cardNumbers.takeIf { it.isNotBlank() },
                                        bank_account_no = bankAccountNo.takeIf { it.isNotBlank() },
                                        provider = provider.takeIf { it.isNotBlank() },
                                        insta_pay_fee = instaPayFee,
                                        label = accountLabel.takeIf { it.isNotBlank() }
                                    )
                                )
                                com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.toast_account_added), Toast.LENGTH_SHORT)
                                onBack()
                            } else if (name.isNotBlank()) {
                                com.oryno.piggy_ledger.ui.ToastUtil.show(context, "Upgrade to Pro to add more accounts", Toast.LENGTH_SHORT)
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
                                    .background(if (isSelected) PinkPrimary.copy(alpha = 0.1f) else Color.White)
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
                                    .padding(horizontal = 4.dp, vertical = 12.dp),
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
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        color = if (isSelected) PinkPrimary else Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }
                    if (type == AccountType.CASH) {
                        Text(
                            text = stringResource(R.string.cash_tracking_desc),
                            fontSize = 12.sp,
                            color = TextLight,
                            modifier = Modifier.padding(top = 2.dp)
                        )
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

                // Starting Balance
                if (type != AccountType.CARD) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.starting_balance), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                        OutlinedTextField(
                            value = startingBalance,
                            onValueChange = { input -> 
                                if (input.isEmpty() || input.all { it.isDigit() || it == '.' }) {
                                    startingBalance = input
                                }
                            },
                            placeholder = { Text(stringResource(R.string.eg_0_00), color = TextLight) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Medium, color = NavyDark, fontSize = 16.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            leadingIcon = {
                                Text(
                                    text = availableCurrencies.find { it.code == currency }?.symbol ?: "EGP",
                                    color = NavyDark,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        )
                    }
                }

                // Currency Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.currency), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .clickable { openSheet(BottomSheetType.CURRENCY_SELECT) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val currInfo = availableCurrencies.find { it.code == currency }
                            Text(
                                text = currInfo?.flag ?: "🇪🇬",
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (currInfo != null) "${currInfo.code} - ${currInfo.name}" else "EGP - Egyptian Pound",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = NavyDark
                            )
                        }
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = TextLight)
                    }
                }
            }

            // SECTION: Credit Card Config
            if (type == AccountType.CARD) {
                SectionCard(title = stringResource(R.string.credit_card_config)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.credit_limit), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                        OutlinedTextField(
                            value = creditLimit,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.all { it.isDigit() || it == '.' }) creditLimit = input
                            },
                            placeholder = { Text(stringResource(R.string.eg_25000), color = TextLight) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Medium, color = NavyDark, fontSize = 16.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            leadingIcon = {
                                Text(
                                    text = availableCurrencies.find { it.code == currency }?.symbol ?: "EGP",
                                    color = NavyDark,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.available), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                        OutlinedTextField(
                            value = availableCredit,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.all { it.isDigit() || it == '.' }) availableCredit = input
                            },
                            placeholder = { Text(stringResource(R.string.eg_12000), color = TextLight) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Medium, color = NavyDark, fontSize = 16.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            leadingIcon = {
                                Text(
                                    text = availableCurrencies.find { it.code == currency }?.symbol ?: "EGP",
                                    color = NavyDark,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.payment_due_day), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                        OutlinedTextField(
                            value = paymentDueDay,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.all { it.isDigit() }) paymentDueDay = input
                            },
                            placeholder = { Text(stringResource(R.string.eg_15), color = TextLight) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Medium, color = NavyDark, fontSize = 16.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                        Text(
                            text = stringResource(R.string.payment_due_day_desc),
                            fontSize = 12.sp,
                            color = TextLight,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // SECTION: SMS & Tracking Settings
            if (type == AccountType.WALLET || type == AccountType.CARD || type == AccountType.BANK) {
                SectionCard(title = stringResource(R.string.sms_settings)) {
                    
                    if (type == AccountType.WALLET) {
                        Text(
                            text = if (provider.isNotBlank()) {
                                stringResource(R.string.sms_auto_link_provider, provider)
                            } else {
                                stringResource(R.string.sms_auto_link_default)
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = NavyDark,
                            lineHeight = 18.sp
                        )
                    }

                    if (type == AccountType.CARD) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.card_numbers_optional), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                            OutlinedTextField(
                                value = cardNumbers,
                                onValueChange = { cardNumbers = it },
                                placeholder = { Text(stringResource(R.string.eg_card_numbers), color = TextLight) },
                                leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = TextLight) },
                                textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Medium, color = NavyDark, fontSize = 16.sp),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors
                            )
                            Text(
                                text = stringResource(R.string.link_card_digits_desc),
                                fontSize = 12.sp,
                                color = TextLight,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    if (type == AccountType.BANK || type == AccountType.CARD) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.bank_account_number_optional), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                            OutlinedTextField(
                                value = bankAccountNo,
                                onValueChange = { bankAccountNo = it },
                                placeholder = { Text(stringResource(R.string.eg_account_number), color = TextLight) },
                                leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = TextLight) },
                                textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Medium, color = NavyDark, fontSize = 16.sp),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors
                            )
                            Text(
                                text = stringResource(R.string.bank_account_desc),
                                fontSize = 12.sp,
                                color = TextLight,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    if (type == AccountType.BANK || type == AccountType.CARD) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.add_instapay_fee),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = NavyDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.instapay_fee_desc),
                                    fontSize = 12.sp,
                                    color = TextLight,
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Switch(
                                checked = instaPayFee,
                                onCheckedChange = { instaPayFee = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = PinkPrimary,
                                    uncheckedThumbColor = Color(0xFF94A3B8),
                                    uncheckedTrackColor = Color(0xFFE2E8F0)
                                )
                            )
                        }
                    }
                }
            }

            // SECTION: Advanced
            SectionCard(title = stringResource(R.string.advanced)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.exclude_from_all_accounts),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NavyDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.exclude_from_all_desc),
                            fontSize = 12.sp,
                            color = TextLight,
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = excludeFromAll,
                        onCheckedChange = { excludeFromAll = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PinkPrimary,
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Bottom Sheets
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFCBD5E1)) }
        ) {
            when (bottomSheetType) {
                BottomSheetType.PROVIDER_SELECT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.select_provider),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = NavyDark,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                        )
                        LazyColumn {
                            items(availableProviders) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            provider = item.name
                                            if (!userCustomizedColor) {
                                                selectedColorHex = item.iconColorHex
                                            }
                                            showBottomSheet = false
                                        }
                                        .padding(horizontal = 24.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BrandLogo(
                                        provider = item.name,
                                        accountType = AccountType.WALLET,
                                        iconColorHex = item.iconColorHex,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = NavyDark
                                    )
                                }
                            }
                        }
                    }
                }
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
                            it.name.contains(currencySearchQuery, ignoreCase = true) ||
                            it.symbol.contains(currencySearchQuery, ignoreCase = true)
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
                BottomSheetType.COLOR_SELECT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.icon_and_color),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = NavyDark,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        // Preview Card
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(selectedColorHex))),
                            contentAlignment = Alignment.Center
                        ) {
                            val currentIcon = customIcons.find { it.first == selectedIconName }?.second ?: Icons.Default.AccountBalance
                            Icon(imageVector = currentIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                        
                        // Colors
                        Text(stringResource(R.string.select_color), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(6),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(customColors) { (colorHex, _) ->
                                val isColorSelected = selectedColorHex == colorHex
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(colorHex)))
                                        .border(
                                            width = if (isColorSelected) 3.dp else 0.dp,
                                            color = if (isColorSelected) NavyDark else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            selectedColorHex = colorHex
                                            userCustomizedColor = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isColorSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Icons
                        Text(stringResource(R.string.select_icon), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(customIcons) { (name, iconVector) ->
                                val isIconSelected = selectedIconName == name
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isIconSelected) PinkPrimary else Color(0xFFF1F5F9))
                                        .clickable {
                                            selectedIconName = name
                                            userCustomizedColor = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = null,
                                        tint = if (isIconSelected) Color.White else Color(0xFF64748B),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { showBottomSheet = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                        ) {
                            Text(stringResource(R.string.done), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        }
                    }
                }
                BottomSheetType.NONE -> {}
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = NavyDark,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}
