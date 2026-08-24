package com.oryno.piggy_ledger.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackSettingsView(
    viewModel: PiggyLedgerViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val authUserName by viewModel.authUserName.collectAsState(initial = "")
    val authUserEmail by viewModel.authUserEmail.collectAsState(initial = "")

    val categoryRows = remember {
        listOf(
            listOf("Bug Report", "Feature Request", "UI / UX"),
            listOf("Sync & Data", "Billing / Pro", "Others")
        )
    }
    var selectedCategory by remember { mutableStateOf("Bug Report") }
    var feedbackMessage by remember { mutableStateOf("") }
    var selectedAttachmentUri by remember { mutableStateOf<Uri?>(null) }

    val attachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedAttachmentUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        // Top Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_icon),
                    tint = NavyDark
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Tell us the problem",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )
        }

        // Main Scrollable Body
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Category Chips (Row by Row)
            categoryRows.forEachIndexed { rowIndex, rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) PinkPrimary else Color(0xFFF1F5F9))
                                .then(
                                    if (!isSelected) Modifier.border(1.dp, Color(0xFFE2E8F0), CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else NavyDark
                            )
                        }
                    }
                }
                if (rowIndex < categoryRows.size - 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Details Title Header with asterisk
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Details",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "*",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = PinkPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Large Multi-line Details Input Box (Fixed dimensions, safe from weight/scroll crashes)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF1F5F9))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    OutlinedTextField(
                        value = feedbackMessage,
                        onValueChange = { if (it.length <= 5000) feedbackMessage = it },
                        placeholder = {
                            Text(
                                text = "Explain what happened, or suggestions.",
                                color = TextLight,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(125.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 14.sp,
                            color = NavyDark
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = PinkPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${feedbackMessage.length}/5000",
                        fontSize = 12.sp,
                        color = TextLight,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Screenshots or videos (Optional)
            Text(
                text = "Screenshots or videos (Optional)",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cozy square add attachment button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                        .clickable {
                            try {
                                attachmentLauncher.launch("*/*")
                            } catch (e: Throwable) {
                                ToastUtil.show(context, "Cannot open file picker", Toast.LENGTH_SHORT)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Attach media",
                        tint = TextLight,
                        modifier = Modifier.size(32.dp)
                    )
                }

                if (selectedAttachmentUri != null) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(76.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Attached File",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NavyDark
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = selectedAttachmentUri?.lastPathSegment ?: "File selected",
                                    fontSize = 12.sp,
                                    color = TextLight,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { selectedAttachmentUri = null }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove attachment",
                                    tint = PinkPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Bottom Fixed Submit Button
        Surface(
            color = Color(0xFFFAFAFA),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Button(
                onClick = {
                    if (feedbackMessage.isBlank()) {
                        ToastUtil.show(context, "Please enter details of the problem", Toast.LENGTH_SHORT)
                        return@Button
                    }

                    try {
                        com.posthog.PostHog.capture(
                            "user_feedback_submitted",
                            properties = mapOf(
                                "category" to selectedCategory,
                                "message_length" to feedbackMessage.length
                            )
                        )
                    } catch (e: Throwable) {
                        android.util.Log.e("Feedback", "Failed analytics capture", e)
                    }

                    ToastUtil.show(context, "Thank you! Your feedback has been submitted.", Toast.LENGTH_LONG)

                    val subject = "[$selectedCategory] Piggy Ledger Feedback"
                    val body = "Category: $selectedCategory\nName: ${authUserName.ifBlank { "User" }}\nEmail: ${authUserEmail.ifBlank { "user@example.com" }}\n\nDetails:\n$feedbackMessage"

                    try {
                        val mailtoUrl = "mailto:contact@piggyapp.top" +
                                "?subject=" + Uri.encode(subject) +
                                "&body=" + Uri.encode(body)
                        val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse(mailtoUrl)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        val chooser = Intent.createChooser(mailIntent, "Send Email...").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(chooser)
                    } catch (e: Throwable) {
                        try {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("contact@piggyapp.top"))
                                putExtra(Intent.EXTRA_SUBJECT, subject)
                                putExtra(Intent.EXTRA_TEXT, body)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            val chooser = Intent.createChooser(sendIntent, "Send Email...").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(chooser)
                        } catch (ex: Throwable) {
                            android.util.Log.e("Feedback", "No email app found", ex)
                        }
                    }

                    feedbackMessage = ""
                    selectedAttachmentUri = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PinkPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Submit",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
