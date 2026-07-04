package com.example

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
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
import com.example.data.PiggyLedgerDatabase
import com.example.data.PiggyLedgerRepository
import com.example.data.UserPreferences
import com.example.ui.PiggyLedgerApp
import com.example.ui.ViewModelFactory
import com.example.ui.theme.PiggyLedgerTheme

class MainActivity : ComponentActivity() {

  private val requestPermissionLauncher = registerForActivityResult(
      ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
      // Permission granted or rejected
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    val database = Room.databaseBuilder(
        applicationContext,
        PiggyLedgerDatabase::class.java,
        "piggy_ledger_db"
    ).build()
    
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


