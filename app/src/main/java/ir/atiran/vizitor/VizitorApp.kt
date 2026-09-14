/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | کلاس Application
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  مقداردهی اولیه دیتابیس محلی + زمان‌بندی سینک پس‌زمینه + کاشت داده دمو
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor

import android.app.Application
import ir.atiran.vizitor.data.repository.VizitorRepository
import ir.atiran.vizitor.data.sync.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VizitorApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val repository = VizitorRepository(this)
        // کاشت داده نمونه برای اولین اجرا (حالت دمو تا اتصال سرور واقعی)
        appScope.launch { repository.ensureSeeded() }
        // فعال‌سازی سرویس همگام‌سازی خودکار پس‌زمینه
        SyncWorker.schedule(this)
    }
}
