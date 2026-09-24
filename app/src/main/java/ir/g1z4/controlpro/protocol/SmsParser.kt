package ir.g1z4.controlpro.protocol

import ir.g1z4.controlpro.domain.ArmState
import ir.g1z4.controlpro.domain.PowerState
import ir.g1z4.controlpro.domain.Severity
import ir.g1z4.controlpro.domain.SirenState

enum class SmsKind { STATUS, TRIGGER, OUTPUT, CREDIT, REJECT, REPORT, OTHER }

data class ParsedSms(
    val raw: String,
    val kind: SmsKind,
    val continuation: Boolean,
    val zonesTriggered: Set<Int>,
    val outputs: Map<Int, Boolean>,
    val arm: ArmState?,
    val power: PowerState?,
    val siren: SirenState?,
    val triggers: Set<Int>,
    val remoteSlot: Int?,
    val phoneLineCut: Boolean,
    val jammer: Boolean,
    val batteryMention: String?,
    val rejected: Boolean,
    val severity: Severity
)

object SmsParser {
    private val digitMap = mapOf(
        '۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4',
        '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9',
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
    )

    fun parse(raw: String): ParsedSms {
        val text = raw.trim()
        val folded = fold(text)
        val continuation = folded.endsWith("...") || text.trimEnd().endsWith("…") || folded.endsWith(" -...")
        val zones = zones(folded)
        val outputs = outputs(folded)
        val triggers = triggers(folded)
        val remote = remote(folded)
        val phoneCut = folded.contains("تلفن قطع") || folded.contains("خط تلفن") || folded.contains("phone line")
        val jammer = folded.contains("جمر") || folded.contains("jammer")
        val battery = battery(folded)
        val rejected = isRejected(folded)
        val power = power(folded)
        val arm = arm(folded, outputs.isNotEmpty())
        val siren = siren(folded, triggers)
        val kind = kind(folded, zones, outputs, triggers, rejected, arm, power)
        val severity = when {
            rejected -> Severity.WARNING
            siren == SirenState.SOUNDING || phoneCut || jammer -> Severity.CRITICAL
            zones.isNotEmpty() || power == PowerState.MAINS_OFF || battery != null -> Severity.WARNING
            else -> Severity.INFO
        }
        return ParsedSms(
            raw = text,
            kind = kind,
            continuation = continuation,
            zonesTriggered = zones,
            outputs = outputs,
            arm = arm,
            power = power,
            siren = siren,
            triggers = triggers,
            remoteSlot = remote,
            phoneLineCut = phoneCut,
            jammer = jammer,
            batteryMention = battery,
            rejected = rejected,
            severity = severity
        )
    }

    fun fold(raw: String): String = buildString(raw.length) {
        raw.trim().lowercase().forEach { ch -> append(digitMap[ch] ?: ch) }
    }

    private fun kind(
        folded: String,
        zones: Set<Int>,
        outputs: Map<Int, Boolean>,
        triggers: Set<Int>,
        rejected: Boolean,
        arm: ArmState?,
        power: PowerState?
    ): SmsKind = when {
        rejected -> SmsKind.REJECT
        folded.contains("وضعیت") || zones.isNotEmpty() && folded.contains("وضعیت") -> SmsKind.STATUS
        folded.contains("وضعیت") -> SmsKind.STATUS
        triggers.isNotEmpty() -> SmsKind.TRIGGER
        outputs.isNotEmpty() && arm == null && power == null -> SmsKind.OUTPUT
        folded.contains("شارژ") || folded.contains("اعتبار") || folded.contains("ریال") -> SmsKind.CREDIT
        arm != null || power != null || folded.contains("ریموت") -> SmsKind.REPORT
        else -> SmsKind.OTHER
    }

    private fun zones(folded: String): Set<Int> {
        val found = linkedSetOf<Int>()
        val range = Regex("زون\\s*(\\d+)\\s*[-–تاو]+\\s*(\\d+)")
        range.findAll(folded).forEach { m ->
            val a = m.groupValues[1].toIntOrNull() ?: return@forEach
            val b = m.groupValues[2].toIntOrNull() ?: return@forEach
            val start = minOf(a, b)
            val end = maxOf(a, b)
            if (end - start <= 16) (start..end).forEach { if (it in 1..32) found += it }
        }
        Regex("زون\\s*(\\d+)").findAll(folded).forEach { m ->
            m.groupValues[1].toIntOrNull()?.takeIf { it in 1..32 }?.let { found += it }
        }
        Regex("zone\\s*(\\d+)").findAll(folded).forEach { m ->
            m.groupValues[1].toIntOrNull()?.takeIf { it in 1..32 }?.let { found += it }
        }
        return found
    }

    private fun outputs(folded: String): Map<Int, Boolean> {
        val map = linkedMapOf<Int, Boolean>()
        val rx = Regex("خروجی\\s*(\\d+)\\s*[:\\- ]*\\s*(فعال|روشن|خاموش|غیرفعال|غیر فعال)")
        rx.findAll(folded).forEach { m ->
            val n = m.groupValues[1].toIntOrNull() ?: return@forEach
            if (n !in 1..4) return@forEach
            val word = m.groupValues[2]
            map[n] = word == "فعال" || word == "روشن"
        }
        Regex("out\\s*(\\d+)\\s*(on|off)").findAll(folded).forEach { m ->
            val n = m.groupValues[1].toIntOrNull() ?: return@forEach
            if (n in 1..4) map[n] = m.groupValues[2] == "on"
        }
        return map
    }

    private fun triggers(folded: String): Set<Int> {
        val set = linkedSetOf<Int>()
        Regex("تحریک\\s*(\\d+)").findAll(folded).forEach { m ->
            m.groupValues[1].toIntOrNull()?.takeIf { it in 1..4 }?.let { set += it }
        }
        if (folded.contains("آژیر") && (folded.contains("به صدا") || folded.contains("فعال"))) set += 3
        return set
    }

    private fun remote(folded: String): Int? =
        Regex("ریموت\\s*(\\d+)").find(folded)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 1..15 }

    private fun power(folded: String): PowerState? = when {
        folded.contains("قطع برق") || folded.contains("برق قطع") || folded.contains("برق شهر قطع") || folded.contains("power fail") -> PowerState.MAINS_OFF
        folded.contains("وصل برق") || folded.contains("برق وصل") || folded.contains("برق شهر وصل") || folded.contains("power restore") -> PowerState.MAINS_ON
        else -> null
    }

    private fun arm(folded: String, hasOutput: Boolean): ArmState? {
        if (folded.contains("نیمه فعال") || folded.contains("نیمه‌فعال") || folded.contains("partset")) return ArmState.PART_SET
        val disarmed = folded.contains("غیرفعال") || folded.contains("غیر فعال") || folded.contains("disarm")
        val armed = folded.contains("فعال شد") || folded.contains("سیستم فعال") || folded.contains("دستگاه فعال") || folded.contains("armed")
        if (disarmed && (folded.contains("سیستم") || folded.contains("دستگاه") || folded.contains("ریموت") || !hasOutput)) {
            return ArmState.DISARMED
        }
        if (armed && !disarmed && (folded.contains("سیستم") || folded.contains("دستگاه") || folded.contains("ریموت"))) {
            return ArmState.ARMED
        }
        return null
    }

    private fun siren(folded: String, triggers: Set<Int>): SirenState? = when {
        folded.contains("آژیر") && (folded.contains("قطع") || folded.contains("خاموش")) -> SirenState.QUIET
        folded.contains("آژیر") && (folded.contains("به صدا") || folded.contains("فعال") || folded.contains("هشدار")) -> SirenState.SOUNDING
        3 in triggers && folded.contains("فعال") -> SirenState.SOUNDING
        folded.contains("alarm") -> SirenState.SOUNDING
        else -> null
    }

    private fun battery(folded: String): String? {
        if (!folded.contains("باتری") && !folded.contains("battery")) return null
        return folded.lineSequence().firstOrNull { it.contains("باتری") || it.contains("battery") }?.take(80)
            ?: "اشاره به باتری در پیامک"
    }

    private fun isRejected(folded: String): Boolean {
        val keys = listOf("رمز اشتباه", "رمز نادرست", "دستور نامعتبر", "دستور نامعتبر است", "دسترسی غیر", "غیرمجاز", "invalid", "unauthorized", "wrong password")
        return keys.any { folded.contains(it) }
    }
}
