package com.oryno.piggy_ledger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.SlateDark
import com.oryno.piggy_ledger.ui.theme.TextLight

private val EditorialCreamBg = Color(0xFFFFFAEE)
private val EditorialAmberHighlight = Color(0xFFD97706)

@Composable
fun PwcInsightSlide(
    onContinue: () -> Unit,
    onBack: () -> Unit,
    isSmallScreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val highlightText = stringResource(R.string.onboarding_pwc_title_highlight)
    val restText = stringResource(R.string.onboarding_pwc_title_rest)
    val bodyText = stringResource(R.string.onboarding_pwc_body)
    val sourceText = stringResource(R.string.onboarding_pwc_source)
    val insightTag = stringResource(R.string.onboarding_insight_tag)

    val progressAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progressAnim.animateTo(0.52f, animationSpec = tween(durationMillis = 800))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorialCreamBg)
            .padding(vertical = if (isSmallScreen) 10.dp else 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar: Back button + Official PwC Logo on left, INSIGHT tag on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("onboarding_pwc_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.back_btn),
                        tint = NavyDark,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Official PwC Logo Vector
                Image(
                    painter = painterResource(id = R.drawable.ic_logo_pwc),
                    contentDescription = "PwC Official Logo",
                    modifier = Modifier.height(if (isSmallScreen) 26.dp else 32.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // Right Tag: "INSIGHT"
            Text(
                text = insightTag,
                fontSize = if (isSmallScreen) 13.sp else 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NavyDark,
                letterSpacing = 1.5.sp
            )
        }

        // Scrollable Editorial Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(if (isSmallScreen) 14.dp else 24.dp))

            // Main Title
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = EditorialAmberHighlight)) {
                        append(highlightText)
                    }
                    withStyle(style = SpanStyle(color = NavyDark)) {
                        append(restText)
                    }
                },
                fontSize = if (isSmallScreen) 29.sp else 36.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = if (isSmallScreen) 35.sp else 43.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(if (isSmallScreen) 14.dp else 20.dp))

            // Body Paragraph
            Text(
                text = bodyText,
                fontSize = if (isSmallScreen) 15.sp else 18.sp,
                fontWeight = FontWeight.Normal,
                color = NavyDark.copy(alpha = 0.90f),
                lineHeight = if (isSmallScreen) 22.sp else 26.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(if (isSmallScreen) 24.dp else 36.dp))

            // Sleek Editorial 52% Metric Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.70f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "52%",
                            fontSize = if (isSmallScreen) 28.sp else 34.sp,
                            fontWeight = FontWeight.Black,
                            color = EditorialAmberHighlight
                        )

                        Text(
                            text = "56,000+ Workers • 50 Countries",
                            fontSize = if (isSmallScreen) 11.sp else 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateDark
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressAnim.value)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(EditorialAmberHighlight, Color(0xFFEA580C))
                                    )
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Footer & Continue Button (Matches CNBC card exactly)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = if (isSmallScreen) 6.dp else 12.dp)
        ) {
            Text(
                text = sourceText,
                fontSize = if (isSmallScreen) 12.sp else 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = TextLight,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSmallScreen) 50.dp else 56.dp)
                    .testTag("onboarding_pwc_continue_button"),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyDark,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = stringResource(R.string.continue_btn),
                    fontSize = if (isSmallScreen) 15.sp else 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun GallupInsightSlide(
    onContinue: () -> Unit,
    onBack: () -> Unit,
    isSmallScreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val highlightText = stringResource(R.string.onboarding_gallup_title_highlight)
    val restText = stringResource(R.string.onboarding_gallup_title_rest)
    val bodyText = stringResource(R.string.onboarding_gallup_body)
    val sourceText = stringResource(R.string.onboarding_gallup_source)
    val insightTag = stringResource(R.string.onboarding_insight_tag)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorialCreamBg)
            .padding(vertical = if (isSmallScreen) 10.dp else 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar: Back button + Official Gallup Logo on left, INSIGHT tag on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("onboarding_gallup_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.back_btn),
                        tint = NavyDark,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Official Gallup Logo Image (Pin-sharp transparent PNG in Piggy NavyDark)
                Image(
                    painter = painterResource(id = R.drawable.ic_logo_gallup),
                    contentDescription = "Gallup Official Logo",
                    modifier = Modifier.height(if (isSmallScreen) 18.dp else 22.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // Right Tag: "INSIGHT"
            Text(
                text = insightTag,
                fontSize = if (isSmallScreen) 13.sp else 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NavyDark,
                letterSpacing = 1.5.sp
            )
        }

        // Scrollable Editorial Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(if (isSmallScreen) 14.dp else 24.dp))

            // Main Title
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = EditorialAmberHighlight)) {
                        append(highlightText)
                    }
                    withStyle(style = SpanStyle(color = NavyDark)) {
                        append(restText)
                    }
                },
                fontSize = if (isSmallScreen) 29.sp else 36.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = if (isSmallScreen) 35.sp else 43.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(if (isSmallScreen) 14.dp else 20.dp))

            // Body Paragraph
            Text(
                text = bodyText,
                fontSize = if (isSmallScreen) 15.sp else 18.sp,
                fontWeight = FontWeight.Normal,
                color = NavyDark.copy(alpha = 0.90f),
                lineHeight = if (isSmallScreen) 22.sp else 26.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(if (isSmallScreen) 24.dp else 36.dp))

            // Sleek Editorial 41% Metric Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.70f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "41%",
                            fontSize = if (isSmallScreen) 28.sp else 34.sp,
                            fontWeight = FontWeight.Black,
                            color = EditorialAmberHighlight
                        )

                        Text(
                            text = "128,000 Employees • 160 Countries",
                            fontSize = if (isSmallScreen) 11.sp else 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateDark
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 10-step discrete dot indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..10) {
                            val isStressed = i <= 4
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (isStressed) EditorialAmberHighlight else Color(0xFFE2E8F0)
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Footer & Continue Button (Matches CNBC card exactly)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = if (isSmallScreen) 6.dp else 12.dp)
        ) {
            Text(
                text = sourceText,
                fontSize = if (isSmallScreen) 12.sp else 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = TextLight,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSmallScreen) 50.dp else 56.dp)
                    .testTag("onboarding_gallup_continue_button"),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyDark,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = stringResource(R.string.continue_btn),
                    fontSize = if (isSmallScreen) 15.sp else 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
