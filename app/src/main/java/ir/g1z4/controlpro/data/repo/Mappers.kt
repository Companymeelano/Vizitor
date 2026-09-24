package ir.g1z4.controlpro.data.repo

import ir.g1z4.controlpro.data.db.AlertEntity
import ir.g1z4.controlpro.data.db.CommandEntity
import ir.g1z4.controlpro.data.db.DeviceEntity
import ir.g1z4.controlpro.data.db.EventEntity
import ir.g1z4.controlpro.data.db.OutputEntity
import ir.g1z4.controlpro.data.db.RemoteEntity
import ir.g1z4.controlpro.data.db.SnapshotEntity
import ir.g1z4.controlpro.data.db.ZoneEntity
import ir.g1z4.controlpro.domain.AlertRow
import ir.g1z4.controlpro.domain.ArmState
import ir.g1z4.controlpro.domain.CommandKind
import ir.g1z4.controlpro.domain.CommandRow
import ir.g1z4.controlpro.domain.Device
import ir.g1z4.controlpro.domain.DialerModel
import ir.g1z4.controlpro.domain.EventRow
import ir.g1z4.controlpro.domain.LinkPhase
import ir.g1z4.controlpro.domain.OperatorPreset
import ir.g1z4.controlpro.domain.OutputRow
import ir.g1z4.controlpro.domain.PanelModel
import ir.g1z4.controlpro.domain.PowerState
import ir.g1z4.controlpro.domain.RemoteSlot
import ir.g1z4.controlpro.domain.Severity
import ir.g1z4.controlpro.domain.SirenState
import ir.g1z4.controlpro.domain.Snapshot
import ir.g1z4.controlpro.domain.WiringProfile
import ir.g1z4.controlpro.domain.ZoneKind
import ir.g1z4.controlpro.domain.ZoneRow
import ir.g1z4.controlpro.domain.ZoneState

fun DeviceEntity.toDomain() = Device(
    id, name, phoneRaw, phoneKey,
    PanelModel.valueOf(panel), DialerModel.valueOf(dialer), WiringProfile.valueOf(wiring),
    armOutput, disarmOutput, wiredZones, wireless, expanderZones, subscriptionId,
    OperatorPreset.valueOf(operatorPreset), balanceUssd, rechargeVariant, optionalPanelQuery,
    panelQueryStar, timeoutSec, queryRetry, output4Present, createdAt
)

fun ZoneEntity.toDomain() = ZoneRow(deviceId, key, number, ZoneKind.valueOf(kind), localName, typeNote, ZoneState.valueOf(state), lastTriggerAt, lastRaw)
fun OutputEntity.toDomain() = OutputRow(deviceId, number, localName, reservedFor, stateKnown, on, lastAt)
fun RemoteEntity.toDomain() = RemoteSlot(deviceId, slot, localName, lastSeenAt)
fun EventEntity.toDomain() = EventRow(id, deviceId, at, Severity.valueOf(severity), title, detail, rawMasked, synthetic, kind)
fun AlertEntity.toDomain() = AlertRow(id, deviceId, at, Severity.valueOf(severity), title, body, read, synthetic)
fun CommandEntity.toDomain() = CommandRow(id, deviceId, CommandKind.valueOf(kind), summary, LinkPhase.valueOf(phase), sentAt, finishedAt, responseMasked, errorFa, synthetic)

fun SnapshotEntity.toDomain() = Snapshot(
    deviceId, ArmState.valueOf(arm), armAt, PowerState.valueOf(power), powerAt, SirenState.valueOf(siren), sirenAt,
    creditRaw, creditAt, phoneLineCut, jammer, batteryMention, lastIncomingAt, lastResponseAt,
    LinkPhase.valueOf(link), linkDetail, updatedAt
)

fun Snapshot.toEntity() = SnapshotEntity(
    deviceId, arm.name, armAt, power.name, powerAt, siren.name, sirenAt, creditRaw, creditAt,
    phoneLineCut, jammer, batteryMention, lastIncomingAt, lastResponseAt, link.name, linkDetail, updatedAt
)
