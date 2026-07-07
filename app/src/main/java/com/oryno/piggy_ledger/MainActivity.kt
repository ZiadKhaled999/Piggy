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

class MainActivity : AppCompatActivity() {

  private val requestPermissionLauncher = registerForActivityResult(
      ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
      // Permission granted or rejected
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Update widgets so they reflect language changes or app launches
    com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(this)
    com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(this)
    com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(this)
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    val database = PiggyLedgerDatabase.getInstance(applicationContext)
    
    val repository = PiggyLedgerRepository(database.piggyLedgerDao())
    val userPreferences = UserPreferences(applicationContext)
    val factory = ViewModelFactory(repository, userPreferences, applicationContext)

    setContent {
      PiggyLedgerTheme {
        PiggyLedgerApp(factory)
      }
    }
  }
}


