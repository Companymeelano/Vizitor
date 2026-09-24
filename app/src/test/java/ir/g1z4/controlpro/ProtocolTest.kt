package ir.g1z4.controlpro

import ir.g1z4.controlpro.domain.ArmState
import ir.g1z4.controlpro.domain.CommandKind
import ir.g1z4.controlpro.domain.CommandRequest
import ir.g1z4.controlpro.domain.LinkPhase
import ir.g1z4.controlpro.domain.PowerState
import ir.g1z4.controlpro.domain.SirenState
import ir.g1z4.controlpro.protocol.BuildResult
import ir.g1z4.controlpro.protocol.CommandBuilder
import ir.g1z4.controlpro.protocol.CommandPolicy
import ir.g1z4.controlpro.protocol.Jalali
import ir.g1z4.controlpro.protocol.PhoneNumbers
import ir.g1z4.controlpro.protocol.PinHasher
import ir.g1z4.controlpro.protocol.ReplyMatch
import ir.g1z4.controlpro.protocol.SmsKind
import ir.g1z4.controlpro.protocol.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolTest {
    private fun req(kind: CommandKind, output: Int? = null, extra: String? = null, variant: String = "irancell_hash", star: Boolean = false) =
        CommandRequest(
            kind = kind,
            output = output,
            ussd = if (kind == CommandKind.STORE_USSD) extra else null,
            chargeCode = if (kind == CommandKind.RECHARGE) extra else null,
            rechargeVariant = variant,
            panelStar = star
        )

    private fun body(result: BuildResult) = (result as BuildResult.Ok).body

    @Test
    fun armPulsesMappedOutputOnWithoutTrailingStar() {
        val built = CommandBuilder.build("1234", req(CommandKind.ARM), armOutput = 2, disarmOutput = 3)
        assertEquals("*1234*21", body(built))
        assertFalse(body(built).endsWith("*21*"))
        assertFalse(CommandPolicy.mayInferPanelArmed(ReplyMatch.CONFIRMED))
    }

    @Test
    fun disarmPulsesMappedOutputOnAndStar20IsOnlyOutputOff() {
        assertEquals("*1234*31", body(CommandBuilder.build("1234", req(CommandKind.DISARM), 2, 3)))
        assertEquals("*1234*20", body(CommandBuilder.build("1234", req(CommandKind.OUTPUT_OFF, 2), 2, 3)))
        assertTrue(CommandBuilder.build("123", req(CommandKind.ARM), 2, 3) is BuildResult.Err)
    }

    @Test
    fun statusQueryIsStar01AndPersianPasswordIsFolded() {
        assertEquals("*1234*01", body(CommandBuilder.build("۱۲۳۴", req(CommandKind.QUERY_IO), 2, 3)))
        assertEquals("*1234*00", body(CommandBuilder.build("1234", req(CommandKind.STOP_DIAL), 2, 3)))
    }

    @Test
    fun panelQueryIsOptionalAndStarSuffixIsConfigurable() {
        assertEquals("*1234*22", body(CommandBuilder.build("1234", req(CommandKind.QUERY_PANEL), 2, 3)))
        assertEquals("*1234*22*", body(CommandBuilder.build("1234", req(CommandKind.QUERY_PANEL, star = true), 2, 3)))
    }

    @Test
    fun chargeTemplatesStaySelectable() {
        val code = "123456789012"
        assertEquals("*1234*80", body(CommandBuilder.build("1234", req(CommandKind.QUERY_CREDIT), 2, 3)))
        assertEquals("*1234*81*141*1#", body(CommandBuilder.build("1234", req(CommandKind.STORE_USSD, extra = "*141*1#"), 2, 3)))
        assertEquals("*1234*99*141*$code#", body(CommandBuilder.build("1234", req(CommandKind.RECHARGE, extra = code, variant = "irancell_hash"), 2, 3)))
        assertEquals("*1234*99*141*$code*", body(CommandBuilder.build("1234", req(CommandKind.RECHARGE, extra = code, variant = "irancell_star"), 2, 3)))
        assertEquals("*1234*99*140*#$code#", body(CommandBuilder.build("1234", req(CommandKind.RECHARGE, extra = code, variant = "mci_hash"), 2, 3)))
        assertEquals("*1234*99*140*#$code*", body(CommandBuilder.build("1234", req(CommandKind.RECHARGE, extra = code, variant = "mci_star"), 2, 3)))
        assertTrue(CommandBuilder.build("1234", req(CommandKind.RECHARGE, extra = code, variant = "unknown"), 2, 3) is BuildResult.Err)
    }

    @Test
    fun maskPasswordAndLongDigitRuns() {
        val masked = CommandBuilder.mask("*1234*99*141*123456789012#", "1234")
        assertFalse(masked.contains("1234"))
        assertFalse(masked.contains("123456789012"))
        assertTrue(masked.contains("****"))
    }

    @Test
    fun alarmSmsDoesNotConfirmArm() {
        val parsed = SmsParser.parse("هشدار! آژیر دزدگیر به صدا در آمده است.")
        assertEquals(SirenState.SOUNDING, parsed.siren)
        assertEquals(SmsKind.TRIGGER, parsed.kind)
        assertEquals(ReplyMatch.UNSOLICITED, CommandPolicy.match(CommandKind.ARM, parsed, 2))
        assertEquals(LinkPhase.WAITING, CommandPolicy.phase(1_000, 2_000, 90, ReplyMatch.UNSOLICITED))
    }

    @Test
    fun zoneRangeAndContinuation() {
        val parsed = SmsParser.parse("وضعیت g1 اولترا تحریک زون ۱-۲…")
        assertEquals(setOf(1, 2), parsed.zonesTriggered)
        assertTrue(parsed.continuation)
        assertEquals(SmsKind.STATUS, parsed.kind)
        assertNull(parsed.arm)
    }

    @Test
    fun outputWordsDoNotArmThePanel() {
        val parsed = SmsParser.parse("وضعیت جی ۱ اولترا خروجی ۱ خاموش")
        assertEquals(mapOf(1 to false), parsed.outputs)
        assertNull(parsed.arm)
        assertEquals(ReplyMatch.CONFIRMED, CommandPolicy.match(CommandKind.QUERY_IO, parsed, null))
        assertEquals(ReplyMatch.UNVERIFIED, CommandPolicy.match(CommandKind.ARM, parsed, 2))
    }

    @Test
    fun outputOnReplyConfirmsOnlyThatOutput() {
        val parsed = SmsParser.parse("خروجی 2 فعال شد")
        assertEquals(true, parsed.outputs[2])
        assertNull(parsed.arm)
        assertEquals(ReplyMatch.CONFIRMED, CommandPolicy.match(CommandKind.ARM, parsed, 2))
        assertEquals(ReplyMatch.UNVERIFIED, CommandPolicy.match(CommandKind.ARM, parsed, 3))
        assertEquals(
            "G1 اجرای خروجی فرمان را تأیید کرد. وضعیت خود پنل تا دریافت گزارش واقعی نامشخص می‌ماند.",
            CommandPolicy.userMessage(LinkPhase.CONFIRMED, CommandKind.ARM)
        )
    }

    @Test
    fun powerBatteryAndPhoneCutStayUnsolicited() {
        val power = SmsParser.parse("قطع برق شهر")
        assertEquals(PowerState.MAINS_OFF, power.power)
        assertNull(power.arm)
        assertEquals(ReplyMatch.UNSOLICITED, CommandPolicy.match(CommandKind.ARM, power, 2))

        val battery = SmsParser.parse("باتری دستگاه ضعیف است")
        assertTrue(battery.batteryMention?.contains("باتری") == true)
        assertNull(battery.arm)

        val phone = SmsParser.parse("هشدار تلفن قطع شد")
        assertTrue(phone.phoneLineCut)
        assertEquals(ReplyMatch.UNSOLICITED, CommandPolicy.match(CommandKind.QUERY_IO, phone, null))
    }

    @Test
    fun invalidResponseAndTimeoutAreUnconfirmed() {
        val parsed = SmsParser.parse("سلام، این پیام شخصی است")
        assertEquals(SmsKind.OTHER, parsed.kind)
        assertEquals(ReplyMatch.UNVERIFIED, CommandPolicy.match(CommandKind.QUERY_IO, parsed, null))
        assertEquals(LinkPhase.UNCONFIRMED, CommandPolicy.phase(0, 1_000, 90, ReplyMatch.UNVERIFIED))
        assertEquals(LinkPhase.UNCONFIRMED, CommandPolicy.phase(1_000, 91_000, 90, null))
        assertEquals(LinkPhase.WAITING, CommandPolicy.phase(1_000, 2_000, 90, null))
        assertEquals("پاسخی که فرمان را تأیید کند دریافت نشد.", CommandPolicy.userMessage(LinkPhase.UNCONFIRMED, CommandKind.ARM))
        assertEquals("فرمان توسط دستگاه تأیید نشد.", CommandPolicy.userMessage(LinkPhase.REJECTED, CommandKind.ARM))
    }

    @Test
    fun rejectedPasswordDoesNotLookLikeSuccess() {
        val parsed = SmsParser.parse("رمز اشتباه")
        assertTrue(parsed.rejected)
        assertEquals(ReplyMatch.REJECTED, CommandPolicy.match(CommandKind.ARM, parsed, 2))
        assertEquals(LinkPhase.REJECTED, CommandPolicy.phase(1_000, 2_000, 90, ReplyMatch.REJECTED))
    }

    @Test
    fun permissionDeniedIsExplicit() {
        assertEquals(LinkPhase.PERMISSION_DENIED, CommandPolicy.gate(hasSendPermission = false, developer = false))
        assertNull(CommandPolicy.gate(hasSendPermission = false, developer = true))
        assertNull(CommandPolicy.gate(hasSendPermission = true, developer = false))
    }

    @Test
    fun twoDevicesDoNotShareAPhoneKey() {
        val home = PhoneNumbers.key("+98 912 000 0000")
        val shop = PhoneNumbers.key("09351234567")
        assertEquals("9120000000", home)
        assertEquals("9351234567", shop)
        assertTrue(PhoneNumbers.same("09120000000", "00989120000000"))
        assertFalse(PhoneNumbers.same("09120000000", "09351234567"))
        assertEquals("09120000000", PhoneNumbers.display("۹۱۲۰۰۰۰۰۰۰"))
    }

    @Test
    fun jalaliDates() {
        assertEquals(Jalali.Date(1400, 1, 1), Jalali.fromGregorian(2021, 3, 21))
        assertEquals(Jalali.Date(1403, 1, 1), Jalali.fromGregorian(2024, 3, 20))
        assertEquals(Jalali.Date(1357, 11, 22), Jalali.fromGregorian(1979, 2, 11))
        assertEquals(Jalali.Date(1405, 7, 2), Jalali.fromGregorian(2026, 9, 24))
        assertEquals(Jalali.Date(1404, 1, 1), Jalali.fromGregorian(2025, 3, 21))
    }

    @Test
    fun pinHashIsNotThePin() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("2468", salt)
        assertTrue(PinHasher.verify("2468", salt, hash))
        assertFalse(PinHasher.verify("2469", salt, hash))
        assertFalse(hash.contentEquals("2468".toByteArray()))
    }

    @Test
    fun partSetIsNotACommand() {
        assertFalse(CommandKind.entries.any { it.name == "PART_SET" || it.name == "HOME" })
        val parsed = SmsParser.parse("سیستم نیمه‌فعال شد")
        assertEquals(ArmState.PART_SET, parsed.arm)
    }
}
