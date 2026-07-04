package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.R
import com.example.ui.theme.NavyDark
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.TextLight
import com.example.ui.theme.PurplePrimary

enum class SettingsMode {
    MAIN, FEEDBACK, RATING
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
                painter = painterResource(id = R.drawable.img_piggy_hello),
                contentDescription = null,
                modifier = Modifier.size(200.dp),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Welcome to Your Circle",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Choose how you'd like to start your saving journey.",
                fontSize = 16.sp,
                color = TextLight,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            DashboardCard(
                title = "Start New Goal",
                subtitle = "Set a target.",
                onClick = onNavigateToCreateGoal
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            DashboardCard(
                title = "Payoffs & Loans",
                subtitle = "Manage who you lent money to or who you owe.",
                onClick = onNavigateToLoans
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            TextButton(onClick = onNavigateToMyGoals) {
                Text("Go straight to Dashboard →", color = NavyDark, fontWeight = FontWeight.SemiBold)
            }
            
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
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = NavyDark)
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
                            "Settings & Preferences",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                settingsMode = SettingsMode.FEEDBACK
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = PurplePrimary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Give Feedback", fontWeight = FontWeight.SemiBold, color = NavyDark, modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextLight)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { 
                                settingsMode = SettingsMode.RATING
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = PinkPrimary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Rate the App", fontWeight = FontWeight.SemiBold, color = NavyDark, modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextLight)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    SettingsMode.FEEDBACK -> {
                        var username by remember { mutableStateOf("") }
                        var email by remember { mutableStateOf("") }
                        var message by remember { mutableStateOf("") }
                        
                        Text(
                            "Give Feedback",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = { Text("Message") },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:albhyrytwamrwhy@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "Feedback from $username")
                                    putExtra(Intent.EXTRA_TEXT, "Username: $username\nEmail: $email\n\nMessage:\n$message")
                                }
                                context.startActivity(Intent.createChooser(intent, "Send Feedback"))
                                showSettings = false
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyDark)
                        ) {
                            Text("Send via Email", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    SettingsMode.RATING -> {
                        var rating by remember { mutableIntStateOf(0) }
                        
                        Text(
                            "Rate the App",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            for (i in 1..5) {
                                IconButton(onClick = { rating = i }) {
                                    Icon(
                                        imageVector = if (i <= rating) Icons.Default.Star else Icons.Outlined.Star,
                                        contentDescription = "Star $i",
                                        tint = if (i <= rating) PinkPrimary else TextLight,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:albhyrytwamrwhy@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "Piggy Ledger Rating")
                                    putExtra(Intent.EXTRA_TEXT, "I rated the app: $rating out of 5 stars!")
                                }
                                context.startActivity(Intent.createChooser(intent, "Send Rating"))
                                showSettings = false
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                            enabled = rating > 0
                        ) {
                            Text("Send Rating via Email", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, fontSize = 14.sp, color = TextLight)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextLight
            )
        }
    }
}
