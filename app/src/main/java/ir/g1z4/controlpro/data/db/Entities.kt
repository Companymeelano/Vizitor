package ir.g1z4.controlpro.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phoneRaw: String,
    val phoneKey: String,
    val passwordCipher: String,
    val panel: String,
    val dialer: String,
    val wiring: String,
    val armOutput: Int,
    val disarmOutput: Int,
    val wiredZones: Int,
    val wireless: Int,
    val expanderZones: Int,
    val subscriptionId: Int?,
    val operatorPreset: String,
    val balanceUssd: String,
    val rechargeVariant: String,
    val optionalPanelQuery: Boolean,
    val panelQueryStar: Boolean,
    val timeoutSec: Int,
    val queryRetry: Boolean,
    val output4Present: Boolean,
    val createdAt: Long
)

@Entity(tableName = "zones", primaryKeys = ["deviceId", "key"])
data class ZoneEntity(
    val deviceId: String,
    val key: String,
    val number: Int,
    val kind: String,
    val localName: String,
    val typeNote: String,
    val state: String,
    val lastTriggerAt: Long?,
    val lastRaw: String?
)

@Entity(tableName = "outputs", primaryKeys = ["deviceId", "number"])
data class OutputEntity(
    val deviceId: String,
    val number: Int,
    val localName: String,
    val reservedFor: String?,
    val stateKnown: Boolean,
    val on: Boolean?,
    val lastAt: Long?
)

@Entity(tableName = "remotes", primaryKeys = ["deviceId", "slot"])
data class RemoteEntity(
    val deviceId: String,
    val slot: Int,
    val localName: String,
    val lastSeenAt: Long?
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val deviceId: String,
    val at: Long,
    val severity: String,
    val title: String,
    val detail: String,
    val rawMasked: String,
    val synthetic: Boolean,
    val kind: String
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val deviceId: String,
    val at: Long,
    val severity: String,
    val title: String,
    val body: String,
    val read: Boolean,
    val synthetic: Boolean
)

@Entity(tableName = "commands")
data class CommandEntity(
    @PrimaryKey val id: String,
    val deviceId: String,
    val kind: String,
    val summary: String,
    val phase: String,
    val sentAt: Long?,
    val finishedAt: Long?,
    val responseMasked: String?,
    val errorFa: String?,
    val synthetic: Boolean,
    val outputNumber: Int?
)

@Entity(tableName = "snapshots")
data class SnapshotEntity(
    @PrimaryKey val deviceId: String,
    val arm: String,
    val armAt: Long?,
    val power: String,
    val powerAt: Long?,
    val siren: String,
    val sirenAt: Long?,
    val creditRaw: String?,
    val creditAt: Long?,
    val phoneLineCut: Boolean?,
    val jammer: Boolean?,
    val batteryMention: String?,
    val lastIncomingAt: Long?,
    val lastResponseAt: Long?,
    val link: String,
    val linkDetail: String?,
    val updatedAt: Long
)
