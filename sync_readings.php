<?php
/**
 * Adi NebriEd Water Supply Authority (ዓዲ ነብሪ ኢድ ማይ ቀረብን ክፍሊትን)
 * Production LAN Synchronization Handler (sync_readings.php)
 *
 * Receives batch JSON meter reading payloads from Android mobile units.
 * Executes offline ACID synchronization via MySQL/SQLite PDO transactions.
 */

header('Content-Type: application/json; charset=utf-8');

// Configuration
$dbHost = getenv('DB_HOST') ?: '127.0.0.1';
$dbPort = getenv('DB_PORT') ?: '3306';
$dbName = getenv('DB_NAME') ?: 'adi_nebried_water';
$dbUser = getenv('DB_USER') ?: 'water_user';
$dbPass = getenv('DB_PASS') ?: 'AdiNebriEd@2026_SecureDb';

// Handle preflight CORS if testing from web
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['status' => 'error', 'message' => 'Method Not Allowed. POST required.']);
    exit;
}

// Read and parse JSON payload
$rawInput = file_get_contents('php://input');
if (empty($rawInput)) {
    http_response_code(400);
    echo json_encode(['status' => 'error', 'message' => 'Empty request payload']);
    exit;
}

$payload = json_decode($rawInput, true);
if (json_last_error() !== JSON_ERROR_NONE || !isset($payload['readings']) || !is_array($payload['readings'])) {
    http_response_code(400);
    echo json_encode(['status' => 'error', 'message' => 'Invalid JSON structure or missing readings array']);
    exit;
}

$station = $payload['station'] ?? 'Adi NebriEd';
$readings = $payload['readings'];
$confirmedReadingIds = [];

try {
    // Connect via PDO (fallback to local SQLite if MySQL isn't configured in test environment)
    $dsn = "mysql:host=$dbHost;port=$dbPort;dbname=$dbName;charset=utf8mb4";
    $options = [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES   => false,
    ];

    try {
        $pdo = new PDO($dsn, $dbUser, $dbPass, $options);
    } catch (PDOException $e) {
        // Fallback to local SQLite file for offline staging server
        $sqlitePath = __DIR__ . '/adi_nebried_server_staging.db';
        $pdo = new PDO("sqlite:" . $sqlitePath);
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

        // Ensure tables exist in SQLite staging
        $pdo->exec("CREATE TABLE IF NOT EXISTS server_meter_readings (
            reading_id TEXT PRIMARY KEY,
            customer_id TEXT,
            meter_number TEXT,
            previous_reading REAL,
            current_reading REAL,
            units_consumed REAL,
            water_fee REAL,
            service_fee REAL,
            arrears REAL,
            total_payable REAL,
            amount_paid REAL,
            balance REAL,
            payment_status TEXT,
            payment_method TEXT,
            receipt_number TEXT,
            anomaly_code TEXT,
            reading_timestamp TEXT,
            cashier_agent_id TEXT,
            server_received_at TEXT
        );");

        $pdo->exec("CREATE TABLE IF NOT EXISTS server_customers (
            customer_id TEXT PRIMARY KEY,
            last_reading REAL,
            current_balance REAL,
            updated_at TEXT
        );");
    }

    // ACID Transaction Begin
    $pdo->beginTransaction();

    $stmtReading = $pdo->prepare("
        INSERT OR REPLACE INTO server_meter_readings (
            reading_id, customer_id, meter_number, previous_reading, current_reading,
            units_consumed, water_fee, service_fee, arrears, total_payable,
            amount_paid, balance, payment_status, payment_method, receipt_number,
            anomaly_code, reading_timestamp, cashier_agent_id, server_received_at
        ) VALUES (
            :reading_id, :customer_id, :meter_number, :previous_reading, :current_reading,
            :units_consumed, :water_fee, :service_fee, :arrears, :total_payable,
            :amount_paid, :balance, :payment_status, :payment_method, :receipt_number,
            :anomaly_code, :reading_timestamp, :cashier_agent_id, datetime('now')
        )
    ");

    $stmtCust = $pdo->prepare("
        INSERT INTO server_customers (customer_id, last_reading, current_balance, updated_at)
        VALUES (:customer_id, :current_reading, :balance, datetime('now'))
        ON CONFLICT(customer_id) DO UPDATE SET
            last_reading = excluded.last_reading,
            current_balance = excluded.current_balance,
            updated_at = excluded.updated_at
    ");

    foreach ($readings as $r) {
        $readingId = $r['reading_id'] ?? null;
        $customerId = $r['customer_id'] ?? null;

        if (!$readingId || !$customerId) {
            continue;
        }

        $stmtReading->execute([
            ':reading_id'         => $readingId,
            ':customer_id'        => $customerId,
            ':meter_number'       => $r['meter_number'] ?? '',
            ':previous_reading'   => $r['previous_reading'] ?? 0.0,
            ':current_reading'    => $r['current_reading'] ?? 0.0,
            ':units_consumed'     => $r['units_consumed'] ?? 0.0,
            ':water_fee'          => $r['water_fee'] ?? 0.0,
            ':service_fee'        => $r['service_fee'] ?? 35.0,
            ':arrears'            => $r['arrears'] ?? 0.0,
            ':total_payable'      => $r['total_payable'] ?? 0.0,
            ':amount_paid'        => $r['amount_paid'] ?? 0.0,
            ':balance'            => $r['balance'] ?? 0.0,
            ':payment_status'     => $r['payment_status'] ?? 'UNPAID',
            ':payment_method'     => $r['payment_method'] ?? 'Cash',
            ':receipt_number'     => $r['receipt_number'] ?? '',
            ':anomaly_code'       => $r['anomaly_code'] ?? 'NORMAL',
            ':reading_timestamp'  => $r['reading_timestamp'] ?? date('Y-m-d H:i:s'),
            ':cashier_agent_id'   => $r['cashier_agent_id'] ?? 'AGT-042',
        ]);

        $stmtCust->execute([
            ':customer_id'     => $customerId,
            ':current_reading' => $r['current_reading'] ?? 0.0,
            ':balance'         => $r['balance'] ?? 0.0
        ]);

        $confirmedReadingIds[] = $readingId;
    }

    $pdo->commit();

    echo json_encode([
        'status' => 'success',
        'station' => $station,
        'received_count' => count($readings),
        'confirmed_count' => count($confirmedReadingIds),
        'confirmed_reading_ids' => $confirmedReadingIds,
        'server_timestamp' => time(),
        'message' => 'Batch readings successfully synchronized to Adi NebriEd central ledger.'
    ]);

} catch (Exception $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    http_response_code(500);
    echo json_encode([
        'status' => 'error',
        'message' => 'Database Sync Failed: ' . $e->getMessage()
    ]);
}
