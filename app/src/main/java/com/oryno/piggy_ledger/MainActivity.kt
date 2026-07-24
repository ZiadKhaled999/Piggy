package com.oryno.piggy_ledger

import android.os.Bundle
import android.Manifest
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
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import com.oryno.piggy_ledger.ui.theme.NavyDark

class MainActivity : AppCompatActivity() {

  private var isAuthenticatedByBiometric by mutableStateOf(false)
  private var isBiometricCheckComplete by mutableStateOf(false)
  private var isAuthenticatedByPin by mutableStateOf(false)
  private lateinit var userPreferences: UserPreferences

  private val requestPermissionsLauncher = registerForActivityResult(
      ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
      // Permissions granted or rejected
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    enableEdgeToEdge()
    
    // Schedule background notifications
    com.oryno.piggy_ledger.service.NotificationScheduler.scheduleAll(this)

    // Update widgets so they reflect language changes or app launches
    com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(this)
    com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(this)
    com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(this)
    

    val database = PiggyLedgerDatabase.getInstance(applicationContext)
    
    val repository = PiggyLedgerRepository(database.piggyLedgerDao())
    userPreferences = UserPreferences(applicationContext)
    val factory = ViewModelFactory(repository, userPreferences, applicationContext)

    observeSecuritySettings()
    observeAuthentication()

    setContent {
      PiggyLedgerTheme {
        val pinLock by userPreferences.pinLock.collectAsStateWithLifecycle(null)
        val activePin = pinLock?.takeIf { it.isNotBlank() }
        val isLocked = (isBiometricCheckComplete && !isAuthenticatedByBiometric) || (activePin != null && !isAuthenticatedByPin)

        if (isBiometricCheckComplete) {
            if (!isLocked) {
                PiggyLedgerApp(factory)
            } else {
                PinLockScreen(
                    pinLock = activePin,
                    isAuthenticatedByPin = isAuthenticatedByPin,
                    isAuthenticatedByBiometric = isAuthenticatedByBiometric,
                    onPinSuccess = { isAuthenticatedByPin = true },
                    onBiometricClick = { checkBiometricLock(userPreferences) }
                )
            }
        } else {
            // Loading state while checking preferences
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PinkPrimary)
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
      // Reset PIN auth if we've been gone too long
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
          val pinLockVal = userPreferences.pinLock.first()

          if (isEnabled || pinLockVal != null) {
              val currentTime = System.currentTimeMillis()
              val elapsedSeconds = (currentTime - lastExit) / 1000

              if (lastExit == 0L || elapsedSeconds >= timeoutSeconds) {
                  isAuthenticatedByBiometric = false
                  isAuthenticatedByPin = false
                  isBiometricCheckComplete = false
                  if (isEnabled) {
                      checkBiometricLock(userPreferences)
                  } else {
                      isBiometricCheckComplete = true
                  }
              } else {
                  isAuthenticatedByBiometric = true
                  isAuthenticatedByPin = true
                  isBiometricCheckComplete = true
              }
          } else {
              isAuthenticatedByBiometric = true
              isAuthenticatedByPin = true
              isBiometricCheckComplete = true
          }
      }
  }
}

@Composable
fun PinLockScreen(
    pinLock: String?,
    isAuthenticatedByPin: Boolean,
    isAuthenticatedByBiometric: Boolean,
    onPinSuccess: () -> Unit,
    onBiometricClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val cleanPin = pinLock?.trim() ?: ""
    val targetLength = if (cleanPin.length in 4..6) cleanPin.length else 4

    fun verifyPin() {
        val cleanEntered = enteredPin.trim()
        if (cleanPin.isEmpty() || cleanEntered == cleanPin) {
            onPinSuccess()
        } else {
            pinError = true
            scope.launch {
                delay(700)
                enteredPin = ""
                pinError = false
            }
        }
    }

    fun handleDigitPress(digit: String) {
        if (enteredPin.length < 6) {
            val newPin = enteredPin + digit
            enteredPin = newPin
            pinError = false

            // Auto-unlock if user typed exact PIN match
            if (cleanPin.isNotEmpty() && newPin == cleanPin) {
                onPinSuccess()
            }
        }
    }

    fun handleBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            pinError = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(PinkPrimary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = PinkPrimary
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = context.getString(R.string.security),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (pinError) context.getString(R.string.incorrect_pin) else context.getString(R.string.enter_pin),
                fontSize = 14.sp,
                color = if (pinError) Color(0xFFEF4444) else Color(0xFF64748B),
                fontWeight = if (pinError) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            val displayDots = maxOf(targetLength, enteredPin.length).coerceIn(4, 6)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until displayDots) {
                    val isFilled = i < enteredPin.length
                    val scale by animateFloatAsState(
                        targetValue = if (isFilled) 1.25f else 1.0f,
                        animationSpec = tween(durationMillis = 150),
                        label = "dotScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .scale(scale)
                            .background(
                                color = when {
                                    pinError -> Color(0xFFEF4444)
                                    isFilled -> PinkPrimary
                                    else -> Color.Transparent
                                },
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                color = when {
                                    pinError -> Color(0xFFEF4444)
                                    isFilled -> PinkPrimary
                                    else -> Color(0xFFCBD5E1)
                                },
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            val keypadRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("ok", "0", "backspace")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                keypadRows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "ok" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E))
                                            .clickable { verifyPin() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "OK",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                "backspace" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF1F5F9))
                                            .clickable { handleBackspace() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Backspace",
                                            tint = NavyDark,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF8FAFC))
                                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                                            .clickable { handleDigitPress(key) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NavyDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Prominent OK button below keypad
            Button(
                onClick = { verifyPin() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PinkPrimary,
                    disabledContainerColor = PinkPrimary.copy(alpha = 0.4f)
                ),
                enabled = enteredPin.isNotEmpty()
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OK",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}


