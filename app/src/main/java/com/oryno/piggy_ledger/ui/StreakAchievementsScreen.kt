package com.oryno.piggy_ledger.ui

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Comment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.graphics.Bitmap
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.StreakManager
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun StreakAchievementsScreen(
    viewModel: PiggyLedgerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val reactiveStreakCount by viewModel.streakCount.collectAsStateWithLifecycle()

    var streakPair by remember(reactiveStreakCount) { mutableStateOf(StreakManager.getStreakAndFrozenDates(context)) }
    val currentStreak = streakPair.first
    val frozenDates = streakPair.second
    val longestStreak = remember(currentStreak) { StreakManager.getLongestStreak(context) }
    val hasActionToday = remember(currentStreak) { StreakManager.hasActionToday(context) }
    var displayCalendar by remember { mutableStateOf(Calendar.getInstance()) }

    var showShareModal by remember { mutableStateOf(false) }
    var streakBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("streak.json"))
    val lottieProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    // Pulsing animation for flame glow
    val infiniteTransition = rememberInfiniteTransition(label = "FlamePulse")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FlameScale"
    )

    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 360

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isCompact) 8.dp else 12.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Achievements",
                    color = Color(0xFF0F172A),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Hero Flame Artwork with Glow
            Box(
                modifier = Modifier.size(190.dp),
                contentAlignment = Alignment.Center
            ) {
                // Glow layer 1
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(flameScale)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF7A00).copy(alpha = 0.25f),
                                    Color(0xFFFFD54F).copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )

                // Glow layer 2
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(flameScale * 0.98f)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD54F).copy(alpha = 0.35f),
                                    Color(0xFFFF7A00).copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )

                // Lottie Animation or Big 3D Flame Icon Fallback
                if (composition != null) {
                    LottieAnimation(
                        composition = composition,
                        progress = { lottieProgress },
                        modifier = Modifier
                            .size(180.dp)
                            .scale(flameScale)
                    )
                } else {
                    Text(
                        text = "🔥",
                        fontSize = 96.sp,
                        modifier = Modifier.scale(flameScale)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Streak Title
            Text(
                text = "$currentStreak-Day Streak!",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A),
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Motivational Subtitle
            Text(
                text = if (currentStreak >= 7) "A week of wins! Let's keep that flame alive 🔥"
                else if (currentStreak > 0) "Great consistency! Keep logging your finances daily"
                else "Start your streak today by logging a transaction or updating your ledger!",
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Top Details & Legend Bar (Border removed for seamless design, fully responsive)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isCompact) 8.dp else 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Streak Active
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.streak),
                            contentDescription = null,
                            modifier = Modifier.size(if (isCompact) 18.dp else 20.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "Streak",
                            fontSize = if (isCompact) 11.sp else 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                    }

                    // Streak Freezed
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.streak_frozen),
                            contentDescription = null,
                            modifier = Modifier.size(if (isCompact) 18.dp else 20.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "Freezed",
                            fontSize = if (isCompact) 11.sp else 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0284C7)
                        )
                    }

                    // Streak Missed
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.streak_missed),
                            contentDescription = null,
                            modifier = Modifier.size(if (isCompact) 18.dp else 20.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "Missed",
                            fontSize = if (isCompact) 11.sp else 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE11D48)
                        )
                    }

                    // Longest Streak (Compact Top Badge, border removed)
                    Surface(
                        color = Color(0xFFFFF7ED),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🏆 $longestStreak d",
                                fontSize = if (isCompact) 11.sp else 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC2410C)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Monthly Streak Calendar Grid (Border removed, full-width responsive canvas)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isCompact) 8.dp else 14.dp, vertical = 16.dp)
                ) {
                    val monthNameFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
                    val monthTitle = monthNameFormat.format(displayCalendar.time)

                    val todayCal = Calendar.getInstance()
                    val isCurrentMonth = displayCalendar.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                            displayCalendar.get(Calendar.MONTH) == todayCal.get(Calendar.MONTH)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val nextCal = displayCalendar.clone() as Calendar
                                nextCal.add(Calendar.MONTH, -1)
                                displayCalendar = nextCal
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous Month",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Text(
                            text = monthTitle,
                            color = Color(0xFF0F172A),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        IconButton(
                            onClick = {
                                if (!isCurrentMonth) {
                                    val nextCal = displayCalendar.clone() as Calendar
                                    nextCal.add(Calendar.MONTH, 1)
                                    displayCalendar = nextCal
                                }
                            },
                            enabled = !isCurrentMonth,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next Month",
                                tint = if (!isCurrentMonth) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Day Name Headers (Mon - Sun, balanced across 7 columns)
                    val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        daysOfWeek.forEach { dayLabel ->
                            Text(
                                text = dayLabel,
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Month days computation
                    val cal = displayCalendar.clone() as Calendar
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                    val offset = (firstDayOfWeek + 5) % 7

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val todayStr = dateFormat.format(Calendar.getInstance().time)
                    val actionDates = remember(currentStreak) { StreakManager.getActionDates(context) }

                    val totalGridCells = maxDays + offset
                    val numRows = (totalGridCells + 6) / 7

                    data class LocalDayCell(
                        val dayNum: Int,
                        val isCompleted: Boolean,
                        val isFrozen: Boolean,
                        val isToday: Boolean,
                        val isMissed: Boolean
                    )

                    val gridCells = List(totalGridCells) { index ->
                        if (index < offset || index - offset >= maxDays) {
                            null
                        } else {
                            val dayNum = index - offset + 1
                            cal.set(Calendar.DAY_OF_MONTH, dayNum)
                            val cellDateStr = dateFormat.format(cal.time)
                            val isCompleted = actionDates.contains(cellDateStr)
                            val isFrozen = frozenDates.contains(cellDateStr)
                            val isToday = cellDateStr == todayStr
                            val isPast = cellDateStr < todayStr
                            val isMissed = isPast && !isCompleted && !isFrozen
                            LocalDayCell(dayNum, isCompleted, isFrozen, isToday, isMissed)
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (rowIndex in 0 until numRows) {
                            val rowCells = (0 until 7).map { colIndex ->
                                val idx = rowIndex * 7 + colIndex
                                if (idx < gridCells.size) gridCells[idx] else null
                            }

                            // Contiguous streak segments in this row
                            val completedCols = (0 until 7).filter { col -> rowCells[col]?.isCompleted == true }
                            val streakSegments = mutableListOf<Pair<Int, Int>>()
                            if (completedCols.isNotEmpty()) {
                                var start = completedCols.first()
                                var prev = start
                                for (i in 1 until completedCols.size) {
                                    val curr = completedCols[i]
                                    if (curr == prev + 1) {
                                        prev = curr
                                    } else {
                                        streakSegments.add(Pair(start, prev))
                                        start = curr
                                        prev = curr
                                    }
                                }
                                streakSegments.add(Pair(start, prev))
                            }

                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                val colWidth = maxWidth / 7
                                val cellSize = minOf(colWidth * 0.88f, 38.dp)

                                // Background pills for streak segments (contiguous days connected, clean borderless highlight)
                                streakSegments.forEach { (startCol, endCol) ->
                                    val pillWidth = colWidth * (endCol - startCol) + cellSize
                                    val startOffset = colWidth * startCol + (colWidth - cellSize) / 2
                                    Box(
                                        modifier = Modifier
                                            .offset(x = startOffset)
                                            .width(pillWidth)
                                            .height(cellSize)
                                            .align(Alignment.CenterStart)
                                            .background(Color(0xFFFFF7ED), RoundedCornerShape(cellSize / 2))
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (colIndex in 0 until 7) {
                                        val cell = rowCells[colIndex]
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (cell != null) {
                                                when {
                                                    cell.isCompleted -> {
                                                        Image(
                                                            painter = painterResource(id = R.drawable.streak),
                                                            contentDescription = "Active Streak",
                                                            modifier = Modifier.size(minOf(cellSize * 0.88f, 34.dp))
                                                        )
                                                    }
                                                    cell.isFrozen -> {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(cellSize)
                                                                .background(Color(0xFFF0F9FF), CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Image(
                                                                painter = painterResource(id = R.drawable.streak_frozen),
                                                                contentDescription = "Frozen Streak",
                                                                modifier = Modifier.size(minOf(cellSize * 0.78f, 30.dp))
                                                            )
                                                        }
                                                    }
                                                    cell.isMissed -> {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(cellSize)
                                                                .background(Color(0xFFFEF2F2), CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Image(
                                                                painter = painterResource(id = R.drawable.streak_missed),
                                                                contentDescription = "Missed Streak",
                                                                modifier = Modifier.size(minOf(cellSize * 0.75f, 28.dp))
                                                            )
                                                        }
                                                    }
                                                    else -> {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(cellSize)
                                                                .background(
                                                                    if (cell.isToday) Color(0xFFFFF7ED) else Color(0xFFF8FAFC),
                                                                    CircleShape
                                                                )
                                                                .then(
                                                                    if (cell.isToday) Modifier.border(2.dp, Color(0xFFFF7A00), CircleShape)
                                                                    else Modifier
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "${cell.dayNum}",
                                                                fontSize = if (isCompact) 11.sp else 13.sp,
                                                                fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Medium,
                                                                color = if (cell.isToday) Color(0xFFFF7A00) else Color(0xFF94A3B8)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Share My Streak Button
            Button(
                onClick = {
                    streakBitmap = StreakShareHelper.createStreakImageBitmap(context, currentStreak)
                    showShareModal = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEC4899),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share My Streak",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Share My Streak",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Custom Share Preview Sheet Modal
    if (showShareModal && streakBitmap != null) {
        Dialog(
            onDismissRequest = { showShareModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { showShareModal = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = false) {},
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top area for centered preview image card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White,
                            shadowElevation = 16.dp,
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .wrapContentHeight()
                        ) {
                            Image(
                                bitmap = streakBitmap!!.asImageBitmap(),
                                contentDescription = "Streak Preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1080f / 1280f)
                                    .clip(RoundedCornerShape(24.dp))
                            )
                        }
                    }

                    // Bottom Share Sheet Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        color = Color.White
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Share your streak",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                IconButton(
                                    onClick = { showShareModal = false },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFF1F5F9), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color(0xFF334155),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Action Buttons Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Messages Button
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(Color(0xFF22C55E), CircleShape)
                                            .clickable {
                                                StreakShareHelper.shareToMessages(context, streakBitmap!!)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Comment,
                                            contentDescription = "Messages",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Messages",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF475569)
                                    )
                                }

                                // Save Image Button
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(Color(0xFFF1F5F9), CircleShape)
                                            .clickable {
                                                val saved = StreakShareHelper.saveImageToGallery(context, streakBitmap!!)
                                                if (saved) {
                                                    Toast.makeText(context, "Saved to Gallery! 📸", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Save Image",
                                            tint = Color(0xFF0F172A),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Save Image",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF475569)
                                    )
                                }

                                // More Button
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(Color(0xFFF1F5F9), CircleShape)
                                            .clickable {
                                                StreakShareHelper.shareNativeImage(context, streakBitmap!!)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreHoriz,
                                            contentDescription = "More",
                                            tint = Color(0xFF0F172A),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "More",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
