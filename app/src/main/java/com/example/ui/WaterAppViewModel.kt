package com.example.ui

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CustomerEntity
import com.example.data.MeterReadingEntity
import com.example.data.WaterRepository
import com.example.domain.BillingEngine
import com.example.reporting.LandscapeExcelExporter
import com.example.reporting.ThermalReceiptGenerator
import com.example.security.HardwareLicensingManager
import com.example.security.LicenseState
import com.example.security.TimeIntegrityManager
import com.example.security.TimeIntegrityStatus
import com.example.sync.LanSyncService
import com.example.sync.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WaterAppViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = WaterRepository(database, application)
    private val lanSyncService = LanSyncService()

    // Database reactive streams
    val customers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readings: StateFlow<List<MeterReadingEntity>> = repository.allReadings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Security & Integrity States
    private val _timeIntegrity = MutableStateFlow(TimeIntegrityManager.checkDeviceTimeIntegrity(application))
    val timeIntegrity: StateFlow<TimeIntegrityStatus> = _timeIntegrity.asStateFlow()

    private val _licenseState = MutableStateFlow(HardwareLicensingManager.isLicenseValid(application))
    val licenseState: StateFlow<LicenseState> = _licenseState.asStateFlow()

    // Station & Config States
    private val _stationName = MutableStateFlow("Adi NebriEd")
    val stationName: StateFlow<String> = _stationName.asStateFlow()

    private val _serverIp = MutableStateFlow("192.168.1.100")
    val serverIp: StateFlow<String> = _serverIp.asStateFlow()

    private val _serverPort = MutableStateFlow("80")
    val serverPort: StateFlow<String> = _serverPort.asStateFlow()

    // Modals / Dialogs UI state
    var showDevConsole = MutableStateFlow(false)
        private set

    var activeReceipt = MutableStateFlow<Pair<String, String>?>(null) // (receiptText, receiptNumber)
        private set

    var lastSyncResult = MutableStateFlow<SyncResult?>(null)
        private set

    // Version click counter for secret Dev console access (7 taps)
    private var versionClickCount = 0
    private var lastClickTimestamp = 0L

    init {
        viewModelScope.launch {
            _stationName.value = repository.getConfig("station_name", "Adi NebriEd")
            _serverIp.value = repository.getConfig("server_lan_ip", "192.168.1.100")
            _serverPort.value = repository.getConfig("server_port", "80")
        }
    }

    fun verifyTimeIntegrity() {
        _timeIntegrity.value = TimeIntegrityManager.checkDeviceTimeIntegrity(getApplication())
    }

    fun activateLicense(key: String, expiryTs: Long): Boolean {
        val success = HardwareLicensingManager.activateLicense(
            context = getApplication(),
            enteredKey = key,
            expiryTimestamp = expiryTs,
            stationName = _stationName.value
        )
        if (success) {
            _licenseState.value = HardwareLicensingManager.isLicenseValid(getApplication())
        }
        return success
    }

    fun toggleEmergencyLicenseBypass(enabled: Boolean) {
        HardwareLicensingManager.setEmergencyBypass(getApplication(), enabled)
        _licenseState.value = HardwareLicensingManager.isLicenseValid(getApplication())
    }

    fun onVersionTapped() {
        val now = System.currentTimeMillis()
        if (now - lastClickTimestamp > 2500) {
            versionClickCount = 1
        } else {
            versionClickCount++
        }
        lastClickTimestamp = now

        if (versionClickCount >= 7) {
            versionClickCount = 0
            showDevConsole.value = true
        } else if (versionClickCount >= 4) {
            val remaining = 7 - versionClickCount
            Toast.makeText(getApplication(), "$remaining more taps for Developer Diagnostics", Toast.LENGTH_SHORT).show()
        }
    }

    fun openDevConsoleDirectly() {
        showDevConsole.value = true
    }

    fun closeDevConsole() {
        showDevConsole.value = false
    }

    fun updateStation(station: String) {
        _stationName.value = station
        viewModelScope.launch {
            repository.setConfig("station_name", station)
        }
    }

    fun updateServerConfig(ip: String, port: String) {
        _serverIp.value = ip
        _serverPort.value = port
        viewModelScope.launch {
            repository.setConfig("server_lan_ip", ip)
            repository.setConfig("server_port", port)
        }
    }

    fun recordReading(
        customer: CustomerEntity,
        currentReading: Double,
        anomalyCode: String,
        paymentMethod: String,
        amountPaid: Double
    ) {
        viewModelScope.launch {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val receiptNum = "ANE-${SimpleDateFormat("yyMMddHHmmss", Locale.US).format(Date())}"
            val readingId = "READ-${System.currentTimeMillis()}"

            val bill = BillingEngine.calculateBill(
                previousReading = customer.previousReading,
                currentReading = currentReading,
                pastArrears = customer.arrears,
                amountPaid = amountPaid
            )

            val reading = MeterReadingEntity(
                readingId = readingId,
                customerId = customer.id,
                previousReading = customer.previousReading,
                currentReading = currentReading,
                unitsConsumed = bill.unitsConsumed,
                waterFee = bill.waterFee,
                serviceFee = bill.serviceFee,
                arrears = bill.pastArrears,
                totalPayable = bill.totalDue,
                amountPaid = bill.amountPaid,
                balance = bill.balance,
                paymentStatus = bill.paymentStatus,
                paymentMethod = paymentMethod,
                receiptNumber = receiptNum,
                anomalyCode = anomalyCode,
                readingTimestamp = now,
                cashierAgentId = "AGT-042",
                syncStatus = 0
            )

            // ACID Room Transaction: updates customer previous reading & records reading
            repository.recordMeterReading(reading, newArrears = bill.balance)

            // Generate Thermal Receipt
            val receiptText = ThermalReceiptGenerator.generateReceiptText(
                reading = reading,
                customer = customer,
                stationName = _stationName.value
            )

            activeReceipt.value = Pair(receiptText, receiptNum)
        }
    }

    fun showReceiptForReading(reading: MeterReadingEntity) {
        viewModelScope.launch {
            val customer = repository.getCustomerById(reading.customerId)
            if (customer != null) {
                val text = ThermalReceiptGenerator.generateReceiptText(
                    reading = reading,
                    customer = customer,
                    stationName = _stationName.value
                )
                activeReceipt.value = Pair(text, reading.receiptNumber)
            }
        }
    }

    fun dismissReceipt() {
        activeReceipt.value = null
    }

    fun exportLandscapeExcel(context: Context) {
        viewModelScope.launch {
            val allR = repository.getAllReadingsList()
            val allC = repository.getAllCustomersList().associateBy { it.id }

            val file = LandscapeExcelExporter.generateLandscapeRegisterCsv(
                context = context,
                stationName = _stationName.value,
                cashierName = "Almaz K.",
                readings = allR,
                customerMap = allC
            )

            LandscapeExcelExporter.shareExportFile(context, file)
        }
    }

    fun syncWithLanServer() {
        viewModelScope.launch {
            val pending = repository.getPendingReadings()
            val customerMap = repository.getAllCustomersList().associateBy { it.id }

            val result = lanSyncService.syncReadingsToLan(
                serverIp = _serverIp.value,
                port = _serverPort.value,
                stationName = _stationName.value,
                readings = pending,
                customerMap = customerMap
            )

            if (result.success && result.confirmedIds.isNotEmpty()) {
                repository.markReadingsAsSynced(result.confirmedIds)
            }

            lastSyncResult.value = result
            Toast.makeText(getApplication(), result.message, Toast.LENGTH_LONG).show()
        }
    }

    fun backupDatabase() {
        viewModelScope.launch {
            val file = repository.backupDatabaseToCache()
            if (file != null) {
                Toast.makeText(getApplication(), "Database backup saved to: ${file.name}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(getApplication(), "Database backup failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun purgeSyncedRecords() {
        viewModelScope.launch {
            val purgedCount = repository.purgeSyncedReadings()
            Toast.makeText(getApplication(), "Purged $purgedCount synced records. Pending records preserved.", Toast.LENGTH_LONG).show()
        }
    }

    fun addCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.addCustomer(customer)
        }
    }
}
