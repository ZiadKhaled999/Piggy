package com.example.ui

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.Loan
import com.example.data.LoanType
import com.example.ui.theme.GreenAccent
import com.example.ui.theme.NavyDark
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.TextLight
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
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFF8FAFC),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(3.dp, Color(0xFFCBD5E1))
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
                    border = BorderStroke(3.dp, Color(0xFFCBD5E1))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = GreenAccent, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("OWED TO ME", fontSize = 9.sp, fontWeight = FontWeight.Black, color = GreenAccent)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("$${String.format("%.0f", owedToMe)}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    }
                }
                
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(3.dp, Color(0xFFCBD5E1))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("I OWE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = OrangeAccent)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("$${String.format("%.0f", iOwe)}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    }
                }
                
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(3.dp, Color(0xFFCBD5E1))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("NET LEDGER", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextLight)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (netLedger >= 0) "+$${String.format("%.0f", netLedger)}" else "-$${String.format("%.0f", -netLedger)}", 
                            fontSize = 22.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = if (netLedger >= 0) GreenAccent else OrangeAccent
                        )
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
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
                color = Color.White,
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9).copy(alpha = 0.5f))
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLoan = loan },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val iconTint = if (loan.type == LoanType.LENT) GreenAccent else OrangeAccent
                            val bgTint = if (loan.type == LoanType.LENT) GreenAccent.copy(alpha = 0.1f) else OrangeAccent.copy(alpha = 0.1f)
                            val icon = if (loan.type == LoanType.LENT) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown
                            val prefix = if (loan.type == LoanType.LENT) "+" else "-"
                            val text = if (loan.type == LoanType.LENT) "OWED TO ME" else "I OWE"
                            val textColor = if (loan.type == LoanType.LENT) GreenAccent else OrangeAccent
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(bgTint, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = iconTint)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(loan.contactName, fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 16.sp)
                                    val dateText = if (loan.deadline != null) {
                                        val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                                        "Due ${sdf.format(java.util.Date(loan.deadline))}"
                                    } else {
                                        "Open-Ended"
                                    }
                                    Text(dateText, color = TextLight, fontSize = 12.sp)
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("$prefix$$${String.format("%.2f", loan.amount)}", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(text, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                                ) {
                                    Text("DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NavyDark, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
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
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.width(32.dp).height(4.dp).background(Color(0xFFE5E7EB), RoundedCornerShape(2.dp)))
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "NEW LEDGER ENTRY",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isLent) Color.White else Color.Transparent)
                            .border(
                                if (isLent) BorderStroke(2.dp, Color(0xFF2563EB)) else BorderStroke(0.dp, Color.Transparent),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { isLent = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "I Lent Money", 
                            fontWeight = if (isLent) FontWeight.Bold else FontWeight.Medium, 
                            color = if (isLent) Color(0xFF2563EB) else TextLight,
                            fontSize = 14.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isLent) Color.White else Color.Transparent)
                            .border(
                                if (!isLent) BorderStroke(2.dp, Color(0xFF2563EB)) else BorderStroke(0.dp, Color.Transparent),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { isLent = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "I Borrowed Money", 
                            fontWeight = if (!isLent) FontWeight.Bold else FontWeight.Medium, 
                            color = if (!isLent) Color(0xFF2563EB) else TextLight,
                            fontSize = 14.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        if (hasPermission) {
                            contactPickerLauncher.launch(null)
                        } else {
                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import from Contacts", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AMOUNT ($)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            placeholder = { Text("$ 0.00") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = Color(0xFFF1F5F9),
                                focusedBorderColor = NavyDark
                            )
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("CONTACT NAME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            placeholder = { Text("e.g. Mike Smith") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedBorderColor = NavyDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PHONE NUMBER (OPTIONAL)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            placeholder = { Text("e.g. +1 555...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedBorderColor = NavyDark
                            )
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SOCIAL (OPTIONAL)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = socialDetails,
                            onValueChange = { socialDetails = it },
                            placeholder = { Text("e.g. email") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedBorderColor = NavyDark
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("FLASHBACK NOTE (REQUIRED)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight)
                    Text("to recall exactly what occurred", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Explain exactly what happened, why the money changed hands, or what this is for...") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedBorderColor = NavyDark
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Set a Deadline / Repayment Date?", fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 14.sp)
                            val dateText = if (deadlineDate != null) {
                                val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                                "Deadline: ${sdf.format(java.util.Date(deadlineDate!!))}"
                            } else {
                                "Alert alerts for upcoming due dates."
                            }
                            Text(dateText, color = TextLight, fontSize = 12.sp)
                        }
                        Checkbox(
                            checked = deadlineDate != null,
                            onCheckedChange = { 
                                if (it) showDatePicker = true 
                                else deadlineDate = null 
                            },
                            colors = CheckboxDefaults.colors(checkedColor = NavyDark, uncheckedColor = TextLight)
                        )
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
                            }) { Text("OK", color = NavyDark) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = TextLight) }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
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
                                    note = note
                                )
                            )
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDark)
                ) {
                    Text("RECORD ENTRY TO LEDGER", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                val typeColor = if (selectedLoan!!.type == LoanType.LENT) GreenAccent else OrangeAccent
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
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedLoan!!.type == LoanType.LENT) GreenAccent else NavyDark)
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
