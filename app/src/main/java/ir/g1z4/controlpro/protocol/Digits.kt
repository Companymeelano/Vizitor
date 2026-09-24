package ir.g1z4.controlpro.protocol

object Digits {
    fun ascii(raw: String): String = buildString(raw.length) {
        raw.forEach { ch ->
            append(
                when (ch) {
                    in '۰'..'۹' -> '0' + (ch - '۰')
                    in '٠'..'٩' -> '0' + (ch - '٠')
                    else -> ch
                }
            )
        }
    }
}
