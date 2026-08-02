package com.oryno.piggy_ledger.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight

fun parseHexColor(hex: String?, fallback: Color = Color(0xFFE11D48)): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val clean = hex.trim().removePrefix("#")
        val colorInt = when (clean.length) {
            6 -> android.graphics.Color.parseColor("#$clean")
            8 -> android.graphics.Color.parseColor("#$clean")
            3 -> {
                val expanded = clean.map { "$it$it" }.joinToString("")
                android.graphics.Color.parseColor("#$expanded")
            }
            else -> android.graphics.Color.parseColor("#E11D48")
        }
        Color(colorInt)
    } catch (e: Exception) {
        fallback
    }
}

fun Color.toHex(): String {
    val red = (this.red * 255).toInt().coerceIn(0, 255)
    val green = (this.green * 255).toInt().coerceIn(0, 255)
    val blue = (this.blue * 255).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", red, green, blue)
}

fun getAccountGradient(hexColor: String?): Brush {
    val baseColor = parseHexColor(hexColor)
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(baseColor.toArgb(), hsv)

    val darkHsv = hsv.clone().apply {
        this[2] = (this[2] * 0.70f).coerceIn(0f, 1f)
        this[1] = (this[1] * 1.15f).coerceIn(0f, 1f)
    }
    val darkColor = Color(android.graphics.Color.HSVToColor(darkHsv))

    val lightHsv = hsv.clone().apply {
        this[2] = (this[2] * 1.30f).coerceIn(0f, 1f)
        this[1] = (this[1] * 0.70f).coerceIn(0f, 1f)
    }
    val lightColor = Color(android.graphics.Color.HSVToColor(lightHsv))

    return Brush.linearGradient(
        colors = listOf(darkColor, baseColor, lightColor)
    )
}

data class ColorPreset(val hex: String, val name: String)

val mainPresetColors = listOf(
    ColorPreset("#E11D48", "Crimson"),
    ColorPreset("#3B82F6", "Ocean Blue"),
    ColorPreset("#8B5CF6", "Royal Violet"),
    ColorPreset("#10B981", "Emerald"),
    ColorPreset("#F59E0B", "Amber Gold"),
    ColorPreset("#EA580C", "Sunset Orange"),
    ColorPreset("#EC4899", "Hot Pink"),
    ColorPreset("#06B6D4", "Cyan Teal"),
    ColorPreset("#6366F1", "Indigo"),
    ColorPreset("#1E293B", "Slate"),
    ColorPreset("#4C1D95", "Deep Midnight"),
    ColorPreset("#84CC16", "Lime Green"),
    ColorPreset("#F43F5E", "Rose Coral")
)

val extendedPalettePresets = listOf(
    "#EF4444", "#F97316", "#F59E0B", "#10B981", "#06B6D4", "#3B82F6", "#6366F1", "#8B5CF6", "#D946EF", "#EC4899",
    "#DC2626", "#EA580C", "#D97706", "#059669", "#0891B2", "#2563EB", "#4F46E5", "#7C3AED", "#C026D3", "#DB2777",
    "#991B1B", "#9A3412", "#92400E", "#065F46", "#155E75", "#1E40AF", "#3730A3", "#5B21B6", "#86198F", "#9F1239",
    "#0F172A", "#18181B", "#27272A", "#334155", "#475569", "#64748B", "#71717A", "#94A3B8", "#A1A1AA", "#CBD5E1"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardColorPickerBottomSheet(
    account: Account?,
    onDismiss: () -> Unit,
    onSelectColor: (String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val initialHex = account?.icon_color?.takeIf { it.isNotBlank() } ?: "#E11D48"
    var selectedHex by remember { mutableStateOf(initialHex) }
    var hexInputText by remember { mutableStateOf(initialHex) }
    var showCustomPanel by remember { mutableStateOf(false) }

    // HSV State for Custom Color Picker Panel
    val initialColor = parseHexColor(initialHex)
    val initialHsv = FloatArray(3).apply { android.graphics.Color.colorToHSV(initialColor.toArgb(), this) }
    var hue by remember { FloatStateOption(initialHsv[0]) }
    var saturation by remember { FloatStateOption(initialHsv[1]) }
    var brightness by remember { FloatStateOption(initialHsv[2]) }

    fun updateColorFromHsv(h: Float, s: Float, v: Float) {
        val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
        val newHex = Color(colorInt).toHex()
        selectedHex = newHex
        hexInputText = newHex
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFCBD5E1)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = null,
                            tint = PinkPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.customize_card_color),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = NavyDark
                        )
                    }
                    Text(
                        text = stringResource(R.string.change_card_color_desc, account?.name ?: "Piggy Wallet"),
                        fontSize = 13.sp,
                        color = TextLight,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(Color(0xFFF1F5F9), CircleShape)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = NavyDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Live Mini Card Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(getAccountGradient(selectedHex))
                            .padding(16.dp)
                    ) {
                        // Background decor
                        Box(
                            modifier = Modifier
                                .offset(x = 180.dp, y = (-20).dp)
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                        )

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = (account?.name ?: "PIGGY CARD").uppercase(),
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = selectedHex.uppercase(),
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "$${String.format("%,.2f", account?.current_balance ?: 0.0)}",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black
                                )

                                Text(
                                    text = (account?.provider ?: "WALLET").uppercase(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))

            // Section: Color Selector (Presets vs Custom Panel)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showCustomPanel) stringResource(R.string.custom_color_picker) else stringResource(R.string.main_colors),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = NavyDark
                    )

                    TextButton(
                        onClick = { showCustomPanel = !showCustomPanel },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = if (showCustomPanel) Icons.Default.GridOn else Icons.Default.Palette,
                            contentDescription = null,
                            tint = PinkPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showCustomPanel) "Presets" else stringResource(R.string.more_colors),
                            color = PinkPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Row of main colors (Hidden when showCustomPanel is true)
                AnimatedVisibility(
                    visible = !showCustomPanel,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(mainPresetColors) { preset ->
                            val isSelected = selectedHex.equals(preset.hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(preset.hex))
                                    .clickable {
                                        selectedHex = preset.hex
                                        hexInputText = preset.hex
                                        val colorInt = parseHexColor(preset.hex).toArgb()
                                        val hsv = FloatArray(3)
                                        android.graphics.Color.colorToHSV(colorInt, hsv)
                                        hue = hsv[0]
                                        saturation = hsv[1]
                                        brightness = hsv[2]
                                    }
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) NavyDark else Color.White,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // "+ More" Button at the end of the main colors row
                        item {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(
                                                Color(0xFFEF4444),
                                                Color(0xFFF59E0B),
                                                Color(0xFF10B981),
                                                Color(0xFF06B6D4),
                                                Color(0xFF3B82F6),
                                                Color(0xFF8B5CF6),
                                                Color(0xFFEC4899),
                                                Color(0xFFEF4444)
                                            )
                                        )
                                    )
                                    .clickable { showCustomPanel = true }
                                    .padding(2.5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = "More Colors",
                                            tint = NavyDark,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Custom Color Picker Panel (When "More" is toggled or opened)
            AnimatedVisibility(
                visible = showCustomPanel,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.custom_color_picker),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = NavyDark
                            )

                            // 2D Saturation & Brightness Canvas
                            val pureHueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(pureHueColor)
                                    .pointerInput(hue) {
                                        detectTapGestures { offset ->
                                            val newSat = (offset.x / size.width).coerceIn(0f, 1f)
                                            val newBright = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                                            saturation = newSat
                                            brightness = newBright
                                            updateColorFromHsv(hue, newSat, newBright)
                                        }
                                    }
                            ) {
                                // Saturation overlay (White -> Transparent)
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color.White, Color.Transparent)
                                        )
                                    )
                                    // Brightness overlay (Transparent -> Black)
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black)
                                        )
                                    )
                                }

                                // Selector Indicator Pin
                                val selectorX = saturation * 100 // dynamic normalized visually
                                Box(
                                    modifier = Modifier
                                        .align(
                                            androidx.compose.ui.BiasAlignment(
                                                horizontalBias = (saturation * 2f) - 1f,
                                                verticalBias = ((1f - brightness) * 2f) - 1f
                                            )
                                        )
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(2.dp, NavyDark, CircleShape)
                                )
                            }

                            // Rainbow Hue Slider
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Hue Spectrum",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextLight
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Slider(
                                        value = hue,
                                        onValueChange = { newHue ->
                                            hue = newHue
                                            updateColorFromHsv(newHue, saturation, brightness)
                                        },
                                        valueRange = 0f..360f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.Transparent,
                                            inactiveTrackColor = Color.Transparent
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Extended Swatch Grid
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = stringResource(R.string.preset_palettes),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextLight
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    extendedPalettePresets.forEach { hex ->
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(parseHexColor(hex))
                                                .clickable {
                                                    selectedHex = hex
                                                    hexInputText = hex
                                                    val colorInt = parseHexColor(hex).toArgb()
                                                    val hsv = FloatArray(3)
                                                    android.graphics.Color.colorToHSV(colorInt, hsv)
                                                    hue = hsv[0]
                                                    saturation = hsv[1]
                                                    brightness = hsv[2]
                                                }
                                                .border(
                                                    width = if (selectedHex.equals(hex, ignoreCase = true)) 2.dp else 0.dp,
                                                    color = NavyDark,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Apply Button
            Button(
                onClick = {
                    val finalHex = if (selectedHex.startsWith("#")) selectedHex else "#$selectedHex"
                    onSelectColor(finalHex)
                    ToastUtil.show(context, context.getString(R.string.card_color_updated), Toast.LENGTH_SHORT)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.apply_color),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// Helper class for state handling
private class FloatStateOption(initialValue: Float) : MutableState<Float> {
    private val state = mutableFloatStateOf(initialValue)
    override var value: Float
        get() = state.floatValue
        set(v) { state.floatValue = v }
    override operator fun component1(): Float = value
    override operator fun component2(): (Float) -> Unit = { value = it }
}
