package com.webustallc.dahmer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webustallc.dahmer.viewmodel.ScaleViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ScaleScreen(viewModel: ScaleViewModel) {
    // Force UI updates with simple collectAsState to ensure immediate recomposition
    val weight by viewModel.weight.collectAsState()
    val touchPressure by viewModel.touchPressure.collectAsState()
    val isSensorAvailable by viewModel.isSensorAvailable.collectAsState()
    val unitIsGrams by viewModel.unitIsGrams.collectAsState()
    val isScaleActive by viewModel.isScaleActive.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    
    // Add debug logs to track state changes
    LaunchedEffect(weight, touchPressure, isScaleActive) {
        android.util.Log.d("ScaleScreen", "UI STATE: weight=$weight, pressure=$touchPressure, active=$isScaleActive")
    }

    var showCalibrationDialog by remember { mutableStateOf(false) }
    var showWiki by remember { mutableStateOf(false) }
    var knownWeight by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (isSensorAvailable) {
            viewModel.startScale()
        }
    }

    if (showWiki) {
        WikiScreen(onBackClick = { showWiki = false })
    } else {
        // Landscape Layout - Side by side design
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Side - Large Weighing Area (60% width)
            Column(
                modifier = Modifier.weight(0.6f),
                verticalArrangement = Arrangement.Center
            ) {
                if (!isSensorAvailable) {
                    SensorNotAvailableCard()
                } else {
                    // Large Touch Area for Weight Measurement - Force UI updates
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.2f) // Make it more rectangular for landscape
                            .pointerInteropFilter { event ->
                                android.util.Log.d("ScaleScreen", "Touch intercepted: ${event.action}")
                                
                                val result = viewModel.handleTouchEvent(event)
                                android.util.Log.d("ScaleScreen", "Touch handled: $result, current weight: $weight")
                                true // Consume the event to prevent any ripple effects
                            }
                    ) {
                        LargeWeightArea(
                            weight = weight, // Use real weight from ViewModel
                            pressure = touchPressure,
                            unit = if (unitIsGrams) "grams" else "ounces",
                            isActive = isScaleActive,
                            statusMessage = statusMessage,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Right Side - Controls and Info (40% width)
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .padding(start = 8.dp)
            ) {
                // Header and Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Digital Scale",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row {
                        IconButton(onClick = { showWiki = true }) {
                            Icon(
                                Icons.Default.Help,
                                contentDescription = "Wiki",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { viewModel.toggleUnit() }) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Toggle Unit",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status and Instructions
                InstructionCard(
                    statusMessage = translateStatusMessage(statusMessage),
                    isActive = isScaleActive
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Middle - Pressure Visualization
                PressureVisualization(
                    pressure = touchPressure,
                    weight = weight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )

                // Add flexible space to push buttons to bottom
                Spacer(modifier = Modifier.weight(1f))

                // Bottom - Control Buttons (Always at bottom)
                ControlButtons(
                    onCalibrateClick = { showCalibrationDialog = true },
                    onResetClick = { viewModel.resetCalibration() },
                    onToggleScale = {
                        if (isScaleActive) viewModel.stopScale()
                        else viewModel.startScale()
                    },
                    isScaleActive = isScaleActive,
                    isSensorAvailable = isSensorAvailable
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Calibration Dialog
    if (showCalibrationDialog) {
        CalibrationDialog(
            onDismiss = { showCalibrationDialog = false },
            onZeroCalibration = {
                viewModel.performZeroCalibration()
                showCalibrationDialog = false
            },
            onWeightCalibration = { weight ->
                if (weight.isNotEmpty()) {
                    viewModel.performWeightCalibration(weight.toFloatOrNull() ?: 0f)
                }
                showCalibrationDialog = false
            },
            knownWeight = knownWeight,
            onWeightChange = { knownWeight = it }
        )
    }
}

@Composable
fun LargeWeightArea(
    weight: Float,
    pressure: Float,
    unit: String,
    isActive: Boolean,
    statusMessage: String,
    modifier: Modifier = Modifier
) {
    val animatedWeight by animateFloatAsState(
        targetValue = weight,
        animationSpec = tween(durationMillis = 50), // Much faster animation for instant feedback
        label = "weight_animation"
    )

    val animatedPressure by animateFloatAsState(
        targetValue = pressure,
        animationSpec = tween(durationMillis = 30), // Even faster for pressure
        label = "pressure_animation"
    )

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Weight Display - Large and Clean
                Text(
                    text = if (animatedWeight > 0.01f) {
                        String.format("%.1f", animatedWeight)
                    } else {
                        "0.0"
                    },
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (animatedWeight > 0.01f) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )

                Text(
                    text = "grams",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Force Index Display - Shows measurement strength
                if (animatedPressure > 0.1f) {
                    Text(
                        text = "${String.format("%.1f", animatedPressure)} units",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "0.0 units",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Weighing area indicator
                Text(
                    text = "DIGITAL SCALE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Place object and apply pressure",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            // Touch indicator
            if (animatedWeight > 0.01f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = "Active",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Scale lines decoration
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawScaleLines(this)
            }
        }
    }
}

private fun drawScaleLines(drawScope: DrawScope) {
    val center = Offset(drawScope.size.width / 2, drawScope.size.height / 2)
    val color = Color.Gray.copy(alpha = 0.2f)

    // Draw measurement grid
    val step = 40f
    for (i in 1..5) {
        val offset = step * i
        // Horizontal lines
        drawScope.drawLine(
            color = color,
            start = Offset(center.x - offset, center.y),
            end = Offset(center.x + offset, center.y),
            strokeWidth = 1f
        )
        // Vertical lines
        drawScope.drawLine(
            color = color,
            start = Offset(center.x, center.y - offset),
            end = Offset(center.x, center.y + offset),
            strokeWidth = 1f
        )
    }
}

@Composable
fun PressureVisualization(
    pressure: Float,
    weight: Float,
    modifier: Modifier = Modifier
) {
    val animatedPressure by animateFloatAsState(
        targetValue = pressure,
        animationSpec = tween(durationMillis = 100),
        label = "pressure_viz"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawPressureVisualization(animatedPressure, weight, this)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                if (weight > 0.1f) Icons.Default.TouchApp else Icons.Default.PanTool,
                contentDescription = "Pressure",
                modifier = Modifier.size(32.dp),
                tint = if (weight > 0.1f) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = if (weight > 0.1f) "Measuring" else "Ready",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun drawPressureVisualization(pressure: Float, weight: Float, drawScope: DrawScope) {
    val center = Offset(drawScope.size.width / 2, drawScope.size.height / 2)
    val maxRadius = drawScope.size.minDimension / 2 - 20f

    // Background circles
    for (i in 1..3) {
        val radius = maxRadius * (i / 3f)
        drawScope.drawCircle(
            color = Color.Gray.copy(alpha = 0.1f),
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
    }

    // Pressure visualization
    if (pressure > 0) {
        val normalizedPressure = (pressure / 1000f).coerceIn(0f, 1f)
        val pressureRadius = maxRadius * normalizedPressure

        // Gradient fill for pressure
        val gradient = Brush.radialGradient(
            colors = listOf(
                Color.Blue.copy(alpha = 0.3f),
                Color.Blue.copy(alpha = 0.1f),
                Color.Transparent
            ),
            radius = pressureRadius
        )

        drawScope.drawCircle(
            brush = gradient,
            radius = pressureRadius,
            center = center
        )

        // Outer ring for pressure
        drawScope.drawCircle(
            color = Color.Blue.copy(alpha = 0.8f),
            radius = pressureRadius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
        )
    }

    // Weight indicator (inner circle)
    if (weight > 0.1f) {
        val normalizedWeight = (weight / 100f).coerceIn(0f, 1f)
        val weightRadius = maxRadius * 0.3f * normalizedWeight

        drawScope.drawCircle(
            color = Color.Green.copy(alpha = 0.6f),
            radius = weightRadius,
            center = center
        )
    }
}

@Composable
fun InstructionCard(
    statusMessage: String,
    isActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Bilgi",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun ControlButtons(
    onCalibrateClick: () -> Unit,
    onResetClick: () -> Unit,
    onToggleScale: () -> Unit,
    isScaleActive: Boolean,
    isSensorAvailable: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        // Always visible calibration button
        OutlinedButton(
            onClick = onCalibrateClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = true, // Always enabled for testing
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Calibration", style = MaterialTheme.typography.labelLarge)
        }

        // Always visible reset button
        OutlinedButton(
            onClick = onResetClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = true, // Always enabled for testing
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.secondary,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reset", style = MaterialTheme.typography.labelLarge)
        }

        // Toggle scale button - more prominent
        Button(
            onClick = onToggleScale,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = true, // Always enabled for testing
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isScaleActive) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.primary,
                contentColor = if (isScaleActive) 
                    MaterialTheme.colorScheme.onError 
                else 
                    MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(
                if (isScaleActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isScaleActive) "STOP SCALE" else "START SCALE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Debug info
        Text(
            text = "Sensor: ${if (isSensorAvailable) "✅" else "❌"} | Scale: ${if (isScaleActive) "ON" else "OFF"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SensorNotAvailableCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Warning",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Pressure Sensor Not Found",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This device requires a pressure sensor to use the scale feature.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CalibrationDialog(
    onDismiss: () -> Unit,
    onZeroCalibration: () -> Unit,
    onWeightCalibration: (String) -> Unit,
    knownWeight: String,
    onWeightChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scale Calibration") },
        text = {
            Column {
                Text("To calibrate the scale:")
                Spacer(modifier = Modifier.height(16.dp))
                Text("1. First perform 'Zero Calibration'")
                Spacer(modifier = Modifier.height(8.dp))
                Text("2. Place a known weight object and enter its weight")
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = knownWeight,
                    onValueChange = onWeightChange,
                    label = { Text("Known weight (grams)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onZeroCalibration) {
                    Text("Zero Cal.")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = { onWeightCalibration(knownWeight) },
                    enabled = knownWeight.isNotEmpty()
                ) {
                    Text("Weight Cal.")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun translateStatusMessage(message: String): String {
    return when {
        message.contains("Nesneyi ekrana yerleştirin") -> "Place object on the weighing area"
        message.contains("Hafif basınç algılandı") -> "Light pressure detected"
        message.contains("Ağırlık ölçülüyor") -> "Measuring weight..."
        message.contains("Ağırlık:") -> message.replace("Ağırlık:", "Weight:")
        message.contains("Sıfır kalibrasyonu tamamlandı") -> "Zero calibration completed. Now place known weight."
        message.contains("Kalibrasyon tamamlandı") -> "Calibration completed!"
        message.contains("Kalibrasyon iptal edildi") -> "Calibration cancelled"
        message.contains("Kalibrasyon sıfırlandı") -> "Calibration reset"
        else -> "Place object on the weighing area"
    }
}
