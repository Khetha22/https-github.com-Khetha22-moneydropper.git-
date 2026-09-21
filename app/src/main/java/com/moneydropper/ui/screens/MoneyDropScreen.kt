package com.moneydropper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyDropScreen(
    viewModel: MoneyDropViewModel,
    onNavigateToHistory: () -> Unit
) {
    val state by viewModel.formState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Money Drop") },
                actions = {
                    TextButton(onClick = onNavigateToHistory) {
                        Text("History", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.cashierName,
                onValueChange = { viewModel.setCashierName(it) },
                label = { Text("Cashier Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.bagNumber,
                onValueChange = { viewModel.setBagNumber(it) },
                label = { Text("Bag Number") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    com.moneydropper.ui.components.BagBarcodeScannerButton(
                        onBarcodeScanned = { viewModel.setBarcodeData(it) }
                    )
                }
            )

            OutlinedTextField(
                value = state.declaredAmount,
                onValueChange = { viewModel.setDeclaredAmount(it) },
                label = { Text("Declared Amount") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Shift")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("MORNING", "AFTERNOON", "NIGHT").forEach { shift ->
                    FilterChip(
                        selected = state.shift == shift,
                        onClick = { viewModel.setShift(shift) },
                        label = { Text(shift) }
                    )
                }
            }

            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.setNotes(it) },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Signature Pads
            com.moneydropper.ui.components.SignaturePadField(
                label = "Cashier Signature",
                currentSignature = state.cashierSignature,
                onSignatureCaptured = { viewModel.setCashierSignature(it) }
            )

            com.moneydropper.ui.components.SignaturePadField(
                label = "Manager Signature",
                currentSignature = state.managerSignature,
                onSignatureCaptured = { viewModel.setManagerSignature(it) }
            )

            Button(
                onClick = { viewModel.submitDrop() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSubmitting
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Submit Drop")
                }
            }

            if (state.submitSuccess) {
                AlertDialog(
                    onDismissRequest = { viewModel.resetForm() },
                    title = { Text("Success") },
                    text = { Text("Money Drop has been submitted successfully!") },
                    confirmButton = {
                        TextButton(onClick = { viewModel.resetForm() }) {
                            Text("OK")
                        }
                    }
                )
            }
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
