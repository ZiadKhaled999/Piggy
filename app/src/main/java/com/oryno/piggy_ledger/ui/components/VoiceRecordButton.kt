package com.oryno.piggy_ledger.ui.components

import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.ui.theme.AccentBlue
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import kotlin.random.Random

enum class VoiceRecordState {
    IDLE, RECORDING, CANCELLING
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VoiceRecordButton(
    modifier: Modifier = Modifier,
    onRecordStart: () -> Unit,
    onRecordSend: () -> Unit,
    onRecordCancel: () -> Unit
) {
    var isSheetOpen by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf(VoiceRecordState.IDLE) }
    var initialTouchY by remember { mutableStateOf(0f) }
    val haptic = LocalHapticFeedback.current

    val cancelThreshold = -200f // Slide up pixels to trigger cancel/pause

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isSheetOpen = true
        } else {
            Toast.makeText(context, "Microphone permission is required to record voice", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Full screen dialog for the bottom sheet so it breaks out of the NavHost padding
        AnimatedVisibility(
            visible = isSheetOpen,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isSheetOpen = false }
            )
        }

        AnimatedVisibility(
            visible = isSheetOpen,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            // The Bottom Sheet UI
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp) // Increased height to look like a massive real bottom sheet
                    .shadow(24.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color.White, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .pointerInteropFilter { event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                state = VoiceRecordState.RECORDING
                                initialTouchY = event.rawY
                                onRecordStart()
                                true
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val currentY = event.rawY
                                val dragOffset = currentY - initialTouchY
                                
                                if (dragOffset < cancelThreshold && state != VoiceRecordState.CANCELLING) {
                                    state = VoiceRecordState.CANCELLING
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } else if (dragOffset >= cancelThreshold && state == VoiceRecordState.CANCELLING) {
                                    state = VoiceRecordState.RECORDING
                                }
                                true
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                if (state == VoiceRecordState.CANCELLING || event.action == MotionEvent.ACTION_CANCEL) {
                                    onRecordCancel()
                                } else if (state == VoiceRecordState.RECORDING) {
                                    onRecordSend()
                                }
                                state = VoiceRecordState.IDLE
                                isSheetOpen = false
                                true
                            }
                            else -> false
                        }
                    }
            ) {
                val isCancelling = state == VoiceRecordState.CANCELLING
                val textColor by animateColorAsState(if (isCancelling) Color(0xFFFF5252) else Color(0xFF1E293B))
                val waveColor by animateColorAsState(if (isCancelling) Color(0xFFFF5252) else AccentBlue)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (state == VoiceRecordState.RECORDING) AccentBlue.copy(alpha = 0.05f) else Color.Transparent)
                        .navigationBarsPadding()
                        .padding(vertical = 48.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isCancelling) "Release to cancel" else "Slide up to pause/cancel",
                        color = textColor,
                        fontSize = 24.sp, // Highlighted heading
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        AudioWaveform(color = waveColor, isPlaying = state == VoiceRecordState.RECORDING)
                    }

                    Text(
                        text = if (state == VoiceRecordState.RECORDING) "Recording..." else "Hold anywhere here to record",
                        color = if (state == VoiceRecordState.RECORDING) AccentBlue else Color(0xFF64748B),
                        fontSize = 22.sp, // Highlighted notes text
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = androidx.compose.ui.res.stringResource(com.oryno.piggy_ledger.R.string.voice_prepair_words),
                        color = Color(0xFF64748B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // The floating Mic Button (only visible when sheet is closed)
        AnimatedVisibility(
            visible = !isSheetOpen,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(PinkPrimary)
                    .clickable { 
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            isSheetOpen = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Open Voice Record",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun AudioWaveform(color: Color, isPlaying: Boolean) {
    // A simple mock audio waveform
    val bars = 25
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(100.dp)
    ) {
        for (i in 0 until bars) {
            val infiniteTransition = rememberInfiniteTransition()
            val targetHeight = if (isPlaying) Random.nextInt(10, 100).toFloat() else 6f
            
            val height by infiniteTransition.animateFloat(
                initialValue = 6f,
                targetValue = targetHeight,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = Random.nextInt(400, 800), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "waveform_height"
            )

            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(if (isPlaying) height.dp else 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}
