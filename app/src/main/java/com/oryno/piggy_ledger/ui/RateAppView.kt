package com.oryno.piggy_ledger.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import com.posthog.PostHog
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

data class RatingEmotion(
    val level: Int,
    val emoji: String,
    val labelRes: Int,
    val starsText: String,
    val accentColor: Color,
    val bgLightColor: Color
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RateAppView(
    onBack: () -> Unit,
    onNavigateToFeedback: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val emotions = remember {
        listOf(
            RatingEmotion(
                level = 1,
                emoji = "😡",
                labelRes = R.string.rate_feeling_terrible,
                starsText = "★☆☆☆☆",
                accentColor = Color(0xFFEF4444),
                bgLightColor = Color(0xFFFEE2E2)
            ),
            RatingEmotion(
                level = 2,
                emoji = "🙁",
                labelRes = R.string.rate_feeling_poor,
                starsText = "★★☆☆☆",
                accentColor = Color(0xFFF97316),
                bgLightColor = Color(0xFFFFEDD5)
            ),
            RatingEmotion(
                level = 3,
                emoji = "😐",
                labelRes = R.string.rate_feeling_okay,
                starsText = "★★★☆☆",
                accentColor = Color(0xFF8B5CF6),
                bgLightColor = Color(0xFFEDE9FE)
            ),
            RatingEmotion(
                level = 4,
                emoji = "😊",
                labelRes = R.string.rate_feeling_good,
                starsText = "★★★★☆",
                accentColor = Color(0xFF10B981),
                bgLightColor = Color(0xFFD1FAE5)
            ),
            RatingEmotion(
                level = 5,
                emoji = "🤩",
                labelRes = R.string.rate_feeling_loved_it,
                starsText = "★★★★★",
                accentColor = Color(0xFFF59E0B),
                bgLightColor = Color(0xFFFEF3C7)
            )
        )
    }

    // Default selection is 5 (highest satisfaction) or 4
    var selectedIndex by remember { mutableIntStateOf(4) }
    val currentEmotion = emotions[selectedIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top App Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_icon),
                    tint = NavyDark
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.rate_app_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Title and question prompt
        Text(
            text = stringResource(R.string.rate_how_was_experience),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = NavyDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.rate_drag_to_choose),
            fontSize = 13.sp,
            color = TextLight,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Center Stage: Huge Displayed Emoji with Animated Transition
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            AnimatedContent(
                targetState = currentEmotion,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.75f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)))
                        .togetherWith(fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.85f, animationSpec = tween(150)))
                },
                label = "HugeEmojiTransition"
            ) { emotion ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    emotion.bgLightColor.copy(alpha = 0.9f),
                                    emotion.bgLightColor.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                ) {
                    Text(
                        text = emotion.emoji,
                        fontSize = 86.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Rating Label & Stars
            Text(
                text = stringResource(currentEmotion.labelRes),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = NavyDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentEmotion.starsText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = currentEmotion.accentColor,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                val scoreText = if (Locale.getDefault().language == "ar") {
                    "(%d من ٥)".format(currentEmotion.level)
                } else {
                    "(%d/5)".format(currentEmotion.level)
                }
                
                Text(
                    text = scoreText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextLight
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Guidance text based on rating branch
            Text(
                text = stringResource(
                    if (currentEmotion.level <= 3) R.string.rate_prompt_feedback
                    else R.string.rate_prompt_playstore
                ),
                fontSize = 13.sp,
                color = TextLight,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Lower Arc Gesture Dial (inspired by reference image)
        RadialArcDial(
            emotions = emotions,
            selectedIndex = selectedIndex,
            onIndexChanged = { newIndex ->
                if (newIndex != selectedIndex) {
                    selectedIndex = newIndex
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Bottom Action Button: Continue
        Button(
            onClick = {
                val rating = currentEmotion.level
                try {
                    PostHog.capture(
                        "user_rated_app",
                        properties = mapOf(
                            "stars" to rating,
                            "feeling" to currentEmotion.emoji
                        )
                    )
                } catch (e: Throwable) {
                    // Ignore analytics failure
                }

                if (rating <= 3) {
                    // Ratings 1 to 3 -> Open Feedback screen
                    onNavigateToFeedback()
                } else {
                    // Ratings 4 to 5 -> Open Google Play Store
                    ToastUtil.show(context, context.getString(R.string.rating_submitted_thanks), Toast.LENGTH_SHORT)
                    openPlayStore(context)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = PinkPrimary.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PinkPrimary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = stringResource(R.string.continue_btn),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

/**
 * Convex curved arc dial with 5 feelings, gesture drag indicator and pointer.
 */
@Composable
private fun RadialArcDial(
    emotions: List<RatingEmotion>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val scope = rememberCoroutineScope()
    val animatedProgress = remember { Animatable(selectedIndex.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(selectedIndex) {
        if (!isDragging) {
            animatedProgress.animateTo(
                targetValue = selectedIndex.toFloat(),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val marginPx = with(density) { 32.dp.toPx() }
        val arcLeft = marginPx
        val arcRight = widthPx - marginPx
        val arcWidth = (arcRight - arcLeft).coerceAtLeast(1f)
        val baseY = heightPx - with(density) { 26.dp.toPx() }
        val arcHeight = with(density) { 42.dp.toPx() }

        // Parabolic convex arc formula: y(t) = baseY - 4 * arcHeight * t * (1 - t)
        fun getArcPoint(t: Float): Offset {
            val clampedT = t.coerceIn(0f, 1f)
            val x = arcLeft + clampedT * arcWidth
            val y = baseY - 4f * arcHeight * clampedT * (1f - clampedT)
            return Offset(x, y)
        }

        // Gesture detector across the dial area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(arcLeft, arcRight, isRtl, widthPx) {
                    detectTapGestures { offset ->
                        val logicalX = if (isRtl) widthPx - offset.x else offset.x
                        val t = ((logicalX - arcLeft) / arcWidth).coerceIn(0f, 1f)
                        val targetIdx = (t * (emotions.size - 1)).roundToInt()
                        onIndexChanged(targetIdx)
                        scope.launch {
                            animatedProgress.animateTo(
                                targetValue = targetIdx.toFloat(),
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    }
                }
                .pointerInput(arcLeft, arcRight, isRtl, widthPx) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            val targetIdx = animatedProgress.value.roundToInt().coerceIn(0, emotions.size - 1)
                            onIndexChanged(targetIdx)
                            scope.launch {
                                animatedProgress.animateTo(
                                    targetValue = targetIdx.toFloat(),
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            val targetIdx = animatedProgress.value.roundToInt().coerceIn(0, emotions.size - 1)
                            scope.launch {
                                animatedProgress.animateTo(targetIdx.toFloat())
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val logicalX = if (isRtl) widthPx - change.position.x else change.position.x
                            val t = ((logicalX - arcLeft) / arcWidth).coerceIn(0f, 1f)
                            val rawProgress = t * (emotions.size - 1)
                            scope.launch {
                                animatedProgress.snapTo(rawProgress)
                            }
                            val targetIdx = rawProgress.roundToInt().coerceIn(0, emotions.size - 1)
                            onIndexChanged(targetIdx)
                        }
                    )
                }
        ) {
            // Draw background curved arc and station dots
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path()
                val steps = 60
                for (s in 0..steps) {
                    val t = s.toFloat() / steps
                    val pt = getArcPoint(t)
                    val drawX = if (isRtl) widthPx - pt.x else pt.x
                    if (s == 0) path.moveTo(drawX, pt.y) else path.lineTo(drawX, pt.y)
                }

                // Thick soft purple arc background
                drawPath(
                    path = path,
                    color = Color(0xFFE0E7FE),
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )

                // Station nodes on the curve
                for (i in emotions.indices) {
                    val t = i.toFloat() / (emotions.size - 1)
                    val pt = getArcPoint(t)
                    val drawX = if (isRtl) widthPx - pt.x else pt.x
                    val isSelected = i == selectedIndex
                    
                    // Outer dot
                    drawCircle(
                        color = if (isSelected) Color(0xFF6366F1) else Color(0xFFCBD5E1),
                        radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                        center = Offset(drawX, pt.y)
                    )
                    // Inner white dot
                    drawCircle(
                        color = Color.White,
                        radius = if (isSelected) 3.dp.toPx() else 2.dp.toPx(),
                        center = Offset(drawX, pt.y)
                    )
                }
            }

            // Emotion station preview pills above each node
            for (i in emotions.indices) {
                val t = i.toFloat() / (emotions.size - 1)
                val pt = getArcPoint(t)
                val isSelected = i == selectedIndex

                val nodeXOffset = with(density) { (pt.x).toDp() - 28.dp }
                val nodeYOffset = with(density) { (pt.y).toDp() - 56.dp }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .offset(x = nodeXOffset, y = nodeYOffset)
                        .width(56.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onIndexChanged(i)
                        }
                ) {
                    // Small emoji with slight bounce when selected
                    Text(
                        text = emotions[i].emoji,
                        fontSize = if (isSelected) 26.sp else 18.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${emotions[i].level}",
                        fontSize = if (isSelected) 12.sp else 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) NavyDark else TextLight
                    )
                }
            }

            // The Indicator Knob with Upward Pointer (matching screenshot)
            val currentT = (animatedProgress.value / (emotions.size - 1)).coerceIn(0f, 1f)
            val thumbPt = getArcPoint(currentT)
            val thumbXOffset = with(density) { thumbPt.x.toDp() - 14.dp }
            val thumbYOffset = with(density) { thumbPt.y.toDp() - 14.dp }

            Box(
                modifier = Modifier
                    .offset(x = thumbXOffset, y = thumbYOffset)
                    .size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                // Upward Pointer (inverted triangle matching screenshot)
                Canvas(
                    modifier = Modifier
                        .size(10.dp, 8.dp)
                        .offset(y = (-15).dp)
                ) {
                    val pointerPath = Path().apply {
                        moveTo(0f, size.height)
                        lineTo(size.width, size.height)
                        lineTo(size.width / 2f, 0f)
                        close()
                    }
                    drawPath(
                        path = pointerPath,
                        color = Color(0xFF6366F1)
                    )
                }

                // Glowing Thumb Circle
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .shadow(4.dp, CircleShape, spotColor = Color(0xFF6366F1))
                        .clip(CircleShape)
                        .background(Color(0xFF6366F1))
                        .border(3.dp, Color.White, CircleShape)
                )
            }
        }
    }
}

private fun openPlayStore(context: Context) {
    val packageName = context.packageName
    try {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(marketIntent)
    } catch (e: Exception) {
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (ex: Exception) {
            ToastUtil.show(context, "Could not open Google Play Store", Toast.LENGTH_SHORT)
        }
    }
}
