/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | ذخیرهٔ امن اطلاعات اتصال دیتابیس
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  اطلاعات اتصال SQL Server (شامل رمز) هرگز به‌صورت متن در SharedPreferences
 *  یا DataStore نگه نمی‌دارند؛ یک بلوک JSON با AES-GCM و کلید غیرخارج‌شدنی
 *  Android Keystore رمزنگاری می‌شود (همین الگویی که AuthStore برای توکن
 *  نشست استفاده می‌کند).
 *
 *  قوانین امنیتی:
 *   • رمز در حافظه فقط هنگام اتصال مصرف می‌شود
 *   • هیچ‌گاه در log/crash report نمی‌آید (استثنا در LogCat تنظیم نشده)
 *   • خروج از حساب (logout) → clean
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.data.local

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import ir.atiran.vizitor.data.sql.DbSettings
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

/** نگهداری امن تنظیمات اتصال SQL Server (رمزنگارش AES-GCM + Keystore). */
object SecureDbStore {

    private const val PREFS = "vizitor_db"
    private const val KEY_BLOB = "db_blob_v1"
    private const val KS = "AndroidKeyStore"
    private const val ALIAS = "VizitorDbKey"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (this::appContext.isInitialized) return
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    /** ذخیرهٔ تنظیمات (کل بلوک، از جمله رمز، رمزنگاری می‌شود). */
    fun save(settings: DbSettings) {
        val json = JSONObject().apply {
            put("host", settings.host)
            put("port", settings.port)
            put("database", settings.database)
            put("username", settings.username)
            put("password", settings.password)
            put("useEncryption", settings.useEncryption)
            put("trustServerCert", settings.trustServerCert)
            put("connectTimeoutSec", settings.connectTimeoutSec)
            put("queryTimeoutSec", settings.queryTimeoutSec)
        }.toString()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val enc = cipher.doFinal(json.toByteArray(Charsets.UTF_8))
        // IV + ciphertext در یک رشته Base64
        val blob = Base64.encodeToString(iv + enc, Base64.NO_WRAP)
        prefs.edit().putString(KEY_BLOB, blob).apply()
    }

    /** خواندن تنظیمات؛ اگر چیزی ذخیره نشده یا خطا بود → null. */
    fun load(): DbSettings? {
        if (!this::appContext.isInitialized) return null
        val blob = prefs.getString(KEY_BLOB, null) ?: return null
        return try {
            val raw = Base64.decode(blob, Base64.NO_WRAP)
            val iv = raw.copyOfRange(0, GCM_IV_LENGTH)
            val ct = raw.copyOfRange(GCM_IV_LENGTH, raw.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val json = String(cipher.doFinal(ct), Charsets.UTF_8)
            val o = JSONObject(json)
            DbSettings(
                host = o.getString("host"),
                port = o.optInt("port", 1433),
                database = o.getString("database"),
                username = o.getString("username"),
                password = o.getString("password"),
                useEncryption = o.optBoolean("useEncryption", true),
                trustServerCert = o.optBoolean("trustServerCert", true),
                connectTimeoutSec = o.optInt("connectTimeoutSec", 10),
                queryTimeoutSec = o.optInt("queryTimeoutSec", 30),
            )
        } catch (_: Exception) {
            null
        }
    }

    /** فقط مشخصات بدون رمز (برای نمایش در UI: «مربوط به کاربر … در …»). */
    fun loadMasked(): Pair<String, String>? {
        return load()?.let { it.username to it.masked() }
    }

    /** پاک‌سازی کامل (در هنگام خروج از حساب). */
    fun clear() {
        if (!this::prefs.isInitialized) return
        prefs.edit().remove(KEY_BLOB).apply()
    }

    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(KS).apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KS)
        gen.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return gen.generateKey()
    }
}
