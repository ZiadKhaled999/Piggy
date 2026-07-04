package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.NavyDark
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.TextLight

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var currentPage by remember { mutableIntStateOf(0) }
    
    val pages = listOf(
        Triple(R.drawable.img_piggy_hello, "Hello welcome to Piggy Ledger", "Your modern way to save and track resources."),
        Triple(R.drawable.img_piggy_pool, "Track Your Savings", "Create goals and track your progress. Whether it's for a trip, a gift, or a group project."),
        Triple(R.drawable.img_piggy_track, "Track Progress", "Updates on contributions and goal completion. Stay motivated as you see the progress bar fill up."),
        Triple(R.drawable.img_piggy_hello, "Ready to Start?", "Let's set up your first goal and begin your journey towards smarter savings.") // Reusing hello image for last screen as we didn't generate 4
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        Image(
            painter = painterResource(id = pages[currentPage].first),
            contentDescription = null,
            modifier = Modifier.size(280.dp),
            contentScale = ContentScale.Fit
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = pages[currentPage].second,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = NavyDark,
            textAlign = TextAlign.Center,
            lineHeight = 40.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = pages[currentPage].third,
            fontSize = 16.sp,
            color = TextLight,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(pages.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(if (index == currentPage) 24.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (index == currentPage) NavyDark else NavyDark.copy(alpha = 0.1f))
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentPage > 0) {
                TextButton(onClick = { currentPage-- }) {
                    Text("Back", color = TextLight, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Spacer(modifier = Modifier.width(64.dp))
            }
            
            Button(
                onClick = { 
                    if (currentPage < pages.size - 1) currentPage++ 
                    else onComplete()
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (currentPage > 0) 16.dp else 0.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyDark)
            ) {
                Text(if (currentPage == pages.size - 1) "Get Started" else "Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
