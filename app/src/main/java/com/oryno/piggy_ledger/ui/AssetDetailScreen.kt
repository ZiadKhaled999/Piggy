package com.oryno.piggy_ledger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.AssetEntity
import com.oryno.piggy_ledger.data.AssetType
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import com.oryno.piggy_ledger.ui.wealth.WealthViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailScreen(
    assetId: String,
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
    val asset = remember(uiState.assets, assetId) { uiState.assets.find { it.id == assetId } }

    if (asset == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PinkPrimary)
        }
        return
    }

    val valuation = uiState.assetValuations[asset.id] ?: asset.purchasePrice
    val purchaseCost = if (asset.type == AssetType.CERTIFICATE) asset.principalAmount else asset.purchasePrice
    val pnl = valuation - purchaseCost
    val pnlPct = if (purchaseCost > 0) (pnl / purchaseCost) * 100.0 else 0.0
    val isProfit = pnl >= 0

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.asset_details_title), fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_icon), tint = NavyDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(top = 20.dp + padding.calculateTopPadding(), bottom = 40.dp + padding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Asset Hero Card
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    AssetHeroCard(asset, valuation, pnl, pnlPct, isProfit)
                }
            }

            // 2. Performance Chart Section
            item {
                AssetPerformanceSection(asset, valuation, purchaseCost, isProfit)
            }

            // 3. Live Market Rate Info
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    LiveMarketRateCard(asset, uiState.liveRates)
                }
            }

            // 4. Asset Metadata Details
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    AssetMetadataCard(asset)
                }
            }
        }
    }
}

@Composable
private fun AssetHeroCard(
    asset: AssetEntity,
    valuation: Double,
    pnl: Double,
    pnlPct: Double,
    isProfit: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val (iconRes, color) = when (asset.type) {
                AssetType.GOLD -> R.drawable.ic_asset_gold to Color(0xFFD97706)
                AssetType.SILVER -> R.drawable.ic_asset_silver to Color(0xFF475569)
                AssetType.CERTIFICATE -> R.drawable.ic_asset_certificate to Color(0xFF059669)
            }

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
                    .border(2.dp, color.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = asset.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.amount_egp_fmt, String.format(Locale.getDefault(), "%,.2f", valuation)),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = NavyDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            val badgeFg = if (isProfit) Color(0xFF15803D) else Color(0xFFB91C1C)
            val trendIcon = if (isProfit) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = trendIcon, contentDescription = null, tint = badgeFg, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${if (isProfit) "+" else ""}${String.format(Locale.getDefault(), "%.2f", pnlPct)}% | ${String.format(Locale.getDefault(), "%,.2f", pnl)} ${stringResource(R.string.currency_egp)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeFg
                )
            }
        }
    }
}

@Composable
private fun AssetPerformanceSection(
    asset: AssetEntity,
    currentVal: Double,
    initialVal: Double,
    isProfit: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 20.dp)) {
            Text(text = stringResource(R.string.historical_value_change), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(16.dp))
            
            // Asset Specific Chart (Trend from acquisition to now)
            AssetTrendChart(
                initialValue = initialVal,
                currentValue = currentVal,
                isProfit = isProfit,
                modifier = Modifier.fillMaxWidth().height(160.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

        }
    }
}

@Composable
private fun PerformanceMetric(label: String, value: Double, color: Color) {
    Column {
        Text(text = label, fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.Bold)
        Text(
            text = stringResource(R.string.amount_egp_fmt, String.format(Locale.getDefault(), "%,.0f", value)),
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    }
}

@Composable
private fun AssetTrendChart(
    initialValue: Double,
    currentValue: Double,
    isProfit: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isProfit) Color(0xFF10B981) else Color(0xFFEF4444)
    val points = remember(initialValue, currentValue) {
        val pts = mutableListOf<Float>()
        val count = 15
        for (i in 0 until count) {
            val ratio = i.toFloat() / (count - 1)
            val base = initialValue + (currentValue - initialValue) * ratio
            val jitter = if (i > 0 && i < count - 1) {
                (kotlin.math.sin(i.toDouble() * 2.0) * 0.05 * (currentValue.coerceAtLeast(initialValue)))
            } else 0.0
            pts.add((base + jitter).toFloat())
        }
        pts
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val chartWidthPx = remember { mutableFloatStateOf(0f) }
    val chartHeightPx = remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

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
                            val idx = (offset.x / stepX).roundToInt().coerceIn(0, points.lastIndex)
                            selectedIndex = idx
                        }
                    }
            ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val minVal = points.minOrNull()?.coerceAtMost(initialValue.toFloat()) ?: 0f
                val maxVal = points.maxOrNull()?.coerceAtLeast(currentValue.toFloat()) ?: 1f
                val range = (maxVal - minVal).coerceAtLeast(1f)

                    val width = size.width
                    val height = size.height
                    chartWidthPx.floatValue = width
                    chartHeightPx.floatValue = height

                    val stepX = width / (points.size - 1)
                    val path = Path()
                    val fillPath = Path()

                    points.forEachIndexed { index, value ->
                        val x = index * stepX
                        val norm = (value - minVal) / range
                        val y = height - (norm * height * 0.7f + height * 0.15f)

                        if (index == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, height)
                            fillPath.lineTo(x, y)
                        } else {
                            val prevX = (index - 1) * stepX
                            val prevNorm = (points[index - 1] - minVal) / range
                            val prevY = height - (prevNorm * height * 0.7f + height * 0.15f)
                            val cx = (prevX + x) / 2
                            path.cubicTo(cx, prevY, cx, y, x, y)
                            fillPath.cubicTo(cx, prevY, cx, y, x, y)
                        }
                    }
                    fillPath.lineTo(width, height)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(colors = listOf(color.copy(alpha = 0.2f), Color.Transparent))
                    )
                    drawPath(path = path, color = color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

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
                            val y = height - (ratio * height * 0.7f + height * 0.15f)
                            canvas.nativeCanvas.drawLine(0f, y, width, y, gridPaint)
                        }
                    }

                // Highlight selected point on curve
                selectedIndex?.let { idx ->
                    if (idx in points.indices) {
                        val x = idx * stepX
                        val norm = (points[idx] - minVal) / range
                        val y = height - (norm * height * 0.7f + height * 0.15f)

                        drawIntoCanvas { canvas ->
                            val linePaint = android.graphics.Paint().apply {
                                setColor(android.graphics.Color.parseColor("#94A3B8"))
                                strokeWidth = 1.5.dp.toPx()
                                pathEffect = dashEffect
                                style = android.graphics.Paint.Style.STROKE
                            }
                            canvas.nativeCanvas.drawLine(x, 0f, x, height, linePaint)
                        }

                        drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(x, y))
                        drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(x, y))
                    }
                }
            }

            // Dynamic Tooltip position tracking tapped point
            selectedIndex?.let { idx ->
                if (idx in points.indices && chartWidthPx.floatValue > 0f) {
                    val price = points[idx]
                    val minVal = points.minOrNull()?.coerceAtMost(initialValue.toFloat()) ?: 0f
                    val maxVal = points.maxOrNull()?.coerceAtLeast(currentValue.toFloat()) ?: 1f
                    val range = (maxVal - minVal).coerceAtLeast(1f)

                    val stepX = chartWidthPx.floatValue / (points.size - 1)
                    val pointX = idx * stepX
                    val norm = (price - minVal) / range
                    val pointY = chartHeightPx.floatValue - (norm * chartHeightPx.floatValue * 0.7f + chartHeightPx.floatValue * 0.15f)

                    val pointXDp = with(density) { pointX.toDp() }
                    val pointYDp = with(density) { pointY.toDp() }

                    val tooltipWidthDp = 80.dp
                    val tooltipHeightDp = 28.dp

                    val clampedXDp = (pointXDp - tooltipWidthDp / 2).coerceIn(0.dp, (maxContainerWidth - tooltipWidthDp - 12.dp).coerceAtLeast(0.dp))
                    val clampedYDp = (pointYDp - tooltipHeightDp - 6.dp).coerceIn(0.dp, (maxContainerHeight - tooltipHeightDp).coerceAtLeast(0.dp))

                    Surface(
                        modifier = Modifier.offset(x = clampedXDp, y = clampedYDp),
                        shape = RoundedCornerShape(8.dp),
                        color = color,
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

@Composable
private fun LiveMarketRateCard(asset: AssetEntity, rates: com.oryno.piggy_ledger.data.LiveMarketRatesEntity) {
    if (asset.type == AssetType.CERTIFICATE) return

    val rate = when (asset.type) {
        AssetType.GOLD -> when (asset.purity.lowercase()) {
            "24k" -> rates.gold24k
            "18k" -> rates.gold18k
            else -> rates.gold21k
        }
        AssetType.SILVER -> when (asset.purity) {
            "925" -> rates.silver925
            "900" -> rates.silver900
            else -> rates.silver999
        }
        else -> 0.0
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Update, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.live_market_info), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    val label = stringResource(R.string.current_purity_rate_fmt, asset.purity)
                    Text(text = label, fontSize = 12.sp, color = TextLight)
                    Text(
                        text = stringResource(R.string.egp_per_gram_fmt, String.format(Locale.getDefault(), "%,.0f", rate)),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyDark
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = stringResource(R.string.updated_recently_fmt, "50s"),
                        fontSize = 10.sp,
                        color = TextLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AssetMetadataCard(asset: AssetEntity) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = stringResource(R.string.asset_metadata_title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark)
            Spacer(modifier = Modifier.height(16.dp))

            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            
            DetailItem(stringResource(R.string.acquisition_date), sdf.format(Date(asset.createdAt)), Icons.Default.CalendarToday)
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
            
            if (asset.type == AssetType.CERTIFICATE) {
                DetailItem(stringResource(R.string.principal_amount), stringResource(R.string.amount_egp_fmt, String.format(Locale.getDefault(), "%,.0f", asset.principalAmount)), Icons.Default.AccountBalance)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
                DetailItem(stringResource(R.string.annual_rate), "${asset.annualRate}%", Icons.Default.Percent)
            } else {
                DetailItem(stringResource(R.string.weight_label), stringResource(R.string.weight_grams_fmt, asset.weightGrams.toString()), Icons.Default.Scale)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
                DetailItem(stringResource(R.string.purity_type_label), asset.purity, Icons.Default.Verified)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
                DetailItem(stringResource(R.string.cost_basis), stringResource(R.string.amount_egp_fmt, String.format(Locale.getDefault(), "%,.0f", asset.purchasePrice)), Icons.Default.Payments)
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = TextLight, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, modifier = Modifier.weight(1f), fontSize = 14.sp, color = TextLight)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
    }
}
