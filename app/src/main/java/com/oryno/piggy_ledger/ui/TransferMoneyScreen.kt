package com.oryno.piggy_ledger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.data.AccountType
import com.oryno.piggy_ledger.data.JournalEntryEntity
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.PiggyLedgerRepository
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import com.oryno.piggy_ledger.ui.transfer.TransferViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferMoneyScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val dao = remember { PiggyLedgerDatabase.getInstance(context).piggyLedgerDao() }
    val repository = remember { PiggyLedgerRepository(dao, context) }

    val viewModel: TransferViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return TransferViewModel(repository, dao) as T
        }
    })

    val uiState by viewModel.uiState.collectAsState()

    var showFromAccountPicker by remember { mutableStateOf(false) }
    var showToAccountPicker by remember { mutableStateOf(false) }
    var rotatedAngle by remember { mutableFloatStateOf(0f) }
    val rotation by animateFloatAsState(targetValue = rotatedAngle, animationSpec = tween(300), label = "rotation")

    val fromAccount = uiState.accounts.find { it.id == uiState.fromAccountId }
    val toAccount = uiState.accounts.find { it.id == uiState.toAccountId }

    val enteredAmount = uiState.amountText.toDoubleOrNull() ?: 0.0
    val isOverdraft = fromAccount != null &&
            fromAccount.type != AccountType.CARD &&
            fromAccount.type != AccountType.LIABILITY &&
            enteredAmount > fromAccount.current_balance

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.transfer),
                        fontWeight = FontWeight.Bold,
                        color = NavyDark,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_icon),
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
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Upper section: From / To Cards & Amount (Compact, non-scrolling)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // From & To Accounts Group
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // From Account Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFromAccountPicker = true },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (fromAccount != null) {
                                    BrandLogo(
                                        provider = fromAccount.provider ?: fromAccount.name,
                                        accountType = fromAccount.type,
                                        iconColorHex = fromAccount.icon_color,
                                        logoUrl = fromAccount.logo_url,
                                        modifier = Modifier.size(34.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(PinkPrimary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Column {
                                    Text(
                                        text = fromAccount?.name ?: stringResource(R.string.select_account),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = NavyDark
                                    )
                                    Text(
                                        text = if (fromAccount != null) "${String.format(Locale.getDefault(), "%,.2f", fromAccount.current_balance)} ${fromAccount.currency}" else stringResource(R.string.select_source_account),
                                        fontSize = 11.sp,
                                        color = TextLight
                                    )
                                }
                            }

                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextLight, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Swap Button Row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                rotatedAngle += 180f
                                viewModel.swapAccounts()
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEEF2FF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Swap Accounts",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.rotate(rotation).size(16.dp)
                            )
                        }
                    }

                    // To Account Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showToAccountPicker = true },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (toAccount != null) {
                                    BrandLogo(
                                        provider = toAccount.provider ?: toAccount.name,
                                        accountType = toAccount.type,
                                        iconColorHex = toAccount.icon_color,
                                        logoUrl = toAccount.logo_url,
                                        modifier = Modifier.size(34.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(PinkPrimary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Column {
                                    Text(
                                        text = toAccount?.name ?: stringResource(R.string.select_account),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = NavyDark
                                    )
                                    Text(
                                        text = if (toAccount != null) "${String.format(Locale.getDefault(), "%,.2f", toAccount.current_balance)} ${toAccount.currency}" else stringResource(R.string.select_target_account),
                                        fontSize = 11.sp,
                                        color = TextLight
                                    )
                                }
                            }

                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextLight, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Amount Display Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, if (isOverdraft) Color(0xFFEF4444) else Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.amount_label).uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (uiState.amountText.isEmpty()) "0.00" else uiState.amountText,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isOverdraft) Color(0xFFEF4444) else NavyDark
                        )
                        if (isOverdraft) {
                            Text(
                                text = "Insufficient funds",
                                fontSize = 11.sp,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Light Theme Compact Numeric Keypad
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val buttons = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf(".", "0", "DEL")
                    )

                    buttons.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { label ->
                                Button(
                                    onClick = {
                                        val current = uiState.amountText
                                        when (label) {
                                            "DEL" -> {
                                                if (current.isNotEmpty()) {
                                                    viewModel.updateAmount(current.dropLast(1))
                                                }
                                            }
                                            "." -> {
                                                if (!current.contains(".")) {
                                                    viewModel.updateAmount(current + ".")
                                                }
                                            }
                                            else -> {
                                                if (current == "0") {
                                                    viewModel.updateAmount(label)
                                                } else {
                                                    viewModel.updateAmount(current + label)
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF1F5F9),
                                        contentColor = NavyDark
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    if (label == "DEL") {
                                        Icon(Icons.Default.Backspace, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                                    } else {
                                        Text(text = label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Confirm Transfer Button
                    Button(
                        onClick = {
                            viewModel.executeTransfer {
                                onBack()
                            }
                        },
                        enabled = !uiState.isSubmitting && !isOverdraft && enteredAmount > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(R.string.confirm_btn), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Account Picker Modals
    if (showFromAccountPicker) {
        AccountPickerBottomSheet(
            title = stringResource(R.string.select_account),
            accounts = uiState.accounts,
            selectedAccountId = uiState.fromAccountId,
            onSelect = {
                viewModel.selectFromAccount(it.id)
                showFromAccountPicker = false
            },
            onDismiss = { showFromAccountPicker = false }
        )
    }

    if (showToAccountPicker) {
        AccountPickerBottomSheet(
            title = stringResource(R.string.select_account),
            accounts = uiState.accounts,
            selectedAccountId = uiState.toAccountId,
            onSelect = {
                viewModel.selectToAccount(it.id)
                showToAccountPicker = false
            },
            onDismiss = { showToAccountPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountPickerBottomSheet(
    title: String,
    accounts: List<Account>,
    selectedAccountId: String?,
    onSelect: (Account) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )
            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(accounts) { acc ->
                    val isSelected = acc.id == selectedAccountId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(acc) },
                        color = if (isSelected) PinkPrimary.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isSelected) PinkPrimary else Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            BrandLogo(
                                provider = acc.provider ?: acc.name,
                                accountType = acc.type,
                                iconColorHex = acc.icon_color,
                                logoUrl = acc.logo_url,
                                modifier = Modifier.size(36.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = acc.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = NavyDark
                                )
                                Text(
                                    text = "${acc.type.name} • Available: ${String.format(Locale.getDefault(), "%,.2f", acc.current_balance)} ${acc.currency}",
                                    fontSize = 12.sp,
                                    color = TextLight
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
