package com.oryno.piggy_ledger

import android.os.Bundle
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.room.Room
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.PiggyLedgerRepository
import com.oryno.piggy_ledger.data.UserPreferences
import com.oryno.piggy_ledger.ui.PiggyLedgerApp
import com.oryno.piggy_ledger.ui.ViewModelFactory
import com.oryno.piggy_ledger.ui.theme.PiggyLedgerTheme
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import com.oryno.piggy_ledger.ui.ExpressiveLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import android.view.WindowManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import com.posthog.PostHog
import kotlinx.coroutines.flow.combine

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import com.oryno.piggy_ledger.ui.theme.NavyDark

class MainActivity : AppCompatActivity() {

  private var isAuthenticatedByBiometric by mutableStateOf(false)
  private var isBiometricCheckComplete by mutableStateOf(false)
  private var activeShortcutAction by mutableStateOf<String?>(null)
  private var activeOpenNotificationId by mutableStateOf<String?>(null)
  private lateinit var userPreferences: UserPreferences

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    enableEdgeToEdge()
    
    // Schedule background notifications
    com.oryno.piggy_ledger.service.NotificationScheduler.scheduleAll(this)

    // Request Notification permission for Android 13+ (API 33+) if not granted
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    com.oryno.piggy_ledger.service.NotificationScheduler.scheduleAll(this)
                }
            }.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Update widgets so they reflect language changes or app launches
    com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(this)
    com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(this)
    com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(this)
    

    val database = PiggyLedgerDatabase.getInstance(applicationContext)
    
    val repository = PiggyLedgerRepository(database.piggyLedgerDao(), applicationContext)
    userPreferences = UserPreferences(applicationContext)
    val factory = ViewModelFactory(repository, userPreferences, applicationContext, database)

    observeSecuritySettings()
    observeAuthentication()

    activeOpenNotificationId = intent?.getStringExtra("open_notification_id")
    activeShortcutAction = intent?.getStringExtra("shortcut_action")

    setContent {
      PiggyLedgerTheme {
        val isLocked = (isBiometricCheckComplete && !isAuthenticatedByBiometric)

        if (isBiometricCheckComplete) {
            if (!isLocked) {
                PiggyLedgerApp(
                    factory = factory,
                    openNotificationId = activeOpenNotificationId,
                    shortcutAction = activeShortcutAction,
                    onConsumeShortcut = { activeShortcutAction = null }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = PinkPrimary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { checkBiometricLock(userPreferences) },
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                        ) {
                            Text("Unlock App")
                        }
                    }
                }
            }
        } else {
            // Loading state while checking preferences
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ExpressiveLoadingIndicator(size = 42.dp)
            }
        }
      }
    }
  }

  private fun checkBiometricLock(userPreferences: UserPreferences) {
      lifecycleScope.launch {
          val isEnabled = userPreferences.isBiometricLockEnabled.first()
          if (isEnabled) {
              val biometricManager = BiometricManager.from(this@MainActivity)
              val canAuthenticate = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
              if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                  showBiometricPrompt()
              } else {
                  // Hardware unavailable or nothing enrolled, fall back to allow access
                  // In a real app you might want to show a message or use another fallback.
                  isAuthenticatedByBiometric = true
                  isBiometricCheckComplete = true
              }
          } else {
              isAuthenticatedByBiometric = true
              isBiometricCheckComplete = true
          }
      }
  }

  private fun showBiometricPrompt() {
      val executor = ContextCompat.getMainExecutor(this)
      val biometricPrompt = BiometricPrompt(this, executor,
          object : BiometricPrompt.AuthenticationCallback() {
              override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                  super.onAuthenticationError(errorCode, errString)
                  isAuthenticatedByBiometric = false
                  isBiometricCheckComplete = true
              }

              override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                  super.onAuthenticationSucceeded(result)
                  isAuthenticatedByBiometric = true
                  isBiometricCheckComplete = true
              }

              override fun onAuthenticationFailed() {
                  super.onAuthenticationFailed()
                  // User failed, they can try again or the prompt stays up usually
              }
          })

      val promptInfo = BiometricPrompt.PromptInfo.Builder()
          .setTitle(getString(R.string.biometric_login_title))
          .setSubtitle(getString(R.string.biometric_login_subtitle))
          .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
          .build()

      biometricPrompt.authenticate(promptInfo)
  }

  private fun observeSecuritySettings() {
      lifecycleScope.launch {
          userPreferences.isScreenshotProtectionEnabled.collectLatest { isEnabled ->
              if (isEnabled) {
                  window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
              } else {
                  window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
              }
          }
      }
  }

  private fun observeAuthentication() {
      lifecycleScope.launch {
          combine(
              userPreferences.isAuthenticated,
              userPreferences.authUserEmail,
              userPreferences.authUserName,
              combine(
                  userPreferences.personalizedIntent,
                  userPreferences.personalizedIntensity,
                  userPreferences.savingMode
              ) { intent, intensity, mode -> Triple(intent, intensity, mode) }
          ) { authenticated, email, name, personalization ->
              val (intent, intensity, mode) = personalization
              PersonalizationAuthData(authenticated, email, name, intent, intensity, mode)
          }.collectLatest { data ->
              try {
                  if (data.authenticated && data.email.isNotBlank()) {
                      val props = mutableMapOf<String, Any>()
                      if (data.name.isNotBlank()) props["name"] = data.name
                      props["personalized_intent"] = data.intent
                      props["personalized_intensity"] = data.intensity
                      props["saving_mode"] = data.savingMode
                      props["app_version"] = BuildConfig.VERSION_NAME
                      props["locale"] = java.util.Locale.getDefault().toString()
                      props["plan_type"] = "free"
                      PostHog.identify(data.email, props)
                  } else {
                      PostHog.reset()
                  }
              } catch (e: Exception) {
                  android.util.Log.e("PostHog", "Failed to identify/reset", e)
              }
          }
      }
  }

  private data class PersonalizationAuthData(
      val authenticated: Boolean,
      val email: String,
      val name: String,
      val intent: Int,
      val intensity: Int,
      val savingMode: String
  )

  override fun onStart() {
      super.onStart()
      checkLockStatus()
  }

  override fun onResume() {
      super.onResume()
      // Reset auth if we've been gone too long
      checkLockStatus()
  }

  override fun onStop() {
      super.onStop()
      lifecycleScope.launch {
          userPreferences.saveLastExitTime(System.currentTimeMillis())
      }
  }

  private fun checkLockStatus() {
      lifecycleScope.launch {
          val isEnabled = userPreferences.isBiometricLockEnabled.first()
          val lastExit = userPreferences.lastExitTime.first()
          val timeoutSeconds = userPreferences.lockTimeoutSeconds.first()

          if (isEnabled) {
              val currentTime = System.currentTimeMillis()
              val elapsedSeconds = (currentTime - lastExit) / 1000

              if (lastExit == 0L || elapsedSeconds >= timeoutSeconds) {
                  isAuthenticatedByBiometric = false
                  isBiometricCheckComplete = false
                  checkBiometricLock(userPreferences)
              } else {
                  isAuthenticatedByBiometric = true
                  isBiometricCheckComplete = true
              }
          } else {
              isAuthenticatedByBiometric = true
              isBiometricCheckComplete = true
          }
      }
  }

  override fun onNewIntent(intent: Intent) {
      super.onNewIntent(intent)
      setIntent(intent)
      intent.getStringExtra("open_notification_id")?.let { activeOpenNotificationId = it }
      intent.getStringExtra("shortcut_action")?.let { activeShortcutAction = it }
  }
}



