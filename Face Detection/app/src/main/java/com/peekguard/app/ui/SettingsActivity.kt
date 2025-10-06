package com.peekguard.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peekguard.app.R
import com.peekguard.app.detection.SensitivityLevel
import com.peekguard.app.notification.AlertMode
import com.peekguard.app.ui.theme.PeekGuardTheme

class SettingsActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            PeekGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(
                        onBackPressed = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    
    // Settings state
    var selectedSensitivity by remember { mutableStateOf(SensitivityLevel.MEDIUM) }
    var selectedAlertMode by remember { mutableStateOf(AlertMode.NOTIFICATION_SOUND) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var darkModeEnabled by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.settings),
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        
        // Settings Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Detection Sensitivity Section
            SettingsSection(
                title = stringResource(R.string.sensitivity),
                subtitle = "Adjust face detection sensitivity"
            ) {
                Column(
                    modifier = Modifier.selectableGroup()
                ) {
                    SensitivityLevel.values().forEach { sensitivity ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (selectedSensitivity == sensitivity),
                                    onClick = { selectedSensitivity = sensitivity },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedSensitivity == sensitivity),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = when (sensitivity) {
                                    SensitivityLevel.LOW -> stringResource(R.string.low_sensitivity)
                                    SensitivityLevel.MEDIUM -> stringResource(R.string.medium_sensitivity)
                                    SensitivityLevel.HIGH -> stringResource(R.string.high_sensitivity)
                                },
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
            
            // Alert Mode Section
            SettingsSection(
                title = stringResource(R.string.alert_mode),
                subtitle = "Choose how you want to be notified"
            ) {
                Column(
                    modifier = Modifier.selectableGroup()
                ) {
                    AlertMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (selectedAlertMode == mode),
                                    onClick = { selectedAlertMode = mode },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedAlertMode == mode),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = when (mode) {
                                    AlertMode.NOTIFICATION_SOUND -> stringResource(R.string.notification_sound)
                                    AlertMode.VIBRATION_ONLY -> stringResource(R.string.vibration_only)
                                    AlertMode.SILENT_ICON -> stringResource(R.string.silent_icon)
                                    AlertMode.POPUP_ONLY -> "Popup Only"
                                },
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
            
            // Sound & Vibration Section
            SettingsSection(
                title = "Sound & Vibration",
                subtitle = "Customize notification behavior"
            ) {
                Column {
                    // Sound Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Alert Sound",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Play sound when viewer detected",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { soundEnabled = it }
                        )
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Vibration Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Vibration",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Vibrate when viewer detected",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = vibrationEnabled,
                            onCheckedChange = { vibrationEnabled = it }
                        )
                    }
                }
            }
            
            // App Preferences Section
            SettingsSection(
                title = "App Preferences",
                subtitle = "General app settings"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Dark Mode",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Use dark theme",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = darkModeEnabled,
                        onCheckedChange = { darkModeEnabled = it }
                    )
                }
            }
            
            // Test Section
            SettingsSection(
                title = "Testing",
                subtitle = "Test app functionality"
            ) {
                Button(
                    onClick = {
                        // Test alert functionality
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Test Alert")
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            content()
        }
    }
}