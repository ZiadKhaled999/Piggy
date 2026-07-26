package com.oryno.piggy_ledger.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inbox
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
                    Text(
                        text = stringResource(R.string.pending_transactions),
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
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
                    containerColor = Color(0xFFF4F6F9)
                )
            )
        },
        containerColor = Color(0xFFF4F6F9)
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
                            .size(240.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.no_pending_transactions),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyDark,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.no_pending_transactions_desc),
                        fontSize = 15.sp,
                        color = TextLight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        lineHeight = 22.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                ) {
                    items(pendingTransactions, key = { it.id }) { tx ->
                        PendingTransactionItem(
                            tx = tx,
                            dateFormat = dateFormat,
                            onResolve = {
                                selectedTransaction = tx
                                showResolveBottomSheet = true
                            },
                            onDiscard = {
                                viewModel.deletePendingTransaction(tx.id)
                            }
                        )
                    }
                }
            }

            if (showResolveBottomSheet && selectedTransaction != null) {
                ModalBottomSheet(
                    onDismissRequest = { showResolveBottomSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = Color.White
                ) {
                    ResolveTransactionBottomSheetContent(
                        transaction = selectedTransaction!!,
                        accounts = accounts,
                        onAccountSelected = { accountId ->
                            viewModel.resolvePendingTransaction(selectedTransaction!!.id, accountId)
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
    onResolve: () -> Unit,
    onDiscard: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_item_${tx.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        tint = PinkPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tx.sender,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = NavyDark
                    )
                }
                
                Text(
                    text = String.format(Locale.US, "%s%.2f EGP", if (tx.amount > 0) "+" else "-", Math.abs(tx.amount)),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = if (tx.amount > 0) Color(0xFF10B981) else PinkPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = tx.raw_sms_body,
                fontSize = 14.sp,
                color = NavyDark.copy(alpha = 0.8f),
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            HorizontalDivider(color = Color(0xFFF1F5F9))
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(tx.timestamp)),
                    fontSize = 12.sp,
                    color = TextLight
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onDiscard,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red.copy(alpha = 0.8f))
                    ) {
                        Text(stringResource(R.string.discard_pending_success))
                    }
                    
                    Button(
                        onClick = onResolve,
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.select_account),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResolveTransactionBottomSheetContent(
    transaction: PendingTransaction,
    accounts: List<Account>,
    onAccountSelected: (Long) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.pending_sms_detected),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NavyDark
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFFFF1F2))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.pending_sms_prompt, Math.abs(transaction.amount), "EGP", transaction.sender),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = NavyDark,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"${transaction.raw_sms_body}\"",
                    fontSize = 13.sp,
                    color = TextLight,
                    lineHeight = 18.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.select_account_prompt),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = NavyDark
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (accounts.isEmpty()) {
            Text(
                text = "No accounts available. Please create an account first.",
                color = TextLight,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(accounts) { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .clickable { onAccountSelected(account.id) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BrandLogo(
                            provider = account.provider ?: "",
                            accountType = account.type,
                            iconColorHex = account.icon_color,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = account.name,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${account.type.name} • ${account.current_balance} ${account.currency}",
                                color = TextLight,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
