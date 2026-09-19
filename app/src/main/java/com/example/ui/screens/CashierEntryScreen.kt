package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomerEntity
import com.example.domain.BillingEngine
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CashierEntryScreen(
    customers: List<CustomerEntity>,
    onRecordReading: (
        customer: CustomerEntity,
        currentReading: Double,
        anomalyCode: String,
        paymentMethod: String,
        amountPaid: Double
    ) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    var selectedCustomer by remember {
        mutableStateOf<CustomerEntity?>(customers.firstOrNull())
    }

    // Auto-update selectedCustomer if customer list loads later
    if (selectedCustomer == null && customers.isNotEmpty()) {
        selectedCustomer = customers.first()
    }

    var currentReadingInput by remember { mutableStateOf("") }
    var anomalyCode by remember { mutableStateOf("NORMAL") }
    var isAnomalyDropdownOpen by remember { mutableStateOf(false) }

    var paymentMethod by remember { mutableStateOf("Cash") }
    var amountPaidInput by remember { mutableStateOf("") }

    val anomalyOptions = listOf(
        "NORMAL" to "Normal Operation",
        "BROKEN_DIAL" to "Broken / Cracked Dial",
        "LEAKAGE" to "Pipe / Fitting Leakage",
        "STALLED" to "Stalled / Jammed Mechanism",
        "GATE_LOCKED" to "Compound Gate Locked"
    )

    val currentReadingValue = currentReadingInput.toDoubleOrNull() ?: (selectedCustomer?.previousReading ?: 0.0)
    val pastArrears = selectedCustomer?.arrears ?: 0.0
    val amountPaidValue = amountPaidInput.toDoubleOrNull() ?: 0.0

    val billingResult = remember(selectedCustomer, currentReadingValue, amountPaidValue) {
        val prev = selectedCustomer?.previousReading ?: 0.0
        BillingEngine.calculateBill(
            previousReading = prev,
            currentReading = currentReadingValue,
            pastArrears = pastArrears,
            amountPaid = amountPaidValue
        )
    }

    val filteredCustomers = remember(searchQuery, customers) {
        if (searchQuery.isBlank()) customers.take(6)
        else customers.filter {
            it.fullName.contains(searchQuery, ignoreCase = true) ||
            it.meterNumber.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery, ignoreCase = true) ||
            it.zone.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("cashier_entry_screen")
    ) {
        // Fast Customer Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                isSearchActive = it.isNotBlank()
            },
            label = { Text("Search Customer by Name, Meter #, or Zone") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = ""; isSearchActive = false }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("customer_search_input"),
            shape = RoundedCornerShape(12.dp)
        )

        // Dropdown customer selector results
        if (isSearchActive && filteredCustomers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    filteredCustomers.forEach { cust ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCustomer = cust
                                    searchQuery = ""
                                    isSearchActive = false
                                    currentReadingInput = ""
                                    amountPaidInput = ""
                                    anomalyCode = "NORMAL"
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(cust.fullName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("${cust.meterNumber} • ${cust.zone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("Prev: ${cust.previousReading} m³", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Selected Customer Card
        selectedCustomer?.let { customer ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("selected_customer_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(customer.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("ID: ${customer.id} • ${customer.zone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (customer.arrears > 0) Color(0xFFFFCDD2) else Color(0xFFC8E6C9)
                        ) {
                            Text(
                                text = if (customer.arrears > 0) "Arrears: ${customer.arrears} ETB" else "No Arrears",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (customer.arrears > 0) Color(0xFFB71C1C) else Color(0xFF1B5E20),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Meter Serial Number", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(customer.meterNumber, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Last Reading & Date", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${customer.previousReading} m³ (${customer.previousReadingDate})", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } ?: run {
            Text("No customer selected. Please select a customer to enter reading.")
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Fast Numeric Reading Entry (NO CAMERA CONTROLS PER DESIGN POLICY)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NEW WATER METER READING",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "High-Speed Field Mode (Zero Photos)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = currentReadingInput,
                    onValueChange = {
                        // Allow only positive numbers / decimal
                        if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d*$"""))) {
                            currentReadingInput = it
                        }
                    },
                    label = { Text("Current Reading (m³)") },
                    placeholder = { Text("e.g. ${(selectedCustomer?.previousReading ?: 0.0) + 12.4}") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("current_reading_input"),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        Text(
                            text = "m³",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                )

                // Live Consumption Math & Warning
                val rawDelta = currentReadingValue - (selectedCustomer?.previousReading ?: 0.0)
                val isNegativeDelta = currentReadingInput.isNotEmpty() && rawDelta < 0.0

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Consumption: ΔU = Current - Previous",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isNegativeDelta) "${String.format(Locale.US, "%.2f", rawDelta)} m³ [INVERTED]" else "${String.format(Locale.US, "%.2f", billingResult.unitsConsumed)} m³",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isNegativeDelta) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                // Inverted / Meter Rollover Warning Banner
                AnimatedVisibility(visible = isNegativeDelta) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC62828))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Inverted / Meter Rollover Warning: Current dial reading is lower than previous reading. Anomaly classification mandated below.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFB71C1C),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Anomaly Classification Dropdown
                ExposedDropdownMenuBox(
                    expanded = isAnomalyDropdownOpen,
                    onExpandedChange = { isAnomalyDropdownOpen = it }
                ) {
                    OutlinedTextField(
                        value = anomalyOptions.firstOrNull { it.first == anomalyCode }?.second ?: anomalyCode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Anomaly Classification") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAnomalyDropdownOpen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("anomaly_code_dropdown"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isAnomalyDropdownOpen,
                        onDismissRequest = { isAnomalyDropdownOpen = false }
                    ) {
                        anomalyOptions.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(label, fontWeight = if (code == anomalyCode) FontWeight.Bold else FontWeight.Normal)
                                        if (code != "NORMAL") {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("[$code]", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                },
                                onClick = {
                                    anomalyCode = code
                                    isAnomalyDropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Tiered Tariff Calculation Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "BILLING BREAKDOWN & TIER FORMULA",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                billingResult.tierBreakdowns.forEach { item ->
                    if (item.units > 0 || item.tierLabel.startsWith("Tier 1")) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${item.tierLabel} (${String.format(Locale.US, "%.2f", item.units)} m³ @ ${item.rate} ETB)", fontSize = 12.sp)
                            Text(String.format(Locale.US, "%.2f ETB", item.subtotal), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Fixed Monthly Meter & Service Fee", fontSize = 12.sp)
                    Text(String.format(Locale.US, "%.2f ETB", billingResult.serviceFee), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                if (billingResult.pastArrears > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Past Unpaid Arrears", fontSize = 12.sp, color = Color(0xFFC62828))
                        Text(String.format(Locale.US, "%.2f ETB", billingResult.pastArrears), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TOTAL PAYABLE DUE:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = String.format(Locale.US, "%.2f ETB", billingResult.totalDue),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Payment Collection & Cashier Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "PAYMENT METHOD & COLLECTION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Payment channel chips
                val channels = listOf("Cash", "Telebirr", "CBE Birr", "Unpaid")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    channels.forEach { channel ->
                        FilterChip(
                            selected = paymentMethod == channel,
                            onClick = {
                                paymentMethod = channel
                                if (channel == "Unpaid") {
                                    amountPaidInput = "0.00"
                                } else if (amountPaidInput.isEmpty() || amountPaidInput == "0.00") {
                                    amountPaidInput = String.format(Locale.US, "%.2f", billingResult.totalDue)
                                }
                            },
                            label = { Text(channel) },
                            modifier = Modifier.testTag("payment_chip_$channel")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountPaidInput,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d*$"""))) {
                            amountPaidInput = it
                        }
                    },
                    label = { Text("Amount Paid by Customer (ETB)") },
                    placeholder = { Text(String.format(Locale.US, "%.2f", billingResult.totalDue)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("amount_paid_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Amount Fill Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { amountPaidInput = String.format(Locale.US, "%.2f", billingResult.totalDue) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Exact", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Button(
                        onClick = {
                            val roundedUp = (Math.ceil(billingResult.totalDue / 50.0) * 50.0)
                            amountPaidInput = String.format(Locale.US, "%.2f", roundedUp)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+50 Round", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Button(
                        onClick = {
                            val roundedUp = (Math.ceil(billingResult.totalDue / 100.0) * 100.0)
                            amountPaidInput = String.format(Locale.US, "%.2f", roundedUp)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+100 Round", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Change / Balance status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Remaining Balance:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format(Locale.US, "%.2f ETB", billingResult.balance),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (billingResult.balance > 0.05) Color(0xFFC62828) else Color(0xFF2E7D32)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (billingResult.paymentStatus) {
                            "PAID" -> Color(0xFFC8E6C9)
                            "PARTIAL" -> Color(0xFFFFF9C4)
                            else -> Color(0xFFFFCDD2)
                        }
                    ) {
                        Text(
                            text = billingResult.paymentStatus,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = when (billingResult.paymentStatus) {
                                "PAID" -> Color(0xFF1B5E20)
                                "PARTIAL" -> Color(0xFFF57F17)
                                else -> Color(0xFFB71C1C)
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main Submit Action
        Button(
            onClick = {
                val cust = selectedCustomer
                if (cust == null) {
                    Toast.makeText(context, "Please select a customer first", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val currentVal = currentReadingInput.toDoubleOrNull()
                if (currentVal == null) {
                    Toast.makeText(context, "Please enter a valid numeric current reading", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (currentVal < cust.previousReading && anomalyCode == "NORMAL") {
                    Toast.makeText(context, "Reading is inverted! Please classify the anomaly (e.g. Broken dial, Stalled)", Toast.LENGTH_LONG).show()
                    return@Button
                }

                val paidVal = amountPaidInput.toDoubleOrNull() ?: 0.0

                onRecordReading(
                    cust,
                    currentVal,
                    anomalyCode,
                    paymentMethod,
                    paidVal
                )

                // Reset entry fields for next customer
                currentReadingInput = ""
                amountPaidInput = ""
                anomalyCode = "NORMAL"
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("submit_meter_reading_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF006699)
            )
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Record Reading & Print Receipt",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
