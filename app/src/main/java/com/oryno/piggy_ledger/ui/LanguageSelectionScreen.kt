package com.oryno.piggy_ledger.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight

@Composable
fun LanguageSelectionScreen(onLanguageSelected: () -> Unit) {
    var selectedLanguage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = stringResource(id = R.string.select_language),
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = NavyDark,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = stringResource(id = R.string.choose_language_desc),
            fontSize = 16.sp,
            color = TextLight,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        LanguageOption(
            title = stringResource(id = R.string.english),
            subtitle = stringResource(id = R.string.united_states),
            flagResId = R.drawable.ic_flag_us,
            isSelected = selectedLanguage == "en",
            onClick = { selectedLanguage = "en" }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LanguageOption(
            title = stringResource(id = R.string.arabic),
            subtitle = stringResource(id = R.string.saudi_arabia),
            flagResId = R.drawable.ic_flag_sa,
            isSelected = selectedLanguage == "ar",
            onClick = { selectedLanguage = "ar" }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LanguageOption(
            title = stringResource(id = R.string.egyptian),
            subtitle = stringResource(id = R.string.egypt),
            flagResId = R.drawable.ic_flag_eg,
            isSelected = selectedLanguage == "ar-EG",
            onClick = { selectedLanguage = "ar-EG" }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = {
                selectedLanguage?.let {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(it))
                    onLanguageSelected()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            enabled = selectedLanguage != null,
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
fun LanguageOption(
    title: String,
    subtitle: String,
    flagResId: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) PinkPrimary else Color(0xFFF1F5F9),
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
                painter = painterResource(id = flagResId),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = TextLight
                )
            }
            
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
