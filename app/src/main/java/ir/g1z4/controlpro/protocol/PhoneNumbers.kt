package ir.g1z4.controlpro.protocol

object PhoneNumbers {
    fun key(raw: String): String? {
        val digits = Digits.ascii(raw).filter { it in '0'..'9' }
        val national = when {
            digits.startsWith("0098") && digits.length >= 14 -> digits.removePrefix("0098")
            digits.startsWith("98") && digits.length >= 12 -> digits.removePrefix("98")
            digits.startsWith("0") && digits.length >= 11 -> digits.removePrefix("0")
            else -> digits
        }
        if (national.length != 10 || !national.startsWith("9")) return null
        return national
    }

    fun display(raw: String): String {
        val k = key(raw) ?: return raw.trim()
        return "0$k"
    }

    fun same(a: String, b: String): Boolean {
        val ka = key(a) ?: return false
        val kb = key(b) ?: return false
        return ka == kb
    }
}
