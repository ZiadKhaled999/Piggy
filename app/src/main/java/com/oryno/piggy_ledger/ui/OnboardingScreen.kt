package com.oryno.piggy_ledger.ui
import androidx.compose.ui.res.stringResource
import com.oryno.piggy_ledger.R


import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight

val TealGreen = Color(0xFF43B7A7)

data class OnboardingPageData(
    val imageRes: Int,
    val title: AnnotatedString,
    val subtitle: String
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360
    
    val titleFontSize = if (isSmallScreen) 26.sp else 32.sp
    val subtitleFontSize = if (isSmallScreen) 14.sp else 16.sp
    val buttonFontSize = if (isSmallScreen) 16.sp else 18.sp
    val backButtonFontSize = if (isSmallScreen) 13.sp else 15.sp
    val horizontalPadding = if (isSmallScreen) 16.dp else 24.dp

    var currentPage by remember { mutableIntStateOf(0) }
    
    
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
            .padding(horizontal = horizontalPadding, vertical = 16.dp),
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
                    
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    Text(
                        text = page.title,
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyDark,
                        textAlign = TextAlign.Center,
                        lineHeight = if (isSmallScreen) 32.sp else 40.sp
                    )
                    
                    Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 18.dp))
                    
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
        
        // Lower Content: Indicators & Buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Back Button (Optional/Visible when not on first page)
            if (currentPage > 0) {
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

            // Custom Progress Button
            ProgressNextButton(
                currentPage = currentPage,
                totalPages = pages.size,
                onNext = {
                    if (currentPage < pages.size - 1) {
                        currentPage++
                    } else {
                        onComplete()
                    }
                }
            )
        }
    }
}

@Composable
fun ProgressNextButton(
    currentPage: Int,
    totalPages: Int,
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
            .size(84.dp)
            .clip(CircleShape)
            .clickable(onClick = onNext),
        contentAlignment = Alignment.Center
    ) {
        // Progress Ring with segments
        Canvas(modifier = Modifier.size(72.dp)) {
            val strokeWidth = 4.dp.toPx()
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
                        color = TealGreen,
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
            modifier = Modifier.size(54.dp),
            shape = RoundedCornerShape(20.dp),
            color = buttonColor,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
