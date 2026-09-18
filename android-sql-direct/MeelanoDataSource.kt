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

/** موجودی یک کالا در یک انبار — از ویو خودِ ERP (VW_InventoryAnbars).
 *  quantity = موجودی روی کاغذ؛ available* = قابل فروش، یعنی موجودی منهای
 *  مقداری که پیش‌فاکتورهای بازِ دیگر روی همان کالا/انبار گرفته‌اند. */
data class DbStockRow(
    val shka: Long,
    val warehouseRdf: Int,
    val warehouseName: String,
    val quantity: Double,
    val quantityPiece: Int,
    val availableQuantity: Double = 0.0,
    val availablePiece: Int = 0,
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

    // ── ورود: مطابق دقیق رفتار خودِ ERP (بدون هیچ حدسی) ──────────────────────
    //  شواهد از بدنهٔ توابع واقعی ERP روی همین سرور:
    //    · dbo.SetUserpass:   select convert(varchar(50), user_password) from sys_users ...
    //    · dbo.ChangeUserPassInSalMali:
    //          update ... set user_password = CONVERT(varbinary, @PassWord) ...
    //    · ممیزی روی دادهٔ واقعی: DATALENGTH(user_password)=1 و hex آن '31' (= کاراکتر '1')
    //  ⇒ رمز در این ERP «متن ساده» داخل varbinary است و SQL Server هش نمی‌کند؛
    //    پس PWDCOMPARE هرگز جواب نمی‌دهد و مقایسه باید با CONVERT(varchar(50), …) باشد،
    //    دقیقاً همان‌طور که خودِ ERP رمز را می‌خواند.
    //
    //  نکته: collation دیتابیس SQL_Latin1_General_CP1256_CI_AS است، پس مقایسه
    //  حساس به بزرگی/کوچکی حروف نیست. اگر بعداً معلوم شد ERP حساس است، فقط همین
    //  عبارت به  sys_users.user_password = CONVERT(varbinary(50), ?)  تغییر می‌کند.
    //
    //  امنیت: رمز فقط پارامتر همین کوئری است — در اپ ذخیره نمی‌شود، لاگ نمی‌شود و
    //  در هیچ پیام خطایی چاپ نمی‌شود. (بستهٔ ورود SQL Server نیز در همان handshake
    //  رمزنگاری می‌شود، ولی توصیهٔ ما فعال بودن TLS روی اتصال است.)
    //
    //  IsLocked: نوع واقعی ستون bit NULL است و مقدار هر دو کاربر NULL (تست زندهٔ
    //  sql/07_login_verify.sql v2 روی سرور، 2026-09-18: V1|MATCH و V2=0).
    //  خودِ ERP هم در شرط ورود روی IsLocked فیلتر نمی‌کند (همان تست با همان شرط
    //  ERP ردیف را برگرداند). پس اینجا با CASE به صفر/یک تبدیل می‌شود تا اپ هرگز
    //  NULL را «قفل‌بودن» تفسیر نکند: تنها IsLocked = 1 یعنی قفل. صفر و NULL = باز.
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
                       CASE WHEN sys_users.IsLocked = 1 THEN 1 ELSE 0 END AS is_locked,
                       sys_users.shmo
                  FROM dbo.sys_users
                 WHERE sys_users.user_name = ?
                   AND CONVERT(varchar(50), sys_users.user_password) = ?
                   AND sys_users.active = 1
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 15
                ps.setString(1, username)
                ps.setString(2, password)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) null else DbLoginRow(
                        userId = rs.getInt("user_id"),
                        username = rs.getString("user_name") ?: username,
                        fullName = listOfNotNull(
                            rs.getString("user_fname"), rs.getString("user_lname")
                        ).joinToString(" "),
                        roleId = rs.nullableInt("role_id"),
                        active = rs.getBoolean("active"),
                        locked = rs.getBoolean("is_locked"),
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
                ps.setString(1, ACTIVE_CHAR)
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
                ps.setString(2, ACTIVE_CHAR)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getLong(1) else null }
            }
        }
    }

    // ── موجودی انبارها (از ویو خودِ ERP: موجودی منهای پیش‌فاکتورهای باز) ─────
    //  عدد mojkavah/mojkajoz روی کاغذ است؛ MojodiPish_vah/MojodiPish_joz همان
    //  چیزی است که ERP به عنوان موجودی قابل فروش نشان می‌دهد (سطرهای
    //  subsailfact_pish با sh_f = 0 و active = 't' و Rejected = 0 کم شده‌اند).
    suspend fun stock(shka: Long? = null): List<DbStockRow> =
        db.withConnection { c ->
            val sql = buildString {
                append(
                    """
                    SELECT vz.shka, vz.rdf_anbars, vz.name,
                           vz.mojkavah, vz.mojkajoz,
                           vz.MojodiPish_vah, vz.MojodiPish_joz
                      FROM dbo.VW_InventoryAnbars vz
                     WHERE (? = 0 OR vz.shka = ?)
                     ORDER BY vz.name
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
                            availableQuantity = it.getDouble("MojodiPish_vah"),
                            availablePiece = it.getInt("MojodiPish_joz"),
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
                   AND CUSTOMERS.active = ?
                 ORDER BY CUSTOMERS.MONAME
                 OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 30
                ps.setInt(1, userId)
                ps.setInt(2, companyId ?: 0)
                ps.setInt(3, companyId ?: 0)
                ps.setString(4, ACTIVE_CHAR)
                ps.setInt(5, offset)
                ps.setInt(6, limit)
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
                 WHERE custgroup.Active = 1        -- bit column (verified)
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
    suspend fun visitorIdentity(userId: Int, companyId: Int? = null): DbVisitorIdentity? =
        db.withConnection { c ->
            //  نکتهٔ مهم (تأییدشده با دادهٔ واقعی): visitors.UserID روی این سرور NULL است،
            //  پس اتصال کاربر به ویزیتور از طریق sys_vis است:
            //      sys_users.user_id -> sys_vis.UserID -> sys_vis.shvis -> visitors.vis_rdf
            c.prepareStatement(
                """
                SELECT TOP (1)
                       visitors.vis_rdf, visitors.vis_name, visitors.active,
                       visitors.VIs_region, visitors.vis_city,
                       (SELECT COUNT(*) FROM dbo.sys_cus WHERE sys_cus.UserID = sys_vis.UserID) AS allowed_customers,
                       (SELECT COUNT(*) FROM dbo.sys_kal WHERE sys_kal.UserID = sys_vis.UserID) AS allowed_products,
                       (SELECT COUNT(*) FROM dbo.sys_anb WHERE sys_anb.UserID = sys_vis.UserID) AS allowed_warehouses
                  FROM dbo.sys_vis
                  JOIN dbo.visitors ON visitors.vis_rdf = sys_vis.shvis
                 WHERE sys_vis.UserID = ?
                   AND (? = 0 OR sys_vis.SysID = ?)
                 ORDER BY visitors.vis_rdf
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 20
                ps.setInt(1, userId)
                ps.setInt(2, companyId ?: 0)
                ps.setInt(3, companyId ?: 0)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) null else DbVisitorIdentity(
                        userId = userId,
                        username = "",
                        displayName = rs.getString("vis_name") ?: "",
                        visitorRdf = rs.nullableInt("vis_rdf"),
                        allowedCustomers = rs.getInt("allowed_customers"),
                        allowedProducts = rs.getInt("allowed_products"),
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
         * مقادیر «فعال» — با خروجی واقعی سرور تأیید شده‌اند (بخش G3 ممیزی):
         *   ستون‌های char(1) این ERP مقدار 't' (فعال) و 'f' (غیرفعال) دارند،
         *   نه '1'. ستون‌های bit با 1/0 مقایسه می‌شوند.
         * ابزار tools/check_sql_columns.py این دو مقدار را با فایل
         * docs/schema/meelano-values.tsv تطبیق می‌دهد.
         */
        const val ACTIVE_CHAR = "t"   // value: dbo.inventory.active
        const val ACTIVE_BIT = 1      // value: dbo.kagroup.Active
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
