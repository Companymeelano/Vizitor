package ir.g1z4.controlpro

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import dagger.hilt.android.HiltAndroidApp
import ir.g1z4.controlpro.communication.SmsEngine
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class G1Z4App : Application() {
    @Inject lateinit var engine: SmsEngine

    override fun onCreate() {
        super.onCreate()
        engine.expireStale()
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(wrap(base))
    }

    companion object {
        fun wrap(base: Context): Context {
            val locale = Locale("fa", "IR")
            Locale.setDefault(locale)
            val config = Configuration(base.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            return base.createConfigurationContext(config)
        }
    }
}
