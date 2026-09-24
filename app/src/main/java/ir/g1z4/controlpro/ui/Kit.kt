package ir.g1z4.controlpro.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.g1z4.controlpro.R
import ir.g1z4.controlpro.ui.theme.LocalPalette
import ir.g1z4.controlpro.ui.theme.Vazir

val LocalBiometric = staticCompositionLocalOf<(String, () -> Unit) -> Unit> { { _, ok -> ok() } }

@Composable
fun Backdrop(content: @Composable () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(p.bg, p.bg2, p.bg)))
    ) {
        Box(
            Modifier
                .size(280.dp)
                .align(Alignment.TopCenter)
                .background(Brush.radialGradient(listOf(p.glow, Color.Transparent)))
        )
        content()
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val p = LocalPalette.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(p.surface)
            .border(1.dp, p.stroke, RoundedCornerShape(28.dp))
            .padding(18.dp)
    ) { content() }
}

@Composable
fun Medallion(icon: Int, desc: String, size: Dp = 64.dp, pulse: Boolean = false) {
    val scale by rememberInfiniteTransition(label = "p").animateFloat(
        1f, if (pulse) 1.06f else 1f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "s"
    )
    Box(
        Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(Color(0xFF0B1020))
            .border(1.dp, Color(0x55E0C48A), CircleShape)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center
    ) {
        Image(painterResource(icon), desc, Modifier.fillMaxSize().padding(6.dp), contentScale = ContentScale.Crop)
    }
}

@Composable
fun SectionTitle(text: String) {
    val p = LocalPalette.current
    Text(text, color = p.gold, fontFamily = Vazir, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
fun Body(text: String, muted: Boolean = false) {
    val p = LocalPalette.current
    Text(text, color = if (muted) p.muted else p.ink, fontFamily = Vazir, fontSize = 14.sp, lineHeight = 22.sp)
}

@Composable
fun Headline(text: String) {
    val p = LocalPalette.current
    Text(text, color = p.ink, fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 26.sp)
}

@Composable
fun PremiumButton(text: String, desc: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, danger: Boolean = false) {
    val p = LocalPalette.current
    val bg = when {
        !enabled -> p.unknown.copy(alpha = 0.35f)
        danger -> p.alarm
        else -> p.gold
    }
    val fg = if (danger) Color.White else if (p.dark) Color(0xFF1A1408) else Color.White
    Box(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .semantics { contentDescription = desc }
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = fg, fontFamily = Vazir, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun Unsupported(text: String) {
    val p = LocalPalette.current
    Text("پشتیبانی نمی‌شود", color = p.warn, fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    Spacer(Modifier.height(4.dp))
    Body(text, muted = true)
}

@Composable
fun StatusDot(color: Color, label: String) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, color = p.ink, fontFamily = Vazir, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

object Icons3d {
    val shield = R.drawable.ic_shield
    val lock = R.drawable.ic_lock
    val unlock = R.drawable.ic_unlock
    val siren = R.drawable.ic_siren
    val home = R.drawable.ic_home
    val signal = R.drawable.ic_signal
    val device = R.drawable.ic_device
    val zone = R.drawable.ic_zone
    val battery = R.drawable.ic_battery
    val power = R.drawable.ic_power
    val bell = R.drawable.ic_bell
    val settings = R.drawable.ic_settings
    val reports = R.drawable.ic_reports
    val user = R.drawable.ic_user
    val output = R.drawable.ic_output
}
