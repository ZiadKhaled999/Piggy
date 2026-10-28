package com.oryno.piggy_ledger.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import kotlinx.coroutines.delay
import kotlin.math.abs

data class ServiceCardData(
    val id: String,
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBgColor: Color
)

@Composable
fun ServicesHubSection(
    onNavigateToSeeAll: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToWealth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val services = remember {
        listOf(
            ServiceCardData(
                id = "subscriptions",
                titleRes = R.string.subscriptions_title,
                subtitleRes = R.string.subscriptions_desc,
                icon = Icons.Default.EventRepeat,
                iconColor = Color(0xFFDB2777),
                iconBgColor = Color(0xFFFDF2F8)
            ),
            ServiceCardData(
                id = "wealth",
                titleRes = R.string.wealth_assets_title,
                subtitleRes = R.string.wealth_assets_desc,
                icon = Icons.Default.MonetizationOn,
                iconColor = Color(0xFFD97706),
                iconBgColor = Color(0xFFFEF3C7)
            )
        )
    }

    val virtualCount = 30000
    val initialPage = remember(services.size) {
        if (services.isNotEmpty()) {
            (virtualCount / 2) - ((virtualCount / 2) % services.size)
        } else 0
    }
    val pagerState = rememberPagerState(initialPage = initialPage) { virtualCount }

    LaunchedEffect(pagerState) {
        while (true) {
            delay(3000L)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(
                    page = pagerState.currentPage + 1,
                    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.services_and_features),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateToSeeAll() }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.see_all),
                    fontSize = 13.sp,
                    color = PinkPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = PinkPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Swipeable Cards (Only Subscriptions and Wealth)
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val actualIndex = if (services.isNotEmpty()) (page % services.size + services.size) % services.size else 0
            val item = services[actualIndex]
            val onCardClick = {
                when (item.id) {
                    "subscriptions" -> onNavigateToSubscriptions()
                    "wealth" -> onNavigateToWealth()
                    else -> onNavigateToSeeAll()
                }
            }

            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val scale = 0.95f + (1f - 0.95f) * (1f - abs(pageOffset).coerceIn(0f, 1f))
            val alpha = 0.75f + (1f - 0.75f) * (1f - abs(pageOffset).coerceIn(0f, 1f))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onCardClick() },
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(item.iconBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = item.iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(item.titleRes),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(item.subtitleRes),
                            fontSize = 12.sp,
                            color = TextLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Pager Indicator Dots
        if (services.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentActualIndex = if (services.isNotEmpty()) {
                    (pagerState.currentPage % services.size + services.size) % services.size
                } else 0
                repeat(services.size) { index ->
                    val isSelected = currentActualIndex == index
                    val width by animateDpAsState(targetValue = if (isSelected) 14.dp else 5.dp, label = "dotWidth")
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.5.dp)
                            .height(5.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(if (isSelected) PinkPrimary else Color(0xFFCBD5E1))
                    )
                }
            }
        }
    }
}
