package com.moneydropper.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneydropper.ui.theme.*

private const val PREFS = "money_dropper_prefs"
private const val KEY_PIN = "app_pin"
private const val DEFAULT_PIN = "1234"

@Composable
fun PinLockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var isSetupMode by remember { mutableStateOf(!hasPinSet(context)) }
    var confirmPin by remember { mutableStateOf("") }
    var awaitingConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        if (error) {
            kotlinx.coroutines.delay(600)
            entered = ""
            error = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("💰", fontSize = 52.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Money Dropper",
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = GreenPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    isSetupMode && !awaitingConfirm -> "Set a 4-digit PIN"
                    isSetupMode && awaitingConfirm  -> "Confirm your PIN"
                    else                            -> "Enter PIN"
                },
                color = SubtleGray,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(32.dp))

            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(
                                when {
                                    error            -> ErrorRed
                                    i < entered.length -> GreenPrimary
                                    else             -> SurfaceElevated
                                },
                                CircleShape
                            )
                            .border(1.dp, if (error) ErrorRed else Color(0xFF444444), CircleShape)
                    )
                }
            }

            if (error) {
                Spacer(Modifier.height(8.dp))
                Text("Incorrect PIN", color = ErrorRed, fontSize = 12.sp)
            }

            Spacer(Modifier.height(40.dp))

            // Numpad
            val keys = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")
            val grid = keys.chunked(3)
            grid.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(Modifier.size(72.dp))
                        } else {
                            NumKey(
                                label = key,
                                isBackspace = key == "⌫",
                                onClick = {
                                    when (key) {
                                        "⌫" -> if (entered.isNotEmpty()) entered = entered.dropLast(1)
                                        else -> {
                                            if (entered.length < 4) entered += key
                                            if (entered.length == 4) {
                                                handlePinEntry(
                                                    context, entered, isSetupMode, awaitingConfirm, confirmPin,
                                                    onSetConfirm = { confirmPin = entered; awaitingConfirm = true; entered = "" },
                                                    onPinSet = { isSetupMode = false; awaitingConfirm = false; entered = "" },
                                                    onUnlocked = onUnlocked,
                                                    onError = { error = true }
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (!isSetupMode) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = {
                    // Reset PIN (in production: require manager override)
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit().remove(KEY_PIN).apply()
                    isSetupMode = true
                    awaitingConfirm = false
                    entered = ""
                    confirmPin = ""
                }) {
                    Text("Reset PIN", color = SubtleGray, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun handlePinEntry(
    context: Context,
    entered: String,
    isSetupMode: Boolean,
    awaitingConfirm: Boolean,
    confirmPin: String,
    onSetConfirm: () -> Unit,
    onPinSet: () -> Unit,
    onUnlocked: () -> Unit,
    onError: () -> Unit
) {
    when {
        isSetupMode && !awaitingConfirm -> onSetConfirm()
        isSetupMode && awaitingConfirm -> {
            if (entered == confirmPin) {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putString(KEY_PIN, entered).apply()
                onPinSet()
                onUnlocked()
            } else {
                onError()
            }
        }
        else -> {
            val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_PIN, DEFAULT_PIN)
            if (entered == saved) onUnlocked() else onError()
        }
    }
}

private fun hasPinSet(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(KEY_PIN)

@Composable
private fun NumKey(label: String, isBackspace: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(SurfaceCard, CircleShape)
            .border(1.dp, Color(0xFF333333), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isBackspace) {
            Icon(Icons.Default.Backspace, null, tint = SubtleGray, modifier = Modifier.size(22.dp))
        } else {
            Text(label, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OnDark)
        }
    }
}
