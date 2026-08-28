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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedbackSettingsView(
    viewModel: PiggyLedgerViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val authUserName by viewModel.authUserName.collectAsState(initial = "")
    val authUserEmail by viewModel.authUserEmail.collectAsState(initial = "")

    val categories = remember {
        listOf(
            "Bug Report",
            "Feature Request",
            "UI / UX",
            "Sync & Data",
            "Billing / Pro",
            "Others"
        )
    }
    var selectedCategory by remember { mutableStateOf("Bug Report") }
    var feedbackMessage by remember { mutableStateOf("") }
    var selectedAttachments by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val attachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedAttachments = (selectedAttachments + uris).take(4)
        }
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
            // Responsive Category Option Pills
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val isCompact = screenWidth < 360
            val columns = if (isCompact) 2 else 3
            val chunkedCategories = categories.chunked(columns)

            // Sizing: Small by default, dynamically adjusting smaller on narrow screens so it never overflows or crashes
            val pillFontSize = when {
                screenWidth < 330 -> 10.5.sp
                screenWidth < 360 -> 11.sp
                screenWidth < 400 -> 11.5.sp
                else -> 12.sp // Small by default
            }

            val pillHorizontalPadding = when {
                screenWidth < 330 -> 8.dp
                screenWidth < 360 -> 10.dp
                screenWidth < 400 -> 11.dp
                else -> 12.dp // Small by default
            }

            val pillVerticalPadding = when {
                screenWidth < 360 -> 5.dp
                else -> 6.dp // Small by default
            }

            val pillSpacing = when {
                screenWidth < 360 -> 6.dp
                else -> 8.dp
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(pillSpacing)
            ) {
                chunkedCategories.forEach { rowCategories ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(pillSpacing)
                    ) {
                        rowCategories.forEach { category ->
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
                                    .padding(
                                        horizontal = pillHorizontalPadding,
                                        vertical = pillVerticalPadding
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = category,
                                    fontSize = pillFontSize,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else NavyDark,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
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

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(selectedAttachments) { uri ->
                    val isVideo = context.contentResolver.getType(uri)?.contains("video") == true
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                    ) {
                        // Thumbnail
                        AsyncImage(
                            model = uri,
                            contentDescription = "Attachment",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF2E2E2E)),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Video icon
                        if (isVideo) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Video",
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 6.dp)
                                    .size(24.dp)
                            )
                        }

                        // Close button at top right
                        IconButton(
                            onClick = { selectedAttachments = selectedAttachments - uri },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 8.dp, y = (-8).dp)
                                .size(26.dp)
                                .background(Color(0xAA000000), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }

                if (selectedAttachments.size < 4) {
                    item {
                        // Square add attachment button
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF2E2E2E))
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
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(32.dp)
                            )
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
                        val action = if (selectedAttachments.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND
                        val emailIntent = Intent(action).apply {
                            // Using message/rfc822 to filter for email apps
                            type = "message/rfc822"
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("contact@piggyapp.top"))
                            putExtra(Intent.EXTRA_SUBJECT, subject)
                            putExtra(Intent.EXTRA_TEXT, body)
                            
                            // Use a selector to force email clients (prevents WhatsApp, etc. from showing up)
                            selector = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
                            
                            if (selectedAttachments.isNotEmpty()) {
                                if (selectedAttachments.size == 1) {
                                    putExtra(Intent.EXTRA_STREAM, selectedAttachments.first())
                                } else {
                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(selectedAttachments))
                                }
                                
                                // Proper ClipData for granting URI permissions to all attachments
                                val clip = android.content.ClipData.newRawUri("Attachment", selectedAttachments.first())
                                for (i in 1 until selectedAttachments.size) {
                                    clip.addItem(android.content.ClipData.Item(selectedAttachments[i]))
                                }
                                clipData = clip
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        }

                        try {
                            context.startActivity(Intent.createChooser(emailIntent, "Send Feedback").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } catch (e: Exception) {
                            // Fallback if selector causes ActivityNotFoundException on weird custom OSes
                            emailIntent.selector = null
                            val gmailPackage = "com.google.android.gm"
                            val pm = context.packageManager
                            if (pm.getLaunchIntentForPackage(gmailPackage) != null) {
                                emailIntent.setPackage(gmailPackage)
                                context.startActivity(emailIntent)
                            } else {
                                context.startActivity(Intent.createChooser(emailIntent, "Send Feedback").apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            }
                        }
                    } catch (e: Throwable) {
                        android.util.Log.e("Feedback", "Failed sending feedback", e)
                        ToastUtil.show(context, "Could not open email app", Toast.LENGTH_SHORT)
                    }

                    feedbackMessage = ""
                    selectedAttachments = emptyList()
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
