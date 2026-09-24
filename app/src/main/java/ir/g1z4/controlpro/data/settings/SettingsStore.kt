package ir.g1z4.controlpro.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.g1z4.controlpro.domain.ThemeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("g1z4_settings")

data class AppSettings(
    val theme: ThemeId = ThemeId.LUXURY_DARK,
    val biometric: Boolean = false,
    val autoLockSec: Int = 60,
    val sounds: Boolean = true,
    val notifyAlarm: Boolean = true,
    val notifyZone: Boolean = true,
    val notifyPower: Boolean = true,
    val notifyBattery: Boolean = true,
    val notifyConnection: Boolean = true,
    val notifyArm: Boolean = false,
    val developer: Boolean = false,
    val activeDeviceId: String? = null,
    val pinReady: Boolean = false
)

@Singleton
class SettingsStore @Inject constructor(@ApplicationContext private val context: Context) {
    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            theme = runCatching { ThemeId.valueOf(p[Keys.theme] ?: ThemeId.LUXURY_DARK.name) }.getOrDefault(ThemeId.LUXURY_DARK),
            biometric = p[Keys.biometric] ?: false,
            autoLockSec = p[Keys.autoLock] ?: 60,
            sounds = p[Keys.sounds] ?: true,
            notifyAlarm = p[Keys.nAlarm] ?: true,
            notifyZone = p[Keys.nZone] ?: true,
            notifyPower = p[Keys.nPower] ?: true,
            notifyBattery = p[Keys.nBattery] ?: true,
            notifyConnection = p[Keys.nConn] ?: true,
            notifyArm = p[Keys.nArm] ?: false,
            developer = p[Keys.developer] ?: false,
            activeDeviceId = p[Keys.active],
            pinReady = p[Keys.pinReady] ?: false
        )
    }

    suspend fun update(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    object Keys {
        val theme = stringPreferencesKey("theme")
        val biometric = booleanPreferencesKey("biometric")
        val autoLock = intPreferencesKey("autolock")
        val sounds = booleanPreferencesKey("sounds")
        val nAlarm = booleanPreferencesKey("n_alarm")
        val nZone = booleanPreferencesKey("n_zone")
        val nPower = booleanPreferencesKey("n_power")
        val nBattery = booleanPreferencesKey("n_battery")
        val nConn = booleanPreferencesKey("n_conn")
        val nArm = booleanPreferencesKey("n_arm")
        val developer = booleanPreferencesKey("developer")
        val active = stringPreferencesKey("active")
        val pinReady = booleanPreferencesKey("pin_ready")
    }
}
