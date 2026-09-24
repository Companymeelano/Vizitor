package ir.g1z4.controlpro.protocol

import ir.g1z4.controlpro.domain.CommandKind
import ir.g1z4.controlpro.domain.CommandRequest

sealed class BuildResult {
    data class Ok(val body: String, val summaryFa: String) : BuildResult()
    data class Err(val messageFa: String) : BuildResult()
}

object CommandBuilder {
    private val password = Regex("\\d{4}")
    private val ascii = Regex("[\\x20-\\x7E]+")

    fun build(passwordRaw: String, request: CommandRequest, armOutput: Int, disarmOutput: Int): BuildResult {
        val pwd = Digits.ascii(passwordRaw).trim()
        if (!password.matches(pwd)) {
            return BuildResult.Err("رمز تلفن‌کننده باید چهار رقم انگلیسی باشد.")
        }
        return when (request.kind) {
            CommandKind.STOP_DIAL -> ok("*$pwd*00", "توقف شماره‌گیری")
            CommandKind.QUERY_IO -> ok("*$pwd*01", "استعلام ورودی و خروجی")
            CommandKind.QUERY_PANEL -> {
                val star = if (request.panelStar) "*" else ""
                ok("*$pwd*22$star", "استعلام اختیاری پنل")
            }
            CommandKind.OUTPUT_ON -> output(pwd, request.output, true)
            CommandKind.OUTPUT_OFF -> output(pwd, request.output, false)
            CommandKind.ARM -> output(pwd, armOutput, true, "فعال‌سازی از خروجی $armOutput")
            CommandKind.DISARM -> output(pwd, disarmOutput, true, "غیرفعال‌سازی از خروجی $disarmOutput")
            CommandKind.QUERY_CREDIT -> ok("*$pwd*80", "درخواست مقدار شارژ")
            CommandKind.STORE_USSD -> storeUssd(pwd, request.ussd)
            CommandKind.RECHARGE -> recharge(pwd, request.chargeCode, request.rechargeVariant)
        }
    }

    fun mask(body: String, password: String): String {
        var masked = if (password.isNotBlank()) body.replace(password, "****") else body
        masked = Regex("\\d{8,}").replace(masked) { "*".repeat(it.value.length.coerceAtMost(16)) }
        return masked
    }

    private fun output(pwd: String, number: Int?, on: Boolean, summary: String? = null): BuildResult {
        if (number == null || number !in 1..4) return BuildResult.Err("شماره خروجی معتبر نیست.")
        val flag = if (on) "1" else "0"
        val verb = if (on) "فعال کردن" else "غیرفعال کردن"
        return ok("*$pwd*$number$flag", summary ?: "$verb خروجی $number")
    }

    private fun storeUssd(pwd: String, ussd: String?): BuildResult {
        val formula = ussd?.trim().orEmpty()
        if (formula.isEmpty() || !ascii.matches(formula) || formula.any { it !in "*#0123456789" }) {
            return BuildResult.Err("فرمول شارژ باید فقط با نویسه‌های انگلیسی * # و رقم نوشته شود.")
        }
        val body = if (formula.startsWith("*")) "*$pwd*81$formula" else "*$pwd*81*$formula"
        return ok(body, "ذخیره فرمول موجودی")
    }

    private fun recharge(pwd: String, code: String?, variant: String): BuildResult {
        val digits = Digits.ascii(code.orEmpty()).filter { it in '0'..'9' }
        if (digits.length !in 8..20) return BuildResult.Err("کد شارژ معتبر نیست.")
        val body = when (variant) {
            "irancell_hash" -> "*$pwd*99*141*$digits#"
            "irancell_star" -> "*$pwd*99*141*$digits*"
            "mci_hash" -> "*$pwd*99*140*#$digits#"
            "mci_star" -> "*$pwd*99*140*#$digits*"
            else -> return BuildResult.Err("فرمول افزایش اعتبار انتخاب نشده است.")
        }
        return ok(body, "افزایش اعتبار سیم‌کارت")
    }

    private fun ok(body: String, summary: String): BuildResult {
        if (!ascii.matches(body)) return BuildResult.Err("متن فرمان باید انگلیسی باشد.")
        return BuildResult.Ok(body, summary)
    }
}
