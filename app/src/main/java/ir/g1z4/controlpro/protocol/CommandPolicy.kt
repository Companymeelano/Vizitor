package ir.g1z4.controlpro.protocol

import ir.g1z4.controlpro.domain.CommandKind
import ir.g1z4.controlpro.domain.LinkPhase

enum class ReplyMatch { CONFIRMED, REJECTED, UNSOLICITED, UNVERIFIED }

object CommandPolicy {
    fun match(kind: CommandKind, parsed: ParsedSms, output: Int?): ReplyMatch {
        if (parsed.rejected) return ReplyMatch.REJECTED
        val unsolicited = parsed.kind == SmsKind.TRIGGER ||
            parsed.phoneLineCut ||
            parsed.jammer ||
            (parsed.siren != null && parsed.kind == SmsKind.TRIGGER) ||
            (parsed.kind == SmsKind.REPORT && kind != CommandKind.QUERY_PANEL && kind != CommandKind.QUERY_IO)
        if (unsolicited && parsed.outputs.isEmpty() && parsed.kind != SmsKind.STATUS) return ReplyMatch.UNSOLICITED
        return when (kind) {
            CommandKind.QUERY_IO, CommandKind.QUERY_PANEL -> when {
                parsed.kind == SmsKind.STATUS || parsed.kind == SmsKind.OUTPUT || parsed.outputs.isNotEmpty() -> ReplyMatch.CONFIRMED
                parsed.kind == SmsKind.TRIGGER -> ReplyMatch.UNSOLICITED
                else -> ReplyMatch.UNVERIFIED
            }
            CommandKind.QUERY_CREDIT -> when (parsed.kind) {
                SmsKind.CREDIT -> ReplyMatch.CONFIRMED
                SmsKind.TRIGGER -> ReplyMatch.UNSOLICITED
                else -> ReplyMatch.UNVERIFIED
            }
            CommandKind.OUTPUT_ON, CommandKind.OUTPUT_OFF, CommandKind.ARM, CommandKind.DISARM -> {
                val expected = output
                if (expected != null && parsed.outputs.containsKey(expected)) ReplyMatch.CONFIRMED
                else if (parsed.kind == SmsKind.OUTPUT || parsed.kind == SmsKind.STATUS) ReplyMatch.UNVERIFIED
                else ReplyMatch.UNVERIFIED
            }
            CommandKind.STOP_DIAL, CommandKind.STORE_USSD, CommandKind.RECHARGE ->
                if (parsed.kind == SmsKind.TRIGGER) ReplyMatch.UNSOLICITED else ReplyMatch.UNVERIFIED
        }
    }

    fun phase(sentAt: Long?, now: Long, timeoutSec: Int, match: ReplyMatch?): LinkPhase {
        if (match == ReplyMatch.CONFIRMED) return LinkPhase.CONFIRMED
        if (match == ReplyMatch.REJECTED) return LinkPhase.REJECTED
        if (match == ReplyMatch.UNVERIFIED) return LinkPhase.UNCONFIRMED
        if (sentAt == null) return LinkPhase.QUEUED
        return if (now - sentAt >= timeoutSec * 1000L) LinkPhase.UNCONFIRMED else LinkPhase.WAITING
    }

    fun mayInferPanelArmed(match: ReplyMatch): Boolean = false

    fun gate(hasSendPermission: Boolean, developer: Boolean): LinkPhase? =
        if (!hasSendPermission && !developer) LinkPhase.PERMISSION_DENIED else null

    fun userMessage(phase: LinkPhase, kind: CommandKind): String = when (phase) {
        LinkPhase.CONFIRMED -> when (kind) {
            CommandKind.ARM, CommandKind.DISARM -> "G1 اجرای خروجی فرمان را تأیید کرد. وضعیت خود پنل تا دریافت گزارش واقعی نامشخص می‌ماند."
            CommandKind.QUERY_IO, CommandKind.QUERY_PANEL -> "پاسخ وضعیت دریافت شد."
            CommandKind.QUERY_CREDIT -> "پاسخ شارژ دریافت شد."
            else -> "پاسخ دستگاه دریافت شد."
        }
        LinkPhase.REJECTED -> "فرمان توسط دستگاه تأیید نشد."
        LinkPhase.UNCONFIRMED -> "پاسخی که فرمان را تأیید کند دریافت نشد."
        LinkPhase.FAILED -> "ارسال فرمان انجام نشد."
        LinkPhase.PERMISSION_DENIED -> "دسترسی ارسال پیامک فعال نیست."
        LinkPhase.WAITING, LinkPhase.SENDING, LinkPhase.QUEUED -> "در انتظار پاسخ"
        LinkPhase.IDLE -> "هنوز فرمانی ارسال نشده است."
    }
}
