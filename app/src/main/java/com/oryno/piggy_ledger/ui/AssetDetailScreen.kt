package com.oryno.piggy_ledger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oryno.piggy_ledger.data.AssetEntity
import com.oryno.piggy_ledger.data.AssetType
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import com.oryno.piggy_ledger.ui.wealth.WealthViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas


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
                title = { Text(text = "Asset Details", fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NavyDark)
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
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Asset Hero Card
            item {
                AssetHeroCard(asset, valuation, pnl, pnlPct, isProfit)
            }

            // 2. Performance Chart Section
            item {
                AssetPerformanceSection(asset, valuation, purchaseCost, isProfit)
            }

            // 3. Live Market Rate Info
            item {
                LiveMarketRateCard(asset, uiState.liveRates)
            }

            // 4. Asset Metadata Details
            item {
                AssetMetadataCard(asset)
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
            val (icon, color) = when (asset.type) {
                AssetType.GOLD -> Icons.Default.MonetizationOn to Color(0xFFD97706)
                AssetType.SILVER -> Icons.Default.Shield to Color(0xFF475569)
                AssetType.CERTIFICATE -> Icons.Default.Savings to Color(0xFF059669)
            }

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
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
                text = "${String.format(Locale.getDefault(), "%,.2f", valuation)} EGP",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = NavyDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            val badgeBg = if (isProfit) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
            val badgeFg = if (isProfit) Color(0xFF15803D) else Color(0xFFB91C1C)
            val trendIcon = if (isProfit) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = badgeBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = trendIcon, contentDescription = null, tint = badgeFg, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${if (isProfit) "+" else ""}${String.format(Locale.getDefault(), "%,.2f", pnl)} EGP (${String.format(Locale.getDefault(), "%.2f", pnlPct)}%)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg
                    )
                }
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
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Historical Value Change", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark)
            Spacer(modifier = Modifier.height(16.dp))
            
            // Asset Specific Chart (Trend from acquisition to now)
            AssetTrendChart(
                initialValue = initialVal,
                currentValue = currentVal,
                isProfit = isProfit,
                modifier = Modifier.fillMaxWidth().height(150.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PerformanceMetric("Acquisition", initialVal, Color(0xFF64748B))
                PerformanceMetric("Current", currentVal, PinkPrimary)
            }
        }
    }
}

@Composable
private fun PerformanceMetric(label: String, value: Double, color: Color) {
    Column {
        Text(text = label, fontSize = 11.sp, color = TextLight, fontWeight = FontWeight.Bold)
        Text(
            text = "${String.format(Locale.getDefault(), "%,.0f", value)} EGP",
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
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rightPadding = 70.dp.toPx()
            val width = size.width - rightPadding
            val height = size.height
            
            val points = mutableListOf<Float>()
            val count = 15
            for (i in 0 until count) {
                val ratio = i.toFloat() / (count - 1)
                val base = initialValue + (currentValue - initialValue) * ratio
                val jitter = if (i > 0 && i < count - 1) {
                    (kotlin.math.sin(i.toDouble() * 2.0) * 0.05 * (currentValue.coerceAtLeast(initialValue)))
                } else 0.0
                points.add((base + jitter).toFloat())
            }

            val minVal = points.minOrNull()?.coerceAtMost(initialValue.toFloat()) ?: 0f
            val maxVal = points.maxOrNull()?.coerceAtLeast(currentValue.toFloat()) ?: 1f
            val range = (maxVal - minVal).coerceAtLeast(1f)

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

            // Dashed Grid lines & Right Axis Labels
            val gridLines = 4
            val dashEffect = android.graphics.DashPathEffect(floatArrayOf(8f, 8f), 0f)

            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    setColor(android.graphics.Color.parseColor("#94A3B8"))
                    textSize = 9.sp.toPx()
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
                    val y = height - (ratio * height * 0.7f + height * 0.15f)
                    
                    canvas.nativeCanvas.drawLine(0f, y, width, y, gridPaint)

                    val labelVal = minVal + (maxVal - minVal) * ratio
                    canvas.nativeCanvas.drawText(
                        String.format(Locale.getDefault(), "%,.0f", labelVal),
                        width + 8.dp.toPx(),
                        y + 3.dp.toPx(),
                        paint
                    )
                }

                // Highlight latest value with a badge pill on the right
                val lastVal = currentValue.toFloat()
                val lastNorm = (lastVal - minVal) / range
                val lastY = height - (lastNorm * height * 0.7f + height * 0.15f)

                val badgePaint = android.graphics.Paint().apply {
                    setColor(android.graphics.Color.parseColor(if (isProfit) "#10B981" else "#EF4444"))
                    isAntiAlias = true
                }
                val textPaint = android.graphics.Paint().apply {
                    setColor(android.graphics.Color.WHITE)
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }

                val textStr = String.format(Locale.getDefault(), "%,.0f", lastVal)
                val textWidth = textPaint.measureText(textStr)
                val badgeWidth = textWidth + 12.dp.toPx()
                val badgeHeight = 18.dp.toPx()

                canvas.nativeCanvas.drawRoundRect(
                    width + 6.dp.toPx(),
                    lastY - badgeHeight / 2,
                    width + 6.dp.toPx() + badgeWidth,
                    lastY + badgeHeight / 2,
                    8.dp.toPx(),
                    8.dp.toPx(),
                    badgePaint
                )

                canvas.nativeCanvas.drawText(
                    textStr,
                    width + 6.dp.toPx() + badgeWidth / 2,
                    lastY + 3.dp.toPx(),
                    textPaint
                )
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
                Text(text = "Live Market Info", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    val label = if (asset.type == AssetType.GOLD) "Current ${asset.purity} Gold Rate" else "Current ${asset.purity} Silver Rate"
                    Text(text = label, fontSize = 12.sp, color = TextLight)
                    Text(
                        text = "${String.format(Locale.getDefault(), "%,.0f", rate)} EGP/g",
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
                        text = "Updated 50s ago",
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
            Text(text = "Asset Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark)
            Spacer(modifier = Modifier.height(16.dp))

            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            
            DetailItem("Acquisition Date", sdf.format(Date(asset.createdAt)), Icons.Default.CalendarToday)
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
            
            if (asset.type == AssetType.CERTIFICATE) {
                DetailItem("Principal Amount", "${String.format(Locale.getDefault(), "%,.0f", asset.principalAmount)} EGP", Icons.Default.AccountBalance)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
                DetailItem("Annual Rate", "${asset.annualRate}%", Icons.Default.Percent)
            } else {
                DetailItem("Weight", "${asset.weightGrams} grams", Icons.Default.Scale)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
                DetailItem("Purity / Type", asset.purity, Icons.Default.Verified)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
                DetailItem("Cost Basis", "${String.format(Locale.getDefault(), "%,.0f", asset.purchasePrice)} EGP", Icons.Default.Payments)
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
