package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY full_name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY full_name ASC")
    suspend fun getAllCustomersList(): List<CustomerEntity>

    @Query("""
        SELECT * FROM customers 
        WHERE full_name LIKE '%' || :query || '%' 
           OR meter_number LIKE '%' || :query || '%' 
           OR phone LIKE '%' || :query || '%'
           OR zone LIKE '%' || :query || '%'
        ORDER BY full_name ASC
    """)
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE meter_number = :meterNumber LIMIT 1")
    suspend fun getCustomerByMeter(meterNumber: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("""
        UPDATE customers 
        SET previous_reading = :newReading,
            previous_reading_date = :readingDate,
            arrears = :newArrears
        WHERE id = :customerId
    """)
    suspend fun updateCustomerReadingAndArrears(
        customerId: String,
        newReading: Double,
        readingDate: String,
        newArrears: Double
    )
}

@Dao
interface MeterReadingDao {
    @Query("SELECT * FROM meter_readings ORDER BY reading_timestamp DESC")
    fun getAllReadingsFlow(): Flow<List<MeterReadingEntity>>

    @Query("SELECT * FROM meter_readings ORDER BY reading_timestamp DESC")
    suspend fun getAllReadingsList(): List<MeterReadingEntity>

    @Query("SELECT * FROM meter_readings WHERE sync_status = 0 ORDER BY reading_timestamp ASC")
    suspend fun getPendingSyncReadings(): List<MeterReadingEntity>

    @Query("SELECT COUNT(*) FROM meter_readings WHERE sync_status = 0")
    fun getPendingSyncCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: MeterReadingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadings(readings: List<MeterReadingEntity>)

    @Query("UPDATE meter_readings SET sync_status = :status WHERE reading_id IN (:readingIds)")
    suspend fun updateSyncStatus(readingIds: List<String>, status: Int = 1)

    @Query("DELETE FROM meter_readings WHERE sync_status = 1")
    suspend fun purgeSyncedRecords(): Int

    @Query("SELECT * FROM meter_readings WHERE reading_id = :id LIMIT 1")
    suspend fun getReadingById(id: String): MeterReadingEntity?

    @Query("SELECT COUNT(*) FROM meter_readings")
    fun getTotalReadingsCount(): Flow<Int>
}

@Dao
interface TariffDao {
    @Query("SELECT * FROM tariffs ORDER BY tier_id ASC")
    fun getAllTariffsFlow(): Flow<List<TariffEntity>>

    @Query("SELECT * FROM tariffs ORDER BY tier_id ASC")
    suspend fun getAllTariffsList(): List<TariffEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTariffs(tariffs: List<TariffEntity>)
}

@Dao
interface SystemConfigDao {
    @Query("SELECT value FROM system_config WHERE `key` = :key LIMIT 1")
    suspend fun getConfig(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setConfig(config: SystemConfigEntity)

    @Query("SELECT * FROM system_config")
    suspend fun getAllConfigs(): List<SystemConfigEntity>
}
