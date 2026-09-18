/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | منبع دادهٔ مستقیم SQL Server (دیتابیس Meelano)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  معماری:  UI (Compose) → ViewModel → Repository → DataSource (اینجا) →
 *           SqlConnectionManager → SQL Server 192.168.1.150:1433 → Meelano
 *
 *  ⚠️ قانون قطعی این فایل: هر نام جدول/ستون در کوئری‌ها از خروجی ممیزی
 *  واقعی سرور گرفته شده و در docs/schema/meelano-columns.tsv ثبت است.
 *  هیچ نامی حدس زده نشده؛ ابزار tools/check_sql_columns.py همین را چک می‌کند.
 *
 *  همهٔ توابع suspend هستند و از withConnection استفاده می‌کنند → هیچ کوئری‌ای
 *  روی ترد UI اجرا نمی‌شود و اتصال همیشه به pool برمی‌گردد (بدون leak).
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.data.sql

import ir.atiran.vizitor.data.repository.ServerConfig
import java.sql.ResultSet

// ─────────────────────────────────────────────────────────────────────────────
//  مدل‌های خروجی (سطر خام دیتابیس؛ مپ به Room در Repository انجام می‌شود)
// ─────────────────────────────────────────────────────────────────────────────

/** یک ردیف کالا با هر ۵ سطح قیمت Atiran و موجودی. */
data class DbProduct(
    val shka: Long,
    val name: String,
    val code: String,
    val groupRdf: Int,
    val unit: String,
    val stockVah: Double,
    val stockJoz: Int,
    val pieceWeight: Double,
    val packSize: Double,
    val expirationDate: String?,
    val priceTier1: Long,
    val priceTier2: Long,
    val priceTier3: Long,
    val priceTier4: Long,
    val priceTier5: Long,
    val minPrice: Long,
    val maxPrice: Long,
)

/** یک ردیف مشتری (بر اساس فیلترهای verified: sys_cus و CUSTOMERS). */
data class DbCustomer(
    val shmo: Int,
    val name: String,
    val code: String,
    val groupRdf: Int,
    val groupName: String?,
    val cityRdf: Int?,
    val address: String,
    val phone: String,
    val credit: Long,
    val debt: Long,
    val blackList: Int?,
    val active: String,
    val lat: Double?,
    val lng: Double?,
    val visitorRdf: Int,
)

/** گروه مشتری + سطح قیمت واقعی (custgroup.price = ستون تیر قیمت). */
data class DbCustomerGroup(
    val groupRdf: Int,
    val name: String,
    val priceTier: Int?,
)

/** موجودی یک کالا در یک انبار. */
data class DbStockRow(
    val shka: Long,
    val warehouseRdf: Int,
    val warehouseName: String,
    val quantity: Double,
    val quantityPiece: Int,
)

/** هویت ویزیتور: از visitors (کلید کسب‌وکار) و sys_users (کلید ورود). */
data class DbVisitorIdentity(
    val userId: Int,
    val username: String,
    val displayName: String,
    val visitorRdf: Int?,
    val allowedCustomers: Int,
    val allowedProducts: Int,
    val allowedWarehouses: Int,
)

/** ردیف خام جدول sys_users برای ورود (بدون هیچ ستون رمزی). */
data class DbLoginRow(
    val userId: Int,
    val username: String,
    val fullName: String,
    val roleId: Int?,
    val active: Boolean,
    val locked: Boolean,
    val companyId: Int,
)

/**
 * منبع دادهٔ فقط‌خواندنی روی دیتابیس واقعی Meelano.
 * لایهٔ نوشتن (پیش‌فاکتور) بعد از تأیید بدنهٔ stored procedureها اضافه می‌شود.
 */
class MeelanoDataSource(private val db: SqlConnectionManager) {

    // ── ورود: اعتبارسنجی رمز سمت SQL Server (بدون هش‌سازی در اپ) ─────────────
    //  user_password از نوع varbinary است، پس مقایسه فقط با PWDCOMPARE ممکن است.
    //  کوئری پارامتری است → هیچ رشته‌ای داخل SQL تزریق نمی‌شود.
    suspend fun login(username: String, password: String): DbLoginRow? =
        db.withConnection { c ->
            c.prepareStatement(
                """
                SELECT TOP (1)
                       sys_users.user_id,
                       sys_users.user_name,
                       sys_users.user_fname,
                       sys_users.user_lname,
                       sys_users.role_id,
                       sys_users.active,
                       sys_users.IsLocked,
                       sys_users.shmo
                  FROM dbo.sys_users
                 WHERE sys_users.user_name = ?
                   AND PWDCOMPARE(?, sys_users.user_password) = 1
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 15
                ps.setString(1, username)
                ps.setString(2, password)          // رمز فقط در همین نقطه مصرف می‌شود
                ps.executeQuery().use { rs ->
                    if (!rs.next()) null else DbLoginRow(
                        userId = rs.getInt("user_id"),
                        username = rs.getString("user_name") ?: username,
                        fullName = listOfNotNull(
                            rs.getString("user_fname"), rs.getString("user_lname")
                        ).joinToString(" "),
                        roleId = rs.nullableInt("role_id"),
                        active = rs.getBoolean("active"),
                        locked = rs.getBoolean("IsLocked"),
                        companyId = rs.getInt("shmo"),
                    )
                }
            }
        }

    // ── کاتالوگ کالا + ۵ سطح قیمت + موجودی کل ────────────────────────────────
    //  برای ویزیتور غیرحرفه‌ای اپ فقط به قیمت‌های نقدی (forosh*) نیاز دارد؛
    //  ستون‌های mp*/pv* (تعداد/درصد) در فاز بعد در صورت نیاز استفاده می‌شوند.
    suspend fun products(limit: Int = 500, offset: Int = 0, search: String? = null): List<DbProduct> =
        db.withConnection { c ->
            val like = search?.let { "%$it%" }
            val sql = buildString {
                append(
                    """
                    SELECT inventory.shka, inventory.naka, inventory.coka, inventory.group_rdf,
                           inventory.vahsanj, inventory.mojkavah, inventory.mojkajoz,
                           inventory.vahsp, inventory.tedbastebandi, inventory.ExpirationDate,
                           forosh_price.forosh1, forosh_price.forosh2, forosh_price.forosh3,
                           forosh_price.forosh4, forosh_price.forosh5,
                           forosh_price.MinPrice, forosh_price.MaxPrice
                      FROM dbo.inventory
                      LEFT JOIN dbo.forosh_price ON forosh_price.shka = inventory.shka
                     WHERE inventory.active = ?
                    """.trimIndent()
                )
                if (like != null) append("\n   AND (inventory.naka LIKE ? OR inventory.coka LIKE ?)")
                append("\n     ORDER BY inventory.naka\n     OFFSET ? ROWS FETCH NEXT ? ROWS ONLY")
            }
            c.prepareStatement(sql).use { ps ->
                ps.queryTimeout = 30
                ps.setString(1, ACTIVE_FLAG)
                var i = 2
                if (like != null) { ps.setString(i++, like); ps.setString(i++, like) }
                ps.setInt(i++, offset)
                ps.setInt(i, limit)
                ps.executeQuery().use { rs -> rs.mapRows(::readProduct) }
            }
        }

    /**
     * قیمت یک کالا برای یک سطح (۱..۵) + بررسی بازهٔ مجاز Atiran.
     * `custgroup.price` تیر مشتری است؛ قیمت از همان ستون forosh<n> خوانده می‌شود.
     * هیچ قیمت پیش‌فرض/ساختگی‌ای اینجا وجود ندارد: اگر ردیف قیمت نباشد → null.
     */
    suspend fun priceFor(shka: Long, tier: Int): Long? {
        val column = when (tier.coerceIn(1, 5)) {
            1 -> "forosh1"; 2 -> "forosh2"; 3 -> "forosh3"; 4 -> "forosh4"; else -> "forosh5"
        }
        return db.withConnection { c ->
            c.prepareStatement(
                """
                SELECT forosh_price.$column
                  FROM dbo.forosh_price
                 WHERE forosh_price.shka = ?
                   AND forosh_price.active = ?
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 15
                ps.setLong(1, shka)
                ps.setString(2, ACTIVE_FLAG)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getLong(1) else null }
            }
        }
    }

    // ── موجودی انبارها ──────────────────────────────────────────────────────
    suspend fun stock(shka: Long? = null): List<DbStockRow> =
        db.withConnection { c ->
            val sql = buildString {
                append(
                    """
                    SELECT inventory_anbars.shka, inventory_anbars.rdf_anbars,
                           inventory_anbars.name, inventory_anbars.mojkavah,
                           inventory_anbars.mojkajoz
                      FROM dbo.inventory_anbars
                     WHERE (? = 0 OR inventory_anbars.shka = ?)
                     ORDER BY inventory_anbars.name
                    """.trimIndent()
                )
            }
            c.prepareStatement(sql).use { ps ->
                ps.queryTimeout = 30
                ps.setLong(1, shka ?: 0L)
                ps.setLong(2, shka ?: 0L)
                ps.executeQuery().use { rs ->
                    rs.mapRows {
                        DbStockRow(
                            shka = it.getLong("shka"),
                            warehouseRdf = it.getInt("rdf_anbars"),
                            warehouseName = it.getString("name") ?: "",
                            quantity = it.getDouble("mojkavah"),
                            quantityPiece = it.getInt("mojkajoz"),
                        )
                    }
                }
            }
        }

    // ── مشتریان مجاز یک ویزیتور (فیلتر واقعی جدول sys_cus) ───────────────────
    //  sys_cus: SysID + Shmo + UserID  → شرکت، مشتری مجاز، کاربر
    suspend fun customersFor(userId: Int, companyId: Int?, limit: Int = 500, offset: Int = 0): List<DbCustomer> =
        db.withConnection { c ->
            c.prepareStatement(
                """
                SELECT CUSTOMERS.SHMO, CUSTOMERS.MONAME, CUSTOMERS.code, CUSTOMERS.group_rdf,
                       custgroup.group_name, CUSTOMERS.rdf_city, CUSTOMERS.addre,
                       CUSTOMERS.cell, CUSTOMERS.cred, CUSTOMERS.man, CUSTOMERS.black_list,
                       CUSTOMERS.active, CUSTOMERS.Lat, CUSTOMERS.Lng, CUSTOMERS.vis_rdf
                  FROM dbo.sys_cus
                  JOIN dbo.CUSTOMERS ON CUSTOMERS.SHMO = sys_cus.Shmo
                  LEFT JOIN dbo.custgroup ON custgroup.group_rdf = CUSTOMERS.group_rdf
                 WHERE sys_cus.UserID = ?
                   AND (? = 0 OR sys_cus.SysID = ?)
                 ORDER BY CUSTOMERS.MONAME
                 OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 30
                ps.setInt(1, userId)
                ps.setInt(2, companyId ?: 0)
                ps.setInt(3, companyId ?: 0)
                ps.setInt(4, offset)
                ps.setInt(5, limit)
                ps.executeQuery().use { rs -> rs.mapRows(::readCustomer) }
            }
        }

    /** گروه‌های مشتری + تیر قیمت (پایهٔ کل منطق قیمت Atiran). */
    suspend fun customerGroups(): List<DbCustomerGroup> =
        db.withConnection { c ->
            c.prepareStatement(
                """
                SELECT custgroup.group_rdf, custgroup.group_name, custgroup.price
                  FROM dbo.custgroup
                 WHERE custgroup.Active = 1
                 ORDER BY custgroup.group_name
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 20
                ps.executeQuery().use { rs ->
                    rs.mapRows {
                        DbCustomerGroup(
                            groupRdf = it.getInt("group_rdf"),
                            name = it.getString("group_name") ?: "",
                            priceTier = it.nullableInt("price"),
                        )
                    }
                }
            }
        }

    // ── هویت ویزیتور + دامنهٔ دسترسی (sys_vis / sys_cus / sys_kal / sys_anb) ──
    suspend fun visitorIdentity(userId: Int): DbVisitorIdentity? =
        db.withConnection { c ->
            c.prepareStatement(
                """
                SELECT visitors.vis_rdf, visitors.vis_name, visitors.UserID,
                       (SELECT COUNT(*) FROM dbo.sys_cus WHERE sys_cus.UserID = ?) AS allowed_customers,
                       (SELECT COUNT(*) FROM dbo.sys_vis WHERE sys_vis.UserID = ?) AS allowed_visitors,
                       (SELECT COUNT(*) FROM dbo.sys_anb WHERE sys_anb.UserID = ?) AS allowed_warehouses
                  FROM dbo.visitors
                 WHERE (visitors.UserID = ? OR ? = 0)
                 ORDER BY visitors.vis_rdf
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 20
                ps.setInt(1, userId); ps.setInt(2, userId); ps.setInt(3, userId)
                ps.setInt(4, userId); ps.setInt(5, userId)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) null else DbVisitorIdentity(
                        userId = userId,
                        username = "",
                        displayName = rs.getString("vis_name") ?: "",
                        visitorRdf = rs.nullableInt("vis_rdf"),
                        allowedCustomers = rs.getInt("allowed_customers"),
                        allowedProducts = 0,        // sys_kal ستون‌هایش هنوز verify نشده
                        allowedWarehouses = rs.getInt("allowed_warehouses"),
                    )
                }
            }
        }

    // ── سلامت/متادیتا: نسخهٔ دیتابیس برای نمایش در تنظیمات ───────────────────
    suspend fun databaseInfo(): Triple<String, String, String> =
        db.withConnection { c ->
            c.prepareStatement(
                """
                SELECT DB_NAME() AS db_name,
                       CAST(SERVERPROPERTY('ProductVersion') AS NVARCHAR(64)) AS version,
                       (SELECT COUNT(*) FROM dbo.CUSTOMERS) AS customers
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 15
                ps.executeQuery().use { rs ->
                    if (!rs.next()) Triple("", "", "") else Triple(
                        rs.getString("db_name") ?: "",
                        rs.getString("version") ?: "",
                        rs.getString("customers") ?: "0",
                    )
                }
            }
        }

    // ─────────────────────────────────────────────────────────────────────────
    private companion object {
        /**
         * مقدار «فعال» در ستون‌های char(1) این دیتابیس.
         * ستون‌ها char(1) هستند؛ مقدار واقعی‌شان با اسکریپت 05 (بخش G3) تأیید
         * می‌شود و در صورت تفاوت فقط همین یک ثابت عوض می‌شود.
         */
        const val ACTIVE_FLAG = "1"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  کمک‌تابع‌های ResultSet (مستقل از هر ORM)
// ─────────────────────────────────────────────────────────────────────────────
private inline fun <T> ResultSet.mapRows(read: (ResultSet) -> T): List<T> {
    val out = ArrayList<T>()
    while (next()) out.add(read(this))
    return out
}

private fun ResultSet.nullableInt(column: String): Int? {
    val v = getInt(column)
    return if (wasNull()) null else v
}

private fun readProduct(rs: ResultSet) = DbProduct(
    shka = rs.getLong("shka"),
    name = rs.getString("naka") ?: "",
    code = rs.getString("coka") ?: "",
    groupRdf = rs.getInt("group_rdf"),
    unit = rs.getString("vahsanj") ?: "",
    stockVah = rs.getDouble("mojkavah"),
    stockJoz = rs.getInt("mojkajoz"),
    pieceWeight = rs.getDouble("vahsp"),
    packSize = rs.getDouble("tedbastebandi"),
    expirationDate = rs.getString("ExpirationDate"),
    priceTier1 = rs.getLong("forosh1"),
    priceTier2 = rs.getLong("forosh2"),
    priceTier3 = rs.getLong("forosh3"),
    priceTier4 = rs.getLong("forosh4"),
    priceTier5 = rs.getLong("forosh5"),
    minPrice = rs.getLong("MinPrice"),
    maxPrice = rs.getLong("MaxPrice"),
)

private fun readCustomer(rs: ResultSet) = DbCustomer(
    shmo = rs.getInt("SHMO"),
    name = rs.getString("MONAME") ?: "",
    code = rs.getString("code") ?: "",
    groupRdf = rs.getInt("group_rdf"),
    groupName = rs.getString("group_name"),
    cityRdf = rs.nullableInt("rdf_city"),
    address = rs.getString("addre") ?: "",
    phone = rs.getString("cell") ?: "",
    credit = rs.getLong("cred"),
    debt = rs.getLong("man"),
    blackList = rs.nullableInt("black_list"),
    active = rs.getString("active") ?: "",
    lat = rs.getDouble("Lat").takeIf { !rs.wasNull() },
    lng = rs.getDouble("Lng").takeIf { !rs.wasNull() },
    visitorRdf = rs.getInt("vis_rdf"),
)

/** مقادیر پیش‌فرض پیکربندی که با ممیزی سرور تأیید شده‌اند. */
object MeelanoDefaults {
    const val HOST = "192.168.1.150"      // LAN IP واقعی سرور (تأیید کاربر)
    const val PORT = 1433                  // sys.dm_tcp_listener_states: 0.0.0.0:1433
    const val DATABASE = "Meelano"         // DB_NAME() روی سرور
    const val APP_USER = "vizitor_android" // کاربر فقط‌خواندنی که اسکریپت 01 می‌سازد

    fun toServerConfig(cfg: ServerConfig): ServerConfig =
        cfg.copy(serverIp = HOST, dbPort = PORT)
}
