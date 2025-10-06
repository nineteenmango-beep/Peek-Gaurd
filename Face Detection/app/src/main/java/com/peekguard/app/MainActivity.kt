package com.peekguard.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peekguard.app.service.FaceDetectionService
import com.peekguard.app.ui.DashboardActivity
import com.peekguard.app.ui.SettingsActivity
import com.peekguard.app.ui.theme.PeekGuardTheme
import com.peekguard.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        } else true
        
        if (cameraGranted && notificationGranted) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permissions required for app to function", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            PeekGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onRequestPermissions = { requestPermissions() },
                        onNavigateToSettings = { 
                            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                        },
                        onNavigateToDashboard = {
                            startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                        }
                    )
                }
            }
        }
        
        // Check permissions on startup
        if (!hasRequiredPermissions()) {
            requestPermissions()
        }
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        permissionLauncher.launch(permissions.toTypedArray())
    }
    
    private fun hasRequiredPermissions(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
        
        return cameraPermission && notificationPermission
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onRequestPermissions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val isProtectionEnabled by viewModel.isProtectionEnabled.collectAsState()
    val detectionCount by viewModel.detectionCount.collectAsState()
    val hasPermissions by viewModel.hasPermissions.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.app_name),
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
                IconButton(onClick = onNavigateToDashboard) {
                    Icon(Icons.Default.Dashboard, contentDescription = "Dashboard")
                }
            }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Main Shield Icon
        Card(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(60.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isProtectionEnabled) {
                    Color(0xFF4CAF50)
                } else {
                    Color(0xFFFF5722)
                }
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Shield",
                    modifier = Modifier.size(60.dp),
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Status Text
        Text(
            text = if (isProtectionEnabled) {
                stringResource(R.string.protection_enabled)
            } else {
                stringResource(R.string.protection_disabled)
            },
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (isProtectionEnabled) {
                Color(0xFF4CAF50)
            } else {
                Color(0xFFFF5722)
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Detection count
        if (detectionCount > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFF9800)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Recent Detections",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "$detectionCount viewer(s) detected",
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Privacy Mode Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.privacy_mode),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Permission check
                if (!hasPermissions) {
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5722)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.grant_permission),
                            color = Color.White
                        )
                    }
                } else {
                    // Toggle Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isProtectionEnabled) "ON" else "OFF",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isProtectionEnabled) {
                                Color(0xFF4CAF50)
                            } else {
                                Color(0xFFFF5722)
                            }
                        )
                        
                        Switch(
                            checked = isProtectionEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    viewModel.startProtection()
                                } else {
                                    viewModel.stopProtection()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF4CAF50),
                                checkedTrackColor = Color(0xFF81C784),
                                uncheckedThumbColor = Color(0xFFFF5722),
                                uncheckedTrackColor = Color(0xFFFFAB91)
                            )
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Information Card
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
                    text = "How it works:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• PeekGuard monitors your front camera for faces\n" +
                           "• When multiple faces are detected, you'll get an instant alert\n" +
                           "• All processing happens locally - no data is stored or sent",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Bottom Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Settings")
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            OutlinedButton(
                onClick = onNavigateToDashboard,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Dashboard, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stats")
            }
        }
    }
}