package com.oryno.piggy_ledger.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.NotificationParserPrefs
import com.oryno.piggy_ledger.service.BankNotificationListenerService

data class InstalledApp(val packageName: String, val label: String)

@Composable
fun NotificationParserSettingsView(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { NotificationParserPrefs(context) }
    val enabled by prefs.enabled.collectAsStateWithLifecycle(initialValue = false)
    val watched by prefs.watchedPackages.collectAsStateWithLifecycle(initialValue = emptySet())

    var hasAccess by remember { mutableStateOf(isListenerAccessGranted(context)) }
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    val scope = rememberCoroutineScope()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasAccess = isListenerAccessGranted(context)
                scope.launch {
                    apps = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { loadInstalledApps(context) }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            apps = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { loadInstalledApps(context) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = stringResource(R.string.notification_parser_title),
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (hasAccess)
                        stringResource(R.string.notification_access_granted)
                    else
                        stringResource(R.string.notification_access_required),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { openNotificationAccessSettings(context) },
                    enabled = !hasAccess
                ) {
                    Text(
                        if (hasAccess) stringResource(R.string.notification_access_granted)
                        else stringResource(R.string.grant_notification_access)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.notification_parser_enable),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.notification_parser_enable_desc),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = enabled && hasAccess,
                enabled = hasAccess,
                onCheckedChange = { value ->
                    scope.launch { prefs.setEnabled(value) }
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.notification_parser_select_apps),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.notification_parser_select_apps_desc),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(apps, key = { it.packageName }) { app ->
                val checked = app.packageName in watched
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = hasAccess) {
                            scope.launch {
                                if (checked) prefs.removePackage(app.packageName)
                                else prefs.addPackage(app.packageName)
                            }
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        enabled = hasAccess,
                        onCheckedChange = null
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(app.label, style = MaterialTheme.typography.bodyLarge)
                        Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun isListenerAccessGranted(context: Context): Boolean {
    val enabled = NotificationManagerCompat.getEnabledListenerPackages(context)
    return enabled.contains(context.packageName)
}

private fun openNotificationAccessSettings(context: Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private fun loadInstalledApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    return pm.getInstalledPackages(0)
        .filter { pkg ->
            val ai = pkg.applicationInfo ?: return@filter false
            (ai.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        }
        .map { pkg ->
            InstalledApp(
                packageName = pkg.packageName,
                label = pm.getApplicationLabel(pkg.applicationInfo!!).toString()
            )
        }
        .sortedBy { it.label.lowercase() }
}
