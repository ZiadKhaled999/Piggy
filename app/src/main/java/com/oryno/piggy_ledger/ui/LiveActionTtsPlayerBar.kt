package com.oryno.piggy_ledger.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Modern light-theme Live Action TTS Audio Player Bar matching the screenshot design.
 * Features:
 * - Circular Play/Pause/Resume toggle
 * - Formatted live elapsed timer (00:04)
 * - Animated real-time responsive audio waveform equalizer bars
 * - Restart action to replay from the beginning
 * - Dismiss/Cancel (X) button to stop speech
 */
@Composable
fun LiveActionTtsPlayerBar(
    elapsedSeconds: Int,
    isPaused: Boolean,
    onTogglePlayPause: () -> Unit,
    onRestart: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Circular Play/Pause/Resume button
            Surface(
                onClick = onTogglePlayPause,
                shape = CircleShape,
                color = Color(0xFF0F172A),
                modifier = Modifier.size(38.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Timer (00:04)
            Text(
                text = formattedTime,
                color = Color(0xFF0F172A),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Middle: Live Action Audio Waveform Equalizer
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                LiveAudioWaveformVisualizer(isPlaying = !isPaused)
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Restart Button (optional quick rewind to start)
            IconButton(
                onClick = onRestart,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = "Restart speech",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(17.dp)
                )
            }

            // Close / Cancel Button (X)
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel speech",
                    tint = Color(0xFF334155),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Animated real-time waveform bars mimicking audio frequency and speech energy.
 */
@Composable
fun LiveAudioWaveformVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 28
    val animatables = remember {
        List(barCount) { Animatable(0.25f) }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                animatables.forEachIndexed { index, animatable ->
                    val target = if (index % 5 == 0 || index % 7 == 0) {
                        0.75f + Random.nextFloat() * 0.25f
                    } else if (index % 3 == 0) {
                        0.45f + Random.nextFloat() * 0.40f
                    } else {
                        0.20f + Random.nextFloat() * 0.35f
                    }
                    animatable.animateTo(
                        targetValue = target,
                        animationSpec = tween(
                            durationMillis = 120 + (index % 4) * 40,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
                delay(60)
            }
        } else {
            animatables.forEach { animatable ->
                animatable.animateTo(
                    targetValue = 0.2f,
                    animationSpec = tween(durationMillis = 200, easing = LinearEasing)
                )
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val barWidth = (totalWidth / (barCount * 1.6f)).coerceIn(2.5f, 5.5f)
        val spacing = (totalWidth - (barWidth * barCount)) / (barCount - 1).coerceAtLeast(1)

        val activeBarColor = Color(0xFF0F172A)
        val inactiveBarColor = Color(0xFF94A3B8)

        for (i in 0 until barCount) {
            val progress = animatables[i].value
            val barHeight = (canvasHeight * progress).coerceAtLeast(4f)
            val left = i * (barWidth + spacing)
            val top = (canvasHeight - barHeight) / 2f

            val color = if (isPlaying) activeBarColor else inactiveBarColor

            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
