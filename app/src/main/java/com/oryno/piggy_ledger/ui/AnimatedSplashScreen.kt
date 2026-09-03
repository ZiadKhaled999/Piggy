package com.oryno.piggy_ledger.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import kotlinx.coroutines.delay

@Composable
fun AnimatedSplashScreen(onSplashFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    
    val pathProgress = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "path_progress"
    )

    var startFadeOut by remember { mutableStateOf(false) }

    val fadeOut = animateFloatAsState(
        targetValue = if (startFadeOut) 0f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "fade_out"
    )

    LaunchedEffect(Unit) {
        delay(300)
        startAnimation = true
        delay(1500)
        startFadeOut = true
        delay(500)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PinkPrimary)
            .graphicsLayer(alpha = fadeOut.value)
    ) {
        // Image perfectly centered to match native splash screen
        Image(
            painter = painterResource(id = R.drawable.img_app_logo),
            contentDescription = "Logo",
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.Center)
        )
        
        // Canvas positioned exactly below the image
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 130.dp)
        ) {
            Canvas(modifier = Modifier.width(320.dp).height(100.dp)) {
                val paint = android.graphics.Paint().apply {
                    textSize = 120f
                    typeface = android.graphics.Typeface.create("cursive", android.graphics.Typeface.BOLD_ITALIC)
                    style = android.graphics.Paint.Style.STROKE
                    strokeJoin = android.graphics.Paint.Join.ROUND
                    strokeCap = android.graphics.Paint.Cap.ROUND
                }
                val text = "Piggy Ledger"
                
                // Measure text to center it perfectly
                val textBounds = android.graphics.Rect()
                paint.getTextBounds(text, 0, text.length, textBounds)
                
                val xOffset = (size.width - textBounds.width()) / 2f - textBounds.left
                val yOffset = (size.height + textBounds.height()) / 2f - textBounds.bottom
                
                val textPath = android.graphics.Path()
                paint.getTextPath(text, 0, text.length, xOffset, yOffset, textPath)
                
                val composePath = textPath.asComposePath()
                val pathMeasure = android.graphics.PathMeasure(textPath, false)
                
                val drawnPath = Path()
                
                // Note: a text path consists of multiple contours (one per letter/part)
                // We need to iterate through all contours
                var currentContour = 0
                val totalLength = run {
                    var len = 0f
                    val pm = android.graphics.PathMeasure(textPath, false)
                    do {
                        len += pm.length
                    } while (pm.nextContour())
                    len
                }
                
                val targetLength = totalLength * pathProgress.value
                var currentLength = 0f
                
                val pm = android.graphics.PathMeasure(textPath, false)
                val tempPath = android.graphics.Path()
                
                do {
                    val contourLength = pm.length
                    if (currentLength + contourLength <= targetLength) {
                        pm.getSegment(0f, contourLength, tempPath, true)
                        currentLength += contourLength
                    } else if (currentLength < targetLength) {
                        pm.getSegment(0f, targetLength - currentLength, tempPath, true)
                        currentLength = targetLength
                        break
                    } else {
                        break
                    }
                } while (pm.nextContour())
                
                drawPath(
                    path = tempPath.asComposePath(),
                    color = Color.White,
                    style = Stroke(width = 5f, join = StrokeJoin.Round, cap = StrokeCap.Round)
                )
            }
        }
    }
}
