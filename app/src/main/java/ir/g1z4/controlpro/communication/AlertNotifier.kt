package ir.g1z4.controlpro.communication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.g1z4.controlpro.MainActivity
import ir.g1z4.controlpro.R
import ir.g1z4.controlpro.domain.Severity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertNotifier @Inject constructor(@ApplicationContext private val context: Context) {
    init { ensureChannels() }

    fun show(title: String, body: String, severity: Severity, synthetic: Boolean) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
        val channel = when (severity) {
            Severity.CRITICAL -> "alarm"
            Severity.WARNING -> "zone"
            Severity.INFO -> "connection"
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("route", "alerts")
        }
        val pending = PendingIntent.getActivity(
            context, title.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(if (synthetic) "$body\nاین اعلان آزمایشی است." else body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(if (severity == Severity.CRITICAL) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        listOf(
            NotificationChannel("alarm", "آلارم", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel("zone", "زون", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel("power", "برق", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel("battery", "باتری", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel("connection", "ارتباط", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel("arm", "فعال و غیرفعال", NotificationManager.IMPORTANCE_LOW)
        ).forEach { manager.createNotificationChannel(it) }
    }
}
