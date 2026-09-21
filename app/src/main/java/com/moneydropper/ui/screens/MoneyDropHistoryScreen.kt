package com.moneydropper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneydropper.data.local.MoneyDropEntity
import com.moneydropper.ui.theme.GreenPrimary
import com.moneydropper.ui.theme.SurfaceCard
import com.moneydropper.ui.theme.SubtleGray
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyDropHistoryScreen(
    viewModel: MoneyDropViewModel,
    onBackClick: () -> Unit
) {
    val drops by viewModel.allDrops.collectAsState()
    
    val totalAmount = remember(drops) {
        drops.sumOf { it.declaredAmount }
    }
    
    val totalCount = drops.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Money Drop History") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.exportCsv() }) {
                        Icon(Icons.Default.Share, contentDescription = "Export CSV")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Dashboard Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Total Summary Dashboard", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Entries", color = SubtleGray, fontSize = 13.sp)
                            Text("$totalCount", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Deposited", color = SubtleGray, fontSize = 13.sp)
                            Text(
                                String.format(Locale.US, "R %.2f", totalAmount),
                                color = GreenPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Text("All History Logs", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

            if (drops.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No safe money drops recorded yet.", color = SubtleGray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(drops) { drop ->
                        DropHistoryRow(
                            drop = drop,
                            onPdfExportClick = { viewModel.shareReceiptPdf(drop) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DropHistoryRow(
    drop: MoneyDropEntity,
    onPdfExportClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bag #${drop.bagNumber}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                color = when (drop.status) {
                                    "PENDING" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                    "FLAGGED" -> Color(0xFFE53935).copy(alpha = 0.2f)
                                    else -> GreenPrimary.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = drop.status,
                            color = when (drop.status) {
                                "PENDING" -> Color(0xFFFF9800)
                                "FLAGGED" -> Color(0xFFE53935)
                                else -> GreenPrimary
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Text("Cashier: ${drop.cashierName}", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                Text("Date: ${drop.date} (${drop.shift})", color = SubtleGray, fontSize = 12.sp)
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(Locale.US, "R %.2f", drop.declaredAmount),
                    color = GreenPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onPdfExportClick) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Export PDF",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
