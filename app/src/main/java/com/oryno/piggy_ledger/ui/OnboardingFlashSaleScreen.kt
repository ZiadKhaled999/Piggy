package com.oryno.piggy_ledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight

@Composable
fun OnboardingFlashSaleScreen(
    titleFontSize: androidx.compose.ui.unit.TextUnit,
    subtitleFontSize: androidx.compose.ui.unit.TextUnit,
    titleLineHeight: androidx.compose.ui.unit.TextUnit,
    subtitleLineHeight: androidx.compose.ui.unit.TextUnit,
    isSmallScreen: Boolean
) {
    var selectedPlan by remember { mutableStateOf("annual") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.onboarding_flash_sale_title),
            fontSize = titleFontSize,
            fontWeight = FontWeight.ExtraBold,
            color = NavyDark,
            textAlign = TextAlign.Center,
            lineHeight = titleLineHeight
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.onboarding_flash_sale_subtitle),
            fontSize = subtitleFontSize,
            color = TextLight,
            textAlign = TextAlign.Center,
            lineHeight = subtitleLineHeight,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Large -50% Text
        Text(
            text = stringResource(R.string.onboarding_flash_sale_discount),
            fontSize = if (isSmallScreen) 50.sp else 64.sp,
            fontWeight = FontWeight.Black,
            color = PinkPrimary, // Using Piggy Ledger's primary color
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Don't Miss It Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "🌿", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.onboarding_flash_sale_expires),
                fontSize = if (isSmallScreen) 24.sp else 28.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "🌿", fontSize = 24.sp)
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Two Subscription Cards
        // Annual
        SubscriptionCard(
            title = stringResource(R.string.onboarding_flash_sale_annual),
            description = stringResource(R.string.onboarding_flash_sale_annual_desc, stringResource(R.string.piggy_ledger_brand)),
            newPrice = "$49.99",
            oldPrice = "$99.99",
            isSelected = selectedPlan == "annual",
            onClick = { selectedPlan = "annual" }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Monthly
        SubscriptionCard(
            title = stringResource(R.string.onboarding_flash_sale_monthly),
            description = stringResource(R.string.onboarding_flash_sale_monthly_desc),
            newPrice = "$4.99",
            oldPrice = "$9.99",
            isSelected = selectedPlan == "monthly",
            onClick = { selectedPlan = "monthly" }
        )
    }
}

@Composable
private fun SubscriptionCard(
    title: String,
    description: String,
    newPrice: String,
    oldPrice: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) NavyDark else Color(0xFFF8FAFC)
    val borderColor = if (isSelected) NavyDark else Color(0xFFE2E8F0)
    val titleColor = if (isSelected) Color.White else NavyDark
    val descColor = if (isSelected) Color(0xFF94A3B8) else TextLight
    val newPriceColor = if (isSelected) Color.White else NavyDark
    val oldPriceColor = if (isSelected) Color(0xFF94A3B8) else TextLight
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = descColor
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = newPrice,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = newPriceColor
                )
                Text(
                    text = oldPrice,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = oldPriceColor,
                    textDecoration = TextDecoration.LineThrough
                )
            }
        }
    }
}
