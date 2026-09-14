<?php
/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | وب‌سرویس اصلی (api.php) — PHP 8.3 + SQL Server
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  اکشن‌ها:
 *    ping           → تست اتصال به دیتابیس
 *    catalog        → کاتالوگ کالا با موجودی زنده
 *    customers      → مشتریان + گروه‌ها (CUSTOMERS ⋈ CustGroup)
 *    sal_mali       → تاریخچه سال مالی مشتری برای تحلیل هوش مصنوعی
 *    submit_invoice → صدور فاکتور با BEGIN TRANSACTION روی
 *                     SalesHeader + SalesLines (درج اتمیک)
 * ═══════════════════════════════════════════════════════════════════════════
 */

declare(strict_types=1);

require_once __DIR__ . '/config.php';

// ── هدرهای امنیتی و CORS ─────────────────────────────────────────────────────
header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: DENY');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: Content-Type, X-Api-Key');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

// ── ابزار پاسخ‌دهی ──────────────────────────────────────────────────────────
function respond(bool $success, ?string $message = null, mixed $data = null, int $httpCode = 200): never
{
    http_response_code($httpCode);
    echo json_encode([
        'success' => $success,
        'message' => $message,
        'data'    => $data,
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

// ── احراز هویت با کلید API ──────────────────────────────────────────────────
$providedKey = $_SERVER['HTTP_X_API_KEY'] ?? '';
if (!hash_equals(API_KEY, $providedKey)) {
    respond(false, 'کلید API نامعتبر است', null, 401);
}

$action = $_GET['action'] ?? '';

try {
    match ($action) {
        'ping'           => action_ping(),
        'catalog'        => action_catalog(),
        'customers'      => action_customers(),
        'sal_mali'       => action_sal_mali(),
        'submit_invoice' => action_submit_invoice(),
        default          => respond(false, "اکشن ناشناخته: {$action}", null, 400),
    };
} catch (Throwable $e) {
    error_log('[Vizitor API] ' . $e->getMessage());
    respond(false, 'خطای داخلی سرور: ' . $e->getMessage(), null, 500);
}

// ═══════════════════════════ اکشن‌ها ═══════════════════════════════════════

/** تست اتصال به دیتابیس آتیران. */
function action_ping(): never
{
    $pdo = create_pdo();
    $version = $pdo->query('SELECT @@VERSION AS v')->fetch()['v'] ?? 'unknown';
    respond(true, 'اتصال برقرار است', ['db' => DB_NAME, 'version' => substr($version, 0, 60)]);
}

/** کاتالوگ کالا با موجودی زنده (Live Stock). */
function action_catalog(): never
{
    $pdo   = create_pdo();
    $since = (int) ($_GET['since'] ?? 0);

    $sql = "SELECT
                p.Id          AS id,
                p.Code        AS code,
                p.Name        AS name,
                ISNULL(p.GroupName, N'عمومی') AS group_name,
                CAST(p.Price AS BIGINT) AS price,
                CAST(ISNULL(p.Stock, 0) AS FLOAT) AS stock,
                CAST(ISNULL(p.IsVip, 0) AS BIT) AS is_vip,
                ISNULL(p.UnitName, N'کیلو') AS unit,
                ISNULL(p.PackSize, 1) AS pack_size,
                CAST(ISNULL(p.Price2, p.Price) AS BIGINT) AS price2
            FROM " . TBL_PRODUCTS . " p
            WHERE p.IsActive = 1 AND p.UpdatedAt >= :since
            ORDER BY p.Name";

    $stmt = $pdo->prepare($sql);
    $stmt->execute([':since' => date('Y-m-d H:i:s', $since > 0 ? $since : 946684800)]);
    respond(true, null, $stmt->fetchAll());
}

/**
 * لیست مشتریان — کوئری بهینه با ایندکس روی جداول
 * CUSTOMERS (ایندکس پیشنهادی: IX_CUSTOMERS_City، IX_CUSTOMERS_GroupId)
 * و گروه‌های مشتری از جدول CustGroup.
 */
function action_customers(): never
{
    $pdo   = create_pdo();
    $since = (int) ($_GET['since'] ?? 0);

    $sql = "SELECT
                c.Id            AS id,
                c.Code          AS code,
                c.Name          AS name,
                ISNULL(g.GroupName, N'عمومی') AS group_name,
                ISNULL(c.City, N'')    AS city,
                ISNULL(c.Address, N'') AS address,
                ISNULL(c.Phone, N'')   AS phone,
                ISNULL(c.Lat, 0)       AS lat,
                ISNULL(c.Lng, 0)       AS lng,
                CAST(ISNULL(c.CreditOk, 1) AS BIT) AS credit_ok,
                CAST(ISNULL(c.IsVip, 0) AS BIT)    AS is_vip,
                CAST(ISNULL(c.Balance, 0) AS BIGINT) AS debt,
                ISNULL(DATEDIFF(DAY, c.LastPurchaseDate, GETDATE()), 999) AS last_purchase_days,
                -- درصد افت خرید: مقایسه میانگین ۹۰ روز اخیر با ۹۰ روز پیش از آن
                ISNULL(CAST(
                    CASE WHEN ISNULL(prev.TotalSales, 0) > 0
                         THEN (prev.TotalSales - recent.TotalSales) * 100.0 / prev.TotalSales
                         ELSE 0 END AS INT), 0) AS drop_percent
            FROM " . TBL_CUSTOMERS . " c WITH (NOLOCK)
            LEFT JOIN " . TBL_CUST_GROUP . " g WITH (NOLOCK)
                   ON g.Id = c.GroupId
            OUTER APPLY (
                SELECT ISNULL(SUM(h.FinalAmount), 0) AS TotalSales
                FROM " . TBL_SALES_HEADER . " h WITH (NOLOCK)
                WHERE h.CustomerId = c.Id
                  AND h.CreatedAt >= DATEADD(DAY, -90, GETDATE())
            ) recent
            OUTER APPLY (
                SELECT ISNULL(SUM(h.FinalAmount), 0) AS TotalSales
                FROM " . TBL_SALES_HEADER . " h WITH (NOLOCK)
                WHERE h.CustomerId = c.Id
                  AND h.CreatedAt >= DATEADD(DAY, -180, GETDATE())
                  AND h.CreatedAt <  DATEADD(DAY, -90,  GETDATE())
            ) prev
            WHERE c.IsActive = 1 AND c.UpdatedAt >= :since
            ORDER BY c.Name
            OPTION (RECOMPILE)";

    $stmt = $pdo->prepare($sql);
    $stmt->execute([':since' => date('Y-m-d H:i:s', $since > 0 ? $since : 946684800)]);
    respond(true, null, $stmt->fetchAll());
}

/** تاریخچه سال مالی مشتری — خوراک تحلیل دستیار هوش مصنوعی. */
function action_sal_mali(): never
{
    $pdo        = create_pdo();
    $customerId = (int) ($_GET['customer_id'] ?? 0);

    $sql = "SELECT TOP 50
                s.CustomerId   AS customer_id,
                s.ProductName  AS product_name,
                CAST(SUM(s.Qty) AS FLOAT) AS total_qty,
                FORMAT(s.SaleDate, 'yyyy-MM') AS year_month
            FROM " . TBL_SAL_MALI . " s WITH (NOLOCK)
            WHERE s.CustomerId = :cid
              AND s.SaleDate >= DATEADD(MONTH, -12, GETDATE())
            GROUP BY s.CustomerId, s.ProductName, FORMAT(s.SaleDate, 'yyyy-MM')
            ORDER BY total_qty DESC";

    $stmt = $pdo->prepare($sql);
    $stmt->execute([':cid' => $customerId]);
    respond(true, null, $stmt->fetchAll());
}

/**
 * صدور فاکتور — تراکنش اتمیک:
 *   BEGIN TRANSACTION
 *     INSERT SalesHeader → SCOPE_IDENTITY()
 *     INSERT SalesLines (حلقه روی اقلام)
 *     کاهش موجودی کالاها
 *   COMMIT / ROLLBACK
 */
function action_submit_invoice(): never
{
    $raw = file_get_contents('php://input');
    $body = json_decode($raw ?: 'null', true);

    if (!is_array($body)) {
        respond(false, 'بدنه درخواست JSON معتبر نیست', null, 400);
    }

    $customerId = (int)    ($body['customer_id']  ?? 0);
    $gross      = (int)    ($body['gross_amount'] ?? 0);
    $discount   = (int)    ($body['discount']     ?? 0);
    $final      = (int)    ($body['final_amount'] ?? 0);
    $signature  = isset($body['signature']) ? (string) $body['signature'] : null;
    $note       = isset($body['note']) ? mb_substr((string) $body['note'], 0, 500) : '';
    $items      = $body['items'] ?? [];

    if ($customerId <= 0)       respond(false, 'مشتری معتبر نیست', null, 422);
    if (!is_array($items) || count($items) === 0) respond(false, 'اقلام فاکتور خالی است', null, 422);
    if ($final < 0 || $gross < 0 || $discount < 0) respond(false, 'مبالغ نامعتبر هستند', null, 422);
    if ($gross - $discount !== $final) respond(false, 'مبلغ نهایی با جمع و کسورات همخوانی ندارد', null, 422);

    $pdo = create_pdo();

    // کنترل اعتبار مشتری پیش از صدور
    $check = $pdo->prepare(
        'SELECT CreditOk FROM ' . TBL_CUSTOMERS . ' WHERE Id = :cid AND IsActive = 1'
    );
    $check->execute([':cid' => $customerId]);
    $creditOk = $check->fetchColumn();
    if ($creditOk === false) respond(false, 'مشتری یافت نشد یا غیرفعال است', null, 404);
    if ((int) $creditOk === 0) respond(false, 'مشتری به دلیل بدهی مسدود است (وضعیت اعتباری قرمز)', null, 403);

    // اعتبارسنجی اقلام
    $lines = [];
    foreach ($items as $line) {
        $productId = (int)   ($line['product_id'] ?? 0);
        $qty       = (float) ($line['quantity']   ?? 0);
        $unitPrice = (int)   ($line['unit_price'] ?? 0);
        $lineTotal = (int)   ($line['line_total'] ?? 0);
        if ($productId <= 0 || $qty <= 0 || $unitPrice < 0) {
            respond(false, 'قلم فاکتور نامعتبر است', ['item' => $line], 422);
        }
        $lines[] = [$productId, $qty, $unitPrice, $lineTotal];
    }

    $pdo->exec('SET XACT_ABORT ON');
    $pdo->beginTransaction();

    try {
        // ── ۱) درج هدر فاکتور ───────────────────────────────────────────
        $headerSql = 'INSERT INTO ' . TBL_SALES_HEADER . '
                (CustomerId, GrossAmount, Discount, FinalAmount, SalesmanCode,
                 SignatureImg, Note, Status, CreatedAt)
            VALUES
                (:cid, :gross, :discount, :final, :salesman,
                 :sig, :note, N\'PENDING\', GETDATE());
            SELECT CAST(SCOPE_IDENTITY() AS BIGINT) AS new_id';

        $headerStmt = $pdo->prepare($headerSql);
        $headerStmt->execute([
            ':cid'      => $customerId,
            ':gross'    => $gross,
            ':discount' => $discount,
            ':final'    => $final,
            ':salesman' => SALESMAN_CODE,
            ':sig'      => $signature !== null && $signature !== '' ? base64_decode($signature, true) ?: null : null,
            ':note'     => $note,
        ]);
        $invoiceId = (int) $headerStmt->fetch()['new_id'];

        // شماره فاکتور یکتای قابل خواندن برای اپ
        $invoiceNo = 'INV-' . date('Ym') . '-' . str_pad((string) $invoiceId, 6, '0', STR_PAD_LEFT);

        // ── ۲) درج اقلام فاکتور + کاهش موجودی ──────────────────────────
        $lineStmt = $pdo->prepare(
            'INSERT INTO ' . TBL_SALES_LINES . '
                (InvoiceId, ProductId, Qty, UnitPrice, LineTotal)
             VALUES (:iid, :pid, :qty, :price, :total)'
        );
        $stockStmt = $pdo->prepare(
            'UPDATE ' . TBL_PRODUCTS . ' SET Stock = Stock - :qty WHERE Id = :pid'
        );

        foreach ($lines as [$productId, $qty, $unitPrice, $lineTotal]) {
            $lineStmt->execute([
                ':iid'   => $invoiceId,
                ':pid'   => $productId,
                ':qty'   => $qty,
                ':price' => $unitPrice,
                ':total' => $lineTotal,
            ]);
            $stockStmt->execute([':qty' => $qty, ':pid' => $productId]);
        }

        // ── ۳) به‌روزرسانی تاریخ آخرین خرید مشتری ──────────────────────
        $pdo->prepare('UPDATE ' . TBL_CUSTOMERS . ' SET LastPurchaseDate = GETDATE() WHERE Id = :cid')
            ->execute([':cid' => $customerId]);

        $pdo->commit();

        respond(true, 'فاکتور با موفقیت در دیتابیس آتیران ثبت شد', [
            'invoice_no'  => $invoiceNo,
            'server_time' => time(),
        ]);
    } catch (Throwable $e) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        throw $e;
    }
}
