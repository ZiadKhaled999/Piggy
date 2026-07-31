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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var streakPair by remember { mutableStateOf(StreakManager.getStreakAndFrozenDates(context)) }
    val currentStreak = streakPair.first
    val frozenDates = streakPair.second
    val longestStreak = remember(currentStreak) { StreakManager.getLongestStreak(context) }
    val hasActionToday = remember(currentStreak) { StreakManager.hasActionToday(context) }

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
                .padding(horizontal = 20.dp)
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

            Spacer(modifier = Modifier.height(28.dp))

            // Longest Streak Card (Glassmorphic with horizontal gradient from left to right)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Color(0xFFFED7AA).copy(alpha = 0.8f)),
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFF7ED),
                                    Color(0xFFFFEDD5),
                                    Color(0xFFFED7AA).copy(alpha = 0.7f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    Color.White.copy(alpha = 0.5f),
                                    RoundedCornerShape(18.dp)
                                )
                                .border(1.dp, Color(0xFFFED7AA), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.streak),
                                contentDescription = "Longest Streak",
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(18.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Longest Streak",
                                color = Color(0xFF9A3412),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "$longestStreak days",
                                color = Color(0xFF7C2D12),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Monthly Streak Calendar Grid
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    val currentCal = Calendar.getInstance()
                    val monthNameFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
                    val monthTitle = monthNameFormat.format(currentCal.time)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = monthTitle,
                            color = Color(0xFF0F172A),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Legend row (Streak, Freeze, Missed)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.streak),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text("Streak", fontSize = 10.sp, color = Color(0xFF64748B))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.streak_frozen),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text("Freeze", fontSize = 10.sp, color = Color(0xFF64748B))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.streak_missed),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text("Missed", fontSize = 10.sp, color = Color(0xFF64748B))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Day Name Headers (Mon - Sun)
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
                                modifier = Modifier.width(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Month days computation
                    val cal = Calendar.getInstance()
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
                                    .height(36.dp)
                            ) {
                                val colWidth = maxWidth / 7

                                // Background pills for streak segments (contiguous days connected)
                                streakSegments.forEach { (startCol, endCol) ->
                                    val pillWidth = colWidth * (endCol - startCol) + 36.dp
                                    val startOffset = colWidth * startCol + (colWidth - 36.dp) / 2
                                    Box(
                                        modifier = Modifier
                                            .offset(x = startOffset)
                                            .width(pillWidth)
                                            .height(36.dp)
                                            .background(Color(0xFFFFF7ED), RoundedCornerShape(18.dp))
                                            .border(1.dp, Color(0xFFFDBA74), RoundedCornerShape(18.dp))
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                ) {
                                    for (colIndex in 0 until 7) {
                                        val cell = rowCells[colIndex]
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (cell != null) {
                                                when {
                                                    cell.isCompleted -> {
                                                        Image(
                                                            painter = painterResource(id = R.drawable.streak),
                                                            contentDescription = "Active Streak",
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                    cell.isFrozen -> {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .background(Color(0xFFF0F9FF), CircleShape)
                                                                .border(1.dp, Color(0xFF7DD3FC), CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Image(
                                                                painter = painterResource(id = R.drawable.streak_frozen),
                                                                contentDescription = "Frozen Streak",
                                                                modifier = Modifier.size(22.dp)
                                                            )
                                                        }
                                                    }
                                                    cell.isMissed -> {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .background(Color(0xFFFEF2F2), CircleShape)
                                                                .border(1.dp, Color(0xFFFCA5A5), CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Image(
                                                                painter = painterResource(id = R.drawable.streak_missed),
                                                                contentDescription = "Missed Streak",
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                    else -> {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .background(
                                                                    if (cell.isToday) Color(0xFFFFF7ED) else Color(0xFFF8FAFC),
                                                                    CircleShape
                                                                )
                                                                .border(
                                                                    width = if (cell.isToday) 2.dp else 1.dp,
                                                                    color = if (cell.isToday) Color(0xFFFF7A00) else Color(0xFFE2E8F0),
                                                                    shape = CircleShape
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "${cell.dayNum}",
                                                                fontSize = 11.sp,
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

            // Primary Log Action Button
            Button(
                onClick = {
                    StreakManager.recordAction(context)
                    streakPair = StreakManager.getStreakAndFrozenDates(context)
                    Toast.makeText(context, "Streak recorded for today! 🔥", Toast.LENGTH_SHORT).show()
                },
                enabled = !hasActionToday,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF7A00),
                    disabledContainerColor = Color(0xFFE2E8F0),
                    contentColor = Color.White,
                    disabledContentColor = Color(0xFF94A3B8)
                )
            ) {
                Icon(
                    imageVector = if (hasActionToday) Icons.Default.Check else Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (hasActionToday) "Streak Maintained Today 🎉" else "Record Today's Action 🔥",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
