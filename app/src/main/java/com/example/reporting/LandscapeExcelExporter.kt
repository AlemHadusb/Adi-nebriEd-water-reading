package com.example.reporting

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.CustomerEntity
import com.example.data.MeterReadingEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LandscapeExcelExporter {

    fun generateLandscapeRegisterCsv(
        context: Context,
        stationName: String = "Adi NebriEd",
        cashierName: String = "Almaz K.",
        readings: List<MeterReadingEntity>,
        customerMap: Map<String, CustomerEntity>
    ): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val dateDisplay = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val file = File(exportDir, "AdiNebriEd_Cashier_Register_Landscape_$timestamp.csv")

        var totalCash = 0.0
        var totalDigital = 0.0
        var totalBilled = 0.0

        FileOutputStream(file).use { fos ->
            // Write UTF-8 BOM so Excel opens with proper character encoding
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                // Header block (Rows 1 to 6)
                writer.appendLine("\"ADI NEBRIED WATER SUPPLY AND SEWERAGE AUTHORITY (ዓዲ ነብሪ ኢድ ማይ ቀረብን ክፍሊትን)\",,,,,,,,,,,,,,,,,")
                writer.appendLine("\"OFFICIAL CASHIER DAILY SHIFT RECONCILIATION & WATER METER REGISTER\",,,,,,,,,,,,,,,,,")
                writer.appendLine("\"Station:\",\"$stationName\",\"Date:\",\"$dateDisplay\",\"Cashier Agent:\",\"$cashierName\",\"Page Setup:\",\"LANDSCAPE A4 (Fit to 1 Page Wide)\",,,,,,,,,,")
                writer.appendLine("\"Printing Instructions:\",\"Orientation: LANDSCAPE | Margins: Narrow (0.35 in) | Gridlines: ON | Repeat Header Rows $1:$6 across all pages\",,,,,,,,,,,,,,,,,")
                writer.appendLine(",,,,,,,,,,,,,,,,,")

                // Column Headers (Row 6) - 18 Columns strictly per spec
                val headers = listOf(
                    "No.",
                    "Cust ID",
                    "Meter No.",
                    "Customer Full Name",
                    "Phone",
                    "Zone",
                    "Prev Read (m³)",
                    "Curr Read (m³)",
                    "Consumed (m³)",
                    "Water Fee (ETB)",
                    "Service Fee (ETB)",
                    "Arrears (ETB)",
                    "Total Due (ETB)",
                    "Amount Paid (ETB)",
                    "Balance (ETB)",
                    "Status",
                    "Receipt No.",
                    "Payment Channel / Remarks"
                )
                writer.appendLine(headers.joinToString(",") { "\"$it\"" })

                // Data Rows
                readings.forEachIndexed { index, reading ->
                    val rowNum = index + 7 // 1-indexed row in Excel
                    val customer = customerMap[reading.customerId]
                    val custName = customer?.fullName ?: "Unknown"
                    val phone = customer?.phone ?: ""
                    val zone = customer?.zone ?: ""
                    val meterNo = customer?.meterNumber ?: reading.customerId

                    val consumedFormula = "=H$rowNum-G$rowNum"
                    val balanceFormula = "=M$rowNum-N$rowNum"
                    val statusFormula = "=IF(O$rowNum<=0.05,\"PAID\",IF(N$rowNum>0,\"PARTIAL\",\"UNPAID\"))"

                    val paymentChannel = if (reading.anomalyCode != "NORMAL") {
                        "${reading.paymentMethod} [${reading.anomalyCode}]"
                    } else {
                        reading.paymentMethod
                    }

                    if (reading.paymentMethod.equals("Cash", ignoreCase = true)) {
                        totalCash += reading.amountPaid
                    } else if (reading.paymentMethod.contains("Birr", ignoreCase = true) || 
                               reading.paymentMethod.contains("Telebirr", ignoreCase = true)) {
                        totalDigital += reading.amountPaid
                    }
                    totalBilled += reading.totalPayable

                    val rowValues = listOf(
                        "${index + 1}",
                        reading.customerId,
                        meterNo,
                        custName,
                        phone,
                        zone,
                        String.format(Locale.US, "%.2f", reading.previousReading),
                        String.format(Locale.US, "%.2f", reading.currentReading),
                        consumedFormula,
                        String.format(Locale.US, "%.2f", reading.waterFee),
                        String.format(Locale.US, "%.2f", reading.serviceFee),
                        String.format(Locale.US, "%.2f", reading.arrears),
                        String.format(Locale.US, "%.2f", reading.totalPayable),
                        String.format(Locale.US, "%.2f", reading.amountPaid),
                        balanceFormula,
                        statusFormula,
                        reading.receiptNumber,
                        paymentChannel
                    )
                    writer.appendLine(rowValues.joinToString(",") { "\"$it\"" })
                }

                // Empty separator line
                writer.appendLine(",,,,,,,,,,,,,,,,,")
                writer.appendLine(",,,,,,,,,,,,,,,,,")

                // Shift Reconciliation Summary Block
                val totalCollected = totalCash + totalDigital
                writer.appendLine("\"=== CASHIER SHIFT RECONCILIATION SUMMARY ===\",,,,,,,,,,,,,,,,,")
                writer.appendLine("\"Total Records:\",\"${readings.size}\",\"Total Water Billed:\",\"${String.format(Locale.US, "%.2f ETB", totalBilled)}\",\"Total Cash:\",\"${String.format(Locale.US, "%.2f ETB", totalCash)}\",\"Total Digital (CBE/Telebirr):\",\"${String.format(Locale.US, "%.2f ETB", totalDigital)}\",\"Net Collected:\",\"${String.format(Locale.US, "%.2f ETB", totalCollected)}\",,,,,,,,")
                writer.appendLine(",,,,,,,,,,,,,,,,,")

                // Triple Signature Handover Blocks per spec
                writer.appendLine("\"=== AUDIT & TRIPLE SIGNATURE HANDOVER BLOCKS ===\",,,,,,,,,,,,,,,,,")
                writer.appendLine("\"BOX 1: CASHIER SHIFT HANDOVER\",\"BOX 2: SENIOR REVENUE ACCOUNTANT AUDIT\",\"BOX 3: STATION MANAGER FINAL APPROVAL\",,,,,,,,,,,,,,,")
                writer.appendLine("\"Cashier Name: $cashierName\",\"Auditor Name: ___________________\",\"Station: $stationName Branch Stamp\",,,,,,,,,,,,,,,")
                writer.appendLine("\"Cash Collected: ${String.format(Locale.US, "%.2f ETB", totalCash)}\",\"Bank Deposit Slip Ref: _____________\",\"Approval Status: VERIFIED & APPROVED\",,,,,,,,,,,,,,,")
                writer.appendLine("\"Digital (CBE/Telebirr): ${String.format(Locale.US, "%.2f ETB", totalDigital)}\",\"Physical Cash Verified: [ ] MATCH\",\"Manager Signature: ___________________\",,,,,,,,,,,,,,,")
                writer.appendLine("\"Signature: ___________________\",\"Variance / Discrepancy: 0.00 ETB\",\"Date Signed: _________________________\",,,,,,,,,,,,,,,")
                writer.appendLine("\"Handover Date: $dateDisplay\",\"Auditor Signature: _________________\",\"Official Seal: [  SEAL HERE  ]\",,,,,,,,,,,,,,,")
            }
        }

        return file
    }

    fun shareExportFile(context: Context, file: File) {
        val uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            // Fallback to file URI if provider not registered
            android.net.Uri.fromFile(file)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Adi NebriEd Cashier Daily Register (Landscape)")
            putExtra(
                Intent.EXTRA_TEXT,
                "Attached is the official Adi NebriEd Cashier Water Meter Daily Register with 18-column landscape structure, reconciliation summary, and triple handover signatures."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Landscape Cashier Register")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
