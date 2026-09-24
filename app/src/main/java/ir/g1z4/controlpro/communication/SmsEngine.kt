package ir.g1z4.controlpro.communication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.g1z4.controlpro.BuildConfig
import ir.g1z4.controlpro.data.db.AlertEntity
import ir.g1z4.controlpro.data.db.AppDatabase
import ir.g1z4.controlpro.data.db.CommandEntity
import ir.g1z4.controlpro.data.db.EventEntity
import ir.g1z4.controlpro.data.repo.PanelRepository
import ir.g1z4.controlpro.data.repo.toDomain
import ir.g1z4.controlpro.data.repo.toEntity
import ir.g1z4.controlpro.data.settings.SettingsStore
import ir.g1z4.controlpro.domain.ArmState
import ir.g1z4.controlpro.domain.CommandKind
import ir.g1z4.controlpro.domain.CommandRequest
import ir.g1z4.controlpro.domain.LinkPhase
import ir.g1z4.controlpro.domain.PowerState
import ir.g1z4.controlpro.domain.SirenState
import ir.g1z4.controlpro.domain.ZoneState
import ir.g1z4.controlpro.protocol.BuildResult
import ir.g1z4.controlpro.protocol.CommandBuilder
import ir.g1z4.controlpro.protocol.CommandPolicy
import ir.g1z4.controlpro.protocol.PhoneNumbers
import ir.g1z4.controlpro.protocol.ReplyMatch
import ir.g1z4.controlpro.protocol.SmsKind
import ir.g1z4.controlpro.protocol.SmsParser
import ir.g1z4.controlpro.sound.TonePlayer
import ir.g1z4.controlpro.widget.StatusWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface SmsSender {
    fun send(subscriptionId: Int?, destination: String, body: String)
}

@Singleton
class AndroidSmsSender @Inject constructor(
    @ApplicationContext private val context: Context
) : SmsSender {
    override fun send(subscriptionId: Int?, destination: String, body: String) {
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY).not() &&
            context.getSystemService(TelephonyManager::class.java)?.phoneType == TelephonyManager.PHONE_TYPE_NONE
        ) {
            // Still attempt; some devices report telephony oddly.
        }
        val manager = if (subscriptionId != null && subscriptionId >= 0) {
            SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
        } else if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        val parts = manager.divideMessage(body)
        if (parts.size <= 1) {
            manager.sendTextMessage(destination, null, body, null, null)
        } else {
            manager.sendMultipartTextMessage(destination, null, parts, null, null)
        }
    }
}

@Singleton
class SmsEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val repo: PanelRepository,
    private val settings: SettingsStore,
    private val sender: SmsSender,
    private val tones: TonePlayer,
    private val notifier: AlertNotifier
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    fun enqueue(deviceId: String, request: CommandRequest) {
        scope.launch { enqueueInternal(deviceId, request) }
    }

    fun onIncoming(address: String, body: String, at: Long, done: () -> Unit = {}) {
        scope.launch {
            try {
                ingest(address, body, at, synthetic = false)
            } finally {
                done()
            }
        }
    }

    fun scanInbox(deviceId: String) {
        scope.launch { scan(deviceId) }
    }

    fun expireStale() {
        scope.launch { expire() }
    }

    private suspend fun enqueueInternal(deviceId: String, request: CommandRequest) {
        val device = repo.device(deviceId) ?: return
        val password = repo.password(deviceId)
        if (device.phoneKey.isBlank()) {
            fail(deviceId, request, "شماره دستگاه تنظیم نشده است.")
            return
        }
        if (password == null) {
            fail(deviceId, request, "رمز دستگاه در حافظه امن پیدا نشد.")
            return
        }
        val built = CommandBuilder.build(password, request, device.armOutput, device.disarmOutput)
        if (built is BuildResult.Err) {
            fail(deviceId, request, built.messageFa)
            return
        }
        val ok = built as BuildResult.Ok
        val developer = settings.settings.first().developer && BuildConfig.DEBUG
        val id = UUID.randomUUID().toString()
        db.commands().upsert(
            CommandEntity(
                id, deviceId, request.kind.name, ok.summaryFa, LinkPhase.QUEUED.name,
                null, null, null, null, developer, request.output ?: if (request.kind == CommandKind.ARM) device.armOutput else if (request.kind == CommandKind.DISARM) device.disarmOutput else null
            )
        )
        process(id, deviceId, ok.body, password, request, developer)
    }

    private suspend fun process(id: String, deviceId: String, body: String, password: String, request: CommandRequest, developer: Boolean) {
        val device = repo.device(deviceId) ?: return
        mutex.withLock {
            val current = db.commands().byId(id) ?: return
            if (CommandPolicy.gate(context.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED, developer) == LinkPhase.PERMISSION_DENIED) {
                update(current.copy(phase = LinkPhase.PERMISSION_DENIED.name, errorFa = "دسترسی ارسال پیامک فعال نیست.", finishedAt = System.currentTimeMillis()))
                touchLink(deviceId, LinkPhase.PERMISSION_DENIED, "دسترسی ارسال پیامک فعال نیست.")
                return
            }
            update(current.copy(phase = LinkPhase.SENDING.name))
            touchLink(deviceId, LinkPhase.SENDING, "در حال ارسال")
            try {
                if (developer) {
                    delay(900)
                } else {
                    sender.send(device.subscriptionId, device.phoneRaw, body)
                }
            } catch (ex: Exception) {
                update(current.copy(phase = LinkPhase.FAILED.name, errorFa = "ارسال فرمان انجام نشد.", finishedAt = System.currentTimeMillis()))
                touchLink(deviceId, LinkPhase.FAILED, "ارسال فرمان انجام نشد.")
                tones.play(TonePlayer.Kind.ERROR)
                return
            }
            val sentAt = System.currentTimeMillis()
            update(current.copy(phase = LinkPhase.WAITING.name, sentAt = sentAt))
            touchLink(deviceId, LinkPhase.WAITING, "در انتظار پاسخ")
            tones.play(TonePlayer.Kind.SENT)
            if (developer) {
                delay(700)
                val mock = mockBody(request, device.armOutput, device.disarmOutput)
                ingest(device.phoneRaw, mock, System.currentTimeMillis(), synthetic = true, forcedCommandId = id)
            } else {
                delay(4_000)
                scan(deviceId)
                if (request.kind == CommandKind.QUERY_IO || request.kind == CommandKind.QUERY_CREDIT || request.kind == CommandKind.QUERY_PANEL) {
                    if (device.queryRetry) {
                        val still = findCommand(id)
                        if (still?.phase == LinkPhase.WAITING.name) {
                            delay(device.timeoutSec * 500L)
                            scan(deviceId)
                        }
                    }
                }
            }
            expire()
        }
    }

    private fun mockBody(request: CommandRequest, arm: Int, disarm: Int): String = when (request.kind) {
        CommandKind.ARM -> "آزمایشی: خروجی $arm فعال شد. این پیام سخت‌افزار نیست."
        CommandKind.DISARM -> "آزمایشی: خروجی $disarm فعال شد. این پیام سخت‌افزار نیست."
        CommandKind.OUTPUT_ON -> "آزمایشی: خروجی ${request.output} فعال شد. این پیام سخت‌افزار نیست."
        CommandKind.OUTPUT_OFF -> "آزمایشی: خروجی ${request.output} خاموش. این پیام سخت‌افزار نیست."
        CommandKind.QUERY_IO -> "وضعیت آزمایشی g1 — خروجی 1 خاموش. این پیام سخت‌افزار نیست."
        CommandKind.QUERY_CREDIT -> "آزمایشی: مقدار شارژ از اپراتور دریافت نشد."
        CommandKind.QUERY_PANEL -> "وضعیت آزمایشی. این پیام سخت‌افزار نیست."
        else -> "آزمایشی: پاسخی که فرمان را تأیید کند ساخته نشد."
    }

    private suspend fun ingest(address: String, body: String, at: Long, synthetic: Boolean, forcedCommandId: String? = null) {
        val key = PhoneNumbers.key(address) ?: return
        val device = db.devices().byPhone(key)?.toDomain() ?: return
        val parsed = SmsParser.parse(body)
        val masked = CommandBuilder.mask(body, repo.password(device.id).orEmpty())
        if (!synthetic && db.events().duplicates(device.id, masked, at) > 0) return
        val waiting = forcedCommandId?.let { findCommand(it) } ?: db.commands().waiting(device.id)
        var consumed = false
        if (waiting != null && (waiting.sentAt == null || at + 5_000 >= (waiting.sentAt ?: 0L))) {
            val kind = CommandKind.valueOf(waiting.kind)
            val match = CommandPolicy.match(kind, parsed, waiting.outputNumber)
            if (match != ReplyMatch.UNSOLICITED || forcedCommandId != null) {
                val phase = when (match) {
                    ReplyMatch.CONFIRMED -> LinkPhase.CONFIRMED
                    ReplyMatch.REJECTED -> LinkPhase.REJECTED
                    ReplyMatch.UNVERIFIED -> LinkPhase.UNCONFIRMED
                    ReplyMatch.UNSOLICITED -> LinkPhase.WAITING
                }
                if (phase != LinkPhase.WAITING) {
                    val detail = CommandPolicy.userMessage(phase, kind).let { if (synthetic) "آزمایشی: $it" else it }
                    update(waiting.copy(phase = phase.name, responseMasked = masked, finishedAt = at, errorFa = if (phase == LinkPhase.CONFIRMED) null else detail))
                    touchLink(device.id, phase, detail)
                    tones.play(if (phase == LinkPhase.CONFIRMED) TonePlayer.Kind.SUCCESS else TonePlayer.Kind.ERROR)
                    consumed = true
                }
            }
        }
        if (!synthetic) applyParsed(device.id, parsed, at, masked, synthetic)
        if (!consumed || parsed.kind == SmsKind.TRIGGER || parsed.kind == SmsKind.REPORT || parsed.kind == SmsKind.STATUS) {
            // events always stored
        }
        val title = titleFor(parsed, synthetic)
        db.events().insert(
            EventEntity(UUID.randomUUID().toString(), device.id, at, parsed.severity.name, title, parsed.raw.take(180), masked, synthetic, parsed.kind.name)
        )
        maybeNotify(device.id, device.name, parsed, title, synthetic)
        StatusWidget.refresh(context)
    }

    private suspend fun applyParsed(deviceId: String, parsed: ir.g1z4.controlpro.protocol.ParsedSms, at: Long, masked: String, synthetic: Boolean) {
        val current = db.snapshots().get(deviceId)?.toDomain() ?: ir.g1z4.controlpro.domain.Snapshot.unknown(deviceId)
        var next = current.copy(lastIncomingAt = at, updatedAt = at)
        if (!synthetic) next = next.copy(lastResponseAt = at)
        parsed.arm?.let { next = next.copy(arm = it, armAt = at) }
        parsed.power?.let { next = next.copy(power = it, powerAt = at) }
        parsed.siren?.let { next = next.copy(siren = it, sirenAt = at) }
        if (parsed.kind == SmsKind.CREDIT) next = next.copy(creditRaw = parsed.raw.take(160), creditAt = at)
        if (parsed.phoneLineCut) next = next.copy(phoneLineCut = true)
        if (parsed.jammer) next = next.copy(jammer = true)
        parsed.batteryMention?.let { next = next.copy(batteryMention = it) }
        db.snapshots().upsert(next.toEntity())
        if (parsed.zonesTriggered.isNotEmpty()) {
            val zones = db.zones().observe(deviceId)
            parsed.zonesTriggered.forEach { number ->
                val wired = db.zones().one(deviceId, "WIRED-$number")
                val wireless = db.zones().one(deviceId, "WIRELESS-$number")
                val expander = db.zones().one(deviceId, "EXPANDER-$number")
                listOfNotNull(wired, wireless, expander).forEach { row ->
                    db.zones().upsert(listOf(row.copy(state = ZoneState.TRIGGERED.name, lastTriggerAt = at, lastRaw = masked.take(120))))
                }
            }
        }
        if (parsed.outputs.isNotEmpty()) {
            val rows = db.outputs().list(deviceId)
            val updated = rows.map { row ->
                val value = parsed.outputs[row.number]
                if (value == null) row else row.copy(stateKnown = true, on = value, lastAt = at)
            }
            db.outputs().upsert(updated)
        }
        parsed.remoteSlot?.let { slot ->
            val row = db.remotes().list(deviceId).firstOrNull { it.slot == slot } ?: return@let
            db.remotes().upsert(listOf(row.copy(lastSeenAt = at)))
        }
    }

    private suspend fun maybeNotify(deviceId: String, name: String, parsed: ir.g1z4.controlpro.protocol.ParsedSms, title: String, synthetic: Boolean) {
        val s = settings.settings.first()
        val allow = when {
            parsed.siren == SirenState.SOUNDING || parsed.kind == SmsKind.TRIGGER -> s.notifyAlarm
            parsed.zonesTriggered.isNotEmpty() -> s.notifyZone
            parsed.power != null -> s.notifyPower
            parsed.batteryMention != null -> s.notifyBattery
            parsed.phoneLineCut || parsed.jammer -> s.notifyConnection
            parsed.arm != null -> s.notifyArm
            else -> false
        }
        if (!allow) return
        val shown = if (synthetic) "آزمایشی: $title" else title
        db.alerts().insert(AlertEntity(UUID.randomUUID().toString(), deviceId, System.currentTimeMillis(), parsed.severity.name, shown, name, false, synthetic))
        notifier.show(shown, name, parsed.severity, synthetic)
        if (parsed.severity.name == "CRITICAL") tones.play(TonePlayer.Kind.ALERT)
    }

    private fun titleFor(parsed: ir.g1z4.controlpro.protocol.ParsedSms, synthetic: Boolean): String {
        val base = when {
            parsed.rejected -> "فرمان رد شد"
            parsed.phoneLineCut -> "هشدار قطع خط تلفن"
            parsed.jammer -> "هشدار اختلال شبکه"
            parsed.siren == SirenState.SOUNDING -> "آژیر"
            parsed.power == PowerState.MAINS_OFF -> "قطع برق"
            parsed.power == PowerState.MAINS_ON -> "وصل برق"
            parsed.arm == ArmState.ARMED -> "گزارش فعال شدن"
            parsed.arm == ArmState.DISARMED -> "گزارش غیرفعال شدن"
            parsed.arm == ArmState.PART_SET -> "گزارش نیمه‌فعال"
            parsed.zonesTriggered.isNotEmpty() -> "تحریک زون"
            parsed.outputs.isNotEmpty() -> "وضعیت خروجی"
            parsed.kind == SmsKind.CREDIT -> "پاسخ شارژ"
            parsed.kind == SmsKind.STATUS -> "وضعیت دستگاه"
            else -> "پیامک دستگاه"
        }
        return if (synthetic) "آزمایشی · $base" else base
    }

    private suspend fun scan(deviceId: String) {
        if (context.checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) return
        val device = repo.device(deviceId) ?: return
        val since = (db.snapshots().get(deviceId)?.lastIncomingAt ?: (System.currentTimeMillis() - 15 * 60_000L)) - 5_000
        val cursor = context.contentResolver.query(
            android.provider.Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(android.provider.Telephony.Sms.ADDRESS, android.provider.Telephony.Sms.BODY, android.provider.Telephony.Sms.DATE),
            "date >= ?",
            arrayOf(since.toString()),
            "date ASC"
        ) ?: return
        cursor.use {
            val a = it.getColumnIndex(android.provider.Telephony.Sms.ADDRESS)
            val b = it.getColumnIndex(android.provider.Telephony.Sms.BODY)
            val d = it.getColumnIndex(android.provider.Telephony.Sms.DATE)
            while (it.moveToNext()) {
                val address = it.getString(a) ?: continue
                if (!PhoneNumbers.same(address, device.phoneRaw)) continue
                val body = it.getString(b) ?: continue
                val at = it.getLong(d)
                ingest(address, body, at, synthetic = false)
            }
        }
    }

    private suspend fun expire() {
        val now = System.currentTimeMillis()
        db.commands().allWaiting().forEach { command ->
            val device = repo.device(command.deviceId) ?: return@forEach
            val sent = command.sentAt ?: return@forEach
            if (now - sent >= device.timeoutSec * 1000L && command.phase == LinkPhase.WAITING.name) {
                update(command.copy(phase = LinkPhase.UNCONFIRMED.name, finishedAt = now, errorFa = "پاسخی از دستگاه دریافت نشد."))
                touchLink(command.deviceId, LinkPhase.UNCONFIRMED, "پاسخی از دستگاه دریافت نشد.")
            }
        }
    }

    private suspend fun fail(deviceId: String, request: CommandRequest, message: String) {
        db.commands().upsert(
            CommandEntity(UUID.randomUUID().toString(), deviceId, request.kind.name, request.kind.name, LinkPhase.FAILED.name, null, System.currentTimeMillis(), null, message, false, request.output)
        )
        touchLink(deviceId, LinkPhase.FAILED, message)
    }

    private suspend fun touchLink(deviceId: String, phase: LinkPhase, detail: String) {
        val current = db.snapshots().get(deviceId)?.toDomain() ?: ir.g1z4.controlpro.domain.Snapshot.unknown(deviceId)
        db.snapshots().upsert(current.copy(link = phase, linkDetail = detail, updatedAt = System.currentTimeMillis()).toEntity())
        StatusWidget.refresh(context)
    }

    private suspend fun findCommand(id: String): CommandEntity? = db.commands().byId(id)

    private suspend fun update(entity: CommandEntity) = db.commands().upsert(entity)
}
