package com.oryno.piggy_ledger.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.delay

@Composable
fun LanguageSelectionScreen(
    onLanguageSelected: () -> Unit,
    onAlreadyHaveAccount: () -> Unit
) {
    var selectedLanguage by remember { mutableStateOf<String?>("ar-EG") }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val isSmallScreen = configuration.screenHeightDp < 700 || configuration.screenWidthDp < 360
    
    val titleFontSize = if (isSmallScreen) 24.sp else 32.sp
    val descFontSize = if (isSmallScreen) 14.sp else 16.sp
    val btnFontSize = if (isSmallScreen) 16.sp else 18.sp
    val btnHeight = if (isSmallScreen) 54.dp else 64.dp
    val topSpacerHeight = if (isSmallScreen) 24.dp else 48.dp
    val middleSpacerHeight = if (isSmallScreen) 24.dp else 64.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isSmallScreen) 16.dp else 24.dp)
                .padding(top = if (isSmallScreen) 16.dp else 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(topSpacerHeight))
            
            Text(
                text = stringResource(id = R.string.select_language),
                fontSize = titleFontSize,
                fontWeight = FontWeight.Black,
                color = NavyDark,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = stringResource(id = R.string.choose_language_desc),
                fontSize = descFontSize,
                color = TextLight,
                textAlign = TextAlign.Center,
                lineHeight = if (isSmallScreen) 20.sp else 24.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(middleSpacerHeight))
            
            LanguageOption(
                title = stringResource(id = R.string.english),
                subtitle = stringResource(id = R.string.united_states),
                flagResId = R.drawable.ic_flag_us,
                isSelected = selectedLanguage == "en",
                isSmallScreen = isSmallScreen,
                onClick = { selectedLanguage = "en" }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LanguageOption(
                title = stringResource(id = R.string.arabic),
                subtitle = stringResource(id = R.string.saudi_arabia),
                flagResId = R.drawable.ic_flag_sa,
                isSelected = selectedLanguage == "ar",
                isSmallScreen = isSmallScreen,
                onClick = { selectedLanguage = "ar" }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LanguageOption(
                title = stringResource(id = R.string.egyptian),
                subtitle = stringResource(id = R.string.egypt),
                flagResId = R.drawable.ic_flag_eg,
                isSelected = selectedLanguage == "ar-EG",
                isSmallScreen = isSmallScreen,
                isPremium = true,
                onClick = { selectedLanguage = "ar-EG" }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            androidx.compose.animation.AnimatedVisibility(visible = selectedLanguage != null) {
                val message = when(selectedLanguage) {
                    "ar" -> "يمكنك تغيير اللغة لاحقاً من الإعدادات."
                    "ar-EG" -> "تقدر تغير اللغة بعدين من الإعدادات."
                    else -> "You can change this later in settings."
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFDADADA), RoundedCornerShape(25.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message,
                        color = PinkPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isSmallScreen) 12.sp else 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = if (isSmallScreen) 16.sp else 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isSmallScreen) 16.dp else 24.dp)
                .padding(bottom = if (isSmallScreen) 16.dp else 32.dp, top = 8.dp)
        ) {
            Button(
                onClick = {
                    selectedLanguage?.let {
                        com.oryno.piggy_ledger.data.UserPreferences(context).saveAppLanguageSync(it)
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(it))
                        com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
                        com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(context)
                        onLanguageSelected()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(btnHeight),
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
                    fontSize = btnFontSize,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    selectedLanguage?.let {
                        com.oryno.piggy_ledger.data.UserPreferences(context).saveAppLanguageSync(it)
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(it))
                        com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
                        com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(context)
                    }
                    onAlreadyHaveAccount()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(btnHeight),
                enabled = true,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = NavyDark
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = NavyDark
                )
            ) {
                Text(
                    text = stringResource(id = R.string.already_have_account),
                    fontSize = if (isSmallScreen) 14.sp else 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LanguageOption(
    title: String,
    subtitle: String,
    flagResId: Int,
    isSelected: Boolean,
    isSmallScreen: Boolean = false,
    isPremium: Boolean = false,
    onClick: () -> Unit
) {
    val optionHeight = if (isSmallScreen) 72.dp else 84.dp
    val flagSize = if (isSmallScreen) 36.dp else 48.dp
    val titleSize = if (isSmallScreen) 16.sp else 18.sp
    val subtitleSize = if (isSmallScreen) 12.sp else 14.sp

    Box(modifier = Modifier.fillMaxWidth()) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(optionHeight)
            .border(
                width = if (isSelected) 3.dp else 2.dp,
                color = if (isSelected) PinkPrimary else Color(0xFF94A3B8),
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
                .padding(horizontal = if (isSmallScreen) 16.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = flagResId),
                contentDescription = null,
                modifier = Modifier
                    .size(flagSize)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                Text(
                    text = subtitle,
                    fontSize = subtitleSize,
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
        if (isPremium) {
            val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-4).dp)
                    .size(24.dp)
                    .background(Color.White, CircleShape)
                    .border(1.5.dp, Color(0xFFFBBF24), CircleShape)
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.1f, h * 0.85f)
                        lineTo(w * 0.1f, h * 0.35f)
                        lineTo(w * 0.35f, h * 0.6f)
                        lineTo(w * 0.5f, h * 0.15f)
                        lineTo(w * 0.65f, h * 0.6f)
                        lineTo(w * 0.9f, h * 0.35f)
                        lineTo(w * 0.9f, h * 0.85f)
                        close()
                    }
                    drawPath(path = path, color = Color(0xFFFBBF24))
                }
            }
        }
    }
}
