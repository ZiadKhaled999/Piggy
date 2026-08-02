package com.oryno.piggy_ledger.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    color: Color? = null,
    strokeWidth: Dp = 3.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "m3_expressive_loading")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 50f,
        targetValue = 290f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweep"
    )

    val defaultGradient = listOf(
        Color(0xFF00B0FF),
        Color(0xFF8B5CF6),
        Color(0xFFEC4899),
        Color(0xFF10B981),
        Color(0xFF00B0FF)
    )

    Canvas(
        modifier = modifier.size(size)
    ) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val strokePx = strokeWidth.toPx()
        val radius = (kotlin.math.min(w, h) / 2f) - (strokePx * 1.5f)

        if (radius <= 0f) return@Canvas

        val arcBrush = if (color != null) {
            SolidColor(color)
        } else {
            Brush.sweepGradient(defaultGradient, center = Offset(cx, cy))
        }

        val waveAmplitude = (radius * 0.14f).coerceAtLeast(1.8.dp.toPx())
        val waveFrequency = 6f // 6 wave peaks around circle

        // Draw Wavy Circular Path along the active arc
        rotate(rotationAngle, pivot = Offset(cx, cy)) {
            val wavePath = Path()
            val numSteps = 90
            val sweepRad = Math.toRadians(sweepAngle.toDouble()).toFloat()

            var first = true
            for (i in 0..numSteps) {
                val progress = i.toFloat() / numSteps
                val currentAngleRad = progress * sweepRad

                // Sinusoidal wave modulation along the radius
                val waveOffset = waveAmplitude * sin(waveFrequency * currentAngleRad + wavePhase)
                val r = radius + waveOffset

                val x = cx + r * cos(currentAngleRad)
                val y = cy + r * sin(currentAngleRad)

                if (first) {
                    wavePath.moveTo(x, y)
                    first = false
                } else {
                    wavePath.lineTo(x, y)
                }
            }

            drawPath(
                path = wavePath,
                brush = arcBrush,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        // Draw Inner Morphing Wavy Star Core
        val starRadius = (radius * 0.4f) * scalePulse
        if (starRadius > 1.dp.toPx()) {
            rotate(-rotationAngle * 1.4f, pivot = Offset(cx, cy)) {
                val starPath = Path()
                val points = 6
                val innerRadius = starRadius * 0.45f
                for (i in 0 until points * 2) {
                    val r = if (i % 2 == 0) starRadius else innerRadius
                    val angleRad = (i * Math.PI / points).toFloat()
                    val waveOffset = (1.2.dp.toPx()) * sin(3f * angleRad + wavePhase)
                    val effRadius = (r + waveOffset).coerceAtLeast(0.5f)
                    val x = cx + effRadius * cos(angleRad)
                    val y = cy + effRadius * sin(angleRad)
                    if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
                }
                starPath.close()

                val starBrush = if (color != null) {
                    SolidColor(color)
                } else {
                    Brush.linearGradient(
                        listOf(Color(0xFF8B5CF6), Color(0xFF00B0FF)),
                        start = Offset(cx - starRadius, cy - starRadius),
                        end = Offset(cx + starRadius, cy + starRadius)
                    )
                }
                drawPath(path = starPath, brush = starBrush)
            }
        }
    }
}

