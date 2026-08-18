
package com.oryno.piggy_ledger.ui

import java.util.Locale
import androidx.compose.material.icons.filled.AttachMoney

import androidx.compose.material.icons.filled.ReceiptLong

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight

import androidx.compose.material.icons.filled.ReceiptLong

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight

import android.widget.Toast

import android.Manifest

import android.content.pm.PackageManager

import android.provider.ContactsContract

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.text.BasicTextField

import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.material.icons.automirrored.filled.TrendingDown

import androidx.compose.material.icons.automirrored.filled.TrendingUp

import androidx.compose.material.icons.filled.Add

import androidx.compose.material.icons.filled.Check

import androidx.compose.material.icons.filled.DeleteOutline

import androidx.compose.material.icons.filled.HelpOutline

import androidx.compose.material.icons.filled.Person

import androidx.compose.material.icons.filled.PersonAdd

import androidx.compose.material.icons.filled.Phone

import androidx.compose.material.icons.filled.Info

import androidx.compose.material.icons.filled.Search

import androidx.compose.material.icons.filled.Visibility

import androidx.compose.material.icons.filled.VisibilityOff

import androidx.compose.material3.*

import androidx.compose.ui.res.stringResource

import com.oryno.piggy_ledger.R

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.text.style.TextDecoration

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.core.content.ContextCompat

import com.oryno.piggy_ledger.data.Loan

import com.oryno.piggy_ledger.data.LoanType

import com.oryno.piggy_ledger.ui.theme.PinkAccent

import com.oryno.piggy_ledger.ui.theme.NavyDark

import com.oryno.piggy_ledger.ui.theme.BlackAccent

import androidx.compose.ui.layout.ContentScale

import coil.compose.AsyncImage

import com.oryno.piggy_ledger.ui.theme.PinkPrimary

import com.oryno.piggy_ledger.ui.theme.TextLight

import java.util.UUID

import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    viewModel: PiggyLedgerViewModel,
    onBack: () -> Unit
) {
    val screenContext = LocalContext.current
    val loans by viewModel.loans.collectAsState()
    val allLoanPayments by viewModel.allLoanPayments.collectAsState()
    val isPrivacyMode by viewModel.isPrivacyModeEnabled.collectAsState()
    
    val paymentsByLoanId = remember(allLoanPayments) { allLoanPayments.groupBy { it.loanId } }
    fun getRemainingAmount(loan: Loan): Double {
        val paid = paymentsByLoanId[loan.id]?.sumOf { it.amount } ?: 0.0
        return (loan.amount - paid).coerceAtLeast(0.0)
    }

    var showAddDialog by remember { mutableStateOf(false) }
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedLoan by remember { mutableStateOf<Loan?>(null) }
    var showTimeline by remember { mutableStateOf(false) }
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val owedToMe = loans.filter { !it.isPaidOff && it.type == LoanType.LENT }.sumOf { getRemainingAmount(it) }
    val iOwe = loans.filter { !it.isPaidOff && it.type == LoanType.BORROWED }.sumOf { getRemainingAmount(it) }
    val netLedger = owedToMe - iOwe

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                    .border(1.dp, Color(0xFFE2E8F0), androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_icon), tint = NavyDark, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(stringResource(R.string.loans_payoffs_title), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = NavyDark)
                Text(stringResource(R.string.keep_tabs_subtitle), fontSize = 14.sp, color = TextLight, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = { viewModel.togglePrivacyMode(screenContext) },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                    .border(1.dp, Color(0xFFE2E8F0), androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(
                    imageVector = if (isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle Privacy",
                    tint = NavyDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val isNarrow = maxWidth < 340.dp
            
            if (isNarrow) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFCBD5E1))
                        ) {
                             Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = PinkAccent, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(stringResource(R.string.owed_to_me), fontSize = 8.sp, fontWeight = FontWeight.Black, color = PinkAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(if (isPrivacyMode) "$••••••" else "$${String.format("%.0f", owedToMe)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NavyDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFCBD5E1))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, tint = BlackAccent, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(stringResource(R.string.i_owe), fontSize = 8.sp, fontWeight = FontWeight.Black, color = BlackAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(if (isPrivacyMode) "$••••••" else "$${String.format("%.0f", iOwe)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NavyDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.net_ledger), fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextLight)
                            Text(
                                text = if (isPrivacyMode) "$••••••" else (if (netLedger >= 0) "+$${String.format("%.0f", netLedger)}" else "-$${String.format("%.0f", -netLedger)}"), 
                                fontSize = 15.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = if (netLedger >= 0) PinkAccent else BlackAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFCBD5E1))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFCBD5E1))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = PinkAccent, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(stringResource(R.string.owed_to_me), fontSize = 8.sp, fontWeight = FontWeight.Black, color = PinkAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(if (isPrivacyMode) "$••••••" else "$${String.format("%.0f", owedToMe)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFCBD5E1))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, tint = BlackAccent, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(stringResource(R.string.i_owe), fontSize = 8.sp, fontWeight = FontWeight.Black, color = BlackAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(if (isPrivacyMode) "$••••••" else "$${String.format("%.0f", iOwe)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFCBD5E1))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                                Text(stringResource(R.string.net_ledger), fontSize = 8.sp, fontWeight = FontWeight.Black, color = TextLight, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isPrivacyMode) "$••••••" else (if (netLedger >= 0) "+$${String.format("%.0f", netLedger)}" else "-$${String.format("%.0f", -netLedger)}"), 
                                    fontSize = 16.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = if (netLedger >= 0) PinkAccent else BlackAccent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.search_person_notes), color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextLight, modifier = Modifier.size(20.dp)) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color(0xFFE2E8F0)
                ),
                singleLine = true
            )
            
            Button(
                onClick = { 
                    if (viewModel.canAddLoan(loans.size)) {
                        showAddDialog = true 
                    } else {
                        com.oryno.piggy_ledger.ui.ToastUtil.show(screenContext, "Upgrade to Pro to add more loans", Toast.LENGTH_SHORT)
                    }
                },
                modifier = Modifier.height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.add_record), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabButton(text = stringResource(R.string.active_tab), isSelected = selectedTab == 0, onClick = { selectedTab = 0 }, modifier = Modifier.weight(1f))
            TabButton(text = stringResource(R.string.paid_off_tab), isSelected = selectedTab == 1, onClick = { selectedTab = 1 }, modifier = Modifier.weight(1f))
            TabButton(text = stringResource(R.string.show_all_tab), isSelected = selectedTab == 2, onClick = { selectedTab = 2 }, modifier = Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val filteredLoans = loans.filter { loan ->
            when (selectedTab) {
                0 -> !loan.isPaidOff
                1 -> loan.isPaidOff
                else -> true
            }
        }.filter { loan ->
            searchQuery.isBlank() || loan.contactName.contains(searchQuery, ignoreCase = true) || loan.note.contains(searchQuery, ignoreCase = true)
        }
        
        if (filteredLoans.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 24.dp),
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.5.dp, Color(0xFFCBD5E1))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFFF8FAFC), androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = TextLight.copy(alpha = 0.3f), modifier = Modifier.size(40.dp))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(stringResource(R.string.no_records_found), fontWeight = FontWeight.ExtraBold, color = Color(0xFF334155), fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(R.string.log_lent_borrowed_desc), color = TextLight, textAlign = TextAlign.Center, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredLoans) { loan ->
                    val isPaidOff = loan.isPaidOff
                    val containerColor = if (isPaidOff) Color(0xFFF1F5F9) else PinkPrimary.copy(alpha = 0.03f)
                    val borderColor = if (isPaidOff) Color(0xFFCBD5E1) else PinkPrimary.copy(alpha = 0.15f)
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLoan = loan },
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val iconTint = if (isPaidOff) Color(0xFF94A3B8) else (if (loan.type == LoanType.LENT) PinkAccent else BlackAccent)
                            val bgTint = if (isPaidOff) Color(0xFFE2E8F0) else (if (loan.type == LoanType.LENT) PinkAccent.copy(alpha = 0.1f) else BlackAccent.copy(alpha = 0.1f))
                            val icon = if (loan.type == LoanType.LENT) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown
                            val prefix = if (loan.type == LoanType.LENT) "+" else "-"
                            val text = if (loan.type == LoanType.LENT) stringResource(R.string.owed_to_me) else stringResource(R.string.i_owe)
                            val textColor = if (isPaidOff) Color(0xFF94A3B8) else (if (loan.type == LoanType.LENT) PinkAccent else BlackAccent)
                            
                            // Left side section (occupies flexible space)
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!loan.photoUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = Uri.parse(loan.photoUri),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, iconTint.copy(alpha = 0.3f), CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(bgTint, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = loan.contactName, 
                                        fontWeight = FontWeight.Bold, 
                                        color = if (isPaidOff) Color(0xFF64748B) else NavyDark, 
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textDecoration = if (isPaidOff) TextDecoration.LineThrough else null
                                    )
                                    val formattedDate = remember(loan.deadline) {
                                        if (loan.deadline != null) {
                                            try {
                                                val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                                                sdf.format(java.util.Date(loan.deadline))
                                            } catch (e: Exception) {
                                                ""
                                            }
                                        } else ""
                                    }
                                    val dateText = if (formattedDate.isNotBlank()) {
                                        stringResource(R.string.due_date, formattedDate)
                                    } else {
                                        stringResource(R.string.open_ended)
                                    }
                                    Text(
                                        text = dateText, 
                                        color = TextLight, 
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Right side section (Amount & Details badge, fits compactly)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    val currentAmt = if (isPaidOff) loan.amount else getRemainingAmount(loan)
                                    Text(
                                        text = if (isPrivacyMode) "$prefix $••••••" else "$prefix $${String.format("%.2f", currentAmt)}", 
                                        color = if (isPaidOff) Color(0xFF64748B) else textColor, 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip,
                                        textDecoration = if (isPaidOff) TextDecoration.LineThrough else null
                                    )
                                    Text(
                                        text = text, 
                                        color = textColor, 
                                        fontSize = 9.sp, 
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (isPaidOff) Color(0xFFCBD5E1) else PinkPrimary.copy(alpha = 0.15f))
                                ) {
                                    Text(
                                        text = stringResource(R.string.details_badge), 
                                        fontSize = 9.sp, 
                                        fontWeight = FontWeight.Black, 
                                        color = if (isPaidOff) Color(0xFF94A3B8) else PinkPrimary, 
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        var isLent by remember { mutableStateOf(true) }
        var amountStr by remember { mutableStateOf("") }
        var contactName by remember { mutableStateOf("") }
        var phoneNumber by remember { mutableStateOf("") }
        var emailAddress by remember { mutableStateOf("") }
        var contactPhotoUri by remember { mutableStateOf<String?>(null) }
        var socialDetails by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var hasDeadline by remember { mutableStateOf(false) }
        var deadlineDate by remember { mutableStateOf<Long?>(null) }
        val context = LocalContext.current
        val showDeadlinePicker = {
            val calendar = java.util.Calendar.getInstance()
            deadlineDate?.let { calendar.timeInMillis = it }
            val datePickerDialog = android.app.DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val selectedCal = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.YEAR, year)
                        set(java.util.Calendar.MONTH, month)
                        set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                        set(java.util.Calendar.HOUR_OF_DAY, 23)
                        set(java.util.Calendar.MINUTE, 59)
                        set(java.util.Calendar.SECOND, 59)
                        set(java.util.Calendar.MILLISECOND, 999)
                    }
                    deadlineDate = selectedCal.timeInMillis
                },
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
        var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) }
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            hasPermission = isGranted
        }
        val contactPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
            if (uri != null && hasPermission) {
                try {
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    if (cursor != null && cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            contactName = cursor.getString(nameIndex) ?: ""
                        }
                        
                        val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                        val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                        val photoUriIndex = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
                        
                        if (photoUriIndex != -1) {
                            contactPhotoUri = cursor.getString(photoUriIndex)
                        }

                        if (idIndex != -1 && hasPhoneIndex != -1) {
                            val id = cursor.getString(idIndex)
                            val hasPhone = cursor.getInt(hasPhoneIndex) > 0
                            
                            if (hasPhone) {
                                val pCur = context.contentResolver.query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                    arrayOf(id),
                                    null
                                )
                                if (pCur != null && pCur.moveToFirst()) {
                                    val phoneIndex = pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    if (phoneIndex != -1) {
                                        phoneNumber = pCur.getString(phoneIndex) ?: ""
                                    }
                                    pCur.close()
                                }
                            }
                            
                            // Email
                            val emailCur = context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                                arrayOf(id),
                                null
                            )
                            if (emailCur != null && emailCur.moveToFirst()) {
                                val emailIndex = emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)
                                if (emailIndex != -1) {
                                    emailAddress = emailCur.getString(emailIndex) ?: ""
                                }
                                emailCur.close()
                            }
                        }
                        cursor.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        ModalBottomSheet(
            onDismissRequest = { showAddDialog = false },
            sheetState = addSheetState,
            containerColor = Color.White,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .background(Color(0xFFE2E8F0), RoundedCornerShape(2.dp))
                )
            },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.new_ledger_entry),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = NavyDark,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = stringResource(R.string.fill_all_details),
                        fontSize = 11.sp,
                        color = TextLight,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Dynamic Hero Amount Box
                val themeColor = if (isLent) PinkAccent else BlackAccent
                val themeBg = if (isLent) PinkAccent.copy(alpha = 0.05f) else BlackAccent.copy(alpha = 0.05f)
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = themeBg),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, themeColor.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.transaction_amount_label),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = themeColor,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            BasicTextField(
                                value = amountStr,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() || it == '.' }) {
                                        amountStr = input
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NavyDark,
                                    textAlign = TextAlign.Start
                                ),
                                singleLine = true,
                                modifier = Modifier.widthIn(min = 120.dp),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.Center) {
                                        if (amountStr.isEmpty()) {
                                            Text(
                                                text = stringResource(R.string.zero_amount),
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFCBD5E1),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                }

                // Sleek Custom Toggle (Lent vs Borrowed)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isLent) Color.White else Color.Transparent)
                            .border(
                                if (isLent) BorderStroke(1.5.dp, PinkAccent) else BorderStroke(0.dp, Color.Transparent),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { isLent = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = if (isLent) PinkAccent else TextLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.i_lent), 
                                fontWeight = if (isLent) FontWeight.Bold else FontWeight.Medium, 
                                color = if (isLent) PinkAccent else TextLight,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isLent) Color.White else Color.Transparent)
                            .border(
                                if (!isLent) BorderStroke(1.5.dp, BlackAccent) else BorderStroke(0.dp, Color.Transparent),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { isLent = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (!isLent) BlackAccent else TextLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.i_borrowed), 
                                fontWeight = if (!isLent) FontWeight.Bold else FontWeight.Medium, 
                                color = if (!isLent) BlackAccent else TextLight,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Section: Contact Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!contactPhotoUri.isNullOrBlank()) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = Uri.parse(contactPhotoUri),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .border(3.dp, themeColor.copy(alpha = 0.4f), CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Highlighted Premium Import Button
                        Surface(
                            onClick = {
                                if (hasPermission) {
                                    contactPickerLauncher.launch(null)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                            },
                            color = themeColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(2.dp, themeColor.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (contactName.isBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(themeColor.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = null,
                                            tint = themeColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                                Column(horizontalAlignment = if (contactName.isBlank()) Alignment.Start else Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (contactName.isNotBlank()) contactName else stringResource(R.string.import_from_contacts),
                                        color = NavyDark,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    if (contactName.isBlank()) {
                                        Text(
                                            text = stringResource(R.string.fill_all_details),
                                            color = themeColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.contact_imported_success),
                                            color = themeColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.contact_details_label),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = NavyDark,
                            letterSpacing = 0.5.sp
                        )
                        
                        // Contact Name
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            label = { Text(stringResource(R.string.contact_name_label)) },
                            placeholder = { Text(stringResource(R.string.mike_smith_placeholder)) },
                            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedBorderColor = themeColor,
                                focusedLabelColor = themeColor,
                                unfocusedLabelColor = TextLight,
                                cursorColor = themeColor
                            )
                        )

                        // Phone and Email (side-by-side or stacked cleanly)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text(stringResource(R.string.phone_optional)) },
                                placeholder = { Text(stringResource(R.string.phone_placeholder)) },
                                textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.SemiBold, color = NavyDark, fontSize = 13.sp),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.White,
                                    focusedContainerColor = Color.White,
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedBorderColor = themeColor,
                                    focusedLabelColor = themeColor,
                                    unfocusedLabelColor = TextLight,
                                    cursorColor = themeColor
                                )
                            )
                            
                            OutlinedTextField(
                                value = emailAddress,
                                onValueChange = { emailAddress = it },
                                label = { Text(stringResource(R.string.email_optional)) },
                                placeholder = { Text(stringResource(R.string.email_placeholder)) },
                                textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.SemiBold, color = NavyDark, fontSize = 13.sp),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.White,
                                    focusedContainerColor = Color.White,
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedBorderColor = themeColor,
                                    focusedLabelColor = themeColor,
                                    unfocusedLabelColor = TextLight,
                                    cursorColor = themeColor
                                )
                            )
                        }

                        // Social Details
                        OutlinedTextField(
                            value = socialDetails,
                            onValueChange = { socialDetails = it },
                            label = { Text(stringResource(R.string.social_optional)) },
                            placeholder = { Text(stringResource(R.string.social_placeholder)) },
                            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.SemiBold, color = NavyDark, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedBorderColor = themeColor,
                                focusedLabelColor = themeColor,
                                unfocusedLabelColor = TextLight,
                                cursorColor = themeColor
                            )
                        )
                    }
                }

                // Section: Transaction Memo Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.transaction_memo_header),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = NavyDark,
                            letterSpacing = 0.5.sp
                        )
                        
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text(stringResource(R.string.flashback_note_required)) },
                            placeholder = { Text(stringResource(R.string.flashback_placeholder)) },
                            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Medium, color = NavyDark, fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedBorderColor = themeColor,
                                focusedLabelColor = themeColor,
                                unfocusedLabelColor = TextLight,
                                cursorColor = themeColor
                            )
                        )
                        
                        // Set Deadline Card inside section
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDeadlinePicker() },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.repayment_deadline), fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 13.sp)
                                    val formattedDeadline = remember(deadlineDate) {
                                        if (deadlineDate != null) {
                                            try {
                                                val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                                                sdf.format(java.util.Date(deadlineDate!!))
                                            } catch (e: Exception) {
                                                ""
                                            }
                                        } else ""
                                    }
                                    val dateText = if (formattedDeadline.isNotBlank()) {
                                        stringResource(R.string.due_prefix, formattedDeadline)
                                    } else {
                                        stringResource(R.string.no_deadline_set)
                                    }
                                    Text(dateText, color = TextLight, fontSize = 11.sp)
                                }
                                Checkbox(
                                    checked = deadlineDate != null,
                                    onCheckedChange = { 
                                        if (it) showDeadlinePicker() 
                                        else deadlineDate = null 
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = themeColor, uncheckedColor = TextLight)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val amount = amountStr.replace("$", "").trim().toDoubleOrNull()
                        if (amount != null && contactName.isNotBlank() && note.isNotBlank()) {
                            viewModel.addLoan(
                                Loan(
                                    id = UUID.randomUUID().toString(),
                                    type = if (isLent) LoanType.LENT else LoanType.BORROWED,
                                    amount = amount,
                                    contactName = contactName,
                                    phone = if (phoneNumber.isNotBlank()) phoneNumber else null,
                                    email = if (emailAddress.isNotBlank()) emailAddress else null,
                                    photoUri = contactPhotoUri,
                                    social = if (socialDetails.isNotBlank()) socialDetails else null,
                                    note = note,
                                    deadline = deadlineDate
                                )
                            )
                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.toast_loan_added), Toast.LENGTH_SHORT)
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(stringResource(R.string.record_to_ledger_btn), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                }
            }
        }
    }

    if (selectedLoan != null) {
        val payments by viewModel.getPaymentsForLoan(selectedLoan!!.id).collectAsState(initial = emptyList())
        val paidAmount = payments.sumOf { it.amount }
        val remainingAmount = selectedLoan!!.amount - paidAmount

        ModalBottomSheet(
            onDismissRequest = { 
                selectedLoan = null 
                showTimeline = false
                showDeleteConfirm = false
            },
            sheetState = detailSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .background(Color(0xFFE2E8F0), CircleShape)
                )
            }
        ) {
            if (showTimeline) {
                // TIMELINE VIEW
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    var showAddPayment by remember { mutableStateOf(false) }
                    
                    if (showAddPayment) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showAddPayment = false },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFF1F5F9), CircleShape)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NavyDark)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(R.string.add_payment), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = NavyDark)
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        var payAmount by remember { mutableStateOf("") }
                        var payNote by remember { mutableStateOf("") }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = payAmount,
                                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) payAmount = it },
                                label = { Text(stringResource(R.string.payment_amount)) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PinkPrimary,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                leadingIcon = {
                                    Text("$", fontWeight = FontWeight.Bold, color = NavyDark, modifier = Modifier.padding(start = 16.dp))
                                }
                            )
                            OutlinedTextField(
                                value = payNote,
                                onValueChange = { payNote = it },
                                label = { Text(stringResource(R.string.payment_note)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PinkPrimary,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                )
                            )
                            Button(
                                onClick = {
                                    val amt = payAmount.toDoubleOrNull() ?: 0.0
                                    if (amt > 0) {
                                        viewModel.addLoanPayment(selectedLoan!!.id, amt, payNote.takeIf { it.isNotBlank() })
                                        com.oryno.piggy_ledger.ui.ToastUtil.show(screenContext, screenContext.getString(R.string.toast_loan_payment_added), Toast.LENGTH_SHORT)
                                        showAddPayment = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                            ) {
                                Text(stringResource(R.string.confirm_payment), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { showTimeline = false },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFFF1F5F9), CircleShape)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NavyDark)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(stringResource(R.string.logs_timeline), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = NavyDark)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Button(
                            onClick = { showAddPayment = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary.copy(alpha = 0.12f), contentColor = PinkPrimary),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.add_payment), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        if (payments.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(stringResource(R.string.no_payments_yet), color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(payments) { payment ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                                    .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.AttachMoney, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(22.dp))
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (isPrivacyMode) "$••••••" else "$${String.format(Locale.US, "%.2f", payment.amount)}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = NavyDark
                                                )
                                                if (!payment.note.isNullOrBlank()) {
                                                    Text(payment.note, fontSize = 13.sp, color = TextLight)
                                                }
                                                val sdf = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                                                Text(sdf.format(java.util.Date(payment.timestamp)), fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 2.dp))
                                            }
                                            IconButton(onClick = { viewModel.deleteLoanPayment(payment.id) }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // MAIN DETAIL VIEW
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val typeText = if (selectedLoan!!.type == LoanType.LENT) stringResource(R.string.owed_to_me) else stringResource(R.string.i_owe_this)
                    val typeColor = if (selectedLoan!!.type == LoanType.LENT) PinkAccent else BlackAccent
                    val prefix = if (selectedLoan!!.type == LoanType.LENT) "+" else "-"
                    
                    Surface(
                        color = typeColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(typeText, color = typeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isPrivacyMode) "$prefix $••••••" else "$prefix $${String.format(Locale.US, "%.2f", remainingAmount)}",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                color = typeColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isPrivacyMode) stringResource(R.string.remaining_of_prefix, "$••••••") else stringResource(R.string.remaining_of_prefix, "$${String.format(Locale.US, "%.2f", selectedLoan!!.amount)}"),
                                fontSize = 13.sp,
                                color = TextLight,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Surface(
                        onClick = { showTimeline = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF8FAFC),
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
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(typeColor.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = typeColor, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(stringResource(R.string.logs_timeline), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyDark)
                                    Text("${payments.size} ${if (payments.size == 1) "payment" else "payments"} recorded", fontSize = 12.sp, color = TextLight)
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF94A3B8))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.contact_name_header), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextLight)
                        Text(selectedLoan!!.contactName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    }

                    if (!selectedLoan!!.phone.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.phone_label), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextLight)
                            Text(selectedLoan!!.phone!!, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                        }
                    }

                    if (!selectedLoan!!.email.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.email_label), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextLight)
                            Text(selectedLoan!!.email!!, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                            val formattedDetailDeadline = remember(selectedLoan?.deadline) {
                                val deadline = selectedLoan?.deadline
                                if (deadline != null) {
                                    try {
                                        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                                        sdf.format(java.util.Date(deadline))
                                    } catch (e: Exception) {
                                        ""
                                    }
                                } else ""
                            }
                            val (deadlineLabel, dateValue) = if (formattedDetailDeadline.isNotBlank()) {
                                stringResource(R.string.repayment_deadline_header) to stringResource(R.string.due_prefix, formattedDetailDeadline)
                            } else {
                                stringResource(R.string.repayment_deadline_header) to stringResource(R.string.no_strict_deadline)
                            }
                            
                            Text(deadlineLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextLight)
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = dateValue,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyDark,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = typeColor.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, typeColor.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = typeColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.flashback_recall_note), fontSize = 11.sp, fontWeight = FontWeight.Black, color = typeColor, letterSpacing = 1.sp)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "\"${selectedLoan!!.note}\"",
                                fontSize = 16.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark,
                                lineHeight = 24.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                viewModel.markLoanAsPaid(selectedLoan!!.id)
                                com.oryno.piggy_ledger.ui.ToastUtil.show(screenContext, screenContext.getString(R.string.toast_loan_closed), Toast.LENGTH_SHORT)
                                selectedLoan = null
                            },
                            modifier = Modifier
                                .weight(2f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedLoan!!.type == LoanType.LENT) PinkAccent else NavyDark,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.mark_as_paid_off),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFEBEE),
                                contentColor = Color(0xFFD32F2F)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.delete_button),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    if (showDeleteConfirm) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFEBEE), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.delete_confirm_msg),
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Row {
                                Button(
                                    onClick = {
                                        viewModel.deleteLoan(selectedLoan!!.id)
                                        com.oryno.piggy_ledger.ui.ToastUtil.show(screenContext, screenContext.getString(R.string.toast_loan_deleted), Toast.LENGTH_SHORT)
                                        selectedLoan = null
                                        showDeleteConfirm = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text(stringResource(R.string.yes_btn), fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = { showDeleteConfirm = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text(stringResource(R.string.no_btn), color = NavyDark, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
