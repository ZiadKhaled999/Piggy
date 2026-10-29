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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.*
import androidx.compose.ui.input.pointer.pointerInput
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
        "GOLD" -> when (variant.lowercase()) {
            "24k" -> uiState.liveRates.gold24k
            "18k" -> uiState.liveRates.gold18k
            "overall", "all" -> (uiState.liveRates.gold24k + uiState.liveRates.gold21k + uiState.liveRates.gold18k) / 3.0
            else -> uiState.liveRates.gold21k
        }
        "SILVER" -> when (variant.lowercase()) {
            "925" -> uiState.liveRates.silver925
            "900" -> uiState.liveRates.silver900
            "overall", "all" -> (uiState.liveRates.silver999 + uiState.liveRates.silver925 + uiState.liveRates.silver900) / 3.0
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
                .fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp + padding.calculateTopPadding(), bottom = 32.dp + padding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Current Price Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
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
                    color = Color.White,
                    shadowElevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 20.dp)) {
                        Text(
                            text = stringResource(R.string.price_history_24h),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark,
                            modifier = Modifier.padding(horizontal = 18.dp)
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
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
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
                Column(modifier = Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MarketStatCard(stringResource(R.string.highest_24h), high24h, Icons.Default.TrendingUp, Modifier.weight(1f))
                        MarketStatCard(stringResource(R.string.lowest_24h), low24h, Icons.Default.TrendingDown, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MarketStatCard(stringResource(R.string.market_spread), spread, Icons.Default.SwapHoriz, Modifier.weight(1f))
                        MarketStatCard(stringResource(R.string.volatility_index), volatility, Icons.Default.Speed, Modifier.weight(1f), isCurrency = false)
                    }
                }
            }

            // 4. Market Info Note
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    isCurrency: Boolean = true
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NavyDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = label, fontSize = 12.sp, color = TextLight, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isCurrency) "${String.format(Locale.getDefault(), "%,.2f", value)} ${stringResource(R.string.currency_egp)}" else "${String.format(Locale.getDefault(), "%.1f", value)}%",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
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

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val chartWidthPx = remember { mutableFloatStateOf(0f) }
    val chartHeightPx = remember { mutableFloatStateOf(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
        BoxWithConstraints(modifier = modifier) {
            val maxContainerWidth = maxWidth
            val maxContainerHeight = maxHeight

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            val cw = chartWidthPx.floatValue
                            if (cw <= 0f || points.size < 2) return@detectTapGestures
                            val stepX = cw / (points.size - 1)
                            val idx = (offset.x / stepX).toInt().coerceIn(0, points.lastIndex)
                            selectedIndex = idx
                        }
                    }
            ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val minVal = points.minOrNull() ?: 0f
                val maxVal = points.maxOrNull() ?: 1f
                val range = if (maxVal - minVal == 0f) 1f else maxVal - minVal

                val width = size.width
                val height = size.height
                chartWidthPx.floatValue = width
                chartHeightPx.floatValue = height
                val stepX = width / (points.size - 1)

                val path = Path()
                val fillPath = Path()

                points.forEachIndexed { index, value ->
                    val x = index * stepX
                    val normalized = (value - minVal) / range
                    val y = height - (normalized * height * 0.8f + height * 0.1f)

                    if (index == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                    } else {
                        val prevX = (index - 1) * stepX
                        val prevNorm = (points[index - 1] - minVal) / range
                        val prevY = height - (prevNorm * height * 0.8f + height * 0.1f)

                        val cx = (prevX + x) / 2
                        path.cubicTo(cx, prevY, cx, y, x, y)
                        fillPath.cubicTo(cx, prevY, cx, y, x, y)
                    }
                }

                fillPath.lineTo(width, height)
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

                // Dashed Grid lines
                val gridLines = 4
                val dashEffect = android.graphics.DashPathEffect(floatArrayOf(8f, 8f), 0f)

                drawIntoCanvas { canvas ->
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
                    }
                }

                // Highlight selected point on curve
                selectedIndex?.let { idx ->
                    if (idx in points.indices) {
                        val pointX = idx * stepX
                        val norm = (points[idx] - minVal) / range
                        val pointY = height - (norm * height * 0.8f + height * 0.1f)

                        drawIntoCanvas { canvas ->
                            val linePaint = android.graphics.Paint().apply {
                                setColor(android.graphics.Color.parseColor("#94A3B8"))
                                strokeWidth = 1.5.dp.toPx()
                                pathEffect = dashEffect
                                style = android.graphics.Paint.Style.STROKE
                            }
                            canvas.nativeCanvas.drawLine(pointX, 0f, pointX, height, linePaint)
                        }

                        drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(pointX, pointY))
                        drawCircle(color = PinkPrimary, radius = 4.dp.toPx(), center = Offset(pointX, pointY))
                    }
                }
            }

            // Dynamic Tooltip position tracking tapped point
            selectedIndex?.let { idx ->
                if (idx in points.indices && chartWidthPx.floatValue > 0f) {
                    val price = points[idx]
                    val minVal = points.minOrNull() ?: 0f
                    val maxVal = points.maxOrNull() ?: 1f
                    val range = if (maxVal - minVal == 0f) 1f else maxVal - minVal

                    val stepX = chartWidthPx.floatValue / (points.size - 1)
                    val pointX = idx * stepX
                    val norm = (price - minVal) / range
                    val pointY = chartHeightPx.floatValue - (norm * chartHeightPx.floatValue * 0.8f + chartHeightPx.floatValue * 0.1f)

                    val pointXDp = with(density) { pointX.toDp() }
                    val pointYDp = with(density) { pointY.toDp() }

                    val tooltipWidthDp = 80.dp
                    val tooltipHeightDp = 28.dp

                    val clampedXDp = (pointXDp - tooltipWidthDp / 2).coerceIn(0.dp, (maxContainerWidth - tooltipWidthDp - 12.dp).coerceAtLeast(0.dp))
                    val clampedYDp = (pointYDp - tooltipHeightDp - 6.dp).coerceIn(0.dp, (maxContainerHeight - tooltipHeightDp).coerceAtLeast(0.dp))

                    Surface(
                        modifier = Modifier.offset(x = clampedXDp, y = clampedYDp),
                        shape = RoundedCornerShape(8.dp),
                        color = PinkPrimary,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = stringResource(R.string.amount_egp_fmt, String.format(Locale.getDefault(), "%,.0f", price.toDouble())),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
}

