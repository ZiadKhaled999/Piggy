package com.oryno.piggy_ledger.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.data.AccountType
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import java.util.Locale

data class ProviderOption(
    val name: String,
    val domain: String,
    val defaultColorHex: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    viewModel: PiggyLedgerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) } // 0: Type, 1: Provider, 2: Name/Style, 3: Balance, 4: Automation/Review

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

    // Provider / Logo
    var provider by remember { mutableStateOf("") }
    var logoUrl by remember { mutableStateOf<String?>(null) }

    // Fees
    var instaPayFee by remember { mutableStateOf(true) }

    // Color & Icon Picker State
    var selectedColorHex by remember { mutableStateOf("#1E3A8A") }
    var selectedIconName by remember { mutableStateOf("AccountBalance") }
    var userCustomizedColor by remember { mutableStateOf(false) }

    val accounts by viewModel.allAccounts.collectAsState()

    // Handle back stack navigation
    BackHandler {
        if (currentStep > 0) {
            currentStep--
        } else {
            onBack()
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.new_account),
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyDark,
                            fontSize = 19.sp
                        )
                        Text(
                            text = when (currentStep) {
                                0 -> "Step 1 of 5: Account Type"
                                1 -> "Step 2 of 5: Provider & Brand"
                                2 -> "Step 3 of 5: Details & Theme"
                                3 -> "Step 4 of 5: Currency & Balance"
                                else -> "Step 5 of 5: Automation & Save"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PinkPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentStep > 0) currentStep--
                            else onBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NavyDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // STEP PROGRESS INDICATOR
            LinearProgressIndicator(
                progress = { ((currentStep + 1) / 5f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = PinkPrimary,
                trackColor = Color(0xFFE2E8F0)
            )

            // SLIDE CONTAINER WITH ANIMATED TRANSITIONS
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                label = "AccountStepTransition"
            ) { step ->
                when (step) {
                    0 -> Slide0AccountType(
                        selectedType = type,
                        onTypeSelected = { selected ->
                            type = selected
                            provider = ""
                            logoUrl = null
                            if (!userCustomizedColor) {
                                selectedColorHex = when (selected) {
                                    AccountType.BANK -> "#1E3A8A"
                                    AccountType.CARD -> "#8B5CF6"
                                    AccountType.CASH -> "#10B981"
                                    AccountType.WALLET -> "#EA580C"
                                    else -> "#6366F1"
                                }
                                selectedIconName = when (selected) {
                                    AccountType.BANK -> "AccountBalance"
                                    AccountType.CARD -> "CreditCard"
                                    AccountType.CASH -> "Payments"
                                    AccountType.WALLET -> "AccountBalanceWallet"
                                    else -> "AccountBalance"
                                }
                            }
                            if (selected == AccountType.CASH && name.isBlank()) {
                                name = "Cash Wallet"
                            }
                        },
                        onNext = {
                            if (type == AccountType.CASH) {
                                currentStep = 2 // Skip provider slide for physical cash
                            } else {
                                currentStep = 1
                            }
                        }
                    )

                    1 -> Slide1ProviderSelection(
                        accountType = type,
                        selectedProvider = provider,
                        onProviderSelected = { provName, provColor, provLogoUrl ->
                            provider = provName
                            logoUrl = provLogoUrl
                            name = provName
                            if (!userCustomizedColor) {
                                selectedColorHex = provColor
                            }
                            currentStep = 2
                        },
                        onSkipOrCustom = { customName ->
                            if (customName.isNotBlank()) {
                                provider = customName
                                logoUrl = buildLogoDevUrl(customName)
                                name = customName
                            }
                            currentStep = 2
                        }
                    )

                    2 -> Slide2NameAndCustomization(
                        name = name,
                        onNameChange = { name = it },
                        accountLabel = accountLabel,
                        onAccountLabelChange = { accountLabel = it },
                        selectedColorHex = selectedColorHex,
                        onColorSelected = {
                            selectedColorHex = it
                            userCustomizedColor = true
                        },
                        selectedIconName = selectedIconName,
                        onIconSelected = { selectedIconName = it },
                        provider = provider,
                        accountType = type,
                        logoUrl = logoUrl,
                        onNext = { currentStep = 3 }
                    )

                    3 -> Slide3CurrencyAndBalance(
                        currency = currency,
                        onCurrencySelected = { currency = it },
                        accountType = type,
                        startingBalance = startingBalance,
                        onStartingBalanceChange = { startingBalance = it },
                        creditLimit = creditLimit,
                        onCreditLimitChange = { creditLimit = it },
                        availableCredit = availableCredit,
                        onAvailableCreditChange = { availableCredit = it },
                        paymentDueDay = paymentDueDay,
                        onPaymentDueDayChange = { paymentDueDay = it },
                        onNext = { currentStep = 4 }
                    )

                    4 -> Slide4AutomationAndReview(
                        name = name,
                        accountLabel = accountLabel,
                        accountType = type,
                        currency = currency,
                        startingBalance = startingBalance,
                        creditLimit = creditLimit,
                        availableCredit = availableCredit,
                        provider = provider,
                        logoUrl = logoUrl,
                        selectedColorHex = selectedColorHex,
                        cardNumbers = cardNumbers,
                        onCardNumbersChange = { cardNumbers = it },
                        bankAccountNo = bankAccountNo,
                        onBankAccountNoChange = { bankAccountNo = it },
                        instaPayFee = instaPayFee,
                        onInstaPayFeeChange = { instaPayFee = it },
                        excludeFromAll = excludeFromAll,
                        onExcludeFromAllChange = { excludeFromAll = it },
                        onCreateAccount = {
                            try {
                                com.posthog.PostHog.capture(
                                    event = "button_clicked",
                                    properties = mapOf(
                                        "button_name" to "Create Account Slide",
                                        "screen" to "AddAccountScreen"
                                    )
                                )
                            } catch (e: Exception) {}

                            if (name.isNotBlank() && viewModel.canAddAccount(accounts.size)) {
                                val limit = creditLimit.toDoubleOrNull() ?: 0.0
                                val available = availableCredit.toDoubleOrNull() ?: limit
                                val initialBalance = if (type == AccountType.CARD) {
                                    available - limit
                                } else {
                                    startingBalance.toDoubleOrNull() ?: 0.0
                                }

                                val finalLogoUrl = logoUrl ?: getOnlineProviderLogoUrl(provider)

                                viewModel.addAccount(
                                    Account(
                                        name = name.trim(),
                                        type = type,
                                        icon_color = selectedColorHex,
                                        icon_name = selectedIconName,
                                        logo_url = finalLogoUrl,
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
                                com.oryno.piggy_ledger.ui.ToastUtil.show(
                                    context,
                                    context.getString(R.string.toast_account_added),
                                    Toast.LENGTH_SHORT
                                )
                                onBack()
                            } else if (name.isNotBlank()) {
                                com.oryno.piggy_ledger.ui.ToastUtil.show(
                                    context,
                                    context.getString(R.string.toast_upgrade_pro_accounts),
                                    Toast.LENGTH_SHORT
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

// ==========================================
// SLIDE 0: CHOOSE ACCOUNT TYPE
// ==========================================
@Composable
fun Slide0AccountType(
    selectedType: AccountType,
    onTypeSelected: (AccountType) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SELECT ACCOUNT TYPE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.2.sp
        )
        Text(
            text = "What kind of account are you tracking?",
            fontSize = 15.sp,
            color = TextLight,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        val accountTypes = listOf(
            Triple(AccountType.BANK, "Bank Account", "Checking, Savings & CD accounts"),
            Triple(AccountType.CARD, "Credit / Debit Card", "Revolving credit, limit & due day tracking"),
            Triple(AccountType.WALLET, "E-Wallet", "Vodafone Cash, Orange, e&, WE & InstaPay"),
            Triple(AccountType.CASH, "Cash & Physical Wallet", "Track paper currency and physical spendings")
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            accountTypes.forEach { (type, title, subtitle) ->
                val isSelected = selectedType == type
                val icon = when (type) {
                    AccountType.BANK -> Icons.Default.AccountBalance
                    AccountType.CARD -> Icons.Default.CreditCard
                    AccountType.WALLET -> Icons.Default.AccountBalanceWallet
                    AccountType.CASH -> Icons.Default.Payments
                    else -> Icons.Default.AccountBalance
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onTypeSelected(type) },
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) PinkPrimary else Color(0xFFE2E8F0)
                    ),
                    shadowElevation = if (isSelected) 3.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PinkPrimary.copy(alpha = 0.12f) else Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) PinkPrimary else NavyDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                lineHeight = 16.sp
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { onTypeSelected(type) },
                            colors = RadioButtonDefaults.colors(selectedColor = PinkPrimary)
                        )
                    }
                }
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
        ) {
            Text(stringResource(R.string.next_choose_provider), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

// ==========================================
// SLIDE 1: PROVIDER SELECTION WITH LOGO.DEV
// ==========================================
@Composable
fun Slide1ProviderSelection(
    accountType: AccountType,
    selectedProvider: String,
    onProviderSelected: (name: String, colorHex: String, logoUrl: String) -> Unit,
    onSkipOrCustom: (customName: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var customInput by remember { mutableStateOf("") }

    val walletProviders = remember {
        listOf(
            ProviderOption("Vodafone Cash", "vodafone.com", "#E11D48"),
            ProviderOption("Orange Cash", "orange.com", "#EA580C"),
            ProviderOption("e& Cash", "eand.com", "#16A34A"),
            ProviderOption("WE Pay", "te.eg", "#7C3AED"),
            ProviderOption("InstaPay", "instapay.eg", "#701A75"),
            ProviderOption("Telda", "telda.app", "#0F172A")
        )
    }

    val bankProviders = remember {
        listOf(
            ProviderOption("Commercial International Bank (CIB)", "cibeg.com", "#1E3A8A"),
            ProviderOption("National Bank of Egypt (NBE)", "nbe.com.eg", "#005B41"),
            ProviderOption("Banque Misr", "banquemisr.com", "#900C3F"),
            ProviderOption("QNB Alahli", "qnbalahli.com", "#6F1E51"),
            ProviderOption("Banque Du Caire", "bdc.com.eg", "#E05B00"),
            ProviderOption("Arab African International Bank (AAIB)", "aaib.com", "#1E3A8A"),
            ProviderOption("HSBC Egypt", "hsbc.com.eg", "#E50914"),
            ProviderOption("Alex Bank", "alexbank.com", "#4D1A7F"),
            ProviderOption("Faisal Islamic Bank", "faisalbank.com.eg", "#1B4D3E"),
            ProviderOption("Abu Dhabi Islamic Bank (ADIB)", "adib.eg", "#0D9488"),
            ProviderOption("Credit Agricole Egypt", "credit-agricole.com.eg", "#059669"),
            ProviderOption("Emirates NBD Egypt", "emiratesnbd.com.eg", "#1E3A8A"),
            ProviderOption("Housing and Development Bank (HDB)", "hdb-egypt.com", "#059669"),
            ProviderOption("Egyptian Gulf Bank (EG Bank)", "eg-bank.com", "#0A2540"),
            ProviderOption("SAIB Bank", "saib.com.eg", "#1E3A8A"),
            ProviderOption("Al Baraka Bank", "albaraka.com.eg", "#8B5A2B"),
            ProviderOption("Attijariwafa Bank Egypt", "attijariwafabank.com.eg", "#EAB308"),
            ProviderOption("Arab Bank", "arabbank.com.eg", "#006400"),
            ProviderOption("Abu Dhabi Commercial Bank (ADCB)", "adcb.com.eg", "#D32F2F"),
            ProviderOption("Citibank Egypt", "citibank.com", "#0288D1"),
            ProviderOption("Mashreq Bank", "mashreqbank.com", "#FF5722"),
            ProviderOption("Suez Canal Bank", "scbank.com.eg", "#0D47A1"),
            ProviderOption("The United Bank", "theubeg.com", "#1565C0"),
            ProviderOption("EBank", "ebank.com.eg", "#008080"),
            ProviderOption("First Abu Dhabi Bank (FAB)", "bankfab.com", "#C8102E"),
            ProviderOption("Al Ahli Bank of Kuwait (ABK)", "abkegypt.com", "#003366"),
            ProviderOption("National Bank of Kuwait (NBK)", "nbk.com", "#002A54"),
            ProviderOption("Ahli United Bank (AUB)", "aub.com.eg", "#800020"),
            ProviderOption("Bank ABC Egypt", "bank-abc.com", "#008080"),
            ProviderOption("aiBank (Arab Investment Bank)", "aibank.com.eg", "#0F172A"),
            ProviderOption("MIDBANK", "midbank.com.eg", "#4A154B"),
            ProviderOption("Arab International Bank (AIB)", "aib.com.eg", "#102A43")
        )
    }

    val activeList = if (accountType == AccountType.WALLET) walletProviders else bankProviders
    val filteredList = activeList.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.domain.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = if (accountType == AccountType.WALLET) "SELECT WALLET PROVIDER" else "SELECT BANK / ISSUER",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.2.sp
        )
        Text(
            text = "Official logos are verified online and cached automatically",
            fontSize = 13.sp,
            color = TextLight,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(if (accountType == AccountType.WALLET) "Search e-wallet..." else "Search bank (e.g. CIB, NBE, Misr)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
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

        Spacer(modifier = Modifier.height(14.dp))

        // Providers List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredList) { prov ->
                val logoUrl = buildLogoDevUrl(prov.domain)
                val isSelected = selectedProvider == prov.name

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onProviderSelected(prov.name, prov.defaultColorHex, logoUrl) },
                    color = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) PinkPrimary else Color(0xFFE2E8F0)
                    ),
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BrandLogo(
                            provider = prov.name,
                            accountType = accountType,
                            iconColorHex = prov.defaultColorHex,
                            logoUrl = logoUrl,
                            modifier = Modifier.size(44.dp)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = prov.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = NavyDark
                            )
                            Text(
                                text = prov.domain,
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = if (isSelected) PinkPrimary else Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            // Custom Provider / Other
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Can't find your institution?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customInput,
                                onValueChange = { customInput = it },
                                placeholder = { Text(stringResource(R.string.enter_custom_provider_name)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PinkPrimary,
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onSkipOrCustom(customInput.trim()) },
                                enabled = customInput.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                            ) {
                                Text(stringResource(R.string.use_btn), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Skip to next button
        OutlinedButton(
            onClick = { onSkipOrCustom("") },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
        ) {
            Text(stringResource(R.string.skip_enter_custom_details), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NavyDark)
        }
    }
}

// ==========================================
// SLIDE 2: NAME, LABEL & CUSTOMIZATION
// ==========================================
@Composable
fun Slide2NameAndCustomization(
    name: String,
    onNameChange: (String) -> Unit,
    accountLabel: String,
    onAccountLabelChange: (String) -> Unit,
    selectedColorHex: String,
    onColorSelected: (String) -> Unit,
    selectedIconName: String,
    onIconSelected: (String) -> Unit,
    provider: String,
    accountType: AccountType,
    logoUrl: String?,
    onNext: () -> Unit
) {
    val paletteColors = listOf(
        "#1E3A8A" to "Navy",
        "#3B82F6" to "Blue",
        "#8B5CF6" to "Purple",
        "#0D9488" to "Teal",
        "#10B981" to "Emerald",
        "#F59E0B" to "Amber",
        "#EA580C" to "Orange",
        "#E11D48" to "Rose",
        "#EC4899" to "Pink",
        "#0F172A" to "Slate",
        "#4A154B" to "Plum",
        "#005B41" to "Forest"
    )

    val iconOptions = listOf(
        "AccountBalance" to Icons.Default.AccountBalance,
        "CreditCard" to Icons.Default.CreditCard,
        "Payments" to Icons.Default.Payments,
        "AccountBalanceWallet" to Icons.Default.AccountBalanceWallet,
        "Savings" to Icons.Default.Savings,
        "Star" to Icons.Default.Star,
        "TrendingUp" to Icons.Default.TrendingUp,
        "Home" to Icons.Default.Home
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "NAME & CUSTOMIZATION",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.2.sp
        )
        Text(
            text = "Give your account a recognizable title & styling",
            fontSize = 13.sp,
            color = TextLight,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Live Mini Card Preview
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = try { Color(android.graphics.Color.parseColor(selectedColorHex)) } catch (e: Exception) { NavyDark },
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrandLogo(
                    provider = provider.ifBlank { name },
                    accountType = accountType,
                    iconColorHex = selectedColorHex,
                    logoUrl = logoUrl,
                    modifier = Modifier.size(46.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = name.ifBlank { "Account Name" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    if (accountLabel.isNotBlank()) {
                        Text(
                            text = accountLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Account Name Field
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(stringResource(R.string.account_name_required), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyDark)
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = { Text(stringResource(R.string.account_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = PinkPrimary,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Label / Nickname Field
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(stringResource(R.string.account_label_optional), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyDark)
            OutlinedTextField(
                value = accountLabel,
                onValueChange = onAccountLabelChange,
                placeholder = { Text(stringResource(R.string.account_label_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = PinkPrimary,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Color Palette Picker
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.brand_color_label), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyDark)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                paletteColors.take(6).forEach { (hex, _) ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { onColorSelected(hex) }
                            .border(if (isSelected) 3.dp else 0.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                paletteColors.drop(6).forEach { (hex, _) ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { onColorSelected(hex) }
                            .border(if (isSelected) 3.dp else 0.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onNext,
            enabled = name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
        ) {
            Text(stringResource(R.string.next_balance_currency), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

// ==========================================
// SLIDE 3: CURRENCY & BALANCE
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Slide3CurrencyAndBalance(
    currency: String,
    onCurrencySelected: (String) -> Unit,
    accountType: AccountType,
    startingBalance: String,
    onStartingBalanceChange: (String) -> Unit,
    creditLimit: String,
    onCreditLimitChange: (String) -> Unit,
    availableCredit: String,
    onAvailableCreditChange: (String) -> Unit,
    paymentDueDay: String,
    onPaymentDueDayChange: (String) -> Unit,
    onNext: () -> Unit
) {
    var showCurrencyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CURRENCY & BALANCE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.2.sp
        )
        Text(
            text = "Set account currency and current balance or limits",
            fontSize = 13.sp,
            color = TextLight,
            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
        )

        // Currency Selector Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { showCurrencyDialog = true },
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
                val currInfo = availableCurrencies.find { it.code == currency }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = currInfo?.flag ?: "🇪🇬", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${currInfo?.code ?: "EGP"} (${currInfo?.symbol ?: "EGP"})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NavyDark
                        )
                        Text(
                            text = currInfo?.name ?: "Egyptian Pound",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PinkPrimary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "Change",
                        color = PinkPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (accountType != AccountType.CARD) {
            // Starting Balance for Bank, Wallet, Cash
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.starting_current_balance), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                OutlinedTextField(
                    value = startingBalance,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { it.isDigit() || it == '.' }) {
                            onStartingBalanceChange(input)
                        }
                    },
                    placeholder = { Text(stringResource(R.string.zero_amount_placeholder)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = {
                        Text(
                            text = availableCurrencies.find { it.code == currency }?.symbol ?: currency,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = PinkPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )
            }
        } else {
            // Card Credit Limit & Available Credit
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.total_credit_limit_required), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    OutlinedTextField(
                        value = creditLimit,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() || it == '.' }) onCreditLimitChange(input)
                        },
                        placeholder = { Text(stringResource(R.string.credit_limit_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = {
                            Text(
                                text = currency,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = PinkPrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.currently_available_credit), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    OutlinedTextField(
                        value = availableCredit,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() || it == '.' }) onAvailableCreditChange(input)
                        },
                        placeholder = { Text(creditLimit.ifBlank { "Same as credit limit" }) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = PinkPrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.payment_due_day_label), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    OutlinedTextField(
                        value = paymentDueDay,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() }) onPaymentDueDayChange(input)
                        },
                        placeholder = { Text(stringResource(R.string.due_day_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = PinkPrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
        ) {
            Text(stringResource(R.string.next_automation_review), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }

    // Currency Selection Modal BottomSheet
    if (showCurrencyDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var query by remember { mutableStateOf("") }
        val filtered = availableCurrencies.filter {
            it.code.contains(query, ignoreCase = true) || it.name.contains(query, ignoreCase = true)
        }

        ModalBottomSheet(
            onDismissRequest = { showCurrencyDialog = false },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFCBD5E1)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Currency",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = NavyDark
                    )
                    IconButton(onClick = { showCurrencyDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.search_currency_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextLight) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedBorderColor = PinkPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered) { curr ->
                        val isSelected = curr.code.equals(currency, ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onCurrencySelected(curr.code)
                                    showCurrencyDialog = false
                                },
                            color = if (isSelected) PinkPrimary.copy(alpha = 0.08f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(curr.flag, fontSize = 26.sp)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = curr.code,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isSelected) PinkPrimary else NavyDark
                                    )
                                    Text(curr.name, fontSize = 12.sp, color = TextLight)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = PinkPrimary)
                                } else {
                                    Text(
                                        text = curr.symbol,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B),
                                        fontSize = 15.sp
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

// ==========================================
// SLIDE 4: AUTOMATION & REVIEW
// ==========================================
@Composable
fun Slide4AutomationAndReview(
    name: String,
    accountLabel: String,
    accountType: AccountType,
    currency: String,
    startingBalance: String,
    creditLimit: String,
    availableCredit: String,
    provider: String,
    logoUrl: String?,
    selectedColorHex: String,
    cardNumbers: String,
    onCardNumbersChange: (String) -> Unit,
    bankAccountNo: String,
    onBankAccountNoChange: (String) -> Unit,
    instaPayFee: Boolean,
    onInstaPayFeeChange: (Boolean) -> Unit,
    excludeFromAll: Boolean,
    onExcludeFromAllChange: (Boolean) -> Unit,
    onCreateAccount: () -> Unit
) {
    val currencySym = getCurrencySymbol(currency)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AUTOMATION & REVIEW",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.2.sp
        )
        Text(
            text = "Review your card and configure automated SMS linking",
            fontSize = 13.sp,
            color = TextLight,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // FINAL CARD PREVIEW (Credit Card / Bank Card Aesthetic)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            color = try { Color(android.graphics.Color.parseColor(selectedColorHex)) } catch (e: Exception) { NavyDark },
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrandLogo(
                        provider = provider.ifBlank { name },
                        accountType = accountType,
                        iconColorHex = selectedColorHex,
                        logoUrl = logoUrl,
                        modifier = Modifier.size(46.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = accountType.name,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val displayBalance = if (accountType == AccountType.CARD) {
                    val limit = creditLimit.toDoubleOrNull() ?: 0.0
                    val avail = availableCredit.toDoubleOrNull() ?: limit
                    avail - limit
                } else {
                    startingBalance.toDoubleOrNull() ?: 0.0
                }

                Text(
                    text = "BALANCE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$currencySym ${String.format(Locale.getDefault(), "%,.2f", displayBalance)}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (accountLabel.isNotBlank()) {
                            Text(
                                text = accountLabel,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    if (cardNumbers.isNotBlank()) {
                        Text(
                            text = "•••• ${cardNumbers.takeLast(4)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SMS Linking Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "SMS Automation & Identifier",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = NavyDark
                )

                if (accountType == AccountType.CARD) {
                    OutlinedTextField(
                        value = cardNumbers,
                        onValueChange = onCardNumbersChange,
                        placeholder = { Text(stringResource(R.string.card_digits_hint)) },
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = TextLight) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (accountType == AccountType.BANK) {
                    OutlinedTextField(
                        value = bankAccountNo,
                        onValueChange = onBankAccountNoChange,
                        placeholder = { Text(stringResource(R.string.account_number_hint)) },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = TextLight) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // InstaPay Fee Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.add_instapay_fee_label), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyDark)
                        Text(stringResource(R.string.instapay_fee_desc_detail), fontSize = 11.sp, color = TextLight)
                    }
                    Switch(
                        checked = instaPayFee,
                        onCheckedChange = onInstaPayFeeChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PinkPrimary
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Exclude from Total Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.exclude_from_total_balance), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyDark)
                        Text(stringResource(R.string.exclude_from_total_desc), fontSize = 11.sp, color = TextLight)
                    }
                    Switch(
                        checked = excludeFromAll,
                        onCheckedChange = onExcludeFromAllChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PinkPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save Button
        Button(
            onClick = onCreateAccount,
            enabled = name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.create_account_btn), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
