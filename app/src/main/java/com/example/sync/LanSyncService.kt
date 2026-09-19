package com.example.sync

import com.example.data.CustomerEntity
import com.example.data.MeterReadingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SyncResult(
    val success: Boolean,
    val syncedCount: Int,
    val confirmedIds: List<String>,
    val message: String
)

class LanSyncService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun syncReadingsToLan(
        serverIp: String,
        port: String,
        stationName: String,
        readings: List<MeterReadingEntity>,
        customerMap: Map<String, CustomerEntity>
    ): SyncResult = withContext(Dispatchers.IO) {
        if (readings.isEmpty()) {
            return@withContext SyncResult(
                success = true,
                syncedCount = 0,
                confirmedIds = emptyList(),
                message = "All records are already synced. Zero pending items."
            )
        }

        val cleanedIp = serverIp.trim()
        val cleanedPort = port.trim().ifBlank { "80" }
        val endpoint = if (cleanedIp.startsWith("http://") || cleanedIp.startsWith("https://")) {
            "$cleanedIp/sync_readings.php"
        } else {
            "http://$cleanedIp:$cleanedPort/sync_readings.php"
        }

        try {
            val rootJson = JSONObject().apply {
                put("station", stationName)
                put("sync_timestamp", System.currentTimeMillis() / 1000)
                val readingsArray = JSONArray()

                for (r in readings) {
                    val cust = customerMap[r.customerId]
                    val item = JSONObject().apply {
                        put("reading_id", r.readingId)
                        put("customer_id", r.customerId)
                        put("meter_number", cust?.meterNumber ?: "")
                        put("previous_reading", r.previousReading)
                        put("current_reading", r.currentReading)
                        put("units_consumed", r.unitsConsumed)
                        put("water_fee", r.waterFee)
                        put("service_fee", r.serviceFee)
                        put("arrears", r.arrears)
                        put("total_payable", r.totalPayable)
                        put("amount_paid", r.amountPaid)
                        put("balance", r.balance)
                        put("payment_status", r.paymentStatus)
                        put("payment_method", r.paymentMethod)
                        put("receipt_number", r.receiptNumber)
                        put("anomaly_code", r.anomalyCode)
                        put("reading_timestamp", r.readingTimestamp)
                        put("cashier_agent_id", r.cashierAgentId)
                    }
                    readingsArray.put(item)
                }
                put("readings", readingsArray)
            }

            val body = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(endpoint)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext SyncResult(
                        success = false,
                        syncedCount = 0,
                        confirmedIds = emptyList(),
                        message = "Server HTTP error: ${response.code} ${response.message}"
                    )
                }

                val responseBody = response.body?.string().orEmpty()
                val json = JSONObject(responseBody)
                val status = json.optString("status", "error")

                if (status == "success" || status == "ok") {
                    val confirmedArray = json.optJSONArray("confirmed_reading_ids") ?: JSONArray()
                    val confirmedList = mutableListOf<String>()
                    for (i in 0 until confirmedArray.length()) {
                        confirmedList.add(confirmedArray.getString(i))
                    }

                    return@withContext SyncResult(
                        success = true,
                        syncedCount = confirmedList.size,
                        confirmedIds = confirmedList,
                        message = "Successfully synced ${confirmedList.size} records to $serverIp"
                    )
                } else {
                    val errorMsg = json.optString("message", "Unknown sync error from server")
                    return@withContext SyncResult(
                        success = false,
                        syncedCount = 0,
                        confirmedIds = emptyList(),
                        message = "LAN Sync Rejected: $errorMsg"
                    )
                }
            }
        } catch (e: Exception) {
            SyncResult(
                success = false,
                syncedCount = 0,
                confirmedIds = emptyList(),
                message = "LAN Connection Offline: ${e.localizedMessage ?: "Unable to reach $serverIp"}"
            )
        }
    }
}
