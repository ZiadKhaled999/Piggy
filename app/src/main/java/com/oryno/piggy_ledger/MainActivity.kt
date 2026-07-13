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
import com.posthog.PostHog
import kotlinx.coroutines.flow.combine

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation

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

    // Update widgets so they reflect language changes or app launches
    com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(this)
    com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(this)
    com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(this)
    
    val permissionsToRequest = mutableListOf<String>()
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.RECEIVE_SMS)
    }
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.READ_SMS)
    }
    
    if (permissionsToRequest.isNotEmpty()) {
        requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
    }
    
    val database = PiggyLedgerDatabase.getInstance(applicationContext)
    
    val repository = PiggyLedgerRepository(database.piggyLedgerDao())
    userPreferences = UserPreferences(applicationContext)
    val factory = ViewModelFactory(repository, userPreferences, applicationContext)

    observeSecuritySettings()
    observeAuthentication()

    setContent {
      PiggyLedgerTheme {
        val pinLock by userPreferences.pinLock.collectAsStateWithLifecycle(null)
        val isLocked = (isBiometricCheckComplete && !isAuthenticatedByBiometric) || (pinLock != null && !isAuthenticatedByPin)

        if (isBiometricCheckComplete) {
            if (!isLocked) {
                PiggyLedgerApp(factory)
            } else {
                // Show a "Locked" screen
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = getString(R.string.security),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PinkPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (pinLock != null && !isAuthenticatedByPin) {
                            var enteredPin by remember { mutableStateOf("") }
                            var pinError by remember { mutableStateOf(false) }
                            
                            OutlinedTextField(
                                value = enteredPin,
                                onValueChange = { 
                                    if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                        enteredPin = it
                                        if (it == pinLock) {
                                            isAuthenticatedByPin = true
                                        }
                                    }
                                },
                                label = { Text(getString(R.string.enter_pin)) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                isError = pinError,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PinkPrimary,
                                    focusedLabelColor = PinkPrimary,
                                    cursorColor = PinkPrimary
                                )
                            )
                        }
                        
                        if (!isAuthenticatedByBiometric) {
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.Button(
                                onClick = { checkBiometricLock(userPreferences) },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                            ) {
                                Text(getString(R.string.continue_btn))
                            }
                        }
                    }
                }
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
              // Note: FLAG_SECURE is disabled in the streaming preview to prevent a black screen
              // On a real physical device, this would be uncommented/active.
              /*
              if (isEnabled) {
                  window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
              } else {
                  window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
              }
              */
          }
      }
  }

  private fun observeAuthentication() {
      lifecycleScope.launch {
          combine(
              userPreferences.isAuthenticated,
              userPreferences.authUserEmail,
              userPreferences.authUserName
          ) { authenticated, email, name ->
              Triple(authenticated, email, name)
          }.collectLatest { (authenticated, email, name) ->
              if (authenticated && email.isNotBlank()) {
                  val props = mutableMapOf<String, Any>()
                  if (name.isNotBlank()) props["name"] = name
                  PostHog.identify(email, props)
              } else {
                  PostHog.reset()
              }
          }
      }
  }

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


