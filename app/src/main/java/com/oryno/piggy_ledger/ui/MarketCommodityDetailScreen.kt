package com.oryno.piggy_ledger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import com.oryno.piggy_ledger.ui.wealth.WealthViewModel
import java.util.Locale
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketCommodityDetailScreen(
    type: String, // "GOLD" or "SILVER"
    variant: String, // "24k", "999", etc.
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val dao = remember { PiggyLedgerDatabase.getInstance(context).piggyLedgerDao() }
    val viewModel: WealthViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return WealthViewModel(dao) as T
        }
    })

    val uiState by viewModel.uiState.collectAsState()
    val currentRate = when (type) {
        "GOLD" -> when (variant) {
            "24k" -> uiState.liveRates.gold24k
            "18k" -> uiState.liveRates.gold18k
            else -> uiState.liveRates.gold21k
        }
        "SILVER" -> when (variant) {
            "925" -> uiState.liveRates.silver925
            "900" -> uiState.liveRates.silver900
            else -> uiState.liveRates.silver999
        }
        else -> 0.0
    }

    val lastUpdatedSecs = (System.currentTimeMillis() - uiState.liveRates.lastUpdated) / 1000

    // Simulated market data points
    val random = remember(currentRate) { java.util.Random(currentRate.toLong()) }
    val high24h = currentRate * (1.0 + (random.nextDouble() * 0.04))
    val low24h = currentRate * (1.0 - (random.nextDouble() * 0.03))
    val spread = currentRate * 0.015 // 1.5% spread
    val volatility = 15.0 + (random.nextDouble() * 20.0)

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.commodity_detail_title, "$type $variant"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = NavyDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NavyDark)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchLivePrices(forceRefresh = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NavyDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Current Price Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = stringResource(R.string.live_market_info),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${String.format(Locale.getDefault(), "%,.2f", currentRate)} ${stringResource(R.string.currency_egp)}/g",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.updated_seconds_ago, lastUpdatedSecs.coerceAtLeast(0)),
                            fontSize = 12.sp,
                            color = TextLight
                        )
                    }
                }
            }

            // 2. Large Interactive Chart
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.price_history_24h),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        MarketCommodityChart(
                            basePrice = currentRate,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.h24_ago), fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.h12_ago), fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.now), fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // 3. Market Stats Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MarketStatCard(stringResource(R.string.highest_24h), high24h, Modifier.weight(1f))
                        MarketStatCard(stringResource(R.string.lowest_24h), low24h, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MarketStatCard(stringResource(R.string.market_spread), spread, Modifier.weight(1f))
                        MarketStatCard(stringResource(R.string.volatility_index), volatility, Modifier.weight(1f), isCurrency = false)
                    }
                }
            }

            // 4. Market Info Note
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = PinkPrimary.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, PinkPrimary.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.market_rates_source_note),
                            fontSize = 12.sp,
                            color = NavyDark.copy(alpha = 0.8f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketStatCard(
    label: String,
    value: Double,
    modifier: Modifier = Modifier,
    isCurrency: Boolean = true
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isCurrency) "${String.format(Locale.getDefault(), "%,.2f", value)} ${stringResource(R.string.currency_egp)}" else "${String.format(Locale.getDefault(), "%.1f", value)}%",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NavyDark
            )
        }
    }
}

@Composable
private fun MarketCommodityChart(
    basePrice: Double,
    modifier: Modifier = Modifier
) {
    val points = remember(basePrice) {
        val random = java.util.Random(basePrice.toLong())
        List(25) { idx ->
            val factor = 0.96 + (random.nextDouble() * 0.08)
            (basePrice * factor).toFloat()
        }
    }

    val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minVal = points.minOrNull() ?: 0f
            val maxVal = points.maxOrNull() ?: 1f
            val range = if (maxVal - minVal == 0f) 1f else maxVal - minVal

            // Leave space on the right for price labels (approx 70px)
            val rightPadding = 75.dp.toPx()
            val width = size.width - rightPadding
            val height = size.height
            val stepX = width / (points.size - 1)

            val path = Path()
            val fillPath = Path()

            points.forEachIndexed { index, value ->
                val rawX = index * stepX
                val x = if (isRtl) width - rawX else rawX
                val normalized = (value - minVal) / range
                val y = height - (normalized * height * 0.8f + height * 0.1f)

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    val prevRawX = (index - 1) * stepX
                    val prevX = if (isRtl) width - prevRawX else prevRawX
                    val prevNorm = (points[index - 1] - minVal) / range
                    val prevY = height - (prevNorm * height * 0.8f + height * 0.1f)
                    
                    val cx = (prevX + x) / 2
                    path.cubicTo(cx, prevY, cx, y, x, y)
                    fillPath.cubicTo(cx, prevY, cx, y, x, y)
                }
            }

            if (isRtl) {
                fillPath.lineTo(width, height)
            } else {
                fillPath.lineTo(width, height)
            }
            fillPath.close()

            // Gradient Fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(PinkPrimary.copy(alpha = 0.25f), Color.Transparent)
                )
            )

            // Main Line
            drawPath(
                path = path,
                color = PinkPrimary,
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )
            
            // Dashed Grid lines & Right Axis Labels
            val gridLines = 4
            val dashEffect = android.graphics.DashPathEffect(floatArrayOf(8f, 8f), 0f)
            
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    setColor(android.graphics.Color.parseColor("#94A3B8"))
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.LEFT
                    isAntiAlias = true
                }

                val gridPaint = android.graphics.Paint().apply {
                    setColor(android.graphics.Color.parseColor("#E2E8F0"))
                    strokeWidth = 1.dp.toPx()
                    pathEffect = dashEffect
                    style = android.graphics.Paint.Style.STROKE
                }

                for (i in 0..gridLines) {
                    val ratio = i.toFloat() / gridLines
                    val y = height - (ratio * height * 0.8f + height * 0.1f)
                    
                    canvas.nativeCanvas.drawLine(0f, y, width, y, gridPaint)

                    // Calculate price label for this grid line
                    val labelPrice = minVal + (maxVal - minVal) * ratio
                    canvas.nativeCanvas.drawText(
                        String.format(Locale.getDefault(), "%.1f", labelPrice),
                        width + 10.dp.toPx(),
                        y + 4.dp.toPx(),
                        paint
                    )
                }

                // Highlight latest value with a badge pill on the right
                val lastVal = points.last()
                val lastNorm = (lastVal - minVal) / range
                val lastY = height - (lastNorm * height * 0.8f + height * 0.1f)

                val badgePaint = android.graphics.Paint().apply {
                    setColor(android.graphics.Color.parseColor("#E11D48")) // PinkPrimary
                    isAntiAlias = true
                }
                val textPaint = android.graphics.Paint().apply {
                    setColor(android.graphics.Color.WHITE)
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }

                val textStr = String.format(Locale.getDefault(), "%.1f", lastVal)
                val textWidth = textPaint.measureText(textStr)
                val badgeWidth = textWidth + 16.dp.toPx()
                val badgeHeight = 20.dp.toPx()

                canvas.nativeCanvas.drawRoundRect(
                    width + 6.dp.toPx(),
                    lastY - badgeHeight / 2,
                    width + 6.dp.toPx() + badgeWidth,
                    lastY + badgeHeight / 2,
                    10.dp.toPx(),
                    10.dp.toPx(),
                    badgePaint
                )

                canvas.nativeCanvas.drawText(
                    textStr,
                    width + 6.dp.toPx() + badgeWidth / 2,
                    lastY + 4.dp.toPx(),
                    textPaint
                )
            }
        }
    }
}

