package com.oryno.piggy_ledger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.data.AssetEntity
import com.oryno.piggy_ledger.data.AssetType
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.PortfolioHistoryEntity
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import com.oryno.piggy_ledger.ui.wealth.WealthViewModel
import java.util.Locale
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WealthCommoditiesScreen(
    onBack: () -> Unit,
    onNavigateToAssetDetail: (String) -> Unit,
    onNavigateToMarketDetail: (String, String) -> Unit
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

    var showAssetTypePicker by remember { mutableStateOf(false) }
    var showAddModalType by remember { mutableStateOf<AssetType?>(null) }
    var selectedTimeframe by remember { mutableStateOf("1M") }

    var assetToDelete by remember { mutableStateOf<AssetEntity?>(null) }
    var assetToSell by remember { mutableStateOf<AssetEntity?>(null) }
    var assetForMenu by remember { mutableStateOf<AssetEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAssetTypePicker = true },
                containerColor = PinkPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_asset), fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(16.dp)
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.wealth_portfolio_title),
                        fontWeight = FontWeight.Bold,
                        color = NavyDark,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_icon),
                            tint = NavyDark
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.fetchLivePrices(forceRefresh = true) },
                        enabled = !uiState.isLoadingPrices
                    ) {
                        if (uiState.isLoadingPrices) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = PinkPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.refresh_rates_desc),
                                tint = NavyDark
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp + padding.calculateTopPadding(), bottom = 32.dp + padding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Portfolio Performance Card & Dynamic Timeframe Chart
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 18.dp).padding(top = 20.dp, bottom = 12.dp)) {
                            Text(
                                text = stringResource(R.string.total_portfolio_value),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextLight,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${String.format(Locale.getDefault(), "%,.2f", uiState.totalWealthValue)} ${stringResource(R.string.currency_egp)}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NavyDark
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Real Gain / Loss Badge
                            val isProfit = uiState.overallGainLoss >= 0
                            val badgeBg = if (isProfit) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                            val badgeFg = if (isProfit) Color(0xFF15803D) else Color(0xFFB91C1C)
                            val icon = if (isProfit) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = badgeBg
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = badgeFg,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${if (isProfit) "+" else ""}${String.format(Locale.getDefault(), "%,.2f", uiState.overallGainLoss)} ${stringResource(R.string.currency_egp)} (${String.format(Locale.getDefault(), "%.2f", uiState.overallGainLossPercentage)}%)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeFg
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.total_return),
                                    fontSize = 11.sp,
                                    color = TextLight
                                )
                            }
                        }

                        // Real Dynamic Portfolio Line Chart (Full Width)
                        RealDynamicPortfolioChart(
                            history = uiState.portfolioHistory,
                            currentTotal = uiState.totalWealthValue,
                            timeframe = selectedTimeframe,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        )

                        Column(modifier = Modifier.padding(horizontal = 18.dp).padding(top = 4.dp, bottom = 14.dp)) {
                            // Timeframe filter chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Text(
                                        text = stringResource(R.string.live_status),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("1D", "1W", "1M", "1Y").forEach { tf ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (selectedTimeframe == tf) PinkPrimary else Color(0xFFF1F5F9),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { selectedTimeframe = tf }
                                        ) {
                                            Text(
                                                text = tf,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedTimeframe == tf) Color.White else TextLight,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Modern Commodity Market Rates Section
            item {
                Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.live_commodity_rates),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyDark,
                            letterSpacing = (-0.5).sp
                        )

                        IconButton(
                            onClick = { viewModel.fetchLivePrices(forceRefresh = true) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.refresh_rates_desc),
                                tint = PinkPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val overallGoldRate = (uiState.liveRates.gold24k + uiState.liveRates.gold21k + uiState.liveRates.gold18k) / 3.0

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
                    ) {
                        item { ModernRateCard(stringResource(R.string.overall_gold), overallGoldRate, Color(0xFFD97706), R.drawable.ic_asset_gold) { onNavigateToMarketDetail("GOLD", "Overall") } }
                        item { ModernRateCard(stringResource(R.string.purity_k24), uiState.liveRates.gold24k, Color(0xFFD97706), R.drawable.ic_asset_gold) { onNavigateToMarketDetail("GOLD", "24k") } }
                        item { ModernRateCard(stringResource(R.string.purity_k21), uiState.liveRates.gold21k, Color(0xFFB45309), R.drawable.ic_asset_gold) { onNavigateToMarketDetail("GOLD", "21k") } }
                        item { ModernRateCard(stringResource(R.string.purity_k18), uiState.liveRates.gold18k, Color(0xFF92400E), R.drawable.ic_asset_gold) { onNavigateToMarketDetail("GOLD", "18k") } }
                        item { ModernRateCard(stringResource(R.string.purity_s999), uiState.liveRates.silver999, Color(0xFF475569), R.drawable.ic_asset_silver) { onNavigateToMarketDetail("SILVER", "999") } }
                        item { ModernRateCard(stringResource(R.string.purity_s925), uiState.liveRates.silver925, Color(0xFF64748B), R.drawable.ic_asset_silver) { onNavigateToMarketDetail("SILVER", "925") } }
                        item { ModernRateCard(stringResource(R.string.purity_s900), uiState.liveRates.silver900, Color(0xFF94A3B8), R.drawable.ic_asset_silver) { onNavigateToMarketDetail("SILVER", "900") } }
                    }
                }
            }

            // 3. Asset Diversity Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DiversityIndicator(stringResource(R.string.asset_type_gold), uiState.goldTotalValue, Color(0xFFD97706), R.drawable.ic_asset_gold, Modifier.weight(1f))
                    DiversityIndicator(stringResource(R.string.asset_type_silver), uiState.silverTotalValue, Color(0xFF475569), R.drawable.ic_asset_silver, Modifier.weight(1f))
                    DiversityIndicator(stringResource(R.string.certificate_desc), uiState.certificatesTotalValue, Color(0xFF059669), R.drawable.ic_asset_certificate, Modifier.weight(1f))
                }
            }

            // 4. Holdings Section Header
            item {
                Text(
                    text = stringResource(R.string.asset_holdings_pnl),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark,
                    modifier = Modifier.padding(top = 8.dp).padding(horizontal = 18.dp)
                )
            }

            if (uiState.assets.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.no_holdings_msg),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NavyDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.no_holdings_desc),
                                fontSize = 12.5.sp,
                                color = TextLight,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(uiState.assets, key = { it.id }) { asset ->
                    val valuation = uiState.assetValuations[asset.id] ?: asset.purchasePrice
                    Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                        AssetHoldingCard(
                            asset = asset,
                            valuation = valuation,
                            onCardClick = { onNavigateToAssetDetail(asset.id) },
                            onMenuClick = { assetForMenu = asset }
                        )
                    }
                }
            }
        }
    }


    // Delete Confirmation Bottom Sheet (Secure)
    if (assetToDelete != null) {
        DeleteAssetConfirmationBottomSheet(
            assetName = assetToDelete?.name ?: "",
            onDismiss = { assetToDelete = null },
            onConfirm = {
                assetToDelete?.id?.let { viewModel.deleteAsset(it) }
                assetToDelete = null
            }
        )
    }

    // Asset Action Menu Bottom Sheet
    if (assetForMenu != null) {
        AssetActionMenuBottomSheet(
            asset = assetForMenu!!,
            onDismiss = { assetForMenu = null },
            onSell = {
                assetToSell = assetForMenu
                assetForMenu = null
            },
            onEditDate = {
                val currentAsset = assetForMenu!!
                assetForMenu = null
                val initialTs = if (currentAsset.startDate > 0) currentAsset.startDate else currentAsset.createdAt
                showNativeDatePicker(context, initialTs) { newTs ->
                    viewModel.updateAssetPurchaseDate(currentAsset.id, newTs)
                }
            },
            onDelete = {
                assetToDelete = assetForMenu
                assetForMenu = null
            }
        )
    }

    // Sell / Liquidate Asset Bottom Sheet
    if (assetToSell != null) {
        SellAssetBottomSheet(
            asset = assetToSell!!,
            bankAccounts = uiState.bankAccounts,
            currentValuation = uiState.assetValuations[assetToSell!!.id] ?: assetToSell!!.purchasePrice,
            onDismiss = { assetToSell = null },
            onConfirmSell = { grams, saleAmount, destAccountId ->
                viewModel.sellAsset(
                    assetId = assetToSell!!.id,
                    gramsSold = grams,
                    actualSaleAmount = saleAmount,
                    destinationAccountId = destAccountId,
                    onSuccess = { 
                        assetToSell = null
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.asset_liquidated_success))
                        }
                    }
                )
            }
        )
    }

    // Multi-Step Slide BottomSheet Wizard for Adding Assets
    if (showAddModalType != null) {
        MultiStepAssetWizardBottomSheet(
            type = showAddModalType!!,
            bankAccounts = uiState.bankAccounts,
            liveRates = uiState.liveRates,
            onDismiss = { showAddModalType = null },
            onAddGold = { name, grams, karat, priceOverride, purchaseDateMs, fundingAccountId ->
                viewModel.addGoldAsset(
                    name = name,
                    grams = grams,
                    karat = karat,
                    purchasePriceOverride = priceOverride,
                    purchaseDateMs = purchaseDateMs,
                    fundingAccountId = fundingAccountId,
                    onSuccess = { showAddModalType = null }
                )
            },
            onAddSilver = { name, grams, purity, priceOverride, purchaseDateMs, fundingAccountId ->
                viewModel.addSilverAsset(
                    name = name,
                    grams = grams,
                    purity = purity,
                    purchasePriceOverride = priceOverride,
                    purchaseDateMs = purchaseDateMs,
                    fundingAccountId = fundingAccountId,
                    onSuccess = { showAddModalType = null }
                )
            },
            onAddCertificate = { bank, principal, rate, purchaseDateMs, fundingAccountId ->
                viewModel.addCertificateAsset(
                    bankName = bank,
                    principal = principal,
                    annualInterestRate = rate,
                    purchaseDateMs = purchaseDateMs,
                    fundingAccountId = fundingAccountId,
                    onSuccess = { showAddModalType = null }
                )
            }
        )
    }

    if (showAssetTypePicker) {
        AssetTypePickerBottomSheet(
            onDismiss = { showAssetTypePicker = false },
            onTypeSelected = { type ->
                showAssetTypePicker = false
                showAddModalType = type
            }
        )
    }
}

@Composable
private fun RealDynamicPortfolioChart(
    history: List<PortfolioHistoryEntity>,
    currentTotal: Double,
    timeframe: String,
    modifier: Modifier = Modifier
) {
    val count = when (timeframe) {
        "1D" -> 6
        "1W" -> 7
        "1M" -> 12
        "1Y" -> 24
        else -> 10
    }

    val baseFactor = when (timeframe) {
        "1D" -> 0.02f
        "1W" -> 0.05f
        "1M" -> 0.10f
        "1Y" -> 0.20f
        else -> 0.08f
    }

    val points = remember(history, currentTotal, timeframe) {
        if (history.size >= count) {
            history.takeLast(count).map { it.totalPortfolioValue.toFloat() }
        } else {
            List(count) { idx ->
                val ratio = (idx + 1).toFloat() / count
                val wave = kotlin.math.sin(idx.toDouble()) * baseFactor * currentTotal
                (currentTotal * (0.85 + (ratio * 0.15)) + wave).toFloat()
            }
        }
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val chartWidthPx = remember { mutableFloatStateOf(0f) }
    val chartHeightPx = remember { mutableFloatStateOf(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
        Column(modifier = modifier) {
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
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

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF34D399).copy(alpha = 0.35f), Color.Transparent)
                            )
                        )

                        drawPath(
                            path = path,
                            color = Color(0xFF059669),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
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

                        // Selected point highlight line & dot
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
                                drawCircle(color = Color(0xFF059669), radius = 4.dp.toPx(), center = Offset(pointX, pointY))
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
                                color = Color(0xFF059669),
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

        // X-Axis Labels (Time)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labels = when (timeframe) {
                "1D" -> listOf(stringResource(R.string.h6_ago), stringResource(R.string.now))
                "1W" -> listOf(stringResource(R.string.d7_ago), stringResource(R.string.today))
                "1M" -> listOf(stringResource(R.string.d30_ago), stringResource(R.string.today))
                "1Y" -> listOf(stringResource(R.string.m12_ago), stringResource(R.string.today))
                else -> listOf(stringResource(R.string.past), stringResource(R.string.present))
            }
            labels.forEach { label ->
                Text(text = label, fontSize = 9.sp, color = TextLight, fontWeight = FontWeight.Medium)
            }
        }
    }
}
}

@Composable
private fun ModernRateCard(
    purityTitle: String,
    rate: Double,
    color: Color,
    iconRes: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.5.dp,
        modifier = Modifier.width(142.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = color.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = purityTitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${String.format(Locale.getDefault(), "%,.0f", rate)} ${stringResource(R.string.currency_egp)}",
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Black,
                color = NavyDark
            )

            Text(
                text = stringResource(R.string.per_gram_label),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = TextLight
            )
        }
    }
}

@Composable
private fun AssetHoldingCard(
    asset: AssetEntity,
    valuation: Double,
    onCardClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val purchaseCost = if (asset.type == AssetType.CERTIFICATE) asset.principalAmount else asset.purchasePrice
    val pnl = valuation - purchaseCost
    val pnlPct = if (purchaseCost > 0) (pnl / purchaseCost) * 100.0 else 0.0
    val isProfit = pnl >= 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (iconRes, color) = when (asset.type) {
                AssetType.GOLD -> R.drawable.ic_asset_gold to Color(0xFFD97706)
                AssetType.SILVER -> R.drawable.ic_asset_silver to Color(0xFF475569)
                AssetType.CERTIFICATE -> R.drawable.ic_asset_certificate to Color(0xFF059669)
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF1F5F9))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1.1f)) {
                Text(
                    text = asset.name,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                val desc = when (asset.type) {
                    AssetType.GOLD -> "${asset.weightGrams}g • ${asset.purity}"
                    AssetType.SILVER -> "${asset.weightGrams}g • ${asset.purity}"
                    AssetType.CERTIFICATE -> "${asset.annualRate}% APR"
                }
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    color = TextLight
                )
            }

            // Mini Sparkline P&L Chart
            MiniHoldingSparkline(
                isProfit = isProfit,
                modifier = Modifier
                    .width(48.dp)
                    .height(26.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format(Locale.getDefault(), "%,.0f", valuation)} ${stringResource(R.string.currency_egp)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NavyDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                val pnlColor = if (isProfit) Color(0xFF15803D) else Color(0xFFB91C1C)
                Text(
                    text = "${if (isProfit) "+" else ""}${String.format(Locale.getDefault(), "%,.0f", pnl)} (${String.format(Locale.getDefault(), "%.1f", pnlPct)}%)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = pnlColor
                )
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetActionMenuBottomSheet(
    asset: AssetEntity,
    onDismiss: () -> Unit,
    onSell: () -> Unit,
    onEditDate: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.asset_actions),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )

            Surface(
                onClick = onSell,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PointOfSale, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = stringResource(R.string.action_sell), fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                onClick = onEditDate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = PinkPrimary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = stringResource(R.string.edit_purchase_date), fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, Color(0xFFFEE2E2))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = stringResource(R.string.action_delete), fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteAssetConfirmationBottomSheet(
    assetName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var confirmName by remember { mutableStateOf("") }
    val canDelete = confirmName.trim().equals(assetName.trim(), ignoreCase = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.confirm_delete_asset_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )
            Text(
                text = stringResource(R.string.confirm_delete_asset_desc),
                fontSize = 14.sp,
                color = TextLight
            )

            OutlinedTextField(
                value = confirmName,
                onValueChange = { confirmName = it },
                label = { Text(stringResource(R.string.confirm_delete_asset_hint)) },
                placeholder = { Text(assetName) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Button(
                onClick = onConfirm,
                enabled = canDelete,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
            ) {
                Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

@Composable
private fun MiniHoldingSparkline(
    isProfit: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isProfit) Color(0xFF10B981) else Color(0xFFEF4444)
    val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    
    Canvas(modifier = modifier) {
        val points = if (isProfit) {
            listOf(0.2f, 0.35f, 0.30f, 0.55f, 0.50f, 0.75f, 0.70f, 0.90f)
        } else {
            listOf(0.9f, 0.75f, 0.80f, 0.55f, 0.60f, 0.35f, 0.40f, 0.15f)
        }

        val width = size.width
        val height = size.height
        val stepX = width / (points.size - 1)

        val path = Path()
        points.forEachIndexed { index, pt ->
            val rawX = index * stepX
            val x = if (isRtl) width - rawX else rawX
            val y = height - (pt * height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiStepAssetWizardBottomSheet(
    type: AssetType,
    bankAccounts: List<Account>,
    liveRates: com.oryno.piggy_ledger.data.LiveMarketRatesEntity,
    onDismiss: () -> Unit,
    onAddGold: (name: String, grams: Double, karat: Int, priceOverride: Double?, purchaseDateMs: Long, fundingAccountId: String?) -> Unit,
    onAddSilver: (name: String, grams: Double, purity: String, priceOverride: Double?, purchaseDateMs: Long, fundingAccountId: String?) -> Unit,
    onAddCertificate: (bank: String, principal: Double, rate: Double, purchaseDateMs: Long, fundingAccountId: String?) -> Unit
) {
    var currentSlide by remember { mutableIntStateOf(0) }

    var name by remember { mutableStateOf("") }
    var gramsText by remember { mutableStateOf("") }
    var karat by remember { mutableIntStateOf(21) }
    var purity by remember { mutableStateOf("999") }
    var interestText by remember { mutableStateOf("23.5") }
    var priceOverrideText by remember { mutableStateOf("") }
    var purchaseDateMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    val bankCertDefault = stringResource(R.string.bank_certificate_default)
    var selectedFundingAccountId by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(0, 1, 2, 3).forEach { idx ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(if (idx <= currentSlide) PinkPrimary else Color(0xFFE2E8F0))
                    )
                }
            }

            AnimatedContent(
                targetState = currentSlide,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "WizardSlideTransition"
            ) { slide ->
                when (slide) {
                    0 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = stringResource(R.string.step_format, 1, stringResource(R.string.wizard_details, type.name)),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark
                            )

                            if (type == AssetType.CERTIFICATE) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text(stringResource(R.string.bank_name_label)) },
                                    placeholder = { Text(stringResource(R.string.e_g_cib_nbe)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = gramsText,
                                    onValueChange = { gramsText = it },
                                    label = { Text(stringResource(R.string.principal_amount_label)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = interestText,
                                    onValueChange = { interestText = it },
                                    label = { Text(stringResource(R.string.annual_interest_rate_label)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = { Text("%", fontWeight = FontWeight.Bold) }
                                )
                            } else {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text(stringResource(R.string.asset_name_label)) },
                                    placeholder = { Text(stringResource(R.string.e_g_my_gold_coins)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = gramsText,
                                    onValueChange = { gramsText = it },
                                    label = { Text(stringResource(R.string.weight_grams_label)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                if (type == AssetType.GOLD) {
                                    Text(stringResource(R.string.select_purity_karat), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(24, 21, 18).forEach { k ->
                                            FilterChip(
                                                selected = karat == k,
                                                onClick = { karat = k },
                                                label = { Text(stringResource(R.string.karat_format, k)) }
                                            )
                                        }
                                    }
                                } else if (type == AssetType.SILVER) {
                                    Text(stringResource(R.string.select_purity_type), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("999", "925", "900").forEach { p ->
                                            FilterChip(
                                                selected = purity == p,
                                                onClick = { purity = p },
                                                label = { Text(p) }
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { currentSlide = 1 },
                                enabled = gramsText.toDoubleOrNull() ?: 0.0 > 0,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                            ) {
                                Text(stringResource(R.string.next_step), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    1 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = stringResource(R.string.purchase_date_label),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark
                            )
                            
                            Text(
                                text = "When did you acquire this asset?",
                                fontSize = 14.sp,
                                color = TextLight
                            )

                            val formattedDate = remember(purchaseDateMs) {
                                SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(purchaseDateMs))
                            }

                            Surface(
                                onClick = {
                                    showNativeDatePicker(context, purchaseDateMs) { newTs ->
                                        purchaseDateMs = newTs
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = PinkPrimary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(formattedDate, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = false,
                                    onClick = { purchaseDateMs = System.currentTimeMillis() },
                                    label = { Text(stringResource(R.string.quick_today)) }
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = { purchaseDateMs = System.currentTimeMillis() - (180L * 24 * 3600 * 1000) },
                                    label = { Text(stringResource(R.string.quick_6m_ago)) }
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = { purchaseDateMs = System.currentTimeMillis() - (365L * 24 * 3600 * 1000) },
                                    label = { Text(stringResource(R.string.quick_1y_ago)) }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { currentSlide = 0 },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(stringResource(R.string.back_btn))
                                }
                                Button(
                                    onClick = { currentSlide = 2 },
                                    modifier = Modifier.weight(1.5f).height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                                ) {
                                    Text(stringResource(R.string.next_step), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    2 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = stringResource(R.string.step_purchase_price_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark
                            )

                            val grams = gramsText.toDoubleOrNull() ?: 0.0
                            val estimatedPrice = when (type) {
                                AssetType.GOLD -> grams * (if (karat == 24) liveRates.gold24k else if (karat == 18) liveRates.gold18k else liveRates.gold21k)
                                AssetType.SILVER -> grams * (if (purity == "925") liveRates.silver925 else if (purity == "900") liveRates.silver900 else liveRates.silver999)
                                AssetType.CERTIFICATE -> grams
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(stringResource(R.string.estimated_live_cost), fontSize = 12.sp, color = TextLight)
                                    Text(
                                        text = "${String.format(Locale.getDefault(), "%,.2f", estimatedPrice)} ${stringResource(R.string.currency_egp)}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDark
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = priceOverrideText,
                                onValueChange = { priceOverrideText = it },
                                label = { Text(stringResource(R.string.actual_price_paid)) },
                                placeholder = { Text(stringResource(R.string.leave_blank_live_rate, String.format(Locale.getDefault(), "%.0f", estimatedPrice))) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { currentSlide = 1 },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(stringResource(R.string.back_btn))
                                }
                                Button(
                                    onClick = { currentSlide = 3 },
                                    modifier = Modifier.weight(1.5f).height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                                ) {
                                    Text(stringResource(R.string.next_funding_source), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    3 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = stringResource(R.string.step_funding_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark
                            )

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (type != AssetType.CERTIFICATE) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedFundingAccountId = null },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = selectedFundingAccountId == null,
                                                onClick = { selectedFundingAccountId = null }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(stringResource(R.string.funding_pre_owned), fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                                                Text(stringResource(R.string.funding_pre_owned_desc), fontSize = 11.5.sp, color = TextLight)
                                            }
                                        }

                                        if (bankAccounts.isNotEmpty()) {
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))
                                        }
                                    } else if (selectedFundingAccountId == null && bankAccounts.isNotEmpty()) {
                                        // Auto-select first account for certificates if none selected
                                        LaunchedEffect(Unit) {
                                            selectedFundingAccountId = bankAccounts.first().id
                                        }
                                    }

                                    if (bankAccounts.isNotEmpty()) {
                                        Text(stringResource(R.string.funding_deduct_account), fontSize = 11.5.sp, color = TextLight, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))

                                        bankAccounts.forEach { acc ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedFundingAccountId = acc.id },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = selectedFundingAccountId == acc.id,
                                                    onClick = { selectedFundingAccountId = acc.id }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "${acc.name} (${String.format(Locale.getDefault(), "%,.0f", acc.current_balance)} ${acc.currency})",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = NavyDark
                                                )
                                            }
                                        }
                                    } else if (type == AssetType.CERTIFICATE) {
                                        // If no accounts, show a warning for certificates
                                        Text(
                                            text = "You need a bank account to purchase a certificate.",
                                            color = Color.Red,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { currentSlide = 2 },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(stringResource(R.string.back_btn))
                                }
                                Button(
                                    onClick = {
                                        val grams = gramsText.toDoubleOrNull() ?: 0.0
                                        val priceOverride = priceOverrideText.toDoubleOrNull()
                                        when (type) {
                                            AssetType.GOLD -> onAddGold(name, grams, karat, priceOverride, purchaseDateMs, selectedFundingAccountId)
                                            AssetType.SILVER -> onAddSilver(name, grams, purity, priceOverride, purchaseDateMs, selectedFundingAccountId)
                                            AssetType.CERTIFICATE -> onAddCertificate(name.ifBlank { bankCertDefault }, grams, interestText.toDoubleOrNull() ?: 23.5, purchaseDateMs, selectedFundingAccountId)
                                        }
                                    },
                                    modifier = Modifier.weight(1.8f).height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                                ) {
                                    Text(stringResource(R.string.confirm_acquisition), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SellAssetBottomSheet(
    asset: AssetEntity,
    bankAccounts: List<Account>,
    currentValuation: Double,
    onDismiss: () -> Unit,
    onConfirmSell: (grams: Double, saleAmount: Double, destinationAccountId: String) -> Unit
) {
    var gramsText by remember { mutableStateOf(asset.weightGrams.toString()) }
    var saleAmountText by remember { mutableStateOf(currentValuation.toString()) }
    var selectedAccountId by remember { mutableStateOf(bankAccounts.firstOrNull()?.id ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.liquidate_asset_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )
            Text(
                text = stringResource(R.string.liquidate_desc, asset.name),
                fontSize = 14.sp,
                color = TextLight
            )

            if (asset.type != AssetType.CERTIFICATE) {
                OutlinedTextField(
                    value = gramsText,
                    onValueChange = { gramsText = it },
                    label = { Text(stringResource(R.string.amount_to_sell_grams)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            OutlinedTextField(
                value = saleAmountText,
                onValueChange = { saleAmountText = it },
                label = { Text(stringResource(R.string.expected_cash_proceeds)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Text(
                text = stringResource(R.string.deposit_funds_to),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                bankAccounts.forEach { acc ->
                    Surface(
                        onClick = { selectedAccountId = acc.id },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedAccountId == acc.id) PinkPrimary.copy(alpha = 0.05f) else Color.Transparent,
                        border = BorderStroke(1.dp, if (selectedAccountId == acc.id) PinkPrimary else Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedAccountId == acc.id,
                                onClick = { selectedAccountId = acc.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${acc.name} (${getCurrencySymbol(acc.currency)})", fontSize = 14.sp, color = NavyDark)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val grams = gramsText.toDoubleOrNull() ?: asset.weightGrams
                    val saleAmount = saleAmountText.toDoubleOrNull() ?: currentValuation
                    if (saleAmount > 0 && selectedAccountId.isNotBlank()) {
                        onConfirmSell(grams, saleAmount, selectedAccountId)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text(stringResource(R.string.liquidate_now), fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

@Composable
private fun DiversityIndicator(
    label: String,
    value: Double,
    color: Color,
    iconRes: Int,
    modifier: Modifier = Modifier
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
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = label, fontSize = 12.sp, color = TextLight, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${String.format(Locale.getDefault(), "%,.0f", value)} ${stringResource(R.string.currency_egp)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = NavyDark
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetTypePickerBottomSheet(
    onDismiss: () -> Unit,
    onTypeSelected: (AssetType) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.choose_asset_type),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )
            
            AssetTypeOption(stringResource(R.string.asset_type_gold), stringResource(R.string.gold_desc), R.drawable.ic_asset_gold, Color(0xFFD97706)) {
                onTypeSelected(AssetType.GOLD)
            }
            AssetTypeOption(stringResource(R.string.asset_type_silver), stringResource(R.string.silver_desc), R.drawable.ic_asset_silver, Color(0xFF475569)) {
                onTypeSelected(AssetType.SILVER)
            }
            AssetTypeOption(stringResource(R.string.asset_type_certificate), stringResource(R.string.certificate_desc), R.drawable.ic_asset_certificate, Color(0xFF059669)) {
                onTypeSelected(AssetType.CERTIFICATE)
            }
        }
    }
}

@Composable
private fun AssetTypeOption(title: String, desc: String, iconRes: Int, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                Text(text = desc, fontSize = 12.sp, color = TextLight)
            }
        }
    }
}
