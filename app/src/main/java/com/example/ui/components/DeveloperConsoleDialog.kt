package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.security.HardwareLicensingManager
import com.example.security.TimeIntegrityManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperConsoleDialog(
    currentStation: String,
    currentServerIp: String,
    currentPort: String,
    onDismiss: () -> Unit,
    onSaveStation: (String) -> Unit,
    onSaveServerConfig: (ip: String, port: String) -> Unit,
    onBackupDatabase: () -> Unit,
    onPurgeSyncedRecords: () -> Unit,
    onForceSync: () -> Unit,
    onEmergencyLicenseToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var isAuthenticated by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // Station list per specification
    val stationOptions = listOf("Adi NebriEd", "Adi Kahsu", "Godfey", "Adi Klte")
    var selectedStation by remember { mutableStateOf(currentStation) }
    var isStationExpanded by remember { mutableStateOf(false) }

    // IP Config
    var serverIpInput by remember { mutableStateOf(currentServerIp) }
    var serverPortInput by remember { mutableStateOf(currentPort) }

    // License override state
    var isEmergencyBypass by remember { mutableStateOf(HardwareLicensingManager.isBypassActive(context)) }
    var generatedOfflineKey by remember { mutableStateOf<String?>(null) }
    var isTimeTamperBypassed by remember { mutableStateOf(TimeIntegrityManager.bypassTamperCheckForDev) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
                .testTag("developer_console_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF263238), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Developer & Diagnostics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Adi NebriEd Engineering Shell v2.4",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                if (!isAuthenticated) {
                    // Password Gate (Master Password: MomLove@1)
                    Text(
                        text = "Master Password Authentication Required",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Please authenticate with the enterprise engineering key to access internal diagnostics, DB exports, and station overrides.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            passwordError = null
                        },
                        label = { Text("Master Password") },
                        placeholder = { Text("Enter MomLove@1") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dev_master_password_input"),
                        shape = RoundedCornerShape(10.dp),
                        isError = passwordError != null
                    )

                    if (passwordError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = passwordError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (passwordInput == "MomLove@1") {
                                isAuthenticated = true
                                passwordError = null
                            } else {
                                passwordError = "Access Denied: Incorrect Master Password"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("unlock_dev_console_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238))
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Authenticate Console", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    // Authenticated Developer Tools
                    // 1. Station Selector
                    Text(
                        text = "1. STATION REGION SELECTOR",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = isStationExpanded,
                        onExpandedChange = { isStationExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedStation,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Active Water Station") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStationExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("station_selector_dropdown"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = isStationExpanded,
                            onDismissRequest = { isStationExpanded = false }
                        ) {
                            stationOptions.forEach { station ->
                                DropdownMenuItem(
                                    text = { Text(station) },
                                    onClick = {
                                        selectedStation = station
                                        isStationExpanded = false
                                        onSaveStation(station)
                                        Toast.makeText(context, "Station updated to $station", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 2. IP Configuration
                    Text(
                        text = "2. LAN & SERVER IP CONFIGURATION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = serverIpInput,
                            onValueChange = { serverIpInput = it },
                            label = { Text("Server LAN IP / Domain") },
                            placeholder = { Text("192.168.1.100") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(2f)
                                .testTag("server_ip_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = serverPortInput,
                            onValueChange = { serverPortInput = it },
                            label = { Text("Port") },
                            placeholder = { Text("80") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("server_port_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                serverIpInput = "192.168.1.100"
                                serverPortInput = "80"
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Set LAN 192.168.x", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                serverIpInput = "https://water.adinebried.gov.et"
                                serverPortInput = "443"
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Set Cloud Prod", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            onSaveServerConfig(serverIpInput, serverPortInput)
                            Toast.makeText(context, "Network target saved: $serverIpInput:$serverPortInput", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("save_server_config_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Network Settings")
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3. Database Backup & Sync Management
                    Text(
                        text = "3. DATABASE & SYNC CONTROLS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            onBackupDatabase()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("db_backup_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export SQLite DB to Storage")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onForceSync,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("force_sync_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Force LAN Sync", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onPurgeSyncedRecords,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("purge_synced_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2185B)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Purge Synced Only", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 4. Emergency Offline License Generator & Override
                    Text(
                        text = "4. EMERGENCY LICENSE GENERATOR & OVERRIDES",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Emergency Offline License Override", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Bypasses license validation for testing/field operations", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = isEmergencyBypass,
                                    onCheckedChange = { checked ->
                                        isEmergencyBypass = checked
                                        onEmergencyLicenseToggle(checked)
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Bypass Time Anti-Tamper Check", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("For devices without SIM / NTP connection", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = isTimeTamperBypassed,
                                    onCheckedChange = { checked ->
                                        isTimeTamperBypassed = checked
                                        TimeIntegrityManager.bypassTamperCheckForDev = checked
                                        Toast.makeText(context, if (checked) "Time integrity bypassed for dev" else "Time integrity enforced", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val (exp, key) = HardwareLicensingManager.generateQuickKeyForDevice(context, 365)
                                    generatedOfflineKey = key
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("License Key", key))
                                    Toast.makeText(context, "Generated & Copied Key (365 days)", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate 365-Day SHA-256 Key for This Device", fontSize = 12.sp)
                            }

                            if (generatedOfflineKey != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = generatedOfflineKey!!,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
