package ir.g1z4.controlpro.ui.vm

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.g1z4.controlpro.communication.SmsEngine
import ir.g1z4.controlpro.data.repo.PanelRepository
import ir.g1z4.controlpro.data.security.Vault
import ir.g1z4.controlpro.data.settings.SettingsStore
import ir.g1z4.controlpro.domain.CommandKind
import ir.g1z4.controlpro.domain.CommandRequest
import ir.g1z4.controlpro.protocol.PinHasher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Session @Inject constructor() {
    val unlocked = MutableStateFlow(false)
    val pinReadyLocal = MutableStateFlow(false)
    @Volatile var lastTouch: Long = 0L
    var failed: Int = 0
    var lockedUntil: Long = 0L
}

@HiltViewModel
class AppModel @Inject constructor(
    private val repo: PanelRepository,
    private val settingsStore: SettingsStore,
    private val engine: SmsEngine,
    private val vault: Vault,
    val session: Session
) : ViewModel() {
    val settings = settingsStore.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ir.g1z4.controlpro.data.settings.AppSettings())
    val settingsReady = settingsStore.settings.map { true }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val devices = repo.devices().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val alerts = repo.alerts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun zones(id: String) = repo.zones(id)
    fun outputs(id: String) = repo.outputs(id)
    fun remotes(id: String) = repo.remotes(id)
    fun events(id: String) = repo.events(id)
    fun commands(id: String) = repo.commands(id)
    fun snapshot(id: String) = repo.snapshot(id)

    fun touch() { session.lastTouch = System.currentTimeMillis() }

    fun lockIfNeeded(now: Long = System.currentTimeMillis()): Boolean {
        val timeout = settings.value.autoLockSec
        if (timeout <= 0 || !session.unlocked.value) return !session.unlocked.value
        if (now - session.lastTouch > timeout * 1000L) {
            session.unlocked.value = false
            return true
        }
        return false
    }

    fun setupPin(pin: String, biometric: Boolean) {
        val salt = PinHasher.newSalt()
        vault.putBytes("pin_salt", salt)
        vault.putBytes("pin_hash", PinHasher.hash(pin, salt))
        viewModelScope.launch {
            settingsStore.update {
                it[SettingsStore.Keys.pinReady] = true
                it[SettingsStore.Keys.biometric] = biometric
            }
        }
        session.pinReadyLocal.value = true
        session.unlocked.value = true
        session.lastTouch = System.currentTimeMillis()
    }

    fun checkPin(pin: String): Boolean {
        val now = System.currentTimeMillis()
        if (now < session.lockedUntil) return false
        val salt = vault.getBytes("pin_salt") ?: return false
        val hash = vault.getBytes("pin_hash") ?: return false
        val ok = PinHasher.verify(pin, salt, hash)
        if (ok) {
            session.failed = 0
            session.unlocked.value = true
            session.lastTouch = now
        } else {
            session.failed += 1
            if (session.failed >= 5) session.lockedUntil = now + 30_000
        }
        return ok
    }

    fun lock() { session.unlocked.value = false }

    fun setActive(id: String) = viewModelScope.launch { settingsStore.update { it[SettingsStore.Keys.active] = id } }

    fun saveDevice(existingId: String?, fields: Map<String, String>, onDone: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = repo.save(
                existingId = existingId,
                name = fields["name"].orEmpty(),
                phoneRaw = fields["phone"].orEmpty(),
                password = fields["password"]?.ifBlank { null },
                panel = fields["panel"] ?: "Z4_ULTRA",
                dialer = fields["dialer"] ?: "G1_ULTRA",
                wiring = fields["wiring"] ?: "STANDARD_A2_D3",
                armOutput = fields["arm"]?.toIntOrNull() ?: 2,
                disarmOutput = fields["disarm"]?.toIntOrNull() ?: 3,
                wiredZones = fields["wired"]?.toIntOrNull() ?: 4,
                wireless = fields["wireless"]?.toIntOrNull() ?: 0,
                expanderZones = fields["expander"]?.toIntOrNull() ?: 0,
                subscriptionId = fields["sub"]?.toIntOrNull(),
                operatorPreset = fields["operator"] ?: "IRANCELL",
                balanceUssd = fields["ussd"] ?: "*141*1#",
                rechargeVariant = fields["recharge"] ?: "irancell_hash",
                optionalPanelQuery = fields["panelQuery"] == "1",
                panelQueryStar = fields["panelStar"] == "1",
                timeoutSec = fields["timeout"]?.toIntOrNull() ?: 90,
                queryRetry = fields["retry"] == "1",
                output4Present = fields["out4"] != "0"
            )
            onDone(result)
        }
    }

    fun send(deviceId: String, request: CommandRequest) = engine.enqueue(deviceId, request)
    fun scan(deviceId: String) = engine.scanInbox(deviceId)
    fun renameZone(id: String, key: String, name: String) = viewModelScope.launch { repo.renameZone(id, key, name) }
    fun noteZone(id: String, key: String, note: String) = viewModelScope.launch { repo.noteZone(id, key, note) }
    fun renameOutput(id: String, number: Int, name: String) = viewModelScope.launch { repo.renameOutput(id, number, name) }
    fun renameRemote(id: String, slot: Int, name: String) = viewModelScope.launch { repo.renameRemote(id, slot, name) }
    fun deleteDevice(id: String) = viewModelScope.launch { repo.deleteDevice(id) }
    fun markRead(id: String) = viewModelScope.launch { repo.markAlertRead(id) }
    fun markAllRead() = viewModelScope.launch { repo.markAllAlertsRead() }
    fun deleteAlert(id: String) = viewModelScope.launch { repo.deleteAlert(id) }
    fun clearAlerts() = viewModelScope.launch { repo.clearAlerts() }

    fun updateSetting(block: suspend () -> Unit) = viewModelScope.launch { block() }
    suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) = settingsStore.update(block)

    fun exportJson(): String {
        val root = JSONObject()
        root.put("schema", 1)
        root.put("note", "پشتیبان محلی. رمز دستگاه‌ها عمداً صادر نمی‌شود.")
        val arr = JSONArray()
        devices.value.forEach { d ->
            arr.put(JSONObject().apply {
                put("id", d.id); put("name", d.name); put("phone", d.phoneRaw)
                put("panel", d.panel.name); put("dialer", d.dialer.name)
            })
        }
        root.put("devices", arr)
        return root.toString(2)
    }

    fun command(kind: CommandKind, output: Int? = null, extra: String? = null, variant: String = "irancell_hash", star: Boolean = false) =
        CommandRequest(
            kind = kind,
            output = output,
            ussd = if (kind == CommandKind.STORE_USSD) extra else null,
            chargeCode = if (kind == CommandKind.RECHARGE) extra else null,
            rechargeVariant = variant,
            panelStar = star,
            sensitive = kind == CommandKind.ARM || kind == CommandKind.DISARM || kind == CommandKind.OUTPUT_ON || kind == CommandKind.OUTPUT_OFF || kind == CommandKind.RECHARGE || kind == CommandKind.STOP_DIAL || kind == CommandKind.STORE_USSD
        )
}

fun writeText(context: android.content.Context, uri: Uri, text: String) {
    context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
}
