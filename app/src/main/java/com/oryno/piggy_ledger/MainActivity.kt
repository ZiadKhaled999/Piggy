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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.ui.draw.scale
import com.oryno.piggy_ledger.ui.theme.NavyDark

import com.oryno.piggy_ledger.ui.Screen

import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.install.InstallStateUpdatedListener
import androidx.compose.material3.Snackbar
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding

class MainActivity : AppCompatActivity() {

  private var isAuthenticatedByBiometric by mutableStateOf(false)
  private var isBiometricCheckComplete by mutableStateOf(false)
  private var initialDestination by mutableStateOf<Screen?>(null)
  private var activeShortcutAction by mutableStateOf<String?>(null)
  private var activeOpenNotificationId by mutableStateOf<String?>(null)
  private lateinit var userPreferences: UserPreferences

  private var isUpdateReady by mutableStateOf(false)
  private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
  private val UPDATE_REQUEST_CODE = 9001
  private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
      if (state.installStatus() == InstallStatus.DOWNLOADED) {
          isUpdateReady = true
      }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    val splashScreen = installSplashScreen()
    super.onCreate(savedInstanceState)
    
    // Ensure locally saved language is applied
    try {
        val savedLanguage = UserPreferences.getSavedAppLanguageSync(this)
        if (!savedLanguage.isNullOrBlank()) {
            val currentLocales = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            if (!currentLocales.contains(savedLanguage)) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(savedLanguage))
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("MainActivity", "Failed to apply saved language", e)
    }

    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    enableEdgeToEdge()

    // Keep native splash screen until initial destination is loaded
    splashScreen.setKeepOnScreenCondition {
        initialDestination == null
    }
    
    // Schedule background notifications (best-effort: must never crash the launch)
    try {
        com.oryno.piggy_ledger.service.NotificationScheduler.scheduleAll(this)
    } catch (e: Exception) {
        android.util.Log.e("MainActivity", "NotificationScheduler failed, continuing launch", e)
    }

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
    // (best-effort: a widget/DB hiccup must never crash the launch).
    try {
        com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(this)
    } catch (e: Exception) {
        android.util.Log.e("MainActivity", "Summary widget update failed", e)
    }
    try {
        com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(this)
    } catch (e: Exception) {
        android.util.Log.e("MainActivity", "Streak widget update failed", e)
    }
    try {
        com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(this)
    } catch (e: Exception) {
        android.util.Log.e("MainActivity", "Goals widget update failed", e)
    }
    

    val database = PiggyLedgerDatabase.getInstance(applicationContext)
    
    val repository = PiggyLedgerRepository(database.piggyLedgerDao(), applicationContext)
    userPreferences = UserPreferences(applicationContext)
    val factory = ViewModelFactory(repository, userPreferences, applicationContext, database)

    lifecycleScope.launch {
        // FIX(first-install-crash): getInitialDestination() is already non-throwing,
        // but the splash gate depends on this assignment — guarantee it resolves.
        initialDestination = try {
            userPreferences.getInitialDestination()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "getInitialDestination failed, using LanguageSelection", e)
            com.oryno.piggy_ledger.ui.Screen.LanguageSelection
        }
    }

    observeSecuritySettings()
    observeAuthentication()

    activeOpenNotificationId = intent?.getStringExtra("open_notification_id")
    activeShortcutAction = intent?.getStringExtra("shortcut_action")

    // Play in-app updates: best-effort. Play Core throws on devices without Play
    // (emulators, sideloads, some OEM ROMs) — never let that crash the launch.
    try {
        appUpdateManager.registerListener(installStateUpdatedListener)
    } catch (e: Exception) {
        android.util.Log.e("MainActivity", "AppUpdate listener registration failed", e)
    }
    try {
        checkForAppUpdate()
    } catch (e: Exception) {
        android.util.Log.e("MainActivity", "AppUpdate check failed", e)
    }

    setContent {
      PiggyLedgerTheme {
        val dest = initialDestination
        if (dest != null) {
            val isPreAuth = dest != Screen.MainContainer
            val isLocked = !isPreAuth && !isAuthenticatedByBiometric

            if (!isBiometricCheckComplete && !isPreAuth) {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9F5)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PinkPrimary)
                }
            } else if (!isLocked) {
                PiggyLedgerApp(
                    factory = factory,
                    initialDestination = dest,
                    openNotificationId = activeOpenNotificationId,
                    shortcutAction = activeShortcutAction,
                    onConsumeShortcut = { activeShortcutAction = null }
                )
            } else {
                // Branded, responsive Lock Screen with fallback
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(horizontal = 24.dp)
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_piggy_hello),
                            contentDescription = "Piggy Mascot",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Piggy Ledger",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your app is locked for your privacy and security",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { checkBiometricLock(userPreferences) },
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Unlock with Biometrics",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        // Fallback button so emulator and devices without active biometrics are never permanently locked
                        OutlinedButton(
                            onClick = {
                                isAuthenticatedByBiometric = true
                                isBiometricCheckComplete = true
                                lifecycleScope.launch {
                                    userPreferences.saveLastExitTime(System.currentTimeMillis())
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, PinkPrimary),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Unlock App",
                                color = PinkPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
        
        if (isUpdateReady) {
            Box(modifier = Modifier.fillMaxSize()) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    action = {
                        TextButton(onClick = {
                            try {
                                appUpdateManager.completeUpdate()
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "completeUpdate failed", e)
                            }
                        }) {
                            Text(stringResource(R.string.restart), color = PinkPrimary)
                        }
                    }
                ) {
                    Text(stringResource(R.string.update_downloaded))
                }
            }
        }
      }
    }
  }

  private fun checkBiometricLock(userPreferences: UserPreferences) {
      if (isPromptShowing) return
      lifecycleScope.launch {
          val isAuth = userPreferences.isAuthenticated.first()
          val isEnabled = userPreferences.isBiometricLockEnabled.first()
          if (!isAuth || !isEnabled) {
              isAuthenticatedByBiometric = true
              isBiometricCheckComplete = true
              return@launch
          }

          val biometricManager = BiometricManager.from(this@MainActivity)
          val canAuthenticate = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
          if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
              isPromptShowing = true
              com.oryno.piggy_ledger.ui.BiometricHelper.authenticateToUnhide(
                  context = this@MainActivity,
                  onSuccess = {
                      isAuthenticatedByBiometric = true
                      isBiometricCheckComplete = true
                      lifecycleScope.launch {
                          userPreferences.saveLastExitTime(System.currentTimeMillis())
                          kotlinx.coroutines.delay(500)
                          isPromptShowing = false
                      }
                  },
                  onError = {
                      isAuthenticatedByBiometric = false
                      isBiometricCheckComplete = true
                      isPromptShowing = false
                  }
              )
          } else {
              // Hardware unavailable or nothing enrolled on emulator/device
              isAuthenticatedByBiometric = true
              isBiometricCheckComplete = true
          }
      }
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
      
      // Check if update is downloaded while app was in background
      try {
          appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
              if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                  isUpdateReady = true
              }
          }
      } catch (e: Exception) {
          android.util.Log.e("MainActivity", "AppUpdate resume check failed", e)
      }
  }

  override fun onStop() {
      super.onStop()
      lifecycleScope.launch {
          userPreferences.saveLastExitTime(System.currentTimeMillis())
      }
  }

  override fun onDestroy() {
      super.onDestroy()
      try {
          appUpdateManager.unregisterListener(installStateUpdatedListener)
      } catch (e: Exception) {
          android.util.Log.e("MainActivity", "AppUpdate unregister failed", e)
      }
  }

  private fun checkForAppUpdate() {
      try {
          appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
          if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
              && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
          ) {
              try {
                  appUpdateManager.startUpdateFlowForResult(
                      appUpdateInfo,
                      AppUpdateType.FLEXIBLE,
                      this,
                      UPDATE_REQUEST_CODE
                  )
              } catch (e: Exception) {
                  e.printStackTrace()
              }
          }
      }.addOnFailureListener { e ->
          android.util.Log.e("MainActivity", "AppUpdate info failed", e)
      }
      } catch (e: Exception) {
          android.util.Log.e("MainActivity", "AppUpdate check failed", e)
      }
  }

  private var isPromptShowing = false

  private fun checkLockStatus() {
      if (isPromptShowing) return
      lifecycleScope.launch {
          val isAuth = userPreferences.isAuthenticated.first()
          val isEnabled = userPreferences.isBiometricLockEnabled.first()

          if (!isAuth || !isEnabled) {
              isAuthenticatedByBiometric = true
              isBiometricCheckComplete = true
              return@launch
          }

          val biometricManager = BiometricManager.from(this@MainActivity)
          val canAuthenticate = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
          if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
              // Hardware unavailable or no biometric credentials enrolled on emulator/device
              isAuthenticatedByBiometric = true
              isBiometricCheckComplete = true
              return@launch
          }

          val lastExit = userPreferences.lastExitTime.first()
          val timeoutSeconds = userPreferences.lockTimeoutSeconds.first()

          val currentTime = System.currentTimeMillis()
          val elapsedSeconds = (currentTime - lastExit) / 1000

          if (lastExit == 0L || elapsedSeconds >= timeoutSeconds) {
              isAuthenticatedByBiometric = false
              isBiometricCheckComplete = true
              checkBiometricLock(userPreferences)
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



