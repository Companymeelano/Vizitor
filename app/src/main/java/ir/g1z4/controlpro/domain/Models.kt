package ir.g1z4.controlpro.domain

enum class PanelModel(val title: String) {
    Z4("Classic Z4"),
    Z4_ULTRA("Classic Z4 Ultra"),
    UNSPECIFIED("نامشخص")
}

enum class DialerModel(val title: String, val hasLog: Boolean, val defaultOutputs: Int) {
    G1("G1", false, 3),
    G1_PLUS("G1+", false, 3),
    G1_PRO("G1 Pro", true, 4),
    G1_ULTRA("G1 Ultra", true, 4)
}

enum class WiringProfile { STANDARD_A2_D3, CUSTOM }

enum class ArmState { UNKNOWN, ARMED, DISARMED, PART_SET }

enum class PowerState { UNKNOWN, MAINS_ON, MAINS_OFF }

enum class SirenState { UNKNOWN, SOUNDING, QUIET }

enum class LinkPhase {
    IDLE, QUEUED, SENDING, WAITING, CONFIRMED, UNCONFIRMED, REJECTED, FAILED, PERMISSION_DENIED
}

enum class ZoneKind { WIRED, WIRELESS, EXPANDER }

enum class ZoneState { UNKNOWN, TRIGGERED }

enum class CommandKind {
    STOP_DIAL, QUERY_IO, QUERY_PANEL, OUTPUT_ON, OUTPUT_OFF, ARM, DISARM, QUERY_CREDIT, STORE_USSD, RECHARGE
}

enum class Severity { CRITICAL, WARNING, INFO }

enum class ThemeId { LUXURY_DARK, MIDNIGHT, LUXURY_LIGHT, PURE_MINIMAL }

enum class OperatorPreset { IRANCELL, MCI, CUSTOM }

data class Device(
    val id: String,
    val name: String,
    val phoneRaw: String,
    val phoneKey: String,
    val panel: PanelModel,
    val dialer: DialerModel,
    val wiring: WiringProfile,
    val armOutput: Int,
    val disarmOutput: Int,
    val wiredZones: Int,
    val wireless: Int,
    val expanderZones: Int,
    val subscriptionId: Int?,
    val operatorPreset: OperatorPreset,
    val balanceUssd: String,
    val rechargeVariant: String,
    val optionalPanelQuery: Boolean,
    val panelQueryStar: Boolean,
    val timeoutSec: Int,
    val queryRetry: Boolean,
    val output4Present: Boolean,
    val createdAt: Long
)

data class ZoneRow(
    val deviceId: String,
    val key: String,
    val number: Int,
    val kind: ZoneKind,
    val localName: String,
    val typeNote: String,
    val state: ZoneState,
    val lastTriggerAt: Long?,
    val lastRaw: String?
)

data class OutputRow(
    val deviceId: String,
    val number: Int,
    val localName: String,
    val reservedFor: String?,
    val stateKnown: Boolean,
    val on: Boolean?,
    val lastAt: Long?
)

data class RemoteSlot(
    val deviceId: String,
    val slot: Int,
    val localName: String,
    val lastSeenAt: Long?
)

data class EventRow(
    val id: String,
    val deviceId: String,
    val at: Long,
    val severity: Severity,
    val title: String,
    val detail: String,
    val rawMasked: String,
    val synthetic: Boolean,
    val kind: String
)

data class AlertRow(
    val id: String,
    val deviceId: String,
    val at: Long,
    val severity: Severity,
    val title: String,
    val body: String,
    val read: Boolean,
    val synthetic: Boolean
)

data class CommandRow(
    val id: String,
    val deviceId: String,
    val kind: CommandKind,
    val summary: String,
    val phase: LinkPhase,
    val sentAt: Long?,
    val finishedAt: Long?,
    val responseMasked: String?,
    val errorFa: String?,
    val synthetic: Boolean
)

data class Snapshot(
    val deviceId: String,
    val arm: ArmState,
    val armAt: Long?,
    val power: PowerState,
    val powerAt: Long?,
    val siren: SirenState,
    val sirenAt: Long?,
    val creditRaw: String?,
    val creditAt: Long?,
    val phoneLineCut: Boolean?,
    val jammer: Boolean?,
    val batteryMention: String?,
    val lastIncomingAt: Long?,
    val lastResponseAt: Long?,
    val link: LinkPhase,
    val linkDetail: String?,
    val updatedAt: Long
) {
    companion object {
        fun unknown(deviceId: String) = Snapshot(
            deviceId = deviceId,
            arm = ArmState.UNKNOWN,
            armAt = null,
            power = PowerState.UNKNOWN,
            powerAt = null,
            siren = SirenState.UNKNOWN,
            sirenAt = null,
            creditRaw = null,
            creditAt = null,
            phoneLineCut = null,
            jammer = null,
            batteryMention = null,
            lastIncomingAt = null,
            lastResponseAt = null,
            link = LinkPhase.IDLE,
            linkDetail = null,
            updatedAt = 0L
        )
    }
}

data class CommandRequest(
    val kind: CommandKind,
    val output: Int? = null,
    val ussd: String? = null,
    val chargeCode: String? = null,
    val rechargeVariant: String = "irancell_hash",
    val panelStar: Boolean = false,
    val sensitive: Boolean = false
)
