package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SystemConfigEntity::class,
        TariffEntity::class,
        CustomerEntity::class,
        MeterReadingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun meterReadingDao(): MeterReadingDao
    abstract fun tariffDao(): TariffDao
    abstract fun systemConfigDao(): SystemConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "adi_nebried_water.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val nowStr = "2026-09-01 08:00:00"

            // 1. Initial System Configs
            val configs = listOf(
                SystemConfigEntity("station_name", "Adi NebriEd", nowStr),
                SystemConfigEntity("station_tigrinya", "ዓዲ ነብሪ ኢድ ማይ ቀረብን ክፍሊትን", nowStr),
                SystemConfigEntity("server_lan_ip", "192.168.1.100", nowStr),
                SystemConfigEntity("server_port", "80", nowStr),
                SystemConfigEntity("cashier_agent_name", "Almaz K.", nowStr),
                SystemConfigEntity("cashier_agent_id", "AGT-042", nowStr),
                SystemConfigEntity("currency", "ETB", nowStr)
            )
            configs.forEach { database.systemConfigDao().setConfig(it) }

            // 2. Default Tiered Tariffs
            val tariffs = listOf(
                TariffEntity(tierId = 1, minUnits = 0.0, maxUnits = 5.0, ratePerUnit = 10.00, fixedServiceFee = 35.00),
                TariffEntity(tierId = 2, minUnits = 5.01, maxUnits = 15.0, ratePerUnit = 18.50, fixedServiceFee = 35.00),
                TariffEntity(tierId = 3, minUnits = 15.01, maxUnits = 30.0, ratePerUnit = 28.00, fixedServiceFee = 35.00),
                TariffEntity(tierId = 4, minUnits = 30.01, maxUnits = 999999.0, ratePerUnit = 42.00, fixedServiceFee = 35.00)
            )
            database.tariffDao().insertTariffs(tariffs)

            // 3. Realistic Initial Customers in Adi NebriEd
            val customers = listOf(
                CustomerEntity(
                    id = "CUST-001",
                    meterNumber = "WM-ANE-1001",
                    fullName = "Hagos Gebremariam",
                    phone = "+251914101010",
                    zone = "Zone 01 - Downtown",
                    previousReading = 142.50,
                    previousReadingDate = "2026-08-15",
                    arrears = 0.0,
                    status = "ACTIVE"
                ),
                CustomerEntity(
                    id = "CUST-002",
                    meterNumber = "WM-ANE-1002",
                    fullName = "Letebirhan Woldegebriel",
                    phone = "+251914202020",
                    zone = "Zone 01 - Downtown",
                    previousReading = 89.20,
                    previousReadingDate = "2026-08-15",
                    arrears = 45.0,
                    status = "ACTIVE"
                ),
                CustomerEntity(
                    id = "CUST-003",
                    meterNumber = "WM-ANE-1003",
                    fullName = "Tewolde Teklehaimanot",
                    phone = "+251914303030",
                    zone = "Zone 02 - Upper Market",
                    previousReading = 312.00,
                    previousReadingDate = "2026-08-16",
                    arrears = 0.0,
                    status = "ACTIVE"
                ),
                CustomerEntity(
                    id = "CUST-004",
                    meterNumber = "WM-ANE-1004",
                    fullName = "Abeba Berhe",
                    phone = "+251914404040",
                    zone = "Zone 02 - Upper Market",
                    previousReading = 205.80,
                    previousReadingDate = "2026-08-16",
                    arrears = 120.0,
                    status = "ACTIVE"
                ),
                CustomerEntity(
                    id = "CUST-005",
                    meterNumber = "WM-ANE-1005",
                    fullName = "Berhane Asgedom",
                    phone = "+251914505050",
                    zone = "Kebele 03 - Residential",
                    previousReading = 58.40,
                    previousReadingDate = "2026-08-17",
                    arrears = 0.0,
                    status = "ACTIVE"
                ),
                CustomerEntity(
                    id = "CUST-006",
                    meterNumber = "WM-ANE-1006",
                    fullName = "Roman Kahsay",
                    phone = "+251914606060",
                    zone = "Kebele 03 - Residential",
                    previousReading = 118.00,
                    previousReadingDate = "2026-08-17",
                    arrears = 250.0,
                    status = "ACTIVE"
                ),
                CustomerEntity(
                    id = "CUST-007",
                    meterNumber = "WM-ANE-1007",
                    fullName = "Kidanemariam Tesfay",
                    phone = "+251914707070",
                    zone = "Godfey Border",
                    previousReading = 430.10,
                    previousReadingDate = "2026-08-18",
                    arrears = 0.0,
                    status = "ACTIVE"
                ),
                CustomerEntity(
                    id = "CUST-008",
                    meterNumber = "WM-ANE-1008",
                    fullName = "Alganesh Girmay",
                    phone = "+251914808080",
                    zone = "Adi Klte Area",
                    previousReading = 76.50,
                    previousReadingDate = "2026-08-18",
                    arrears = 0.0,
                    status = "ACTIVE"
                )
            )
            database.customerDao().insertCustomers(customers)
        }
    }
}
