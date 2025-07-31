package com.kino.screenrecorder.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kino.screenrecorder.R
import com.kino.screenrecorder.data.model.AudioSource
import com.kino.screenrecorder.data.model.RecordingMode
import com.kino.screenrecorder.data.model.RecordingSettings
import com.kino.screenrecorder.data.model.Resolution
import com.kino.screenrecorder.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val recordingSettings by viewModel.recordingSettings.collectAsState(initial = null)
    val showResetDialog by viewModel.showResetDialog.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error snackbar
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (recordingSettings != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Video Settings
                SettingsSection(
                    title = stringResource(R.string.video_settings)
                ) {
                    // Frame Rate
                    FrameRateSelector(
                        selectedFrameRate = recordingSettings!!.frameRate,
                        onFrameRateSelected = viewModel::updateFrameRate
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Resolution
                    ResolutionSelector(
                        selectedResolution = recordingSettings!!.resolution,
                        onResolutionSelected = viewModel::updateResolution
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Show touches
                    SwitchSetting(
                        title = stringResource(R.string.show_touches),
                        description = "Show touch points during recording",
                        checked = recordingSettings!!.showTouches,
                        onCheckedChange = viewModel::updateShowTouches
                    )
                }
                
                // Audio Settings
                SettingsSection(
                    title = stringResource(R.string.audio_settings)
                ) {
                    AudioSourceSelector(
                        selectedAudioSource = recordingSettings!!.audioSource,
                        onAudioSourceSelected = viewModel::updateAudioSource
                    )
                }
                
                // Recording Settings
                SettingsSection(
                    title = "Recording Settings"
                ) {
                    RecordingModeSelector(
                        selectedMode = recordingSettings!!.recordingMode,
                        onModeSelected = viewModel::updateRecordingMode
                    )
                    
                    if (recordingSettings!!.recordingMode == RecordingMode.TIMED) {
                        Spacer(modifier = Modifier.height(16.dp))
                        NumberSetting(
                            title = "Timer Duration",
                            description = "Recording duration in seconds",
                            value = recordingSettings!!.timedRecordingDuration,
                            onValueChange = viewModel::updateTimedRecordingDuration,
                            range = 5f..300f
                        )
                    }
                    
                    if (recordingSettings!!.recordingMode == RecordingMode.INSTANT_REPLAY) {
                        Spacer(modifier = Modifier.height(16.dp))
                        NumberSetting(
                            title = "Instant Replay Duration",
                            description = "Duration of instant replay buffer",
                            value = recordingSettings!!.instantReplayDuration,
                            onValueChange = viewModel::updateInstantReplayDuration,
                            range = 10f..120f
                        )
                    }
                }
                
                // General Settings
                SettingsSection(
                    title = stringResource(R.string.general_settings)
                ) {
                    OutlinedButton(
                        onClick = viewModel::showResetDialog,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.reset_to_default))
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading settings...")
            }
        }
    }
    
    // Reset confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideResetDialog,
            title = { Text(stringResource(R.string.reset_confirmation_title)) },
            text = { Text(stringResource(R.string.reset_confirmation_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetToDefaults()
                        viewModel.hideResetDialog()
                    }
                ) {
                    Text(stringResource(R.string.reset))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideResetDialog) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            content()
        }
    }
}

@Composable
private fun FrameRateSelector(
    selectedFrameRate: Int,
    onFrameRateSelected: (Int) -> Unit
) {
    val frameRates = listOf(30, 60)
    
    Column {
        Text(
            text = "Frame Rate",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        frameRates.forEach { frameRate ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedFrameRate == frameRate,
                        onClick = { onFrameRateSelected(frameRate) }
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedFrameRate == frameRate,
                    onClick = { onFrameRateSelected(frameRate) }
                )
                Text(
                    text = "$frameRate FPS",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ResolutionSelector(
    selectedResolution: Resolution,
    onResolutionSelected: (Resolution) -> Unit
) {
    val resolutions = Resolution.values()
    
    Column {
        Text(
            text = "Resolution",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        resolutions.forEach { resolution ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedResolution == resolution,
                        onClick = { onResolutionSelected(resolution) }
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedResolution == resolution,
                    onClick = { onResolutionSelected(resolution) }
                )
                Text(
                    text = resolution.displayName,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AudioSourceSelector(
    selectedAudioSource: AudioSource,
    onAudioSourceSelected: (AudioSource) -> Unit
) {
    val audioSources = AudioSource.values()
    
    Column {
        Text(
            text = "Audio Source",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        audioSources.forEach { audioSource ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedAudioSource == audioSource,
                        onClick = { onAudioSourceSelected(audioSource) }
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedAudioSource == audioSource,
                    onClick = { onAudioSourceSelected(audioSource) }
                )
                Text(
                    text = audioSource.displayName,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun RecordingModeSelector(
    selectedMode: RecordingMode,
    onModeSelected: (RecordingMode) -> Unit
) {
    val modes = RecordingMode.values()
    
    Column {
        Text(
            text = "Recording Mode",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        modes.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedMode == mode,
                        onClick = { onModeSelected(mode) }
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedMode == mode,
                    onClick = { onModeSelected(mode) }
                )
                Text(
                    text = mode.displayName,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun NumberSetting(
    title: String,
    description: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: ClosedFloatingPointRange<Float>
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = range,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$value",
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}