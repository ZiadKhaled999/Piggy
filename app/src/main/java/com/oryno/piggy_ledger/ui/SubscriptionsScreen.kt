package com.oryno.piggy_ledger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.PiggyLedgerRepository
import com.oryno.piggy_ledger.data.SubscriptionEntity
import com.oryno.piggy_ledger.ui.subscription.CatalogItem
import com.oryno.piggy_ledger.ui.subscription.SubscriptionViewModel
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val dao = remember { PiggyLedgerDatabase.getInstance(context).piggyLedgerDao() }
    val repository = remember { PiggyLedgerRepository(dao, context) }

    val viewModel: SubscriptionViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return SubscriptionViewModel(repository, dao) as T
        }
    })

    val uiState by viewModel.uiState.collectAsState()

    var showAddModal by remember { mutableStateOf(false) }
    var selectedCatalogItem by remember { mutableStateOf<CatalogItem?>(null) }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Subscriptions & Recurring Bills",
                        fontWeight = FontWeight.Bold,
                        color = NavyDark,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NavyDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedCatalogItem = null
                    showAddModal = true
                },
                containerColor = PinkPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subscription")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Total Recurring Expense Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Recurring Expense",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextLight
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFCE7F3)
                            ) {
                                Text(
                                    text = "${uiState.subscriptions.size} Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDB2777),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format(Locale.getDefault(), "%,.2f", uiState.totalMonthlyCost)} EGP / mo",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Yearly commitment: ~${String.format(Locale.getDefault(), "%,.0f", uiState.totalYearlyCost)} EGP",
                            fontSize = 12.sp,
                            color = TextLight
                        )
                    }
                }
            }

            // 2. Popular Services Catalog Horizontal Bar (Logo.dev powered)
            item {
                Column {
                    Text(
                        text = "Popular Services Catalog",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(viewModel.catalogServices, key = { it.domain }) { service ->
                            CatalogServiceCard(
                                service = service,
                                onClick = {
                                    selectedCatalogItem = service
                                    showAddModal = true
                                }
                            )
                        }
                    }
                }
            }

            // 3. Active Subscriptions List
            item {
                Text(
                    text = "My Active Subscriptions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (uiState.subscriptions.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventRepeat,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No subscriptions added yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NavyDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap any service above or tap + to track your recurring memberships.",
                                fontSize = 12.5.sp,
                                color = TextLight,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(uiState.subscriptions, key = { it.id }) { sub ->
                    SubscriptionCard(
                        subscription = sub,
                        onDelete = { viewModel.deleteSubscription(sub.id) }
                    )
                }
            }
        }
    }

    // Add Subscription BottomSheet
    if (showAddModal) {
        AddSubscriptionBottomSheet(
            catalogItem = selectedCatalogItem,
            onDismiss = {
                showAddModal = false
                selectedCatalogItem = null
            },
            onSave = { name, domain, amount, cycle, accountId, autoDebit ->
                viewModel.addSubscription(name, domain, amount, cycle, accountId, autoDebit)
                showAddModal = false
                selectedCatalogItem = null
            }
        )
    }
}

@Composable
private fun CatalogServiceCard(
    service: CatalogItem,
    onClick: () -> Unit
) {
    val logoUrl = remember(service.domain) { buildLogoDevUrl(service.domain) }

    Surface(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = service.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = service.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${service.defaultAmount.toInt()} EGP",
                fontSize = 11.sp,
                color = TextLight,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SubscriptionCard(
    subscription: SubscriptionEntity,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val renewalStr = formatter.format(Date(subscription.nextRenewalDate))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                if (!subscription.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(subscription.logoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = subscription.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Text(
                        text = subscription.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = NavyDark,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subscription.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Next renewal: $renewalStr",
                    fontSize = 12.sp,
                    color = TextLight
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format(Locale.getDefault(), "%,.2f", subscription.amount)} ${subscription.currency}",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Subscription",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSubscriptionBottomSheet(
    catalogItem: CatalogItem?,
    onDismiss: () -> Unit,
    onSave: (name: String, domain: String, amount: Double, cycle: String, accountId: String, autoDebit: Boolean) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (catalogItem != null) "Add ${catalogItem.name}" else "Add New Subscription",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )

            var name by remember { mutableStateOf(catalogItem?.name ?: "") }
            var domain by remember { mutableStateOf(catalogItem?.domain ?: "") }
            var amountText by remember { mutableStateOf(catalogItem?.defaultAmount?.toString() ?: "") }
            var billingCycle by remember { mutableStateOf("MONTHLY") }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Service Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = domain,
                onValueChange = { domain = it },
                label = { Text("Website Domain (for logo.dev icon)") },
                placeholder = { Text("e.g. spotify.com") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Subscription Cost (EGP)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Text("Billing Frequency", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NavyDark)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("MONTHLY" to "Monthly", "YEARLY" to "Yearly", "WEEKLY" to "Weekly").forEach { (code, label) ->
                    FilterChip(
                        selected = billingCycle == code,
                        onClick = { billingCycle = code },
                        label = { Text(label) }
                    )
                }
            }

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && amount > 0) {
                        onSave(name, domain, amount, billingCycle, "", false)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
            ) {
                Text("Save Subscription", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
