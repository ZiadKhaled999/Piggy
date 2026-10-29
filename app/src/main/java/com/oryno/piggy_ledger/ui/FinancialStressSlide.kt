package com.oryno.piggy_ledger.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.SlateDark
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FinancialStressSlide(
    onContinue: () -> Unit,
    onBack: () -> Unit,
    isSmallScreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val highlightText = stringResource(R.string.onboarding_insight_title_highlight)
    val restText = stringResource(R.string.onboarding_insight_title_rest)
    val subtitleText = stringResource(R.string.onboarding_insight_subtitle)
    val insightTag = stringResource(R.string.onboarding_insight_tag)

    val editorialCreamBg = Color(0xFFFFFAEE)
    val editorialAmberHighlight = Color(0xFFD97706)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(editorialCreamBg)
            .padding(vertical = if (isSmallScreen) 10.dp else 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar: Back button, CNBC logo, INSIGHT header
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
                        .testTag("onboarding_insight_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.back_btn),
                        tint = NavyDark,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Official CNBC Logo Vector
                Image(
                    painter = painterResource(id = R.drawable.ic_logo_cnbc),
                    contentDescription = "CNBC Official Logo",
                    modifier = Modifier.height(if (isSmallScreen) 26.dp else 32.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
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

        // Scrollable / Adaptive Body Content
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
                    withStyle(style = SpanStyle(color = editorialAmberHighlight)) {
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

            // Subtitle
            Text(
                text = subtitleText,
                fontSize = if (isSmallScreen) 15.sp else 18.sp,
                fontWeight = FontWeight.Normal,
                color = NavyDark.copy(alpha = 0.90f),
                lineHeight = if (isSmallScreen) 22.sp else 26.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(if (isSmallScreen) 10.dp else 18.dp))

            // Tangled Scribble Illustration matching screenshot
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSmallScreen) 230.dp else 300.dp),
                contentAlignment = Alignment.Center
            ) {
                TangledScribbleCanvas(
                    color = editorialAmberHighlight,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Bottom Continue Button (Black pill button matching screenshot)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = if (isSmallScreen) 4.dp else 8.dp)
        ) {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSmallScreen) 50.dp else 56.dp)
                    .testTag("onboarding_insight_continue_button"),
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

private data class ScribbleLoop(
    val startDeg: Float,
    val sweepDeg: Float,
    val rxRatio: Float,
    val ryRatio: Float,
    val offXRatio: Float,
    val offYRatio: Float
)

/**
 * Procedurally draws the organic tangled yarn/scribble in Piggy Ledger Pink,
 * with smooth bezier curves and a trailing line extending off the right edge of the screen.
 */
@Composable
fun TangledScribbleCanvas(
    color: Color = PinkPrimary,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900)
        )
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val cx = width * 0.48f
        val cy = height * 0.50f
        val radiusX = width * 0.30f
        val radiusY = height * 0.38f

        val strokeWidthPx = 3.dp.toPx()

        val path = Path()

        // 1. Organic overlapping loops forming the dense tangled ball
        val loops = listOf(
            ScribbleLoop(0f, 370f, 0.95f, 0.88f, 0f, 0f),
            ScribbleLoop(25f, 380f, 0.82f, 1.05f, 0.04f, -0.05f),
            ScribbleLoop(55f, 365f, 1.02f, 0.78f, -0.03f, 0.04f),
            ScribbleLoop(85f, 390f, 0.75f, 0.95f, 0.06f, 0.02f),
            ScribbleLoop(120f, 370f, 0.90f, 0.85f, -0.05f, -0.04f),
            ScribbleLoop(155f, 385f, 0.85f, 1.02f, 0.02f, 0.06f),
            ScribbleLoop(190f, 360f, 1.05f, 0.72f, -0.04f, -0.02f),
            ScribbleLoop(220f, 395f, 0.70f, 0.92f, 0.05f, -0.06f),
            ScribbleLoop(260f, 375f, 0.92f, 0.80f, -0.02f, 0.05f),
            ScribbleLoop(295f, 385f, 0.80f, 0.98f, 0.04f, 0.03f),
            ScribbleLoop(330f, 370f, 0.98f, 0.75f, -0.05f, -0.03f),

            // Inner dense chaotic crossover loops
            ScribbleLoop(15f, 360f, 0.62f, 0.65f, 0.02f, 0.01f),
            ScribbleLoop(70f, 360f, 0.58f, 0.72f, -0.03f, -0.02f),
            ScribbleLoop(135f, 360f, 0.68f, 0.55f, 0.04f, 0.02f),
            ScribbleLoop(205f, 360f, 0.52f, 0.68f, -0.02f, 0.03f),
            ScribbleLoop(275f, 360f, 0.64f, 0.58f, 0.01f, -0.04f),

            // Outer tendril loops giving authentic loose scribble texture
            ScribbleLoop(40f, 365f, 1.08f, 0.92f, 0.02f, -0.04f),
            ScribbleLoop(160f, 370f, 0.92f, 1.10f, -0.03f, 0.02f),
            ScribbleLoop(245f, 360f, 1.04f, 0.88f, 0.01f, 0.04f)
        )

        for ((index, loop) in loops.withIndex()) {
            val rx = radiusX * loop.rxRatio
            val ry = radiusY * loop.ryRatio
            val loopCx = cx + radiusX * loop.offXRatio
            val loopCy = cy + radiusY * loop.offYRatio

            val startRad = Math.toRadians(loop.startDeg.toDouble())
            val startX = (loopCx + rx * cos(startRad)).toFloat()
            val startY = (loopCy + ry * sin(startRad)).toFloat()

            if (index == 0) {
                path.moveTo(startX, startY)
            } else {
                path.lineTo(startX, startY)
            }

            // Approximate ellipse using 6 cubic bezier segments
            val steps = 6
            val stepRad = Math.toRadians((loop.sweepDeg / steps).toDouble())
            for (s in 1..steps) {
                val currentAngle = startRad + (s - 1) * stepRad
                val nextAngle = startRad + s * stepRad
                val midAngle = (currentAngle + nextAngle) / 2.0

                // Control points for cubic bezier
                val cpDist = 1.15
                val cp1x = (loopCx + rx * cos(currentAngle) * cpDist).toFloat()
                val cp1y = (loopCy + ry * sin(currentAngle) * cpDist).toFloat()
                val cp2x = (loopCx + rx * cos(midAngle) * cpDist).toFloat()
                val cp2y = (loopCy + ry * sin(midAngle) * cpDist).toFloat()
                val endX = (loopCx + rx * cos(nextAngle)).toFloat()
                val endY = (loopCy + ry * sin(nextAngle)).toFloat()

                path.cubicTo(cp1x, cp1y, cp2x, cp2y, endX, endY)
            }
        }

        // 2. Trailing thread extending off to the right screen edge
        // Starts from the knot at roughly (cx + radiusX * 0.75f, cy + radiusY * 0.15f)
        val exitStartY = cy + radiusY * 0.22f
        val exitStartX = cx + radiusX * 0.80f

        path.lineTo(exitStartX, exitStartY)

        // Smooth wave leading out to the right edge
        val exitMid1X = cx + radiusX * 1.05f
        val exitMid1Y = cy + radiusY * 0.25f

        val exitMid2X = cx + radiusX * 1.30f
        val exitMid2Y = cy + radiusY * 0.20f

        val exitEndX = width + 20f
        val exitEndY = cy + radiusY * 0.20f

        path.cubicTo(exitMid1X, exitMid1Y, exitMid2X, exitMid2Y, exitEndX, exitEndY)

        // Draw the complete scribble with Piggy Pink styling
        drawPath(
            path = path,
            color = color.copy(alpha = animatedProgress.value),
            style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
