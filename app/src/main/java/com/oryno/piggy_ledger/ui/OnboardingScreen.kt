package com.oryno.piggy_ledger.ui
import androidx.compose.ui.res.stringResource
import com.oryno.piggy_ledger.R

import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.os.Build
import android.Manifest
import android.widget.Toast

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import com.oryno.piggy_ledger.ui.theme.SlateDark
import com.oryno.piggy_ledger.ui.theme.AccentBlue
import com.clerk.api.Clerk

val TealGreen = Color(0xFF43B7A7)

data class OnboardingPageData(
    val imageRes: Int,
    val title: AnnotatedString,
    val subtitle: String
)

@Composable
fun OnboardingScreen(onComplete: (Int, Int, String) -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360
    
    val titleFontSize = if (isSmallScreen) 24.sp else 32.sp
    val subtitleFontSize = if (isSmallScreen) 13.sp else 16.sp
    val cardTitleFontSize = if (isSmallScreen) 14.sp else 16.sp
    val cardDescFontSize = if (isSmallScreen) 11.sp else 12.sp
    val emojiBoxSize = if (isSmallScreen) 42.dp else 50.dp
    val emojiFontSize = if (isSmallScreen) 20.sp else 24.sp
    val buttonFontSize = if (isSmallScreen) 16.sp else 18.sp
    val backButtonFontSize = if (isSmallScreen) 14.sp else 16.sp
    val horizontalPadding = if (isSmallScreen) 16.dp else 24.dp
    val sectionSpacing = if (isSmallScreen) 20.dp else 28.dp

    var currentPage by remember { mutableIntStateOf(0) }
    var selectedIntent by remember { mutableIntStateOf(-1) }
    var selectedIntensity by remember { mutableIntStateOf(-1) }

    var relatesToLoans by remember { mutableStateOf<Boolean?>(null) }
    var relatesToAccounts by remember { mutableStateOf<Boolean?>(null) }
    var relatesToEmergency by remember { mutableStateOf<Boolean?>(null) }

    var selectedSavingMode by remember { mutableStateOf("piggy") }
    
    // Register SMS permission launcher
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsReceivedGranted = permissions[Manifest.permission.RECEIVE_SMS] ?: false
        val smsReadGranted = permissions[Manifest.permission.READ_SMS] ?: false
        
        if (smsReceivedGranted || smsReadGranted) {
            Toast.makeText(context, context.getString(R.string.onboarding_sms_granted), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, context.getString(R.string.onboarding_sms_denied), Toast.LENGTH_SHORT).show()
        }
        currentPage++
    }

    // Register Notification permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Toast.makeText(context, context.getString(R.string.onboarding_notif_granted), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, context.getString(R.string.onboarding_notif_denied), Toast.LENGTH_SHORT).show()
        }
        currentPage++
    }

    val welcomeTo = stringResource(R.string.onboarding_welcome_to)
    val appName = stringResource(R.string.app_name)
    val subtitle1 = stringResource(R.string.onboarding_subtitle_1)
    
    val poolYour = stringResource(R.string.onboarding_pool_your)
    val savings = stringResource(R.string.onboarding_savings)
    val subtitle2 = stringResource(R.string.onboarding_subtitle_2)
    
    val trackStr = stringResource(R.string.onboarding_track)
    val progress = stringResource(R.string.onboarding_progress)
    val together = stringResource(R.string.onboarding_together)
    val subtitle3 = stringResource(R.string.onboarding_subtitle_3)
    
    val readyTo = stringResource(R.string.onboarding_ready_to)
    val startStr = stringResource(R.string.onboarding_start)
    val subtitle4 = stringResource(R.string.onboarding_subtitle_4)

    fun requestSmsPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECEIVE_SMS)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_SMS)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            smsPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Toast.makeText(context, context.getString(R.string.onboarding_sms_granted), Toast.LENGTH_SHORT).show()
            currentPage++
        }
    }

    fun requestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Toast.makeText(context, context.getString(R.string.onboarding_notif_granted), Toast.LENGTH_SHORT).show()
                currentPage++
            }
        } else {
            Toast.makeText(context, context.getString(R.string.onboarding_notif_granted), Toast.LENGTH_SHORT).show()
            currentPage++
        }
    }

    LaunchedEffect(currentPage) {
        if (currentPage == 3) {
            val hasSms = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
            if (!hasSms) {
                requestSmsPermissions()
            }
        } else if (currentPage == 4) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasNotif = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                if (!hasNotif) {
                    requestNotificationPermissions()
                }
            }
        }
    }
    
    val pages = listOf(
        OnboardingPageData(
            imageRes = R.drawable.img_piggy_hello,
            title = buildAnnotatedString {
                append(welcomeTo + "\n")
                withStyle(style = SpanStyle(color = PinkPrimary)) {
                    append(appName)
                }
            },
            subtitle = subtitle1
        ),
        OnboardingPageData(
            imageRes = R.drawable.img_piggy_pool,
            title = buildAnnotatedString {
                append(poolYour + " ")
                withStyle(style = SpanStyle(color = PinkPrimary)) {
                    append(savings)
                }
            },
            subtitle = subtitle2
        ),
        OnboardingPageData(
            imageRes = R.drawable.img_piggy_track,
            title = buildAnnotatedString {
                append(trackStr + " ")
                withStyle(style = SpanStyle(color = PinkPrimary)) {
                    append(progress)
                }
                append("\n" + together)
            },
            subtitle = subtitle3
        ),
        OnboardingPageData(
            imageRes = R.drawable.wallet_illustration_1783782766357,
            title = buildAnnotatedString {
                append(stringResource(R.string.onboarding_sms_title))
            },
            subtitle = stringResource(R.string.onboarding_sms_subtitle)
        ),
        OnboardingPageData(
            imageRes = R.drawable.img_settings_feedback,
            title = buildAnnotatedString {
                append(stringResource(R.string.onboarding_notif_title))
            },
            subtitle = stringResource(R.string.onboarding_notif_subtitle)
        ),
        OnboardingPageData(
            imageRes = R.drawable.img_piggy_hello,
            title = buildAnnotatedString { append(stringResource(R.string.onboarding_personalize_intent_title)) },
            subtitle = stringResource(R.string.onboarding_personalize_intent_subtitle)
        ),

        OnboardingPageData(
            imageRes = R.drawable.img_piggy_hello,
            title = buildAnnotatedString { append(stringResource(R.string.onboarding_personalize_intensity_title)) },
            subtitle = stringResource(R.string.onboarding_personalize_intensity_subtitle)
        ),
        OnboardingPageData(
            imageRes = R.drawable.img_piggy_hello,
            title = buildAnnotatedString { append("") },
            subtitle = ""
        ),
        OnboardingPageData(
            imageRes = R.drawable.img_piggy_hello,
            title = buildAnnotatedString { append("") },
            subtitle = ""
        ),
        OnboardingPageData(
            imageRes = R.drawable.img_piggy_hello,
            title = buildAnnotatedString { append("") },
            subtitle = ""
        ),

        OnboardingPageData(
            imageRes = R.drawable.img_piggy_hello,
            title = buildAnnotatedString { append(stringResource(R.string.onboarding_streak_title)) },
            subtitle = stringResource(R.string.onboarding_streak_subtitle)
        ),
        OnboardingPageData(
            imageRes = R.drawable.img_piggy_hello,
            title = buildAnnotatedString { append(stringResource(R.string.onboarding_personalize_roadmap_title)) },
            subtitle = stringResource(R.string.onboarding_personalize_roadmap_subtitle)
        ),
        OnboardingPageData(
            imageRes = R.drawable.img_piggy_hello,
            title = buildAnnotatedString { append(stringResource(R.string.onboarding_choose_pace_title)) },
            subtitle = stringResource(R.string.onboarding_choose_pace_subtitle)
        ),
        OnboardingPageData(
            imageRes = R.drawable.img_app_logo,
            title = buildAnnotatedString {
                append(readyTo + " ")
                withStyle(style = SpanStyle(color = PinkPrimary)) {
                    append(startStr)
                }
                append("?")
            },
            subtitle = subtitle4
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = if (currentPage in 7..9) 0.dp else horizontalPadding, vertical = if (currentPage in 7..9) 6.dp else (if (isSmallScreen) 12.dp else 16.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper Content with Crossfade Page Transition
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = currentPage,
                label = "onboarding_page_fade"
            ) { pageIndex ->
                if (pageIndex == 3) {
                    // SMS / MESSAGES PERMISSION SLIDE - Phone Mockup matching reference design
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 12.dp))

                        // Phone Frame with Messages Interface
                        Box(
                            modifier = Modifier
                                .width(if (isSmallScreen) 250.dp else 290.dp)
                                .height(if (isSmallScreen) 270.dp else 310.dp)
                                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                                .background(Color.White)
                                .border(
                                    width = 1.5.dp,
                                    color = Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                                ),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                // Top Navigation Chevron
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = null,
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier
                                        .size(26.dp)
                                        .padding(top = 2.dp)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // App Title ("Messages" / "الرسائل")
                                Text(
                                    text = stringResource(R.string.onboarding_messages_header),
                                    fontSize = if (isSmallScreen) 22.sp else 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                                // Row 1: Detailed Bank SMS item
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF94A3B8)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(R.string.onboarding_sms_mock_sender),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NavyDark
                                            )
                                            Text(
                                                text = "9:41 AM",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF64748B)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = stringResource(R.string.onboarding_sms_mock_body),
                                            fontSize = 11.sp,
                                            color = TextLight,
                                            lineHeight = 15.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                                // Row 2: Skeleton Placeholder 1
                                SmsSkeletonRow()

                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                                // Row 3: Skeleton Placeholder 2
                                SmsSkeletonRow()

                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                                // Row 4: Skeleton Placeholder 3
                                SmsSkeletonRow()
                            }
                        }

                        Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 24.dp))

                        // Main Title
                        Text(
                            text = stringResource(R.string.onboarding_sms_title),
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyDark,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isSmallScreen) 28.sp else 34.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Subtitle
                        Text(
                            text = stringResource(R.string.onboarding_sms_subtitle),
                            fontSize = subtitleFontSize,
                            color = TextLight,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isSmallScreen) 20.sp else 24.sp,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 24.dp))

                        // Enable SMS Button
                        Button(
                            onClick = { requestSmsPermissions() },
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(52.dp)
                                .testTag("grant_sms_permission_button"),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_sms_btn),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Skip Button
                        TextButton(
                            onClick = {
                                Toast.makeText(context, context.getString(R.string.onboarding_sms_denied), Toast.LENGTH_SHORT).show()
                                currentPage++
                            },
                            modifier = Modifier.testTag("skip_sms_permission_button")
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_sms_skip),
                                color = TextLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (pageIndex == 4) {
                    // NOTIFICATION PERMISSION SLIDE - Larger realistic phone mockup + overlapping card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 12.dp))

                        // Outer Container that holds Phone Shell + Overlapping Notification Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isSmallScreen) 260.dp else 300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // 1. Phone Frame Shell (Sleek Phone Silhouette)
                            Box(
                                modifier = Modifier
                                    .width(if (isSmallScreen) 230.dp else 270.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFFF8FAFC),
                                                Color(0xFFF1F5F9).copy(alpha = 0.6f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                                    ),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Phone speaker notch
                                    Box(
                                        modifier = Modifier
                                            .width(40.dp)
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFCBD5E1).copy(alpha = 0.6f))
                                    )

                                    Spacer(modifier = Modifier.height(if (isSmallScreen) 14.dp else 20.dp))

                                    // Giant Lockscreen Clock Display ("9:41")
                                    Text(
                                        text = "9:41",
                                        fontSize = if (isSmallScreen) 62.sp else 74.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        letterSpacing = (-2).sp
                                    )
                                }
                            }

                            // 2. Floating Notification Card (Overlaps phone shell and extends wider horizontally!)
                            Card(
                                modifier = Modifier
                                    .width(if (isSmallScreen) 280.dp else 330.dp)
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = if (isSmallScreen) 16.dp else 24.dp),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(if (isSmallScreen) 12.dp else 16.dp)
                                ) {
                                    // Header Row: App Logo (No Pink Background!), App Name, Time
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.img_app_logo),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.Crop
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = stringResource(R.string.piggy_ledger_brand).uppercase(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B),
                                            letterSpacing = 0.5.sp
                                        )

                                        Spacer(modifier = Modifier.weight(1f))

                                        Text(
                                            text = "9:41 AM",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Notification Title (No Emojis)
                                    Text(
                                        text = stringResource(R.string.onboarding_notif_card_title),
                                        fontSize = if (isSmallScreen) 14.sp else 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDark
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Notification Body
                                    Text(
                                        text = stringResource(R.string.onboarding_notif_card_body),
                                        fontSize = if (isSmallScreen) 11.sp else 12.sp,
                                        color = TextLight,
                                        lineHeight = if (isSmallScreen) 15.sp else 17.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 24.dp))

                        // Main Title (No Emojis)
                        Text(
                            text = stringResource(R.string.onboarding_notif_title),
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyDark,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isSmallScreen) 28.sp else 34.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Subtitle
                        Text(
                            text = stringResource(R.string.onboarding_notif_subtitle),
                            fontSize = subtitleFontSize,
                            color = TextLight,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isSmallScreen) 20.sp else 24.sp,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 24.dp))

                        // Enable Notifications Button
                        Button(
                            onClick = { requestNotificationPermissions() },
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(52.dp)
                                .testTag("grant_notif_permission_button"),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_notif_btn),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Skip Button
                        TextButton(
                            onClick = {
                                Toast.makeText(context, context.getString(R.string.onboarding_notif_denied), Toast.LENGTH_SHORT).show()
                                currentPage++
                            },
                            modifier = Modifier.testTag("skip_notif_permission_button")
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_notif_skip),
                                color = TextLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (pageIndex == 5) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_personalize_intent_title),
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyDark,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isSmallScreen) 32.sp else 40.sp
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = stringResource(R.string.onboarding_personalize_intent_subtitle),
                            fontSize = subtitleFontSize,
                            color = TextLight,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isSmallScreen) 20.sp else 24.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(sectionSpacing))
                        
                        val intents = listOf(
                            Pair(stringResource(R.string.onboarding_personalize_intent_personal), stringResource(R.string.onboarding_personalize_intent_personal_desc)) to "💰",
                            Pair(stringResource(R.string.onboarding_personalize_intent_loans), stringResource(R.string.onboarding_personalize_intent_loans_desc)) to "🤝",
                            Pair(stringResource(R.string.onboarding_personalize_intent_auto), stringResource(R.string.onboarding_personalize_intent_auto_desc)) to "⚡"
                        )
                        
                        intents.forEachIndexed { index, pair ->
                            val (textPair, emoji) = pair
                            val (title, desc) = textPair
                            val isSelected = selectedIntent == index
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(if (isSmallScreen) 1f else 0.96f)
                                    .padding(vertical = if (isSmallScreen) 6.dp else 8.dp)
                                    .clickable { selectedIntent = index }
                                    .testTag("intent_card_$index"),
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) Color(0xFFFFF1F2) else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 3.dp else 1.5.dp,
                                    color = if (isSelected) PinkPrimary else Color(0xFFCBD5E1)
                                ),
                                shadowElevation = if (isSelected) 3.dp else 0.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = if (isSmallScreen) 12.dp else 16.dp, vertical = if (isSmallScreen) 10.dp else 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(emojiBoxSize)
                                            .clip(CircleShape)
                                            .background(if (isSelected) PinkPrimary.copy(alpha = 0.15f) else Color(0xFFF1F5F9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = emoji,
                                            fontSize = emojiFontSize
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(if (isSmallScreen) 10.dp else 14.dp))
                                    
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = title,
                                            fontSize = cardTitleFontSize,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) PinkPrimary else NavyDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = desc,
                                            fontSize = cardDescFontSize,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSelected) SlateDark else TextLight,
                                            lineHeight = if (isSmallScreen) 14.sp else 16.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedIntent = index },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = PinkPrimary,
                                            unselectedColor = Color(0xFFCBD5E1)
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else if (pageIndex == 6) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_personalize_intensity_title),
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyDark,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isSmallScreen) 32.sp else 40.sp
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = stringResource(R.string.onboarding_personalize_intensity_subtitle),
                            fontSize = subtitleFontSize,
                            color = TextLight,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isSmallScreen) 20.sp else 24.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(sectionSpacing))
                        
                        val intensities = listOf(
                            Pair(stringResource(R.string.onboarding_personalize_intensity_casual), stringResource(R.string.onboarding_personalize_intensity_casual_desc)) to "🌱",
                            Pair(stringResource(R.string.onboarding_personalize_intensity_balanced), stringResource(R.string.onboarding_personalize_intensity_balanced_desc)) to "⚡",
                            Pair(stringResource(R.string.onboarding_personalize_intensity_aggressive), stringResource(R.string.onboarding_personalize_intensity_aggressive_desc)) to "🔥"
                        )
                        
                        intensities.forEachIndexed { index, pair ->
                            val (textPair, emoji) = pair
                            val (title, desc) = textPair
                            val isSelected = selectedIntensity == index
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(if (isSmallScreen) 1f else 0.96f)
                                    .padding(vertical = if (isSmallScreen) 6.dp else 8.dp)
                                    .clickable { selectedIntensity = index }
                                    .testTag("intensity_card_$index"),
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) Color(0xFFFFF1F2) else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 3.dp else 1.5.dp,
                                    color = if (isSelected) PinkPrimary else Color(0xFFCBD5E1)
                                ),
                                shadowElevation = if (isSelected) 3.dp else 0.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(if (isSmallScreen) 12.dp else 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(emojiBoxSize)
                                                .clip(CircleShape)
                                                .background(if (isSelected) PinkPrimary.copy(alpha = 0.15f) else Color(0xFFF1F5F9)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = emoji,
                                                fontSize = emojiFontSize
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.width(if (isSmallScreen) 10.dp else 14.dp))
                                        
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = title,
                                                fontSize = cardTitleFontSize,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) PinkPrimary else NavyDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            val rateLabel = if (index == 0) "5% – 10%" else if (index == 1) "15% – 20%" else "30%+"
                                            Text(
                                                text = stringResource(R.string.onboarding_saving_rate_label, rateLabel),
                                                fontSize = if (isSmallScreen) 10.sp else 11.sp,
                                                color = if (isSelected) PinkPrimary else TextLight,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedIntensity = index },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = PinkPrimary,
                                                unselectedColor = Color(0xFFCBD5E1)
                                            )
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = desc,
                                        fontSize = cardDescFontSize,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) SlateDark else TextLight,
                                        lineHeight = if (isSmallScreen) 14.sp else 16.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Custom visual speed progress bar inside the card
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFFE2E8F0))
                                    ) {
                                        val fillRatio = if (index == 0) 0.12f else if (index == 1) 0.25f else 0.50f
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fillRatio)
                                                .fillMaxHeight()
                                                .background(if (isSelected) PinkPrimary else Color(0xFF94A3B8))
                                        )
                                    }
                                }
                            }
                        }
                    }
                
                } else if (pageIndex in 7..9) {
                    val cardBgColor = when (pageIndex) {
                        7 -> Color(0xFFB38952) // Warm Amber / Burnt Ochre
                        8 -> Color(0xFF5B78A7) // Slate Indigo / Periwinkle
                        else -> Color(0xFF386851) // Deep Sage / Forest Green
                    }
                    val relatableText = when (pageIndex) {
                        7 -> stringResource(R.string.onboarding_relatable_statement_1)
                        8 -> stringResource(R.string.onboarding_relatable_statement_2)
                        else -> stringResource(R.string.onboarding_relatable_statement_3)
                    }
                    val imageRes = when (pageIndex) {
                        7 -> R.drawable.img_relatable_debt_1785176852844
                        8 -> R.drawable.img_relatable_accounts_1785176864908
                        else -> R.drawable.img_relatable_emergency_1785176876312
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Bar: Navigation & Progress Step Dashes
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9))
                                    .clickable { currentPage-- },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = stringResource(R.string.back_btn),
                                    tint = NavyDark,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val currentStep = pageIndex - 6
                                val totalSteps = 5
                                for (step in 1..totalSteps) {
                                    val isFilled = step <= currentStep + 1
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (isFilled) Color(0xFFE5A641) else Color(0xFFE2E8F0))
                                    )
                                }
                            }
                        }

                        // Middle Content: Header Title + Card (Aligned to Top)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // Header Title
                            Text(
                                text = stringResource(R.string.onboarding_relatable_header),
                                color = NavyDark,
                                fontSize = if (isSmallScreen) 20.sp else 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                lineHeight = if (isSmallScreen) 26.sp else 30.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Full-Width Card with Compact Height
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(cardBgColor)
                                    .padding(if (isSmallScreen) 14.dp else 18.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "“",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = if (isSmallScreen) 40.sp else 48.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        lineHeight = 24.sp,
                                        modifier = Modifier
                                            .align(Alignment.Start)
                                            .offset(y = (-2).dp)
                                    )

                                    Text(
                                        text = relatableText,
                                        color = Color.White,
                                        fontSize = if (isSmallScreen) 14.sp else 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        lineHeight = if (isSmallScreen) 20.sp else 24.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Custom Generated Illustration with controlled height
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (isSmallScreen) 120.dp else 150.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.White.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = imageRes),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Absolute Bottom: Choice Buttons ("No" and "Yes") Full-Width
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp, top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    when (pageIndex) {
                                        7 -> relatesToLoans = false
                                        8 -> relatesToAccounts = false
                                        9 -> relatesToEmergency = false
                                    }
                                    if (currentPage < pages.size - 1) currentPage++
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                shape = RoundedCornerShape(27.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = cardBgColor,
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.no_label),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Button(
                                onClick = {
                                    when (pageIndex) {
                                        7 -> {
                                            relatesToLoans = true
                                            selectedIntent = 1
                                        }
                                        8 -> relatesToAccounts = true
                                        9 -> relatesToEmergency = true
                                    }
                                    if (currentPage < pages.size - 1) currentPage++
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                shape = RoundedCornerShape(27.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = cardBgColor,
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.yes_label),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
} else if (pageIndex == 10) {
                    // STREAK / HABIT SLIDE
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(if (isSmallScreen) 10.dp else 20.dp))

                        Text(
                            text = stringResource(R.string.onboarding_streak_title),
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyDark,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isSmallScreen) 32.sp else 40.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(R.string.onboarding_streak_subtitle),
                            fontSize = subtitleFontSize,
                            color = TextLight,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isSmallScreen) 20.sp else 24.sp,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(if (isSmallScreen) 36.dp else 52.dp))

                        // HABIT Streak Visual Container
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(if (isSmallScreen) 0.95f else 0.90f)
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val habitLetters = stringResource(R.string.onboarding_habit_letters).split(",")
                            val days = stringResource(R.string.onboarding_streak_days).split(",")
                            val completedSteps = if (habitLetters.size <= 4) 3 else 3

                            val letterFontSize = if (isSmallScreen) {
                                if (habitLetters.size <= 4) 42.sp else 36.sp
                            } else {
                                if (habitLetters.size <= 4) 50.sp else 44.sp
                            }

                            val circleSize = if (isSmallScreen) {
                                if (habitLetters.size <= 4) 52.dp else 46.dp
                            } else {
                                if (habitLetters.size <= 4) 62.dp else 54.dp
                            }

                            val checkIconSize = if (isSmallScreen) {
                                if (habitLetters.size <= 4) 28.dp else 24.dp
                            } else {
                                if (habitLetters.size <= 4) 34.dp else 28.dp
                            }

                            // Letters Row (H A B I T or ع ا د ة)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                habitLetters.forEachIndexed { index, letter ->
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (index < completedSteps) {
                                            Text(
                                                text = letter,
                                                fontSize = letterFontSize,
                                                fontWeight = FontWeight.Black,
                                                color = NavyDark
                                            )
                                        } else {
                                            Text(
                                                text = letter,
                                                fontSize = letterFontSize,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFCBD5E1).copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 24.dp))

                            // Circles Row (Checkmark icons for active days, empty circles for upcoming days)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                days.forEachIndexed { index, day ->
                                    val isCompleted = index < completedSteps
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        if (isCompleted) {
                                            Box(
                                                modifier = Modifier
                                                    .size(circleSize)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                Color(0xFF34D399),
                                                                Color(0xFF059669)
                                                            )
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(checkIconSize)
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(circleSize)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFF8FAFC))
                                                    .border(
                                                        width = 3.dp,
                                                        color = Color(0xFFCBD5E1),
                                                        shape = CircleShape
                                                    )
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = day,
                                            fontSize = if (isSmallScreen) 11.sp else 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCompleted) NavyDark else Color(0xFF94A3B8),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(if (isSmallScreen) 20.dp else 30.dp))
                    }
                } else if (pageIndex == 11) {
                    val focusName = when (selectedIntent) {
                        0 -> stringResource(R.string.onboarding_personalize_intent_personal)
                        1 -> stringResource(R.string.onboarding_personalize_intent_loans)
                        else -> stringResource(R.string.onboarding_personalize_intent_auto)
                    }

                    val focusDesc = when (selectedIntent) {
                        0 -> stringResource(R.string.onboarding_step_workspace_desc_personal)
                        1 -> stringResource(R.string.onboarding_step_workspace_desc_loans)
                        else -> stringResource(R.string.onboarding_step_workspace_desc_auto)
                    }

                    val intensityName = when (selectedIntensity) {
                        0 -> stringResource(R.string.onboarding_personalize_intensity_casual)
                        1 -> stringResource(R.string.onboarding_personalize_intensity_balanced)
                        else -> stringResource(R.string.onboarding_personalize_intensity_aggressive)
                    }

                    val intensityValue = when (selectedIntensity) {
                        0 -> "5% – 10%"
                        1 -> "15% – 20%"
                        else -> "30%+"
                    }

                    val intensityDesc = when (selectedIntensity) {
                        0 -> stringResource(R.string.onboarding_step_intensity_desc_casual)
                        1 -> stringResource(R.string.onboarding_step_intensity_desc_balanced)
                        else -> stringResource(R.string.onboarding_step_intensity_desc_aggressive)
                    }

                    val isSmsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                    
                    val syncTitle = stringResource(R.string.onboarding_step_sync_title)
                    val syncDesc = if (isSmsGranted) {
                        stringResource(R.string.onboarding_step_sync_desc_granted)
                    } else {
                        stringResource(R.string.onboarding_step_sync_desc_manual)
                    }

                    val milestoneTitle = stringResource(R.string.onboarding_step_milestone_title)
                    val milestoneDesc = when (selectedIntent) {
                        0 -> stringResource(R.string.onboarding_step_milestone_desc_personal)
                        1 -> stringResource(R.string.onboarding_step_milestone_desc_loans)
                        else -> stringResource(R.string.onboarding_step_milestone_desc_auto)
                    }

                    val step1Title = stringResource(R.string.onboarding_step_workspace_title, focusName)
                    val step2Title = stringResource(R.string.onboarding_step_intensity_title, intensityName, intensityValue)
                    
                    val stepsList = mutableListOf(
                        Triple(step1Title, focusDesc, "👥"),
                        Triple(step2Title, intensityDesc, "📈"),
                        Triple(syncTitle, syncDesc, if (isSmsGranted) "⚡" else "📋"),
                        Triple(milestoneTitle, milestoneDesc, "🎯")
                    )
                    
                    if (relatesToLoans == true) {
                        stepsList.add(0, Triple(stringResource(R.string.onboarding_step_debt_title), stringResource(R.string.onboarding_step_debt_desc), "💸"))
                    }
                    if (relatesToAccounts == true) {
                        stepsList.add(1, Triple(stringResource(R.string.onboarding_step_accounts_title), stringResource(R.string.onboarding_step_accounts_desc), "🔗"))
                    }
                    if (relatesToEmergency == true) {
                        stepsList.add(Triple(stringResource(R.string.onboarding_step_emergency_title), stringResource(R.string.onboarding_step_emergency_desc), "🛡️"))
                    }
                    
                    val steps = stepsList.toList()

                    // AI ROAMAP LOADING LOGIC
                    var roadmapStep by remember { mutableStateOf(-1) } // -1: Thinking, 0-3: Steps, 4: Done
                    var thinkingPhase by remember { mutableStateOf(0) } // 0: Thinking, 1: Sketching, 2: Planning
                    
                    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
                    val shimmerAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 0.6f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "shimmer_alpha"
                    )

                    LaunchedEffect(Unit) {
                        if (roadmapStep == -1) {
                            delay(800)
                            thinkingPhase = 1 // Sketching
                            delay(800)
                            thinkingPhase = 2 // Planning
                            delay(1000)
                            
                            for (i in 0 until steps.size) {
                                roadmapStep = i
                                delay(1200)
                            }
                            roadmapStep = steps.size // All Done
                        }
                    }

                    if (roadmapStep == -1) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(54.dp),
                                    color = PinkPrimary,
                                    strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.height(28.dp))
                                val text = when(thinkingPhase) {
                                    0 -> stringResource(R.string.onboarding_ai_thinking)
                                    1 -> stringResource(R.string.onboarding_ai_sketching)
                                    else -> stringResource(R.string.onboarding_ai_making_plan)
                                }
                                Text(
                                    text = text,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NavyDark
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_personalize_roadmap_title),
                                fontSize = titleFontSize,
                                fontWeight = FontWeight.ExtraBold,
                                color = NavyDark,
                                textAlign = TextAlign.Center,
                                lineHeight = if (isSmallScreen) 32.sp else 40.sp
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Text(
                                text = stringResource(R.string.onboarding_personalize_roadmap_subtitle),
                                fontSize = subtitleFontSize,
                                color = TextLight,
                                textAlign = TextAlign.Center,
                                lineHeight = if (isSmallScreen) 20.sp else 24.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(28.dp))
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(if (isSmallScreen) 1f else 0.96f)
                                    .padding(bottom = if (isSmallScreen) 12.dp else 16.dp),
                                shape = RoundedCornerShape(24.dp),
                                color = Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shadowElevation = 1.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = if (isSmallScreen) 12.dp else 16.dp, vertical = if (isSmallScreen) 16.dp else 20.dp)
                                ) {
                                    steps.forEachIndexed { index, step ->
                                        if (index <= roadmapStep) {
                                            val (stepTitle, stepDesc, stepEmoji) = step
                                            val isSyncing = index == roadmapStep && roadmapStep < 4
                                            
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = if (index < steps.size - 1) (if (isSmallScreen) 12.dp else 16.dp) else 0.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(end = if (isSmallScreen) 10.dp else 14.dp)
                                                ) {
                                                    if (isSyncing) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Box(
                                                                modifier = Modifier.size(36.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                CircularProgressIndicator(
                                                                    modifier = Modifier.size(24.dp),
                                                                    color = PinkPrimary,
                                                                    strokeWidth = 2.dp
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = stringResource(R.string.onboarding_ai_syncing),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = PinkPrimary
                                                            )
                                                        }
                                                    } else {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    (when (index) {
                                                                        0 -> PinkPrimary
                                                                        1 -> PinkPrimary
                                                                        2 -> AccentBlue
                                                                        else -> Color(0xFFF59E0B)
                                                                    }).copy(alpha = 0.15f)
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = stepEmoji,
                                                                fontSize = 18.sp
                                                            )
                                                        }
                                                    }
                                                    
                                                    if (index < steps.size - 1) {
                                                        val startColor = when (index) {
                                                            0 -> PinkPrimary
                                                            1 -> PinkPrimary
                                                            2 -> AccentBlue
                                                            else -> Color(0xFFF59E0B)
                                                        }
                                                        val endColor = when (index + 1) {
                                                            0 -> PinkPrimary
                                                            1 -> PinkPrimary
                                                            2 -> AccentBlue
                                                            else -> Color(0xFFF59E0B)
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .width(2.dp)
                                                                .height(if (isSmallScreen) 40.dp else 54.dp)
                                                                .background(
                                                                    Brush.verticalGradient(
                                                                        colors = listOf(
                                                                            startColor.copy(alpha = 0.4f),
                                                                            endColor.copy(alpha = 0.4f)
                                                                        )
                                                                    )
                                                                )
                                                        )
                                                    }
                                                }
                                                
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(top = 2.dp)
                                                ) {
                                                    if (isSyncing) {
                                                        Column {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth(0.7f)
                                                                    .height(18.dp)
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(Color(0xFFE2E8F0).copy(alpha = shimmerAlpha))
                                                            )
                                                            Spacer(modifier = Modifier.height(10.dp))
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth(0.9f)
                                                                    .height(12.dp)
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(Color(0xFFE2E8F0).copy(alpha = shimmerAlpha))
                                                            )
                                                            Spacer(modifier = Modifier.height(6.dp))
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth(0.5f)
                                                                    .height(12.dp)
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(Color(0xFFE2E8F0).copy(alpha = shimmerAlpha))
                                                            )
                                                        }
                                                    } else {
                                                        Text(
                                                            text = stepTitle,
                                                            fontSize = if (isSmallScreen) 14.sp else 15.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = NavyDark,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = stepDesc,
                                                            fontSize = if (isSmallScreen) 11.sp else 12.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = TextLight,
                                                            lineHeight = if (isSmallScreen) 15.sp else 17.sp,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (pageIndex == 12) {
                    val anim1 = remember { Animatable(0f) }
                    val anim2 = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        anim1.animateTo(1f, animationSpec = tween(durationMillis = 800))
                    }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(150)
                        anim2.animateTo(1f, animationSpec = tween(durationMillis = 800))
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(top = 0.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_supercharge_title),
                                fontSize = titleFontSize,
                                fontWeight = FontWeight.ExtraBold,
                                color = NavyDark,
                                textAlign = TextAlign.Center,
                                lineHeight = if (isSmallScreen) 32.sp else 40.sp
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = stringResource(R.string.onboarding_supercharge_subtitle),
                                fontSize = subtitleFontSize,
                                color = TextLight,
                                textAlign = TextAlign.Center,
                                lineHeight = if (isSmallScreen) 20.sp else 24.sp,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                        
                        // Comparison visualization
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isSmallScreen) 260.dp else 380.dp)
                                .padding(horizontal = horizontalPadding)
                                .align(Alignment.Center),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 12.dp else 32.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Solo Card (Smaller)
                                val soloHeight = if (isSmallScreen) 28.dp else 38.dp
                                Column(
                                    modifier = Modifier
                                        .weight(0.4f)
                                        .graphicsLayer {
                                            translationY = 40.dp.toPx() * (1f - anim1.value)
                                            alpha = anim1.value
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(soloHeight * anim1.value),
                                        shape = RoundedCornerShape(if (isSmallScreen) 8.dp else 12.dp),
                                        color = Color(0xFF0F172A),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                        shadowElevation = 2.dp
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("x1", fontWeight = FontWeight.Bold, color = Color.White, fontSize = if (isSmallScreen) 12.sp else 14.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.onboarding_supercharge_solo),
                                        fontSize = if (isSmallScreen) 12.sp else 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextLight
                                    )
                                }

                                // With Piggy Card (7x taller)
                                Column(
                                    modifier = Modifier
                                        .weight(0.6f)
                                        .graphicsLayer {
                                            translationY = 40.dp.toPx() * (1f - anim2.value)
                                            alpha = anim2.value
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height((soloHeight * 7) * anim2.value),
                                        shape = RoundedCornerShape(if (isSmallScreen) 12.dp else 16.dp),
                                        color = Color.White,
                                        border = androidx.compose.foundation.BorderStroke(2.dp, PinkPrimary),
                                        shadowElevation = 8.dp
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(PinkPrimary, Color(0xFFF43F5E))
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "x7",
                                                    fontSize = if (isSmallScreen) 34.sp else 54.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = stringResource(R.string.onboarding_supercharge_faster),
                                                    fontSize = if (isSmallScreen) 12.sp else 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    letterSpacing = if (isSmallScreen) 1.sp else 2.sp
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.onboarding_supercharge_with_piggy, stringResource(R.string.piggy_ledger_brand)),
                                        fontSize = if (isSmallScreen) 14.sp else 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PinkPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Visible
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val page = pages[pageIndex]
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = page.imageRes),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .aspectRatio(1f),
                            contentScale = ContentScale.Fit
                        )
                        
                        Spacer(modifier = Modifier.height(30.dp))
                        
                        Text(
                            text = page.title,
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyDark,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isSmallScreen) 32.sp else 40.sp
                        )
                        
                        Spacer(modifier = Modifier.height(if (isSmallScreen) 10.dp else 14.dp))
                        
                        Text(
                            text = page.subtitle,
                            fontSize = subtitleFontSize,
                            color = TextLight,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isSmallScreen) 20.sp else 24.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )




                    }
                }
            }
        }
        
        // Lower Content: Indicators & Buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isSmallScreen) 16.dp else 32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Back Button (Optional/Visible when not on first page)
            if (currentPage > 0 && currentPage !in 7..9) {
                TextButton(
                    onClick = { currentPage-- },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .testTag("onboarding_back_button"),
                    colors = ButtonDefaults.textButtonColors(contentColor = TextLight)
                ) {
                    Text(
                        text = stringResource(R.string.back_btn),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }


            if (currentPage !in 7..9) {
                // Custom Progress Button
                ProgressNextButton(
                    currentPage = currentPage,
                    totalPages = pages.size,
                    isSmallScreen = isSmallScreen,
                    onNext = {
                        if (currentPage < pages.size - 1) {
                            if (currentPage == 3) {
                                requestSmsPermissions()
                            } else if (currentPage == 4) {
                                requestNotificationPermissions()
                            } else if (currentPage == 5 && selectedIntent == -1) {
                                Toast.makeText(context, context.getString(R.string.please_select_option), Toast.LENGTH_SHORT).show()
                            } else if (currentPage == 6 && selectedIntensity == -1) {
                                Toast.makeText(context, context.getString(R.string.please_select_option), Toast.LENGTH_SHORT).show()
                            } else {
                                currentPage++
                            }
                        } else {
                            onComplete(selectedIntent, selectedIntensity, selectedSavingMode)
                        }
                    }
                )
            }

        }
    }
}

@Composable
fun ProgressNextButton(
    currentPage: Int,
    totalPages: Int,
    isSmallScreen: Boolean,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (currentPage + 1).toFloat() / totalPages
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 700),
        label = "progress_anim"
    )
    
    val buttonColor = if (currentPage == totalPages - 1) PinkPrimary else NavyDark
    
    Box(
        modifier = modifier
            .size(if (isSmallScreen) 74.dp else 84.dp)
            .clip(CircleShape)
            .clickable(onClick = onNext),
        contentAlignment = Alignment.Center
    ) {
        // Progress Ring with segments
        Canvas(modifier = Modifier.size(if (isSmallScreen) 62.dp else 72.dp)) {
            val strokeWidth = (if (isSmallScreen) 3.dp else 4.dp).toPx()
            val gap = 8f // Gap between segments in degrees
            val segmentMaxSweep = (360f / totalPages) - gap
            
            for (i in 0 until totalPages) {
                val startAngle = -90f + (i * (360f / totalPages)) + (gap / 2)
                
                // Background segment
                drawArc(
                    color = Color(0xFFE2E8F0),
                    startAngle = startAngle,
                    sweepAngle = segmentMaxSweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
 
                // Smoothly fill segments based on animated progress
                val segmentProgress = ((animatedProgress * totalPages) - i).coerceIn(0f, 1f)
                
                if (segmentProgress > 0f) {
                    drawArc(
                        color = PinkPrimary,
                        startAngle = startAngle,
                        sweepAngle = segmentMaxSweep * segmentProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }
        
        // Inner Button
        Surface(
            modifier = Modifier.size(if (isSmallScreen) 48.dp else 54.dp),
            shape = RoundedCornerShape(if (isSmallScreen) 16.dp else 20.dp),
            color = buttonColor,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(if (isSmallScreen) 24.dp else 28.dp)
                )
            }
        }
    }
}


@Composable
private fun SmsSkeletonRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFE2E8F0))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFE2E8F0))
                )
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE2E8F0))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE2E8F0))
            )
        }
    }
}
