package com.moneydropper

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.moneydropper.ui.MoneyDropperNavGraph
import com.moneydropper.ui.screens.PinLockScreen
import com.moneydropper.ui.theme.MoneyDropperTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* proceed regardless */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MoneyDropperTheme(darkTheme = true) {
                var unlocked by remember { mutableStateOf(false) }

                if (!unlocked) {
                    PinLockScreen(onUnlocked = { unlocked = true })
                } else {
                    val navController = rememberNavController()
                    MoneyDropperNavGraph(navController = navController)
                }
            }
        }
    }
}
