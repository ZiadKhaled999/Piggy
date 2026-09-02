package com.oryno.piggy_ledger.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Send
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val authUserName by viewModel.authUserName.collectAsStateWithLifecycle()
    val authUserEmail by viewModel.authUserEmail.collectAsStateWithLifecycle()

    val categoryList = remember {
        listOf(
            Pair(R.string.feedback_cat_bug_report, "Bug Report"),
            Pair(R.string.feedback_cat_feature_request, "Feature Request"),
            Pair(R.string.feedback_cat_ui_ux, "UI / UX"),
            Pair(R.string.feedback_cat_sync_data, "Sync & Data"),
            Pair(R.string.feedback_cat_billing_pro, "Billing / Pro"),
            Pair(R.string.feedback_cat_others, "Others")
        )
    }
    var selectedCategoryTechnical by remember { mutableStateOf("Bug Report") }
    var feedbackMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .imePadding()
    ) {
        // Top Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
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
                text = stringResource(R.string.tell_us_problem),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )
        }

        // Main Scrollable Body with tight, efficient spacing
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Category Label
            Text(
                text = stringResource(R.string.feedback_category_label),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = NavyDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Responsive Category Option Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categoryList.forEach { (resId, techName) ->
                    val isSelected = selectedCategoryTechnical == techName
                    val localizedLabel = stringResource(resId)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) PinkPrimary else Color(0xFFF1F5F9))
                            .then(
                                if (!isSelected) Modifier.border(1.dp, Color(0xFFE2E8F0), CircleShape)
                                else Modifier
                            )
                            .clickable { selectedCategoryTechnical = techName }
                            .padding(
                                horizontal = 14.dp,
                                vertical = 7.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = localizedLabel,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) Color.White else NavyDark,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Details Title Header with asterisk
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.feedback_details_label),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NavyDark
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "*",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PinkPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Multi-line Details Input Box without wasted space
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF1F5F9))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    OutlinedTextField(
                        value = feedbackMessage,
                        onValueChange = { if (it.length <= 5000) feedbackMessage = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.feedback_details_placeholder),
                                color = TextLight,
                                fontSize = 13.5.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 13.5.sp,
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

                    Text(
                        text = "${feedbackMessage.length}/5000",
                        fontSize = 11.5.sp,
                        color = TextLight,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }



            Spacer(modifier = Modifier.height(12.dp))
        }

        // Bottom Fixed Submit Button
        Surface(
            color = Color(0xFFFAFAFA),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Button(
                onClick = {
                    if (feedbackMessage.isBlank()) {
                        ToastUtil.show(context, context.getString(R.string.feedback_error_empty), Toast.LENGTH_SHORT)
                        return@Button
                    }

                    try {
                        com.posthog.PostHog.capture(
                            "user_feedback_submitted",
                            properties = mapOf(
                                "category" to selectedCategoryTechnical,
                                "message_length" to feedbackMessage.length
                            )
                        )
                    } catch (e: Throwable) {
                        android.util.Log.e("Feedback", "Failed analytics capture", e)
                    }

                    val senderName = authUserName.ifBlank { "User" }
                    val senderEmail = authUserEmail.ifBlank { "Not provided" }
                    val subject = "[$selectedCategoryTechnical] Piggy Ledger Feedback"
                    val body = "Name: $senderName\nEmail: $senderEmail\nCategory: $selectedCategoryTechnical\n\nDetails:\n$feedbackMessage"

                    val recipientEmail = "contact@piggyapp.top"
                    val mailtoUri = Uri.parse("mailto:$recipientEmail?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")

                    // Target Gmail app directly with fallback
                    val gmailIntent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
                        setPackage("com.google.android.gm")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, body)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    try {
                        context.startActivity(gmailIntent)
                    } catch (e: Exception) {
                        // Fallback to any email client supporting mailto
                        val fallbackIntent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                            putExtra(Intent.EXTRA_SUBJECT, subject)
                            putExtra(Intent.EXTRA_TEXT, body)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(fallbackIntent)
                        } catch (e2: Exception) {
                            try {
                                val chooserIntent = Intent.createChooser(fallbackIntent, context.getString(R.string.feedback_send_chooser_title)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(chooserIntent)
                            } catch (e3: Exception) {
                                ToastUtil.show(context, context.getString(R.string.feedback_cannot_open_email), Toast.LENGTH_SHORT)
                            }
                        }
                    }

                    ToastUtil.show(context, context.getString(R.string.feedback_redirecting_email), Toast.LENGTH_SHORT)
                    feedbackMessage = ""
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PinkPrimary,
                    contentColor = Color.White
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.feedback_submit_btn),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
