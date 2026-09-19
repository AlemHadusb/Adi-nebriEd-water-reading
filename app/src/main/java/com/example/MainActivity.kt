package com.example

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.HardwareLicensingManager
import com.example.ui.WaterAppViewModel
import com.example.ui.components.DeveloperConsoleDialog
import com.example.ui.components.LicenseGateDialog
import com.example.ui.components.ThermalReceiptDialog
import com.example.ui.components.TimeTamperDialog
import com.example.ui.screens.CashierEntryScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.ReadingsRegisterScreen
import com.example.ui.screens.TariffsScreen
import com.example.ui.theme.MyApplicationTheme

enum class MainTab(val title: String, val icon: ImageVector) {
    CASHIER_ENTRY("Cashier Entry", Icons.Default.Speed),
    REGISTER("Daily Register", Icons.Default.ReceiptLong),
    CUSTOMERS("Customers", Icons.Default.People),
    TARIFFS("Tariffs & Station", Icons.Default.WaterDrop)
}

class MainActivity : ComponentActivity() {

    private val viewModel: WaterAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                WaterAppMainContent(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-verify time integrity whenever cashier returns to app
        viewModel.verifyTimeIntegrity()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterAppMainContent(viewModel: WaterAppViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(MainTab.CASHIER_ENTRY) }

    val customers by viewModel.customers.collectAsState()
    val readings by viewModel.readings.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val timeIntegrity by viewModel.timeIntegrity.collectAsState()
    val licenseState by viewModel.licenseState.collectAsState()
    val stationName by viewModel.stationName.collectAsState()
    val serverIp by viewModel.serverIp.collectAsState()
    val serverPort by viewModel.serverPort.collectAsState()
    val showDevConsole by viewModel.showDevConsole.collectAsState()
    val activeReceipt by viewModel.activeReceipt.collectAsState()

    val customerMap = remember(customers) { customers.associateBy { it.id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Adi NebriEd Water",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ዓዲ ነብሪ ኢድ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Station: $stationName • Cashier: Almaz K. • ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                            // Secret 7-tap area on version label
                            Text(
                                text = "v2.4",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .clickable { viewModel.onVersionTapped() }
                                    .testTag("app_version_tap_target")
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openDevConsoleDirectly() },
                        modifier = Modifier.testTag("open_dev_console_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Developer & Diagnostics",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                MainTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            if (tab == MainTab.REGISTER && pendingSyncCount > 0) {
                                BadgedBox(badge = { Badge { Text("$pendingSyncCount") } }) {
                                    Icon(tab.icon, contentDescription = tab.title)
                                }
                            } else {
                                Icon(tab.icon, contentDescription = tab.title)
                            }
                        },
                        label = { Text(tab.title, fontSize = 10.sp, maxLines = 1) },
                        modifier = Modifier.testTag("nav_tab_${tab.name}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.CASHIER_ENTRY -> {
                    CashierEntryScreen(
                        customers = customers,
                        onRecordReading = { cust, reading, anomaly, method, paid ->
                            viewModel.recordReading(cust, reading, anomaly, method, paid)
                        }
                    )
                }
                MainTab.REGISTER -> {
                    ReadingsRegisterScreen(
                        readings = readings,
                        customerMap = customerMap,
                        pendingSyncCount = pendingSyncCount,
                        onExportLandscapeExcel = {
                            viewModel.exportLandscapeExcel(context)
                        },
                        onSyncWithLan = {
                            viewModel.syncWithLanServer()
                        },
                        onViewReceipt = { reading ->
                            viewModel.showReceiptForReading(reading)
                        }
                    )
                }
                MainTab.CUSTOMERS -> {
                    CustomersScreen(
                        customers = customers,
                        onAddCustomer = { newCust ->
                            viewModel.addCustomer(newCust)
                        }
                    )
                }
                MainTab.TARIFFS -> {
                    TariffsScreen(
                        stationName = stationName,
                        cashierName = "Almaz K.",
                        cashierId = "AGT-042"
                    )
                }
            }
        }
    }

    // MANDATORY SECURITY OVERLAY 1: Anti-Backdating Time Integrity Enforcement
    if (timeIntegrity.isTampered) {
        TimeTamperDialog(
            onOpenSettings = {
                try {
                    context.startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
                } catch (e: Exception) {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            },
            onVerifyAgain = {
                viewModel.verifyTimeIntegrity()
            }
        )
    }

    // MANDATORY SECURITY OVERLAY 2: Hardware-Locked Station License Enforcement
    if (!timeIntegrity.isTampered && !licenseState.isLicensed) {
        val androidId = remember { HardwareLicensingManager.getHardwareAndroidId(context) }
        LicenseGateDialog(
            androidId = androidId,
            stationName = stationName,
            onActivate = { key, expiry ->
                viewModel.activateLicense(key, expiry)
            },
            onBypassForDemo = {
                viewModel.toggleEmergencyLicenseBypass(true)
            }
        )
    }

    // DEVELOPER & DIAGNOSTICS CONSOLE (Password: MomLove@1)
    if (showDevConsole) {
        DeveloperConsoleDialog(
            currentStation = stationName,
            currentServerIp = serverIp,
            currentPort = serverPort,
            onDismiss = { viewModel.closeDevConsole() },
            onSaveStation = { viewModel.updateStation(it) },
            onSaveServerConfig = { ip, port -> viewModel.updateServerConfig(ip, port) },
            onBackupDatabase = { viewModel.backupDatabase() },
            onPurgeSyncedRecords = { viewModel.purgeSyncedRecords() },
            onForceSync = { viewModel.syncWithLanServer() },
            onEmergencyLicenseToggle = { viewModel.toggleEmergencyLicenseBypass(it) }
        )
    }

    // THERMAL RECEIPT MODAL
    activeReceipt?.let { (receiptText, receiptNumber) ->
        ThermalReceiptDialog(
            receiptText = receiptText,
            receiptNumber = receiptNumber,
            onDismiss = { viewModel.dismissReceipt() }
        )
    }
}
