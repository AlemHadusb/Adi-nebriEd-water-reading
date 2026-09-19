package com.example.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WaterRepository(
    private val database: AppDatabase,
    private val context: Context
) {
    private val customerDao = database.customerDao()
    private val readingDao = database.meterReadingDao()
    private val tariffDao = database.tariffDao()
    private val configDao = database.systemConfigDao()

    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    fun searchCustomers(query: String): Flow<List<CustomerEntity>> {
        return if (query.isBlank()) {
            customerDao.getAllCustomers()
        } else {
            customerDao.searchCustomers(query.trim())
        }
    }

    suspend fun getCustomerById(id: String): CustomerEntity? = customerDao.getCustomerById(id)

    suspend fun addCustomer(customer: CustomerEntity) {
        customerDao.insertCustomer(customer)
    }

    val allReadings: Flow<List<MeterReadingEntity>> = readingDao.getAllReadingsFlow()
    val pendingSyncCount: Flow<Int> = readingDao.getPendingSyncCount()

    suspend fun getAllReadingsList(): List<MeterReadingEntity> = readingDao.getAllReadingsList()

    suspend fun getAllCustomersList(): List<CustomerEntity> = customerDao.getAllCustomersList()

    suspend fun getPendingReadings(): List<MeterReadingEntity> = readingDao.getPendingSyncReadings()

    /**
     * ACID Transaction:
     * 1. Inserts Meter Reading
     * 2. Updates Customer previous_reading, reading date, and carries over any remaining balance into arrears
     */
    suspend fun recordMeterReading(
        reading: MeterReadingEntity,
        newArrears: Double
    ) {
        database.withTransaction {
            readingDao.insertReading(reading)
            customerDao.updateCustomerReadingAndArrears(
                customerId = reading.customerId,
                newReading = reading.currentReading,
                readingDate = reading.readingTimestamp.take(10),
                newArrears = newArrears
            )
        }
    }

    suspend fun markReadingsAsSynced(ids: List<String>) {
        if (ids.isNotEmpty()) {
            readingDao.updateSyncStatus(ids, status = 1)
        }
    }

    suspend fun purgeSyncedReadings(): Int {
        return readingDao.purgeSyncedRecords()
    }

    suspend fun getConfig(key: String, defaultValue: String): String {
        return configDao.getConfig(key) ?: defaultValue
    }

    suspend fun setConfig(key: String, value: String) {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        configDao.setConfig(SystemConfigEntity(key, value, now))
    }

    suspend fun backupDatabaseToCache(): File? {
        return try {
            val dbFile = context.getDatabasePath("adi_nebried_water.db")
            if (!dbFile.exists()) return null

            val backupDir = File(context.cacheDir, "backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFile = File(backupDir, "adi_nebried_water_backup_$timestamp.db")

            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }
            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
