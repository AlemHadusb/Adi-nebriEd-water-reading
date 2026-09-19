package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "system_config")
data class SystemConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "key")
    val key: String,
    @ColumnInfo(name = "value")
    val value: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)

@Entity(tableName = "tariffs")
data class TariffEntity(
    @PrimaryKey
    @ColumnInfo(name = "tier_id")
    val tierId: Int,
    @ColumnInfo(name = "min_units")
    val minUnits: Double,
    @ColumnInfo(name = "max_units")
    val maxUnits: Double,
    @ColumnInfo(name = "rate_per_unit")
    val ratePerUnit: Double,
    @ColumnInfo(name = "fixed_service_fee")
    val fixedServiceFee: Double
)

@Entity(
    tableName = "customers",
    indices = [Index(value = ["meter_number"], unique = true)]
)
data class CustomerEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "meter_number")
    val meterNumber: String,
    @ColumnInfo(name = "full_name")
    val fullName: String,
    @ColumnInfo(name = "phone")
    val phone: String,
    @ColumnInfo(name = "zone")
    val zone: String,
    @ColumnInfo(name = "previous_reading")
    val previousReading: Double,
    @ColumnInfo(name = "previous_reading_date")
    val previousReadingDate: String,
    @ColumnInfo(name = "arrears")
    val arrears: Double = 0.0,
    @ColumnInfo(name = "status")
    val status: String = "ACTIVE"
)

@Entity(
    tableName = "meter_readings",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["customer_id"]),
        Index(value = ["sync_status"]),
        Index(value = ["reading_timestamp"])
    ]
)
data class MeterReadingEntity(
    @PrimaryKey
    @ColumnInfo(name = "reading_id")
    val readingId: String,
    @ColumnInfo(name = "customer_id")
    val customerId: String,
    @ColumnInfo(name = "previous_reading")
    val previousReading: Double,
    @ColumnInfo(name = "current_reading")
    val currentReading: Double,
    @ColumnInfo(name = "units_consumed")
    val unitsConsumed: Double,
    @ColumnInfo(name = "water_fee")
    val waterFee: Double,
    @ColumnInfo(name = "service_fee")
    val serviceFee: Double,
    @ColumnInfo(name = "arrears")
    val arrears: Double,
    @ColumnInfo(name = "total_payable")
    val totalPayable: Double,
    @ColumnInfo(name = "amount_paid")
    val amountPaid: Double = 0.0,
    @ColumnInfo(name = "balance")
    val balance: Double,
    @ColumnInfo(name = "payment_status")
    val paymentStatus: String, // 'PAID', 'PARTIAL', 'UNPAID'
    @ColumnInfo(name = "payment_method")
    val paymentMethod: String, // 'Cash', 'Telebirr', 'CBE Birr', 'Unpaid'
    @ColumnInfo(name = "receipt_number")
    val receiptNumber: String,
    @ColumnInfo(name = "anomaly_code")
    val anomalyCode: String, // 'NORMAL', 'BROKEN_DIAL', 'LEAKAGE', 'STALLED', 'GATE_LOCKED'
    @ColumnInfo(name = "reading_timestamp")
    val readingTimestamp: String,
    @ColumnInfo(name = "cashier_agent_id")
    val cashierAgentId: String,
    @ColumnInfo(name = "sync_status")
    val syncStatus: Int = 0 // 0 = Pending, 1 = Synced
)

data class ReadingWithCustomer(
    val reading: MeterReadingEntity,
    val customer: CustomerEntity
)
