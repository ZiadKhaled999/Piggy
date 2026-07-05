package com.oryno.piggy_ledger.ui

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
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
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
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    viewModel: PiggyLedgerViewModel,
    onBack: () -> Unit
) {
    val loans by viewModel.loans.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedLoan by remember { mutableStateOf<Loan?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val owedToMe = loans.filter { !it.isPaidOff && it.type == LoanType.LENT }.sumOf { it.amount }
    val iOwe = loans.filter { !it.isPaidOff && it.type == LoanType.BORROWED }.sumOf { it.amount }
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NavyDark, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Loans & Payoffs", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = NavyDark)
                Text("Keep tabs on who owes who", fontSize = 14.sp, color = TextLight, fontWeight = FontWeight.Medium)
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
                                    Text("OWED TO ME", fontSize = 8.sp, fontWeight = FontWeight.Black, color = PinkAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$${String.format("%.0f", owedToMe)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NavyDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                    Text("I OWE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = BlackAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$${String.format("%.0f", iOwe)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NavyDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                            Text("NET LEDGER", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextLight)
                            Text(
                                text = if (netLedger >= 0) "+$${String.format("%.0f", netLedger)}" else "-$${String.format("%.0f", -netLedger)}", 
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
                                    Text("OWED TO ME", fontSize = 8.sp, fontWeight = FontWeight.Black, color = PinkAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("$${String.format("%.0f", owedToMe)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                    Text("I OWE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = BlackAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("$${String.format("%.0f", iOwe)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                Text("NET LEDGER", fontSize = 8.sp, fontWeight = FontWeight.Black, color = TextLight, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (netLedger >= 0) "+$${String.format("%.0f", netLedger)}" else "-$${String.format("%.0f", -netLedger)}", 
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
                placeholder = { Text("Search person or notes...", color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Medium) },
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
                onClick = { showAddDialog = true },
                modifier = Modifier.height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Record", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
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
            TabButton(text = "Active", isSelected = selectedTab == 0, onClick = { selectedTab = 0 }, modifier = Modifier.weight(1f))
            TabButton(text = "Paid Off", isSelected = selectedTab == 1, onClick = { selectedTab = 1 }, modifier = Modifier.weight(1f))
            TabButton(text = "Show All", isSelected = selectedTab == 2, onClick = { selectedTab = 2 }, modifier = Modifier.weight(1f))
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
                    Text("No transaction records found", fontWeight = FontWeight.ExtraBold, color = Color(0xFF334155), fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Log who you lent to or who you borrowed from to populate your ledger.", color = TextLight, textAlign = TextAlign.Center, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 16.dp))
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
                            val text = if (loan.type == LoanType.LENT) "OWED TO ME" else "I OWE"
                            val textColor = if (isPaidOff) Color(0xFF94A3B8) else (if (loan.type == LoanType.LENT) PinkAccent else BlackAccent)
                            
                            // Left side section (occupies flexible space)
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(bgTint, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
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
                                    val dateText = if (loan.deadline != null) {
                                        val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                                        "Due ${sdf.format(java.util.Date(loan.deadline))}"
                                    } else {
                                        "Open-Ended"
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
                                    Text(
                                        text = "$prefix$$${String.format("%.2f", loan.amount)}", 
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
                                        text = "DETAILS", 
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
        var socialDetails by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var hasDeadline by remember { mutableStateOf(false) }
        var deadlineDate by remember { mutableStateOf<Long?>(null) }
        var showDatePicker by remember { mutableStateOf(false) }
        val context = LocalContext.current
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
                                    socialDetails = emailCur.getString(emailIndex) ?: ""
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
                        text = "NEW LEDGER ENTRY",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = NavyDark,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Fill all details",
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
                            text = "TRANSACTION AMOUNT",
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
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            BasicTextField(
                                value = amountStr,
                                onValueChange = { amountStr = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NavyDark,
                                    textAlign = TextAlign.Center
                                ),
                                singleLine = true,
                                modifier = Modifier.widthIn(min = 120.dp),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.Center) {
                                        if (amountStr.isEmpty()) {
                                            Text(
                                                text = "0.00",
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
                                text = "I Lent", 
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
                                text = "I Borrowed", 
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
                        Text(
                            text = "CONTACT DETAILS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = NavyDark,
                            letterSpacing = 0.5.sp
                        )
                        
                        // Contact Name with integrated Native Picker inside it
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            label = { Text("Contact Name") },
                            placeholder = { Text("e.g. Mike Smith") },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (hasPermission) {
                                            contactPickerLauncher.launch(null)
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Pick Contact",
                                        tint = themeColor
                                    )
                                }
                            },
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

                        // Phone and Social (side-by-side or stacked cleanly)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Phone (Optional)") },
                                placeholder = { Text("e.g. +1 555...") },
                                textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.SemiBold, color = NavyDark, fontSize = 13.sp),
                                modifier = Modifier.weight(1f),
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
                            
                            OutlinedTextField(
                                value = socialDetails,
                                onValueChange = { socialDetails = it },
                                label = { Text("Social (Optional)") },
                                placeholder = { Text("e.g. email / handle") },
                                textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.SemiBold, color = NavyDark, fontSize = 13.sp),
                                modifier = Modifier.weight(1f),
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
                            text = "TRANSACTION MEMO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = NavyDark,
                            letterSpacing = 0.5.sp
                        )
                        
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Flashback Note (Required)") },
                            placeholder = { Text("Why did the money change hands? Recall details easily later...") },
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
                                .clickable { showDatePicker = true },
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
                                    Text("Repayment Deadline?", fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 13.sp)
                                    val dateText = if (deadlineDate != null) {
                                        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                                        "Due: ${sdf.format(java.util.Date(deadlineDate!!))}"
                                    } else {
                                        "No deadline set"
                                    }
                                    Text(dateText, color = TextLight, fontSize = 11.sp)
                                }
                                Checkbox(
                                    checked = deadlineDate != null,
                                    onCheckedChange = { 
                                        if (it) showDatePicker = true 
                                        else deadlineDate = null 
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = themeColor, uncheckedColor = TextLight)
                                )
                            }
                        }
                    }
                }

                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                deadlineDate = datePickerState.selectedDateMillis
                                showDatePicker = false
                            }) { Text("Confirm", color = themeColor, fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = TextLight) }
                        }
                    ) {
                        DatePicker(state = datePickerState)
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
                                    social = if (socialDetails.isNotBlank()) socialDetails else null,
                                    note = note,
                                    deadline = deadlineDate
                                )
                            )
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
                    Text("RECORD TO LEDGER", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                }
            }
        }
    }

    if (selectedLoan != null) {
        ModalBottomSheet(
            onDismissRequest = { 
                selectedLoan = null 
                showDeleteConfirm = false
            },
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.width(32.dp).height(4.dp).background(Color(0xFFE5E7EB), RoundedCornerShape(2.dp)))
                }
                Spacer(modifier = Modifier.height(24.dp))
                
                val typeText = if (selectedLoan!!.type == LoanType.LENT) "OWED TO ME" else "I OWE THIS"
                val typeColor = if (selectedLoan!!.type == LoanType.LENT) PinkAccent else BlackAccent
                val prefix = if (selectedLoan!!.type == LoanType.LENT) "+" else "-"
                
                Surface(
                    color = typeColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(typeText, color = typeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "$prefix$$${String.format("%.2f", selectedLoan!!.amount)}",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("CONTACT NAME", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextLight)
                    Text(selectedLoan!!.contactName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("REPAYMENT DEADLINE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextLight)
                    Text("Open-Ended (No strict deadline)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("FLASHBACK RECALL NOTE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("\"${selectedLoan!!.note}\"", fontSize = 14.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = NavyDark)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        viewModel.markLoanAsPaid(selectedLoan!!.id)
                        selectedLoan = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedLoan!!.type == LoanType.LENT) PinkAccent else NavyDark)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MARK AS PAID OFF & SETTLE", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (showDeleteConfirm) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFFFEBEE), RoundedCornerShape(12.dp)).padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Are you absolutely sure you want to delete?", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row {
                            Button(
                                onClick = {
                                    // TODO: Add delete functionality to viewmodel if needed
                                    // For now we just close it
                                    selectedLoan = null
                                    showDeleteConfirm = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("Yes", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { showDeleteConfirm = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("No", color = NavyDark, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    TextButton(
                        onClick = { showDeleteConfirm = true }
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = TextLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Record", color = TextLight, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
