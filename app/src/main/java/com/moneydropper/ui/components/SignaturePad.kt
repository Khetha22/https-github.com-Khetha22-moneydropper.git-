package com.moneydropper.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.moneydropper.ui.theme.GreenPrimary
import com.moneydropper.ui.theme.SurfaceCard
import com.moneydropper.ui.theme.SubtleGray

@Composable
fun SignaturePadField(
    label: String,
    currentSignature: Bitmap?,
    onSignatureCaptured: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPad by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (currentSignature != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                    .border(1.dp, GreenPrimary, RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        // Just click to clear/resign
                    }
            ) {
                androidx.compose.foundation.Image(
                    bitmap = currentSignature.asImageBitmap(),
                    contentDescription = "$label Preview",
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    contentScale = ContentScale.Fit
                )

                Button(
                    onClick = { showPad = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceCard,
                        contentColor = GreenPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Redo", fontSize = 12.sp)
                }
            }
        } else {
            OutlinedButton(
                onClick = { showPad = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444444))
            ) {
                Text("Tap to Sign $label", color = SubtleGray)
            }
        }
    }

    if (showPad) {
        Dialog(
            onDismissRequest = { showPad = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            SignatureCaptureView(
                label = label,
                onSave = { bmp ->
                    onSignatureCaptured(bmp)
                    showPad = false
                },
                onDismiss = { showPad = false }
            )
        }
    }
}

@Composable
private fun SignatureCaptureView(
    label: String,
    onSave: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val points = remember { mutableStateListOf<Offset>() }
    // Maintain a list of paths/lines to handle clear breaks between drag events
    // For simplicity, we can use a list of offsets where Offset.Unspecified means a break.
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(SurfaceCard, RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Sign as $label", color = Color.White, fontSize = 16.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Clear, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .onGloballyPositioned { canvasSize = it.size }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                points.add(offset)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                points.add(change.position)
                            },
                            onDragEnd = {
                                points.add(Offset.Unspecified)
                            }
                        )
                    }
            ) {
                ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                    if (points.isNotEmpty()) {
                        val path = ComposePath()
                        var first = true
                        for (point in points) {
                            if (point == Offset.Unspecified) {
                                first = true
                            } else {
                                if (first) {
                                    path.moveTo(point.x, point.y)
                                    first = false
                                } else {
                                    path.lineTo(point.x, point.y)
                                }
                            }
                        }
                        drawPath(
                            path = path,
                            color = Color.Black,
                            style = Stroke(
                                width = 4.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                if (points.isEmpty()) {
                    Text(
                        text = "Sign here",
                        color = Color.LightGray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { points.clear() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF666666))
                ) {
                    Text("Clear", color = Color.White)
                }

                Button(
                    onClick = {
                        if (points.isNotEmpty() && canvasSize.width > 0 && canvasSize.height > 0) {
                            val bitmap = Bitmap.createBitmap(
                                canvasSize.width,
                                canvasSize.height,
                                Bitmap.Config.ARGB_8888
                            )
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            
                            val paint = Paint().apply {
                                color = android.graphics.Color.BLACK
                                style = Paint.Style.STROKE
                                strokeWidth = 12f
                                strokeCap = Paint.Cap.ROUND
                                strokeJoin = Paint.Join.ROUND
                                isAntiAlias = true
                            }

                            val androidPath = Path()
                            var first = true
                            for (point in points) {
                                if (point == Offset.Unspecified) {
                                    first = true
                                } else {
                                    if (first) {
                                        androidPath.moveTo(point.x, point.y)
                                        first = false
                                    } else {
                                        androidPath.lineTo(point.x, point.y)
                                    }
                                }
                            }
                            canvas.drawPath(androidPath, paint)
                            onSave(bitmap)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Save", color = Color.White)
                }
            }
        }
    }
}
