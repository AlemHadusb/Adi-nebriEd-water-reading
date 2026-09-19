package com.example.domain

import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

data class TierBreakdownItem(
    val tierLabel: String,
    val units: Double,
    val rate: Double,
    val subtotal: Double
)

data class BillCalculationResult(
    val previousReading: Double,
    val currentReading: Double,
    val unitsConsumed: Double,
    val isInvertedRollover: Boolean,
    val tierBreakdowns: List<TierBreakdownItem>,
    val waterFee: Double,
    val serviceFee: Double,
    val pastArrears: Double,
    val totalDue: Double,
    val amountPaid: Double,
    val balance: Double,
    val paymentStatus: String // 'PAID', 'PARTIAL', 'UNPAID'
)

object BillingEngine {

    const val FIXED_SERVICE_FEE: Double = 35.00

    fun calculateBill(
        previousReading: Double,
        currentReading: Double,
        pastArrears: Double = 0.0,
        amountPaid: Double = 0.0
    ): BillCalculationResult {
        val rawDelta = currentReading - previousReading
        val isInverted = rawDelta < 0.0
        val consumption = if (isInverted) 0.0 else round2(rawDelta)

        val breakdowns = mutableListOf<TierBreakdownItem>()
        var remaining = consumption
        var calculatedWaterFee = 0.0

        // Tier 1: 0 to 5 m3 @ 10.00 ETB/m3
        if (remaining > 0) {
            val tier1Units = min(5.0, remaining)
            val cost = round2(tier1Units * 10.00)
            breakdowns.add(TierBreakdownItem("Tier 1 (0 - 5 m³)", tier1Units, 10.00, cost))
            calculatedWaterFee += cost
            remaining -= tier1Units
        } else {
            breakdowns.add(TierBreakdownItem("Tier 1 (0 - 5 m³)", 0.0, 10.00, 0.0))
        }

        // Tier 2: 5.01 to 15 m3 (next 10 m3) @ 18.50 ETB/m3
        if (remaining > 0) {
            val tier2Units = min(10.0, remaining)
            val cost = round2(tier2Units * 18.50)
            breakdowns.add(TierBreakdownItem("Tier 2 (5 - 15 m³)", tier2Units, 18.50, cost))
            calculatedWaterFee += cost
            remaining -= tier2Units
        }

        // Tier 3: 15.01 to 30 m3 (next 15 m3) @ 28.00 ETB/m3
        if (remaining > 0) {
            val tier3Units = min(15.0, remaining)
            val cost = round2(tier3Units * 28.00)
            breakdowns.add(TierBreakdownItem("Tier 3 (15 - 30 m³)", tier3Units, 28.00, cost))
            calculatedWaterFee += cost
            remaining -= tier3Units
        }

        // Tier 4: > 30 m3 @ 42.00 ETB/m3
        if (remaining > 0) {
            val tier4Units = remaining
            val cost = round2(tier4Units * 42.00)
            breakdowns.add(TierBreakdownItem("Tier 4 (> 30 m³)", tier4Units, 42.00, cost))
            calculatedWaterFee += cost
        }

        val roundedWaterFee = round2(calculatedWaterFee)
        val roundedArrears = round2(pastArrears)
        val totalDue = round2(roundedWaterFee + FIXED_SERVICE_FEE + roundedArrears)
        val roundedPaid = round2(amountPaid)
        val balance = round2(max(0.0, totalDue - roundedPaid))

        val paymentStatus = when {
            balance <= 0.05 && totalDue > 0.0 -> "PAID"
            roundedPaid > 0.0 -> "PARTIAL"
            else -> "UNPAID"
        }

        return BillCalculationResult(
            previousReading = previousReading,
            currentReading = currentReading,
            unitsConsumed = consumption,
            isInvertedRollover = isInverted,
            tierBreakdowns = breakdowns,
            waterFee = roundedWaterFee,
            serviceFee = FIXED_SERVICE_FEE,
            pastArrears = roundedArrears,
            totalDue = totalDue,
            amountPaid = roundedPaid,
            balance = balance,
            paymentStatus = paymentStatus
        )
    }

    private fun round2(value: Double): Double {
        return (value * 100.0).roundToLong() / 100.0
    }

    fun formatEtb(value: Double): String {
        return String.format(Locale.US, "%.2f ETB", value)
    }

    fun formatUnits(units: Double): String {
        return String.format(Locale.US, "%.2f m³", units)
    }
}
