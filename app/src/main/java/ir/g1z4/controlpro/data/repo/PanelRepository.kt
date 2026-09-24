package ir.g1z4.controlpro.data.repo

import ir.g1z4.controlpro.data.db.AppDatabase
import ir.g1z4.controlpro.data.db.DeviceEntity
import ir.g1z4.controlpro.data.db.OutputEntity
import ir.g1z4.controlpro.data.db.RemoteEntity
import ir.g1z4.controlpro.data.db.ZoneEntity
import ir.g1z4.controlpro.data.security.Vault
import ir.g1z4.controlpro.data.settings.SettingsStore
import ir.g1z4.controlpro.domain.Device
import ir.g1z4.controlpro.domain.DialerModel
import ir.g1z4.controlpro.domain.Snapshot
import ir.g1z4.controlpro.domain.WiringProfile
import ir.g1z4.controlpro.domain.ZoneKind
import ir.g1z4.controlpro.protocol.PhoneNumbers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PanelRepository @Inject constructor(
    private val db: AppDatabase,
    private val vault: Vault,
    private val settings: SettingsStore
) {
    fun devices(): Flow<List<Device>> = db.devices().observe().map { list -> list.map { it.toDomain() } }
    fun zones(id: String) = db.zones().observe(id).map { it.map { z -> z.toDomain() } }
    fun outputs(id: String) = db.outputs().observe(id).map { it.map { o -> o.toDomain() } }
    fun remotes(id: String) = db.remotes().observe(id).map { it.map { r -> r.toDomain() } }
    fun events(id: String) = db.events().observe(id).map { it.map { e -> e.toDomain() } }
    fun allEvents() = db.events().observeAll().map { it.map { e -> e.toDomain() } }
    fun alerts() = db.alerts().observe().map { it.map { a -> a.toDomain() } }
    fun commands(id: String) = db.commands().observe(id).map { it.map { c -> c.toDomain() } }
    fun snapshot(id: String): Flow<Snapshot> = db.snapshots().observe(id).map { it?.toDomain() ?: Snapshot.unknown(id) }

    suspend fun device(id: String): Device? = db.devices().get(id)?.toDomain()
    suspend fun password(id: String): String? = db.devices().get(id)?.let { vault.open(it.passwordCipher) }

    suspend fun save(
        existingId: String?,
        name: String,
        phoneRaw: String,
        password: String?,
        panel: String,
        dialer: String,
        wiring: String,
        armOutput: Int,
        disarmOutput: Int,
        wiredZones: Int,
        wireless: Int,
        expanderZones: Int,
        subscriptionId: Int?,
        operatorPreset: String,
        balanceUssd: String,
        rechargeVariant: String,
        optionalPanelQuery: Boolean,
        panelQueryStar: Boolean,
        timeoutSec: Int,
        queryRetry: Boolean,
        output4Present: Boolean
    ): Result<String> {
        val key = PhoneNumbers.key(phoneRaw) ?: return Result.failure(IllegalArgumentException("شماره دستگاه معتبر نیست."))
        val normalizedPassword = password?.let { ir.g1z4.controlpro.protocol.Digits.ascii(it).trim() }
        if (normalizedPassword != null && !normalizedPassword.matches(Regex("\\d{4}"))) {
            return Result.failure(IllegalArgumentException("رمز تلفن‌کننده باید چهار رقم باشد."))
        }
        val id = existingId ?: UUID.randomUUID().toString()
        val previous = db.devices().get(id)
        val cipher = when {
            normalizedPassword != null -> vault.seal(normalizedPassword)
            previous != null -> previous.passwordCipher
            else -> return Result.failure(IllegalArgumentException("رمز تلفن‌کننده وارد نشده است."))
        }
        val dialerModel = DialerModel.valueOf(dialer)
        val outputs = if (output4Present) 4 else dialerModel.defaultOutputs.coerceAtMost(3)
        db.devices().upsert(
            DeviceEntity(
                id = id,
                name = name.trim().ifBlank { "دزدگیر" },
                phoneRaw = PhoneNumbers.display(phoneRaw),
                phoneKey = key,
                passwordCipher = cipher,
                panel = panel,
                dialer = dialer,
                wiring = wiring,
                armOutput = armOutput.coerceIn(1, 4),
                disarmOutput = disarmOutput.coerceIn(1, 4),
                wiredZones = wiredZones.coerceIn(1, 12),
                wireless = wireless.coerceIn(0, 15),
                expanderZones = expanderZones.coerceIn(0, 8),
                subscriptionId = subscriptionId,
                operatorPreset = operatorPreset,
                balanceUssd = balanceUssd,
                rechargeVariant = rechargeVariant,
                optionalPanelQuery = optionalPanelQuery,
                panelQueryStar = panelQueryStar,
                timeoutSec = timeoutSec.coerceIn(30, 180),
                queryRetry = queryRetry,
                output4Present = outputs == 4,
                createdAt = previous?.createdAt ?: System.currentTimeMillis()
            )
        )
        seedChildren(id, wiredZones.coerceIn(1, 12), wireless.coerceIn(0, 15), expanderZones.coerceIn(0, 8), outputs, WiringProfile.valueOf(wiring), armOutput, disarmOutput)
        if (db.snapshots().get(id) == null) db.snapshots().upsert(Snapshot.unknown(id).toEntity())
        settings.update { it[SettingsStore.Keys.active] = id }
        return Result.success(id)
    }

    private suspend fun seedChildren(
        id: String,
        wired: Int,
        wireless: Int,
        expander: Int,
        outputs: Int,
        wiring: WiringProfile,
        armOutput: Int,
        disarmOutput: Int
    ) {
        val zones = mutableListOf<ZoneEntity>()
        suspend fun add(kind: ZoneKind, number: Int, defaultName: String) {
            val key = "${kind.name}-$number"
            val old = db.zones().one(id, key)
            zones += ZoneEntity(
                id, key, number, kind.name,
                old?.localName ?: defaultName,
                old?.typeNote.orEmpty(),
                old?.state ?: "UNKNOWN",
                old?.lastTriggerAt,
                old?.lastRaw
            )
        }
        repeat(wired) { add(ZoneKind.WIRED, it + 1, "زون ${it + 1}") }
        repeat(wireless) { add(ZoneKind.WIRELESS, it + 1, "بی‌سیم ${it + 1}") }
        repeat(expander) { add(ZoneKind.EXPANDER, it + 1, "اکسپندر ${it + 1}") }
        db.zones().clear(id)
        db.zones().upsert(zones)

        val oldOutputs = db.outputs().list(id).associateBy { it.number }
        val outputRows = (1..outputs).map { n ->
            val reserved = when {
                wiring == WiringProfile.STANDARD_A2_D3 && n == armOutput -> "فعال‌سازی پنل"
                wiring == WiringProfile.STANDARD_A2_D3 && n == disarmOutput -> "غیرفعال‌سازی پنل"
                else -> null
            }
            val old = oldOutputs[n]
            OutputEntity(
                id, n, old?.localName ?: "خروجی $n", reserved,
                old?.stateKnown ?: false, old?.on, old?.lastAt
            )
        }
        db.outputs().clear(id)
        db.outputs().upsert(outputRows)

        val oldRemotes = db.remotes().list(id).associateBy { it.slot }
        db.remotes().clear(id)
        db.remotes().upsert((1..15).map { slot ->
            val old = oldRemotes[slot]
            RemoteEntity(id, slot, old?.localName ?: "جایگاه $slot", old?.lastSeenAt)
        })
    }

    suspend fun renameZone(deviceId: String, key: String, name: String) {
        val row = db.zones().one(deviceId, key) ?: return
        db.zones().upsert(listOf(row.copy(localName = name.trim().ifBlank { row.localName })))
    }

    suspend fun noteZone(deviceId: String, key: String, note: String) {
        val row = db.zones().one(deviceId, key) ?: return
        db.zones().upsert(listOf(row.copy(typeNote = note)))
    }

    suspend fun renameOutput(deviceId: String, number: Int, name: String) {
        val row = db.outputs().list(deviceId).firstOrNull { it.number == number } ?: return
        db.outputs().upsert(listOf(row.copy(localName = name.trim().ifBlank { row.localName })))
    }

    suspend fun renameRemote(deviceId: String, slot: Int, name: String) {
        val row = db.remotes().list(deviceId).firstOrNull { it.slot == slot } ?: return
        db.remotes().upsert(listOf(row.copy(localName = name.trim().ifBlank { row.localName })))
    }

    suspend fun deleteDevice(id: String) {
        db.devices().delete(id)
        db.zones().clear(id)
        db.outputs().clear(id)
        db.remotes().clear(id)
        db.events().clear(id)
        db.commands().clear(id)
        db.snapshots().delete(id)
        db.alerts().clearDevice(id)
    }

    suspend fun markAlertRead(id: String) = db.alerts().markRead(id)
    suspend fun markAllAlertsRead() = db.alerts().markAllRead()
    suspend fun deleteAlert(id: String) = db.alerts().delete(id)
    suspend fun clearAlerts() = db.alerts().clear()
}
