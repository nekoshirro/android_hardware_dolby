/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import org.lunaris.dolby.R
import org.lunaris.dolby.domain.models.EqualizerUiState
import org.lunaris.dolby.ui.components.ModernConfirmDialog
import org.lunaris.dolby.ui.viewmodel.EqualizerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernEqualizerScreen(
    viewModel: EqualizerViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.dolby_preset),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset")
                    }
                    if (uiState is EqualizerUiState.Success) {
                        val state = uiState as EqualizerUiState.Success
                        if (state.currentPreset.isUserDefined) {
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is EqualizerUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is EqualizerUiState.Success -> {
                ModernEqualizerContent(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is EqualizerUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        SavePresetDialog(
            onSave = { name ->
                val error = viewModel.savePreset(name)
                if (error == null) {
                    showSaveDialog = false
                }
                error
            },
            onDismiss = { showSaveDialog = false }
        )
    }

    if (showDeleteDialog && uiState is EqualizerUiState.Success) {
        val state = uiState as EqualizerUiState.Success
        ModernConfirmDialog(
            title = stringResource(R.string.dolby_geq_delete_preset),
            message = stringResource(R.string.dolby_geq_delete_preset_prompt),
            icon = Icons.Default.Delete,
            onConfirm = {
                viewModel.deletePreset(state.currentPreset)
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showResetDialog) {
        ModernConfirmDialog(
            title = stringResource(R.string.dolby_geq_reset_gains),
            message = stringResource(R.string.dolby_geq_reset_gains_prompt),
            icon = Icons.Default.RestartAlt,
            onConfirm = {
                viewModel.resetGains()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }
}

@Composable
private fun ModernEqualizerContent(
    state: EqualizerUiState.Success,
    viewModel: EqualizerViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            ModernPresetSelector(
                presets = state.presets,
                currentPreset = state.currentPreset,
                onPresetSelected = { viewModel.setPreset(it) }
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Frequency Response",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                FrequencyResponseCurve(
                    bandGains = state.bandGains,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 70.dp), 
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.dolby_geq_slider_label_gain),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(state.bandGains) { index, bandGain ->
                        ModernEqualizerBand(
                            frequency = bandGain.frequency,
                            gain = bandGain.gain,
                            onGainChange = { newGain ->
                                viewModel.setBandGain(index, newGain)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernPresetSelector(
    presets: List<org.lunaris.dolby.domain.models.EqualizerPreset>,
    currentPreset: org.lunaris.dolby.domain.models.EqualizerPreset,
    onPresetSelected: (org.lunaris.dolby.domain.models.EqualizerPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.padding(20.dp)) {
        Text(
            text = stringResource(R.string.dolby_geq_preset),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (currentPreset.isUserDefined) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = currentPreset.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            }
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                presets.forEach { preset ->
                    DropdownMenuItem(
                        text = { 
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(preset.name)
                                if (preset.isUserDefined) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        onClick = {
                            onPresetSelected(preset)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernEqualizerBand(
    frequency: Int,
    gain: Int,
    onGainChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderValue by remember(gain) { mutableFloatStateOf(gain / 10f) }

    Column(
        modifier = modifier
            .width(64.dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = "%.1f".format(sliderValue),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {
                onGainChange((sliderValue * 10).toInt())
            },
            valueRange = -15f..15f,
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = 270f
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        Constraints(
                            minWidth = constraints.minHeight,
                            maxWidth = constraints.maxHeight,
                            minHeight = constraints.minWidth,
                            maxHeight = constraints.maxHeight,
                        )
                    )
                    layout(placeable.height, placeable.width) {
                        placeable.place(-placeable.width, 0)
                    }
                }
                .weight(1f)
                .width(48.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        
        Text(
            text = if (frequency >= 1000) "${frequency / 1000}k" else "$frequency",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FrequencyResponseCurve(
    bandGains: List<org.lunaris.dolby.domain.models.BandGain>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    
    Canvas(modifier = modifier.background(surfaceColor.copy(alpha = 0.3f))) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        
        drawLine(
            color = surfaceColor,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 2f
        )
        
        for (i in 1..4) {
            val y = (height / 5) * i
            drawLine(
                color = surfaceColor.copy(alpha = 0.3f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }
        
        if (bandGains.isNotEmpty()) {
            val path = Path()
            val stepX = width / (bandGains.size - 1)
            
            bandGains.forEachIndexed { index, bandGain ->
                val x = index * stepX
                val normalizedGain = (bandGain.gain / 150f).coerceIn(-1f, 1f)
                val y = centerY - (normalizedGain * centerY * 0.8f)
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 4f)
            )
            
            val fillPath = Path().apply {
                addPath(path)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.3f),
                        primaryColor.copy(alpha = 0.05f)
                    )
                )
            )
        }
    }
}

@Composable
private fun SavePresetDialog(
    onSave: (String) -> String?,
    onDismiss: () -> Unit
) {
    var presetName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { 
            Text(
                stringResource(R.string.dolby_geq_new_preset),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { 
                        presetName = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.dolby_geq_preset_name)) },
                    isError = errorMessage != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val error = onSave(presetName)
                    if (error != null) {
                        errorMessage = error
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}
