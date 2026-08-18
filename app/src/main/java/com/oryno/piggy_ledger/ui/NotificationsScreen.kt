package com.oryno.piggy_ledger.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.NotificationItem
import com.oryno.piggy_ledger.data.NotificationRemoteManager
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    isPremium: Boolean,
    openNotificationId: String? = null
) {
    val context = LocalContext.current
    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var readIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var dismissedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    var selectedNotification by remember { mutableStateOf<NotificationItem?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var showFilterSheet by remember { mutableStateOf(false) }
    var filterType by remember { mutableStateOf("All") } // "All", "Unread", "Read"

    LaunchedEffect(Unit) {
        notifications = NotificationRemoteManager.getSavedNotifications(context, isPremium)
        readIds = NotificationRemoteManager.getReadIds(context)
        dismissedIds = NotificationRemoteManager.getDismissedIds(context)
        
        if (openNotificationId != null) {
            val toOpen = notifications.find { it.id == openNotificationId }
            if (toOpen != null) {
                if (!readIds.contains(toOpen.id)) {
                    NotificationRemoteManager.markAsRead(context, toOpen.id)
                    readIds = readIds + toOpen.id
                }
                selectedNotification = toOpen
                showBottomSheet = true
            }
        }
    }

    val filteredNotifications = remember(notifications, readIds, dismissedIds, filterType) {
        val notDismissed = notifications.filter { !dismissedIds.contains(it.id) }
        when (filterType) {
            "Unread" -> notDismissed.filter { !readIds.contains(it.id) }
            "Read" -> notDismissed.filter { readIds.contains(it.id) }
            else -> notDismissed
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NavyDark, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Announcements", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = NavyDark, modifier = Modifier.weight(1f))
            
            IconButton(
                onClick = { showFilterSheet = true },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = NavyDark, modifier = Modifier.size(20.dp))
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Showing: $filterType", fontSize = 14.sp, color = TextLight, fontWeight = FontWeight.Medium)
            
            TextButton(
                onClick = {
                    val allIds = notifications.map { it.id }
                    NotificationRemoteManager.markAllAsRead(context, allIds)
                    readIds = readIds + allIds
                }
            ) {
                Text("Mark all as read", color = PinkPrimary, fontWeight = FontWeight.Bold)
            }
        }

        if (filteredNotifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFCBD5E1))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No announcements yet", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("We'll notify you when there's news", fontSize = 14.sp, color = TextLight)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredNotifications) { item ->
                    val isRead = readIds.contains(item.id)
                    NotificationCard(
                        item = item, 
                        isRead = isRead,
                        onClick = {
                            if (!isRead) {
                                NotificationRemoteManager.markAsRead(context, item.id)
                                readIds = readIds + item.id
                            }
                            selectedNotification = item
                            showBottomSheet = true
                        }
                    )
                }
            }
        }
    }

    if (showBottomSheet && selectedNotification != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFFFF0F5), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(selectedNotification!!.title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = NavyDark)
                Spacer(modifier = Modifier.height(16.dp))
                Text(selectedNotification!!.body, fontSize = 15.sp, color = NavyDark, lineHeight = 24.sp)
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { showBottomSheet = false },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                ) {
                    Text("Got it", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text("Filter Announcements", fontSize = 22.sp, fontWeight = FontWeight.Black, color = NavyDark)
                Spacer(modifier = Modifier.height(24.dp))
                
                listOf("All", "Unread", "Read").forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { 
                                filterType = option 
                                showFilterSheet = false
                            }
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(option, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = NavyDark)
                        if (filterType == option) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = PinkPrimary)
                        }
                    }
                    if (option != "Read") {
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(item: NotificationItem, isRead: Boolean, onClick: () -> Unit) {
    val animatedBgColor by animateColorAsState(
        targetValue = if (isRead) Color.White else Color(0xFFFFF0F5),
        animationSpec = tween(durationMillis = 400),
        label = "notificationBgFade"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isRead) Color(0xFFF1F5F9) else PinkPrimary.copy(alpha = 0.2f),
        animationSpec = tween(durationMillis = 400),
        label = "notificationBorderFade"
    )
    val animatedIconBg by animateColorAsState(
        targetValue = if (isRead) Color(0xFFF8FAFC) else Color.White,
        animationSpec = tween(durationMillis = 400),
        label = "notificationIconBgFade"
    )
    val animatedIconTint by animateColorAsState(
        targetValue = if (isRead) Color(0xFF94A3B8) else PinkPrimary,
        animationSpec = tween(durationMillis = 400),
        label = "notificationIconTintFade"
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (isRead) TextLight else NavyDark.copy(alpha = 0.8f),
        animationSpec = tween(durationMillis = 400),
        label = "notificationTextColorFade"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = animatedBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, animatedBorderColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(animatedIconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = animatedIconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.body,
                    fontSize = 14.sp,
                    color = animatedTextColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
