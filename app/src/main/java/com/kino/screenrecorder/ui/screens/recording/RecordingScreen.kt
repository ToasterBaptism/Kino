package com.kino.screenrecorder.ui.screens.recording

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kino.screenrecorder.R
import com.kino.screenrecorder.data.model.RecordingMode
import com.kino.screenrecorder.data.model.RecordingState
import com.kino.screenrecorder.ui.theme.RecordingRed
import com.kino.screenrecorder.ui.viewmodel.RecordingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecordingViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recordingState by viewModel.recordingState.collectAsState()
    val recordingDuration by viewModel.recordingDuration.collectAsState()
    val recordingSettings by viewModel.recordingSettings.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Screen recording permission launcher
    val screenRecordingLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startRecording(result.resultCode, result.data)
        } else {
            // Permission denied
            scope.launch {
                snackbarHostState.showSnackbar("Screen recording permission is required")
            }
        }
    }
    
    // Show error snackbar
    LaunchedEffect(error) {
        error?.let { errorMessage ->
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Screen Recording") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Recording status indicator
            RecordingStatusIndicator(
                recordingState = recordingState,
                recordingDuration = recordingDuration,
                recordingMode = recordingSettings.recordingMode
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Recording settings summary
            RecordingSettingsSummary(
                frameRate = recordingSettings.frameRate,
                resolution = recordingSettings.resolution.displayName,
                audioSource = recordingSettings.audioSource.displayName,
                recordingMode = recordingSettings.recordingMode.displayName
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Control buttons
            RecordingControls(
                recordingState = recordingState,
                recordingMode = recordingSettings.recordingMode,
                onStartRecording = {
                    viewModel.requestScreenRecordingPermission(screenRecordingLauncher)
                },
                onStopRecording = viewModel::stopRecording,
                onPauseRecording = viewModel::pauseRecording,
                onResumeRecording = viewModel::resumeRecording,
                onSaveReplay = viewModel::saveInstantReplay
            )
        }
    }
}

@Composable
private fun RecordingStatusIndicator(
    recordingState: RecordingState,
    recordingDuration: Long,
    recordingMode: RecordingMode
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Recording indicator
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    when (recordingState) {
                        RecordingState.RECORDING -> RecordingRed
                        RecordingState.PAUSED -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (recordingState) {
                    RecordingState.RECORDING -> Icons.Default.RadioButtonChecked
                    RecordingState.PAUSED -> Icons.Default.Pause
                    else -> Icons.Default.RadioButtonChecked
                },
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (recordingState == RecordingState.IDLE) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    Color.White
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Status text
        Text(
            text = when (recordingState) {
                RecordingState.RECORDING -> when (recordingMode) {
                    RecordingMode.INSTANT_REPLAY -> stringResource(R.string.instant_replay_active)
                    RecordingMode.TIMED -> "Timed Recording Active"
                    else -> "Recording..."
                }
                RecordingState.PAUSED -> stringResource(R.string.recording_paused)
                RecordingState.PROCESSING -> "Processing..."
                else -> "Ready to Record"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Duration
        if (recordingState != RecordingState.IDLE) {
            val duration = recordingDuration / 1000
            val minutes = duration / 60
            val seconds = duration % 60
            
            Text(
                text = "%02d:%02d".format(minutes, seconds),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RecordingSettingsSummary(
    frameRate: Int,
    resolution: String,
    audioSource: String,
    recordingMode: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Recording Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SettingRow("Mode", recordingMode)
            SettingRow("Resolution", resolution)
            SettingRow("Frame Rate", "${frameRate} FPS")
            SettingRow("Audio", audioSource)
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RecordingControls(
    recordingState: RecordingState,
    recordingMode: RecordingMode,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onSaveReplay: () -> Unit
) {
    when (recordingState) {
        RecordingState.IDLE -> {
            Button(
                onClick = onStartRecording,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RecordingRed
                )
            ) {
                Icon(
                    imageVector = Icons.Default.RadioButtonChecked,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.start_recording),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        RecordingState.RECORDING -> {
            Column {
                if (recordingMode == RecordingMode.INSTANT_REPLAY) {
                    // Save replay button for instant replay mode
                    Button(
                        onClick = onSaveReplay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.save_replay),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Pause button
                    OutlinedButton(
                        onClick = onPauseRecording,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.pause_recording))
                    }
                    
                    // Stop button
                    Button(
                        onClick = onStopRecording,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.stop_recording))
                    }
                }
            }
        }
        
        RecordingState.PAUSED -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Resume button
                Button(
                    onClick = onResumeRecording,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.resume_recording))
                }
                
                // Stop button
                OutlinedButton(
                    onClick = onStopRecording,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.stop_recording))
                }
            }
        }
        
        RecordingState.PROCESSING -> {
            Button(
                onClick = { },
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Processing...")
            }
        }
    }
}