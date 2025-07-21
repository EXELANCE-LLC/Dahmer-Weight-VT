package com.webustallc.dahmer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WikiScreen(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        TopAppBar(
            title = {
                Text(
                    "Digital Scale Wiki",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Introduction
            item {
                WikiCard(
                    title = "How to Use Digital Scale",
                    icon = Icons.Default.Help,
                    content = "This app transforms your phone into a digital scale using pressure sensors and touch detection. Follow the instructions below for accurate measurements."
                )
            }

            // Quick Start
            item {
                WikiCard(
                    title = "Quick Start Guide",
                    icon = Icons.Default.PlayArrow,
                    content = """
                        1. Place your phone on a flat, stable surface
                        2. Tap 'Start' to activate the scale
                        3. Place object on the weighing area (large card)
                        4. Press gently and wait for stable reading
                        5. Record your measurement
                    """.trimIndent()
                )
            }

            // Calibration Guide
            item {
                WikiCard(
                    title = "Calibration Instructions",
                    icon = Icons.Default.Settings,
                    content = """
                        IMPORTANT: Proper calibration is essential for accuracy!
                        
                        Zero Calibration:
                        • Place your finger/hand on the weighing area
                        • Tap 'Calibration' → 'Zero Cal.'
                        • This sets the baseline pressure
                        
                        Weight Calibration:
                        • Use a known weight (coin, small object)
                        • Place object on your finger/hand over the weighing area
                        • Enter the known weight in grams
                        • Tap 'Weight Cal.'
                    """.trimIndent()
                )
            }

            // Troubleshooting
            item {
                WikiCard(
                    title = "Troubleshooting",
                    icon = Icons.Default.Warning,
                    content = """
                        Object Not Detected:
                        • First calibrate with your finger/hand
                        • Place object on your finger for support
                        • Ensure object makes contact with screen
                        • Apply gentle, steady pressure
                        
                        Inaccurate Readings:
                        • Recalibrate the scale
                        • Use a stable surface
                        • Avoid vibrations and movement
                        • Clean the screen surface
                    """.trimIndent()
                )
            }

            // Best Practices
            item {
                WikiCard(
                    title = "Best Practices",
                    icon = Icons.Default.CheckCircle,
                    content = """
                        For Accurate Measurements:
                        
                        • Use your finger as a platform for small objects
                        • Keep the phone on a stable surface
                        • Apply consistent, gentle pressure
                        • Wait for readings to stabilize
                        • Calibrate regularly with known weights
                        
                        Supported Objects:
                        • Coins, keys, jewelry (1-50g)
                        • Pills, small components (0.1-5g)
                        • Office supplies (5-100g)
                    """.trimIndent()
                )
            }

            // Technical Info
            item {
                WikiCard(
                    title = "Technical Information",
                    icon = Icons.Default.Info,
                    content = """
                        How It Works:
                        • Combines touch pressure (60%)
                        • Barometric pressure sensor (30%)
                        • Accelerometer data (10%)
                        
                        Accuracy Range:
                        • Best: 1-100 grams
                        • Resolution: ±0.1 gram
                        • Depends on device sensors
                        
                        Requirements:
                        • Android 11+ (API 30)
                        • Pressure sensor (barometer)
                        • Touch pressure support
                    """.trimIndent()
                )
            }

            // Safety Notice
            item {
                WikiCard(
                    title = "Safety & Limitations",
                    icon = Icons.Default.Security,
                    content = """
                        Important Notices:
                        
                        ⚠️ This app is for educational/entertainment purposes
                        ⚠️ Not suitable for medical or commercial use
                        ⚠️ Results may vary between devices
                        ⚠️ Do not use for critical measurements
                        
                        Limitations:
                        • Accuracy depends on device sensors
                        • Environmental factors affect readings
                        • Regular calibration required
                        • Maximum weight: ~200 grams
                    """.trimIndent()
                )
            }
        }
    }
}

@Composable
fun WikiCard(
    title: String,
    icon: ImageVector,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}
