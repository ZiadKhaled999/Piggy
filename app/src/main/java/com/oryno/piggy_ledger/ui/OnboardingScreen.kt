package com.oryno.piggy_ledger.ui
import androidx.compose.ui.res.stringResource
import com.oryno.piggy_ledger.R


import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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

data class OnboardingPageData(
    val imageRes: Int,
    val title: AnnotatedString,
    val subtitle: String
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
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
            .padding(horizontal = 24.dp, vertical = 16.dp),
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
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyDark,
                        textAlign = TextAlign.Center,
                        lineHeight = 40.sp
                    )
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    Text(
                        text = page.subtitle,
                        fontSize = 16.sp,
                        color = TextLight,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
        
        // Lower Content: Indicators & Buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Page Indicator Dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                repeat(pages.size) { index ->
                    val dotWidth by animateDpAsState(
                        targetValue = if (index == currentPage) 24.dp else 8.dp,
                        label = "dot_width"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(
                                if (index == currentPage) NavyDark else Color(0xFFE2E8F0)
                            )
                    )
                }
            }
            
            // Buttons Row matching the exact styles
            if (currentPage == 0) {
                Button(
                    onClick = { currentPage++ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("onboarding_continue_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDark)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.continue_btn),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { currentPage-- },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("onboarding_back_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.2.dp, Color(0xFFE2E8F0)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = TextLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.back_btn),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextLight
                            )
                        }
                    }
                    
                    Button(
                        onClick = {
                            if (currentPage < pages.size - 1) {
                                currentPage++
                            } else {
                                onComplete()
                            }
                        },
                        modifier = Modifier
                            .weight(2f)
                            .height(56.dp)
                            .testTag(if (currentPage == pages.size - 1) "onboarding_get_started_button" else "onboarding_continue_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDark)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentPage == pages.size - 1) stringResource(R.string.get_started) else stringResource(R.string.continue_btn),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
