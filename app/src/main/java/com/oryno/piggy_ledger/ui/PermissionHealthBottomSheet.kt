package com.oryno.piggy_ledger.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextDark
import com.oryno.piggy_ledger.ui.theme.TextLight

object PermissionUtils {
    fun hasSmsPermission(context: Context): Boolean {
        val receiveGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        return receiveGranted || readGranted
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasMicrophonePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openNotificationSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        } catch (_: Exception) {}
        openAppSettings(context)
    }
}

/**
 * Health checker composable that automatically monitors permission states
 * on app resume and displays the non-dismissible bottom sheet if critical
 * permissions (SMS and Notifications) are not enabled.
 */
@Composable
fun PermissionHealthChecker() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasSms by remember { mutableStateOf(PermissionUtils.hasSmsPermission(context)) }
    var hasNotification by remember { mutableStateOf(PermissionUtils.hasNotificationPermission(context)) }
    var hasContacts by remember { mutableStateOf(PermissionUtils.hasContactsPermission(context)) }
    var hasMic by remember { mutableStateOf(PermissionUtils.hasMicrophonePermission(context)) }

    var isManuallyClosed by remember { mutableStateOf(false) }

    val isCriticalGranted = hasSms && hasNotification

    // Lifecycle observer to re-check permissions every time app is resumed (e.g. from Settings or on launch)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasSms = PermissionUtils.hasSmsPermission(context)
                hasNotification = PermissionUtils.hasNotificationPermission(context)
                hasContacts = PermissionUtils.hasContactsPermission(context)
                hasMic = PermissionUtils.hasMicrophonePermission(context)

                // If critical permissions are ever disabled, immediately reset manual close flag so sheet reopens instantly
                if (!PermissionUtils.hasSmsPermission(context) || !PermissionUtils.hasNotificationPermission(context)) {
                    isManuallyClosed = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Show bottom sheet if critical permissions are missing, or if open and not closed yet
    val shouldShow = !isCriticalGranted || (!isManuallyClosed && !isCriticalGranted)

    if (shouldShow && !isManuallyClosed) {
        PermissionHealthBottomSheet(
            hasSms = hasSms,
            hasNotification = hasNotification,
            hasContacts = hasContacts,
            hasMic = hasMic,
            onRefreshPermissions = {
                hasSms = PermissionUtils.hasSmsPermission(context)
                hasNotification = PermissionUtils.hasNotificationPermission(context)
                hasContacts = PermissionUtils.hasContactsPermission(context)
                hasMic = PermissionUtils.hasMicrophonePermission(context)
            },
            onDismiss = {
                if (isCriticalGranted) {
                    isManuallyClosed = true
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionHealthBottomSheet(
    hasSms: Boolean,
    hasNotification: Boolean,
    hasContacts: Boolean,
    hasMic: Boolean,
    onRefreshPermissions: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isCriticalGranted = hasSms && hasNotification

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newState ->
            if (!isCriticalGranted) {
                newState != SheetValue.Hidden
            } else {
                true
            }
        }
    )

    // Intercept back button when critical permissions are not granted
    BackHandler(enabled = !isCriticalGranted) {
        // Prevent dismissal while critical permissions are missing
    }

    // Permission Launchers
    val smsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRefreshPermissions()
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onRefreshPermissions()
    }

    val contactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onRefreshPermissions()
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onRefreshPermissions()
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (isCriticalGranted) {
                onDismiss()
            }
        },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color.White,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = isCriticalGranted
        ),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color(0xFFE2E8F0)
            )
        },
        modifier = Modifier.testTag("permission_health_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Mascot Image at top + Close Button on Top Right (only unlocked)
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Centered Piggy Mascot Image
                Image(
                    painter = painterResource(id = R.drawable.img_piggy_hello),
                    contentDescription = "Piggy Mascot",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .align(Alignment.Center)
                )

                // Close Button appears only when critical permissions are enabled
                if (isCriticalGranted) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .align(Alignment.TopEnd)
                            .testTag("permission_sheet_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title & Subtitle (Clean & Minimal)
            Text(
                text = stringResource(R.string.permissions_sheet_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.permissions_sheet_subtitle),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = TextLight,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Minimalist List of Permission Toggle Rows
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1. SMS (Critical)
                PermissionToggleRow(
                    icon = Icons.Default.Sms,
                    title = stringResource(R.string.permissions_sms_title),
                    subtitle = stringResource(R.string.permissions_sms_desc),
                    isChecked = hasSms,
                    isRequired = true,
                    onToggle = { enable ->
                        if (enable) {
                            smsLauncher.launch(
                                arrayOf(
                                    Manifest.permission.RECEIVE_SMS,
                                    Manifest.permission.READ_SMS
                                )
                            )
                        } else {
                            PermissionUtils.openAppSettings(context)
                        }
                    }
                )

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // 2. Notifications (Critical)
                PermissionToggleRow(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.permissions_notif_title),
                    subtitle = stringResource(R.string.permissions_notif_desc),
                    isChecked = hasNotification,
                    isRequired = true,
                    onToggle = { enable ->
                        if (enable) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                PermissionUtils.openNotificationSettings(context)
                            }
                        } else {
                            PermissionUtils.openNotificationSettings(context)
                        }
                    }
                )

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // 3. Contacts (Optional)
                PermissionToggleRow(
                    icon = Icons.Default.Contacts,
                    title = stringResource(R.string.permissions_contacts_title),
                    subtitle = stringResource(R.string.permissions_contacts_desc),
                    isChecked = hasContacts,
                    isRequired = false,
                    onToggle = { enable ->
                        if (enable) {
                            contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                        } else {
                            PermissionUtils.openAppSettings(context)
                        }
                    }
                )

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // 4. Microphone (Optional)
                PermissionToggleRow(
                    icon = Icons.Default.Mic,
                    title = stringResource(R.string.permissions_mic_title),
                    subtitle = stringResource(R.string.permissions_mic_desc),
                    isChecked = hasMic,
                    isRequired = false,
                    onToggle = { enable ->
                        if (enable) {
                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            PermissionUtils.openAppSettings(context)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Bottom Action Button
            if (isCriticalGranted) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("permission_continue_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PinkPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.permissions_continue_btn),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            } else {
                Button(
                    onClick = {
                        if (!hasSms) {
                            smsLauncher.launch(
                                arrayOf(
                                    Manifest.permission.RECEIVE_SMS,
                                    Manifest.permission.READ_SMS
                                )
                            )
                        } else if (!hasNotification) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                PermissionUtils.openNotificationSettings(context)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("permission_grant_missing_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PinkPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.permissions_grant_all_critical),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    isRequired: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isChecked) Color(0xFFF0FDF4)
                    else if (isRequired) Color(0xFFFFF1F2)
                    else Color(0xFFF8FAFC)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isChecked) Color(0xFF16A34A)
                else if (isRequired) PinkPrimary
                else Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Subtitle
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                if (isRequired && !isChecked) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFFFF1F2)
                    ) {
                        Text(
                            text = stringResource(R.string.permissions_required_badge),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PinkPrimary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = TextLight
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Clean Material 3 Switch Toggle
        Switch(
            checked = isChecked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PinkPrimary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFCBD5E1),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
