/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | دیتابیس محلی Room (Offline-First Core)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProductEntity::class,
        CustomerEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        SalMaliHistoryEntity::class,
        CartItemEntity::class,
        ChatMessageEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun products(): ProductDao
    abstract fun customers(): CustomerDao
    abstract fun invoices(): InvoiceDao
    abstract fun salMali(): SalMaliDao
    abstract fun cart(): CartDao
    abstract fun chat(): ChatDao

    companion object {
        const val DB_NAME = "vizitor.db"

        /** نسخه ۳ ← ۴: افزودن ستون «در انتظار تأیید حسابداری» به جدول مشتریان. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN pendingApproval INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** نسخه ۴ ← ۵: جدول پیام‌های گفتگوی ویزیتورها. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `chat_messages` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`senderName` TEXT NOT NULL, `senderUsername` TEXT NOT NULL, " +
                    "`senderPhone` TEXT NOT NULL DEFAULT '', `text` TEXT NOT NULL, " +
                    "`type` TEXT NOT NULL DEFAULT 'TEXT', `timeLong` INTEGER NOT NULL, " +
                    "`pinned` INTEGER NOT NULL DEFAULT 0, `mine` INTEGER NOT NULL DEFAULT 0)"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
