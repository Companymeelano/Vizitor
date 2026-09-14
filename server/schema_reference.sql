/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | مرجع ساختار جداول (schema_reference.sql)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  این اسکریپت صرفاً مرجع ستون‌های مورد انتظار وب‌سرویس است.
 *  اگر دیتابیس حسابداری آتیران این جداول را از قبل دارد، تغییری ندهید و
 *  فقط نام جداول/ستون‌ها را در server/config.php و server/api.php تطبیق دهید.
 * ═══════════════════════════════════════════════════════════════════════════
 */

-- ایندکسهای پیشنهادی برای کوئری‌های بهینه (در صورت نبود):
-- CREATE INDEX IX_CUSTOMERS_City   ON dbo.CUSTOMERS (City)    INCLUDE (GroupId, IsActive);
-- CREATE INDEX IX_CUSTOMERS_Group  ON dbo.CUSTOMERS (GroupId) INCLUDE (Name);
-- CREATE INDEX IX_SALESHEADER_CUST ON dbo.SalesHeader (CustomerId, CreatedAt) INCLUDE (FinalAmount);

/* ساختار مرجع در صورت نیاز به ایجاد جداول جدید:

CREATE TABLE dbo.Products (
    Id        INT IDENTITY(1,1) PRIMARY KEY,
    Code      NVARCHAR(40)  NOT NULL,       -- بارکد
    Name      NVARCHAR(200) NOT NULL,
    GroupName NVARCHAR(100) NULL,
    Price     DECIMAL(18,0) NOT NULL DEFAULT 0,
    Stock     FLOAT         NOT NULL DEFAULT 0,
    IsVip     BIT           NOT NULL DEFAULT 0,
    IsActive  BIT           NOT NULL DEFAULT 1,
    UpdatedAt DATETIME2     NOT NULL DEFAULT GETDATE()
);

CREATE TABLE dbo.CustGroup (
    Id        INT IDENTITY(1,1) PRIMARY KEY,
    GroupName NVARCHAR(100) NOT NULL
);

CREATE TABLE dbo.CUSTOMERS (
    Id               INT IDENTITY(1,1) PRIMARY KEY,
    Code             NVARCHAR(40)  NOT NULL,
    Name             NVARCHAR(200) NOT NULL,
    GroupId          INT NULL REFERENCES dbo.CustGroup(Id),
    City             NVARCHAR(100) NULL,
    Address          NVARCHAR(400) NULL,
    Phone            NVARCHAR(30)  NULL,
    Lat              FLOAT NULL DEFAULT 0,
    Lng              FLOAT NULL DEFAULT 0,
    CreditOk         BIT   NOT NULL DEFAULT 1,
    IsVip            BIT   NOT NULL DEFAULT 0,
    IsActive         BIT   NOT NULL DEFAULT 1,
    LastPurchaseDate DATETIME2 NULL,
    UpdatedAt        DATETIME2 NOT NULL DEFAULT GETDATE()
);

CREATE TABLE dbo.SalesHeader (
    Id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    CustomerId   INT NOT NULL REFERENCES dbo.CUSTOMERS(Id),
    GrossAmount  BIGINT NOT NULL,
    Discount     BIGINT NOT NULL DEFAULT 0,
    FinalAmount  BIGINT NOT NULL,
    SalesmanCode NVARCHAR(20) NOT NULL,
    SignatureImg VARBINARY(MAX) NULL,
    Status       NVARCHAR(20) NOT NULL DEFAULT N'PENDING',
    CreatedAt    DATETIME2 NOT NULL DEFAULT GETDATE()
);

CREATE TABLE dbo.SalesLines (
    Id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    InvoiceId  BIGINT NOT NULL REFERENCES dbo.SalesHeader(Id),
    ProductId  INT NOT NULL REFERENCES dbo.Products(Id),
    Qty        FLOAT NOT NULL,
    UnitPrice  BIGINT NOT NULL,
    LineTotal  BIGINT NOT NULL
);

CREATE TABLE dbo.sal_mali (
    Id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    CustomerId  INT NOT NULL,
    ProductName NVARCHAR(200) NOT NULL,
    Qty         FLOAT NOT NULL,
    SaleDate    DATETIME2 NOT NULL
);
*/
