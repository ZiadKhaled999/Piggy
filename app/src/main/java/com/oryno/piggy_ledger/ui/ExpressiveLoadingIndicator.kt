package com.oryno.piggy_ledger.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Material Design 3 Expressive Contained Loading Indicator.
 *
 * Implements the official M3 Expressive Contained Loading Indicator specification
 * (https://m3.material.io/components/loading-indicator/overview).
 *
 * Features a circular or rounded container with an active, solid shape morphing
 * and rotating inside it. The solid indicator seamlessly morphs across canonical
 * Material 3 shapes (4-lobed clover/flower, circle, 8-scalloped cookie, rounded squircle/diamond).
 */
@Composable
fun ContainedLoadingIndicator(
    modifier: Modifier = Modifier,
    containerSize: Dp = 36.dp,
    indicatorSize: Dp = 20.dp,
    containerColor: Color = Color(0xFFFCE7F3), // Light pink container container-tint
    indicatorColor: Color = Color(0xFFDB2777), // Solid M3 expressive pink
    containerShape: Shape = CircleShape,
    elevation: Dp = 0.dp
) {
    Surface(
        modifier = modifier.size(containerSize),
        shape = containerShape,
        color = containerColor,
        shadowElevation = elevation
    ) {
        Box(
            modifier = Modifier.size(containerSize),
            contentAlignment = Alignment.Center
        ) {
            ExpressiveLoadingIndicator(
                size = indicatorSize,
                color = indicatorColor,
                isFilled = true,
                hasTrack = false
            )
        }
    }
}

/**
 * Material Design 3 Expressive Loading Indicator.
 *
 * Implements the official M3 Expressive shape-morphing loading indicator specification
 * (https://m3.material.io/components/loading-indicator/overview).
 *
 * Continuously and smoothly morphs between canonical M3 Expressive geometric shapes:
 * 1. 4-lobed Clover / Flower (signature M3 shape)
 * 2. Circle
 * 3. Scalloped Cookie / 8-pointed Petal
 * 4. Rounded Squircle / Diamond
 *
 * Features fluid rotation, organic spring/ease morphing, and subtle breathing scale.
 * Supports both solid filled shapes (canonical) and stroked outline styles.
 */
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    color: Color? = null,
    strokeWidth: Dp = 3.dp,
    trackColor: Color? = null,
    isFilled: Boolean = true,
    hasTrack: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "m3_expressive_loading_indicator")

    // Continuous 4-phase shape morph progression (0 -> 4)
    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "morphProgress"
    )

    // Smooth continuous rotation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val activeColor = color ?: PinkPrimary
    val resolvedTrackColor = trackColor ?: activeColor.copy(alpha = 0.16f)

    Canvas(
        modifier = modifier.size(size)
    ) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val strokePx = strokeWidth.toPx()
        val baseRadius = if (isFilled) {
            (kotlin.math.min(w, h) / 2f) * 0.92f
        } else {
            (kotlin.math.min(w, h) / 2f) - (strokePx * 1.15f)
        }

        if (baseRadius <= 0f) return@Canvas

        // 1. Draw subtle background track ring if enabled
        if (hasTrack) {
            drawCircle(
                color = resolvedTrackColor,
                radius = baseRadius * 0.88f,
                center = Offset(cx, cy),
                style = Stroke(width = strokePx * 0.75f, cap = StrokeCap.Round)
            )
        }

        // 2. Compute current morph parameters
        val phase = morphProgress.toInt() % 4
        val nextPhase = (phase + 1) % 4
        val fraction = morphProgress - morphProgress.toInt()
        val easedFraction = FastOutSlowInEasing.transform(fraction)

        // Subtle expressive breathing scale
        val breathingScale = 0.94f + 0.06f * sin(morphProgress * Math.PI.toFloat())

        // 3. Build smooth morphing shape path
        val path = Path()
        val numSteps = 120
        val stepAngle = (2 * Math.PI / numSteps).toFloat()

        for (i in 0..numSteps) {
            val theta = i * stepAngle
            val rA = getM3ShapeRadius(phase, theta, baseRadius)
            val rB = getM3ShapeRadius(nextPhase, theta, baseRadius)
            val r = rA + (rB - rA) * easedFraction

            val x = cx + r * cos(theta)
            val y = cy + r * sin(theta)

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()

        // 4. Render the expressive rotating & morphing shape (solid or outline)
        rotate(rotationAngle, pivot = Offset(cx, cy)) {
            scale(breathingScale, pivot = Offset(cx, cy)) {
                if (isFilled) {
                    drawPath(
                        path = path,
                        color = activeColor
                    )
                } else {
                    drawPath(
                        path = path,
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
    }
}

/**
 * Calculates the radius at angle [theta] for canonical Material Design 3 Expressive shapes:
 * - 0: 4-lobed Clover / Flower (signature M3 shape)
 * - 1: Circle
 * - 2: Scalloped Cookie / 8-lobed Flower
 * - 3: Rounded Squircle / Diamond
 */
private fun getM3ShapeRadius(shapeIndex: Int, theta: Float, baseRadius: Float): Float {
    return when (shapeIndex) {
        0 -> {
            // Shape 0: 4-lobed Clover / Flower
            baseRadius * (0.76f + 0.24f * cos(4f * theta))
        }
        1 -> {
            // Shape 1: Smooth Circle
            baseRadius * 0.88f
        }
        2 -> {
            // Shape 2: Scalloped Cookie / 8-pointed Petal
            baseRadius * (0.82f + 0.18f * cos(8f * theta))
        }
        3 -> {
            // Shape 3: Rounded Squircle / Soft Diamond
            val cosT = cos(theta)
            val sinT = sin(theta)
            val cos4 = cosT * cosT * cosT * cosT
            val sin4 = sinT * sinT * sinT * sinT
            val denom = sqrt(sqrt(cos4 + sin4 + 0.0001f))
            val squircleFactor = (1f / denom).coerceIn(0.85f, 1.22f)
            baseRadius * 0.72f * squircleFactor
        }
        else -> baseRadius * 0.88f
    }
}
