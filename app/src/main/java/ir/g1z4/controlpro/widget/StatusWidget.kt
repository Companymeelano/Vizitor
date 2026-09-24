package ir.g1z4.controlpro.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dagger.hilt.android.EntryPointAccessors
import ir.g1z4.controlpro.MainActivity
import ir.g1z4.controlpro.R
import ir.g1z4.controlpro.di.WidgetEntryPoint
import ir.g1z4.controlpro.domain.ArmState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

class StatusWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        refresh(context)
    }

    companion object {
        private val io = Executors.newSingleThreadExecutor()

        fun refresh(context: Context) {
            val app = context.applicationContext
            io.execute {
                val manager = AppWidgetManager.getInstance(app)
                val ids = manager.getAppWidgetIds(ComponentName(app, StatusWidget::class.java))
                if (ids.isEmpty()) return@execute
                val built = views(app)
                ids.forEach { manager.updateAppWidget(it, built) }
            }
        }

        private fun views(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_status)
            val data = runCatching { read(context) }.getOrNull()
            views.setTextViewText(R.id.widget_name, data?.first ?: "دستگاهی انتخاب نشده")
            views.setTextViewText(R.id.widget_status, "وضعیت: ${data?.second ?: "نامشخص"}")
            views.setTextViewText(R.id.widget_alert, "آخرین هشدار: نامشخص")
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("route", "dashboard")
            }
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(context, 41, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
            return views
        }

        private fun read(context: Context): Pair<String, String>? = runBlocking {
            val entry = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
            val active = entry.settings().settings.first().activeDeviceId
            val devices = entry.database().devices().all()
            val device = devices.firstOrNull { it.id == active } ?: devices.firstOrNull() ?: return@runBlocking null
            val arm = entry.database().snapshots().get(device.id)?.arm
            val status = when (arm) {
                ArmState.ARMED.name -> "فعال"
                ArmState.DISARMED.name -> "غیرفعال"
                ArmState.PART_SET.name -> "نیمه‌فعال"
                else -> "نامشخص"
            }
            device.name to status
        }
    }
}
