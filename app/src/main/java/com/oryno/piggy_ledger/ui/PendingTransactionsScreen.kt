package com.oryno.piggy_ledger.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.PendingTransaction
import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingTransactionsScreen(
    viewModel: PiggyLedgerViewModel,
    onBack: () -> Unit
) {
    val pendingTransactions by viewModel.allPendingTransactions.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState()
    
    var selectedTransaction by remember { mutableStateOf<PendingTransaction?>(null) }
    var showResolveBottomSheet by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.pending_transactions),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = NavyDark
                        )
                        if (pendingTransactions.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PinkPrimary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${pendingTransactions.size}",
                                    color = PinkPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = NavyDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8FAFC)
                )
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (pendingTransactions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_pending_empty),
                        contentDescription = null,
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.no_pending_transactions),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyDark,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.no_pending_transactions_desc),
                        fontSize = 14.sp,
                        color = TextLight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp),
                        lineHeight = 20.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
                ) {
                    items(pendingTransactions, key = { it.id }) { tx ->
                        PendingTransactionItem(
                            tx = tx,
                            dateFormat = dateFormat,
                            onClick = {
                                selectedTransaction = tx
                                showResolveBottomSheet = true
                            }
                        )
                    }
                }
            }

            if (showResolveBottomSheet && selectedTransaction != null) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showResolveBottomSheet = false
                        selectedTransaction = null
                    },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = Color.White,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = NavyDark.copy(alpha = 0.2f)) }
                ) {
                    ResolveTransactionBottomSheetContent(
                        transaction = selectedTransaction!!,
                        accounts = accounts,
                        dateFormat = dateFormat,
                        onAccountSelected = { accountId ->
                            viewModel.resolvePendingTransaction(selectedTransaction!!.id, accountId)
                            showResolveBottomSheet = false
                            selectedTransaction = null
                        },
                        onDiscard = {
                            viewModel.deletePendingTransaction(selectedTransaction!!.id)
                            showResolveBottomSheet = false
                            selectedTransaction = null
                        },
                        onClose = {
                            showResolveBottomSheet = false
                            selectedTransaction = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PendingTransactionItem(
    tx: PendingTransaction,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 0.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_item_${tx.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Icon Badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PinkPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = PinkPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Middle Column: Sender / Name and Date
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                val displayName = tx.sender.ifBlank { tx.merchant }.ifBlank { stringResource(R.string.pending_sms_detected) }
                Text(
                    text = displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NavyDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = dateFormat.format(Date(tx.timestamp)),
                    fontSize = 12.sp,
                    color = TextLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Column: Amount & Chevron Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(Locale.US, "%s%.2f EGP", if (tx.amount > 0) "+" else "-", Math.abs(tx.amount)),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = if (tx.amount > 0) Color(0xFF10B981) else PinkPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ResolveTransactionBottomSheetContent(
    transaction: PendingTransaction,
    accounts: List<Account>,
    dateFormat: SimpleDateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) },
    onAccountSelected: (String) -> Unit,
    onDiscard: (() -> Unit)? = null,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.pending_sms_detected),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NavyDark
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        // Transaction Highlight Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFF1F2),
            border = BorderStroke(1.dp, PinkPrimary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.sender.ifBlank { transaction.merchant },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = NavyDark
                    )
                    Text(
                        text = String.format(Locale.US, "%s%.2f EGP", if (transaction.amount > 0) "+" else "-", Math.abs(transaction.amount)),
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = if (transaction.amount > 0) Color(0xFF10B981) else PinkPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = dateFormat.format(Date(transaction.timestamp)),
                    fontSize = 12.sp,
                    color = TextLight
                )

                if (transaction.raw_sms_body.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "\"${transaction.raw_sms_body}\"",
                            fontSize = 12.sp,
                            color = NavyDark.copy(alpha = 0.8f),
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Account Selection Header
        Text(
            text = stringResource(R.string.select_account_prompt),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = NavyDark
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Accounts List
        if (accounts.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Text(
                    text = "No accounts available. Please create an account first.",
                    color = TextLight,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                accounts.forEach { account ->
                    Surface(
                        onClick = { onAccountSelected(account.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BrandLogo(
                                provider = account.provider ?: "",
                                accountType = account.type,
                                iconColorHex = account.icon_color,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.name,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyDark,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${account.type.name} • ${account.current_balance} ${account.currency}",
                                    color = TextLight,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Optional Discard Button
        if (onDiscard != null) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onDiscard,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.discard_transaction),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
    }
}
