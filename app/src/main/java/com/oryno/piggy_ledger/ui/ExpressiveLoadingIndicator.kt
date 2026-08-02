package com.oryno.piggy_ledger.ui

import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.StrokeJoin
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    color: Color? = null,
    strokeWidth: Dp = 3.5.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "m3_expressive_loading")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 100f,
        targetValue = 260f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweep"
    )

    val activeColor = color ?: PinkPrimary
    val trackColor = activeColor.copy(alpha = 0.22f)

    Canvas(
        modifier = modifier.size(size)
    ) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val strokePx = strokeWidth.toPx()
        val radius = (kotlin.math.min(w, h) / 2f) - (strokePx * 1.2f)

        if (radius <= 0f) return@Canvas

        // 1. Draw Background Soft Circle Ring
        drawCircle(
            color = trackColor,
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        // 2. Draw Foreground Wavy Spinning Arc
        rotate(rotationAngle, pivot = Offset(cx, cy)) {
            val wavePath = Path()
            val numSteps = 180
            val sweepRad = Math.toRadians(sweepAngle.toDouble()).toFloat()
            val waveAmplitude = strokePx * 0.38f
            val waveFrequency = 10f // Smooth wavy curves

            var first = true
            for (i in 0..numSteps) {
                val progress = i.toFloat() / numSteps
                val currentAngleRad = progress * sweepRad

                // Smooth sinusoidal radial displacement
                val waveOffset = waveAmplitude * sin(waveFrequency * currentAngleRad)
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
                color = activeColor,
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}


