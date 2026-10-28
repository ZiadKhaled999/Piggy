package com.oryno.piggy_ledger.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.Goal
import com.oryno.piggy_ledger.ui.theme.NavyDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositBottomSheet(
    goal: Goal,
    currentSavedAmount: Double,
    currencySymbol: String,
    onConfirm: (amount: Double, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }
    var confirmedAmount by remember { mutableDoubleStateOf(0.0) }
    var isNoteFocused by remember { mutableStateOf(false) }
    val noteFocusRequester = remember { FocusRequester() }

    val currentAmount = amountStr.toDoubleOrNull() ?: 0.0

    // Auto-dismiss and commit after success screen
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            delay(1700)
            onConfirm(confirmedAmount, note)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (isSuccess) {
                onConfirm(confirmedAmount, note)
            }
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = if (isSuccess) Color(0xFF10B981) else MaterialTheme.colorScheme.surface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        AnimatedContent(
            targetState = isSuccess,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
            },
            label = "DepositSheetTransition"
        ) { targetSuccess ->
            if (targetSuccess) {
                // Success Screen (Vivid Green Checkmark State)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 520.dp)
                        .background(Color(0xFF10B981))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onConfirm(confirmedAmount, note)
                            onDismiss()
                        }
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.weight(1f))

                        // Large White Checkmark Icon
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Success",
                                tint = Color.White,
                                modifier = Modifier.size(54.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Stamped Deposited Amount
                        Text(
                            text = "$currencySymbol ${String.format(Locale.US, "%,.2f", confirmedAmount)}",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Goal name
                        Text(
                            text = goal.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )

                        if (note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "“$note”",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.weight(1.2f))

                        // Tap anywhere to close text
                        Text(
                            text = stringResource(R.string.tap_anywhere_to_close),
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Keypad & Entry Screen
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp)
                        .padding(top = 12.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Drag Pill Indicator
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFCBD5E1))
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Top Header: Goal Name & Current Balance
                    Text(
                        text = "${goal.name} • $currencySymbol${String.format(Locale.US, "%,.2f", currentSavedAmount)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 2. Hero Amount Display
                    val displayAmount = if (amountStr.isEmpty()) "0" else amountStr
                    val amountColor = if (currentAmount > 0.0) Color(0xFF15803D) else NavyDark

                    Text(
                        text = "$currencySymbol $displayAmount",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Distinguished Off-White Note Field with Smooth Rounded Corners & Click State
                    val noteBgColor by animateColorAsState(
                        targetValue = if (isNoteFocused) Color(0xFFF9FBFC) else Color(0xFFF1F3F6),
                        animationSpec = tween(200),
                        label = "NoteBgColor"
                    )
                    val noteBorderColor by animateColorAsState(
                        targetValue = if (isNoteFocused) Color(0xFF10B981) else Color(0xFFE2E6ED),
                        animationSpec = tween(200),
                        label = "NoteBorderColor"
                    )

                    Box(
                        modifier = Modifier
                            .wrapContentWidth()
                            .widthIn(min = 210.dp, max = 310.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(noteBgColor)
                            .border(
                                border = BorderStroke(
                                    width = if (isNoteFocused) 1.5.dp else 1.dp,
                                    color = noteBorderColor
                                ),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                noteFocusRequester.requestFocus()
                            }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = if (isNoteFocused) Color(0xFF10B981) else Color(0xFF94A3B8),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            BasicTextField(
                                value = note,
                                onValueChange = { if (it.length <= 36) note = it },
                                singleLine = true,
                                modifier = Modifier
                                    .focusRequester(noteFocusRequester)
                                    .onFocusChanged { isNoteFocused = it.isFocused },
                                textStyle = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NavyDark,
                                    textAlign = TextAlign.Center
                                ),
                                cursorBrush = SolidColor(Color(0xFF10B981)),
                                decorationBox = { innerTextField ->
                                    if (note.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.note_placeholder_deposit),
                                            fontSize = 13.sp,
                                            color = Color(0xFF94A3B8),
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            if (note.isNotEmpty() && isNoteFocused) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear note",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { note = "" }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 4. Swipe Right to Confirm Area
                    SwipeRightToConfirmBar(
                        isEnabled = currentAmount > 0.0,
                        onConfirmed = {
                            if (currentAmount > 0.0) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                confirmedAmount = currentAmount
                                isSuccess = true
                            } else {
                                ToastUtil.show(context, context.getString(R.string.enter_amount_first), Toast.LENGTH_SHORT)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 5. Sleek, Smooth Custom Numeric Keypad
                    CustomNumericKeypad(
                        onDigitClick = { digit ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (amountStr.length < 9) {
                                if (amountStr == "0") {
                                    amountStr = digit.toString()
                                } else {
                                    val parts = amountStr.split(".")
                                    if (parts.size == 2 && parts[1].length >= 2) {
                                        // Max 2 decimal digits
                                    } else {
                                        amountStr += digit
                                    }
                                }
                            }
                        },
                        onDecimalClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (!amountStr.contains(".")) {
                                amountStr = if (amountStr.isEmpty()) "0." else "$amountStr."
                            }
                        },
                        onBackspaceClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (amountStr.isNotEmpty()) {
                                amountStr = amountStr.dropLast(1)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Modern "Swipe right to confirm" track with a smooth sliding thumb and fill effect.
 */
@Composable
private fun SwipeRightToConfirmBar(
    isEnabled: Boolean,
    onConfirmed: () -> Unit
) {
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        val density = LocalDensity.current
        var trackWidthPx by remember { mutableFloatStateOf(0f) }
        val thumbSizeDp = 48.dp
        val thumbSizePx = with(density) { thumbSizeDp.toPx() }
        val maxDragPx = (trackWidthPx - thumbSizePx - with(density) { 8.dp.toPx() }).coerceAtLeast(0f)

        val dragOffset = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()

        val progress = if (maxDragPx > 0f) (dragOffset.value / maxDragPx).coerceIn(0f, 1f) else 0f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFFF1F5F9))
                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(28.dp))
                .onSizeChanged { size ->
                    trackWidthPx = size.width.toFloat()
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Green fill behind thumb as it slides
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(with(density) { (dragOffset.value + thumbSizePx + with(density) { 4.dp.toPx() }).toDp() })
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.15f + (progress * 0.45f)))
            )

            // Center Guide Text
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.swipe_right_to_confirm),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isEnabled) Color(0xFF475569) else Color(0xFF94A3B8),
                    modifier = Modifier.padding(start = 24.dp)
                )
            }

            // Draggable Arrow Thumb
            Box(
                modifier = Modifier
                    .offset { IntOffset(dragOffset.value.roundToInt() + with(density) { 4.dp.roundToPx() }, 0) }
                    .size(thumbSizeDp)
                    .shadow(elevation = 3.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(if (progress > 0.8f) Color(0xFF10B981) else Color.White)
                    .pointerInput(maxDragPx, isEnabled) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    if (maxDragPx > 0f && dragOffset.value >= maxDragPx * 0.65f) {
                                        // Successfully completed swipe
                                        dragOffset.animateTo(
                                            targetValue = maxDragPx,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                        )
                                        onConfirmed()
                                        // Reset back after confirmation triggers
                                        delay(200)
                                        dragOffset.snapTo(0f)
                                    } else {
                                        // Snap back
                                        dragOffset.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                        )
                                    }
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    val newOffset = (dragOffset.value + dragAmount).coerceIn(0f, maxDragPx)
                                    dragOffset.snapTo(newOffset)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (progress > 0.8f) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Swipe to confirm",
                    tint = if (progress > 0.8f) Color.White else Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Ultra-smooth, rounded capsule numeric keypad matching modern fintech designs.
 */
@Composable
private fun CustomNumericKeypad(
    onDigitClick: (Int) -> Unit,
    onDecimalClick: () -> Unit,
    onBackspaceClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x00F8FAFC),
                        Color(0xFFF8FAFC).copy(alpha = 0.5f)
                    )
                )
            )
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: 1, 2, 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmoothKeypadPillButton(text = "1", modifier = Modifier.weight(1f)) { onDigitClick(1) }
            SmoothKeypadPillButton(text = "2", modifier = Modifier.weight(1f)) { onDigitClick(2) }
            SmoothKeypadPillButton(text = "3", modifier = Modifier.weight(1f)) { onDigitClick(3) }
        }

        // Row 2: 4, 5, 6
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmoothKeypadPillButton(text = "4", modifier = Modifier.weight(1f)) { onDigitClick(4) }
            SmoothKeypadPillButton(text = "5", modifier = Modifier.weight(1f)) { onDigitClick(5) }
            SmoothKeypadPillButton(text = "6", modifier = Modifier.weight(1f)) { onDigitClick(6) }
        }

        // Row 3: 7, 8, 9
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmoothKeypadPillButton(text = "7", modifier = Modifier.weight(1f)) { onDigitClick(7) }
            SmoothKeypadPillButton(text = "8", modifier = Modifier.weight(1f)) { onDigitClick(8) }
            SmoothKeypadPillButton(text = "9", modifier = Modifier.weight(1f)) { onDigitClick(9) }
        }

        // Row 4: ., 0, ⌫
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmoothKeypadPillButton(
                text = ".",
                modifier = Modifier.weight(1f),
                isDot = true
            ) { onDecimalClick() }
            SmoothKeypadPillButton(text = "0", modifier = Modifier.weight(1f)) { onDigitClick(0) }
            SmoothKeypadIconButton(
                modifier = Modifier.weight(1f),
                onClick = onBackspaceClick
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = Color(0xFF334155),
                    modifier = Modifier.size(23.dp)
                )
            }
        }
    }
}

@Composable
private fun SmoothKeypadPillButton(
    text: String,
    modifier: Modifier = Modifier,
    isDot: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "KeypadScale"
    )

    val buttonBgColor by animateColorAsState(
        targetValue = if (isPressed) Color(0xFFDFE4EA) else Color(0xFFEFF2F6),
        animationSpec = tween(100),
        label = "KeypadBgColor"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(58.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(29.dp),
        color = buttonBgColor,
        contentColor = Color(0xFF0F172A),
        interactionSource = interactionSource,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = if (isDot) 28.sp else 25.sp,
                fontWeight = if (isDot) FontWeight.Bold else FontWeight.SemiBold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SmoothKeypadIconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "KeypadIconScale"
    )

    val buttonBgColor by animateColorAsState(
        targetValue = if (isPressed) Color(0xFFDFE4EA) else Color(0xFFEFF2F6),
        animationSpec = tween(100),
        label = "KeypadIconBgColor"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(58.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(29.dp),
        color = buttonBgColor,
        contentColor = Color(0xFF0F172A),
        interactionSource = interactionSource,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
