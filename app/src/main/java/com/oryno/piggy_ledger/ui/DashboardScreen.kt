package com.oryno.piggy_ledger.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.shape.CircleShape
import androidx.core.os.LocaleListCompat
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.filled.Language
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import com.oryno.piggy_ledger.ui.theme.PurplePrimary
import com.oryno.piggy_ledger.ui.theme.AccentBlue

enum class SettingsMode {
    MAIN, FEEDBACK, RATING, BACKUP, RESTORE, LANGUAGE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: PiggyLedgerViewModel,
    onNavigateToCreateGoal: () -> Unit,
    onNavigateToMyGoals: () -> Unit,
    onNavigateToLoans: () -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }
    var settingsMode by remember { mutableStateOf(SettingsMode.MAIN) }
    val context = LocalContext.current

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportData { jsonString ->
                try {
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(jsonString.toByteArray())
                    }
                    Toast.makeText(context, context.getString(R.string.export_success), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.export_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val jsonString = stream.bufferedReader().use { it.readText() }
                    viewModel.importData(
                        jsonString = jsonString,
                        onComplete = {
                            Toast.makeText(context, context.getString(R.string.restore_success), Toast.LENGTH_SHORT).show()
                            showSettings = false
                        },
                        onError = { error ->
                            Toast.makeText(context, context.getString(R.string.restore_failed, error), Toast.LENGTH_LONG).show()
                        }
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.read_file_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Image(
                painter = painterResource(id = R.drawable.img_app_logo),
                contentDescription = stringResource(R.string.piggy_ledger_logo),
                modifier = Modifier.size(200.dp).clip(RoundedCornerShape(32.dp)),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.welcome_to_circle),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.choose_saving_journey),
                fontSize = 16.sp,
                color = TextLight,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onNavigateToMyGoals,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text(
                    stringResource(R.string.go_straight_dashboard),
                    color = PinkPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            DashboardCard(
                title = stringResource(R.string.start_new_goal),
                subtitle = stringResource(R.string.set_target),
                onClick = onNavigateToCreateGoal
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            DashboardCard(
                title = stringResource(R.string.payoffs_loans),
                subtitle = stringResource(R.string.manage_loans),
                onClick = onNavigateToLoans
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        IconButton(
            onClick = { 
                settingsMode = SettingsMode.MAIN
                showSettings = true 
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_icon), tint = NavyDark)
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                when (settingsMode) {
                    SettingsMode.MAIN -> {
                        Text(
                            stringResource(R.string.settings),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { 
                                settingsMode = SettingsMode.LANGUAGE
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PinkPrimary.copy(alpha = 0.03f)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, PinkPrimary.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_settings_language_1783459964887),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(stringResource(R.string.language), fontWeight = FontWeight.SemiBold, color = NavyDark, modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextLight)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                settingsMode = SettingsMode.FEEDBACK
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PinkPrimary.copy(alpha = 0.03f)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, PinkPrimary.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_settings_feedback),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(stringResource(R.string.give_feedback), fontWeight = FontWeight.SemiBold, color = NavyDark, modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextLight)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { 
                                settingsMode = SettingsMode.RATING
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PinkPrimary.copy(alpha = 0.03f)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, PinkPrimary.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_settings_rate),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(stringResource(R.string.rate_app), fontWeight = FontWeight.SemiBold, color = NavyDark, modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextLight)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { 
                                settingsMode = SettingsMode.BACKUP
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PinkPrimary.copy(alpha = 0.03f)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, PinkPrimary.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_settings_backup),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(stringResource(R.string.backup_data), fontWeight = FontWeight.SemiBold, color = NavyDark, modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextLight)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { 
                                settingsMode = SettingsMode.RESTORE
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PinkPrimary.copy(alpha = 0.03f)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, PinkPrimary.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_settings_restore),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(stringResource(R.string.restore_data), fontWeight = FontWeight.SemiBold, color = NavyDark, modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextLight)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    SettingsMode.LANGUAGE -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = { settingsMode = SettingsMode.MAIN }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back_icon),
                                    tint = NavyDark
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.language),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Box(
                            modifier = Modifier.fillMaxWidth().height(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_settings_language_1783459964887),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                        
                        SettingsLanguageOption(
                            title = stringResource(id = R.string.english),
                            subtitle = stringResource(id = R.string.united_states),
                            flagResId = R.drawable.ic_flag_us,
                            isSelected = currentLocale.startsWith("en"),
                            onClick = {
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        SettingsLanguageOption(
                            title = stringResource(id = R.string.arabic),
                            subtitle = stringResource(id = R.string.saudi_arabia),
                            flagResId = R.drawable.ic_flag_sa,
                            isSelected = (currentLocale.startsWith("ar") && !currentLocale.contains("EG")) || (currentLocale.isEmpty() && java.util.Locale.getDefault().language == "ar"),
                            onClick = {
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ar"))
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        SettingsLanguageOption(
                            title = stringResource(id = R.string.egyptian),
                            subtitle = stringResource(id = R.string.egypt),
                            flagResId = R.drawable.ic_flag_eg,
                            isSelected = currentLocale.contains("ar-EG"),
                            onClick = {
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ar-EG"))
                            }
                        )
                    }
                    SettingsMode.FEEDBACK -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = { settingsMode = SettingsMode.MAIN }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back_icon),
                                    tint = NavyDark
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.community_feedback),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Box(
                            modifier = Modifier.fillMaxWidth().height(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_settings_feedback),
                                contentDescription = stringResource(R.string.feedback_illustration),
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.join_community_board),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyDark
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.help_improve),
                                    fontSize = 13.sp,
                                    color = TextLight,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://piggy-ledger.featureos.app"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, context.getString(R.string.browser_error), Toast.LENGTH_SHORT).show()
                                }
                                showSettings = false
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PinkPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.open_feedback_board), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    SettingsMode.RATING -> {
                        var rating by remember { mutableIntStateOf(0) }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = { settingsMode = SettingsMode.MAIN }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back_icon),
                                    tint = NavyDark
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.rate_app_title),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Box(
                            modifier = Modifier.fillMaxWidth().height(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_settings_rate),
                                contentDescription = stringResource(R.string.rate_illustration),
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = stringResource(R.string.enjoying_piggy_ledger),
                            fontSize = 14.sp,
                            color = TextLight,
                            lineHeight = 20.sp
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 1..5) {
                                IconButton(
                                    onClick = { rating = i },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = if (i <= rating) Icons.Default.Star else Icons.Outlined.Star,
                                        contentDescription = "Star $i",
                                        tint = if (i <= rating) PinkPrimary else Color(0xFFCBD5E1),
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = {
                                val mailtoUrl = "mailto:albhyrytwamrwhy@gmail.com" +
                                        "?subject=" + Uri.encode("Piggy Ledger Rating") +
                                        "&body=" + Uri.encode("I rated Piggy Ledger $rating/5 stars!")
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse(mailtoUrl)
                                }
                                try {
                                    context.startActivity(Intent.createChooser(intent, "Send Rating"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, context.getString(R.string.email_error), Toast.LENGTH_SHORT).show()
                                }
                                showSettings = false
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PinkPrimary,
                                contentColor = Color.White
                            ),
                            enabled = rating > 0
                        ) {
                            Text(stringResource(R.string.send_rating), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    SettingsMode.BACKUP -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = { settingsMode = SettingsMode.MAIN }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back_icon),
                                    tint = NavyDark
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.backup_data_title),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Box(
                            modifier = Modifier.fillMaxWidth().height(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_settings_backup),
                                contentDescription = stringResource(R.string.backup_illustration),
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PinkPrimary.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.secure_local_export),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    stringResource(R.string.save_goals_desc),
                                    fontSize = 13.sp,
                                    color = TextLight,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                createDocumentLauncher.launch("piggy_ledger_backup.json")
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PinkPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Text(stringResource(R.string.create_backup_file), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    SettingsMode.RESTORE -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = { settingsMode = SettingsMode.MAIN }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back_icon),
                                    tint = NavyDark
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.restore_data_title),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Box(
                            modifier = Modifier.fillMaxWidth().height(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_settings_restore),
                                contentDescription = stringResource(R.string.restore_illustration),
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AccentBlue.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.import_json_backup),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    stringResource(R.string.restoring_data_replace),
                                    fontSize = 13.sp,
                                    color = TextLight,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                openDocumentLauncher.launch(arrayOf("application/json"))
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PinkPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Text(stringResource(R.string.select_backup_file), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
            }
        }
    }
}
}

@Composable
fun DashboardCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PinkPrimary.copy(alpha = 0.05f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, PinkPrimary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = NavyDark)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, fontSize = 14.sp, color = TextLight, fontWeight = FontWeight.Medium)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = PinkPrimary
            )
        }
    }
}

@Composable
fun SettingsLanguageOption(
    title: String,
    subtitle: String,
    flagResId: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) PinkPrimary else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PinkPrimary.copy(alpha = 0.05f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = flagResId),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
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
