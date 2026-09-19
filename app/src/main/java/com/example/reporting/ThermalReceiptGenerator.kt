package com.example.reporting

import com.example.data.CustomerEntity
import com.example.data.MeterReadingEntity
import java.util.Locale

object ThermalReceiptGenerator {

    fun generateReceiptText(
        reading: MeterReadingEntity,
        customer: CustomerEntity,
        stationName: String = "Adi NebriEd",
        stationTigrinya: String = "ዓዲ ነብሪ ኢድ ማይ ቀረብን ክፍሊትን"
    ): String {
        val divider = "=========================================="
        val subDivider = "------------------------------------------"

        return buildString {
            appendLine("      WATER SUPPLY & SEWERAGE AUTHORITY   ")
            appendLine("           $stationName STATION            ")
            appendLine("       $stationTigrinya")
            appendLine(divider)
            appendLine("OFFICIAL WATER BILL & PAYMENT RECEIPT")
            appendLine("Receipt No:   ${reading.receiptNumber}")
            appendLine("Date & Time:  ${reading.readingTimestamp}")
            appendLine("Cashier:      ${reading.cashierAgentId} (Almaz K.)")
            appendLine(subDivider)
            appendLine("CUSTOMER DETAILS")
            appendLine("Cust ID:      ${customer.id}")
            appendLine("Name:         ${customer.fullName}")
            appendLine("Meter No:     ${customer.meterNumber}")
            appendLine("Zone/Kebele:  ${customer.zone}")
            appendLine("Phone:        ${customer.phone}")
            appendLine(subDivider)
            appendLine("METER CONSUMPTION")
            appendLine("Prev Reading: ${String.format(Locale.US, "%10.2f m³", reading.previousReading)}")
            appendLine("Curr Reading: ${String.format(Locale.US, "%10.2f m³", reading.currentReading)}")
            appendLine("Units Used:   ${String.format(Locale.US, "%10.2f m³", reading.unitsConsumed)}")
            if (reading.anomalyCode != "NORMAL") {
                appendLine("Anomaly Note: ** ${reading.anomalyCode} **")
            }
            appendLine(subDivider)
            appendLine("TARIFF & CHARGES BREAKDOWN")
            appendLine("Water Charge: ${String.format(Locale.US, "%10.2f ETB", reading.waterFee)}")
            appendLine("Service Fee:  ${String.format(Locale.US, "%10.2f ETB", reading.serviceFee)}")
            if (reading.arrears > 0) {
                appendLine("Past Arrears: ${String.format(Locale.US, "%10.2f ETB", reading.arrears)}")
            }
            appendLine(divider)
            appendLine("TOTAL DUE:    ${String.format(Locale.US, "%10.2f ETB", reading.totalPayable)}")
            appendLine("Amount Paid:  ${String.format(Locale.US, "%10.2f ETB", reading.amountPaid)}")
            appendLine("Balance Due:  ${String.format(Locale.US, "%10.2f ETB", reading.balance)}")
            appendLine("Payment Stat: ${reading.paymentStatus}")
            appendLine("Payment Via:  ${reading.paymentMethod}")
            appendLine(divider)
            appendLine("         THANK YOU FOR YOUR PAYMENT!       ")
            appendLine("   Please preserve this receipt for audit  ")
            appendLine("      For inquiries call: 034-440-1234     ")
            appendLine("        Powered by Adi NebriEd WSSA        ")
            appendLine("==========================================")
        }
    }
}
