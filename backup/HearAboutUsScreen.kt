package com.oryno.piggy_ledger.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight

private data class OptionItem(
    val title: String,
    val iconRes: Int
)

@Composable
fun HearAboutUsScreen(
    onContinue: (String) -> Unit
) {
    var selectedSource by remember { mutableStateOf<String?>(null) }
    
    val brandName = stringResource(id = R.string.piggy_ledger_brand)
    val fullTitleText = stringResource(id = R.string.how_did_you_hear_about_us, brandName)
    val annotatedTitle = remember(fullTitleText, brandName) {
        buildAnnotatedString {
            val startIndex = fullTitleText.indexOf(brandName)
            if (startIndex != -1) {
                append(fullTitleText.substring(0, startIndex))
                withStyle(style = SpanStyle(color = PinkPrimary)) {
                    append(brandName)
                }
                append(fullTitleText.substring(startIndex + brandName.length))
            } else {
                append(fullTitleText)
            }
        }
    }

    val options = listOf(
        OptionItem(stringResource(id = R.string.source_facebook), R.drawable.ic_logo_facebook),
        OptionItem(stringResource(id = R.string.source_tiktok), R.drawable.ic_logo_tiktok),
        OptionItem(stringResource(id = R.string.source_youtube), R.drawable.ic_logo_youtube),
        OptionItem(stringResource(id = R.string.source_friend_or_family), R.drawable.ic_logo_friend),
        OptionItem(stringResource(id = R.string.source_creator_or_influencer), R.drawable.ic_logo_influencer),
        OptionItem(stringResource(id = R.string.source_search_engine), R.drawable.ic_logo_search),
        OptionItem(stringResource(id = R.string.source_google_play), R.drawable.ic_logo_google_play),
        OptionItem(stringResource(id = R.string.source_other), R.drawable.ic_logo_other)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = annotatedTitle,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = NavyDark,
            textAlign = TextAlign.Center,
            lineHeight = 38.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(options) { item ->
                HearAboutUsOption(
                    title = item.title,
                    iconRes = item.iconRes,
                    isSelected = selectedSource == item.title,
                    onClick = { selectedSource = item.title }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { selectedSource?.let { onContinue(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            enabled = selectedSource != null,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NavyDark,
                disabledContainerColor = Color(0xFFE2E8F0),
                disabledContentColor = Color(0xFF94A3B8)
            )
        ) {
            Text(
                text = stringResource(id = R.string.continue_btn),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HearAboutUsOption(
    title: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .border(
                width = if (isSelected) 3.dp else 2.dp,
                color = if (isSelected) PinkPrimary else Color(0xFFCBD5E1),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PinkPrimary.copy(alpha = 0.05f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                modifier = Modifier.size(28.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = NavyDark,
                modifier = Modifier.weight(1f)
            )
            
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = PinkPrimary,
                    unselectedColor = Color(0xFFCBD5E1)
                )
            )
        }
    }
}
