package ir.g1z4.controlpro.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.g1z4.controlpro.ui.theme.LocalPalette
import ir.g1z4.controlpro.ui.theme.Vazir

@Composable
fun CommandDeck(
    onArm: () -> Unit,
    onDisarm: () -> Unit,
    onQuery: () -> Unit,
    onOutputs: () -> Unit,
    onAlerts: () -> Unit,
    onSiren: () -> Unit
) {
    val p = LocalPalette.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val deckWidth = maxWidth
        val wide = deckWidth >= 560.dp
        val roomy = deckWidth >= 400.dp
        val tileColumns = if (deckWidth >= 720.dp) 4 else 2
        Column(verticalArrangement = Arrangement.spacedBy(if (roomy) 12.dp else 8.dp)) {
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(132.dp)) {
                    HeroControl(Modifier.weight(1f).fillMaxHeight(), "فعال‌سازی", "پالس خروجی نقشه‌شده", Icons3d.lock, danger = false, onClick = onArm)
                    HeroControl(Modifier.weight(1f).fillMaxHeight(), "غیرفعال‌سازی", "برای قطع آلارم هم همین مسیر است", Icons3d.unlock, danger = true, onClick = onDisarm)
                }
            } else {
                HeroControl(Modifier.fillMaxWidth().height(if (roomy) 92.dp else 84.dp), "فعال‌سازی", "پالس خروجی نقشه‌شده", Icons3d.lock, danger = false, onClick = onArm)
                HeroControl(Modifier.fillMaxWidth().height(if (roomy) 92.dp else 84.dp), "غیرفعال‌سازی", "برای قطع آلارم هم همین مسیر است", Icons3d.unlock, danger = true, onClick = onDisarm)
            }
            Text(
                "نیمه‌فعال فقط با دکمه C ریموت است و از پیامک پشتیبانی نمی‌شود. موفقیت این دکمه‌ها فقط پس از پاسخ واقعی ثبت می‌شود.",
                color = p.muted,
                fontFamily = Vazir,
                fontSize = if (roomy) 12.sp else 11.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            val tiles = listOf(
                Triple("استعلام", Icons3d.signal, onQuery),
                Triple("خروجی‌ها", Icons3d.output, onOutputs),
                Triple("هشدارها", Icons3d.bell, onAlerts),
                Triple("آژیر", Icons3d.siren, onSiren)
            )
            tiles.chunked(tileColumns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().height(if (roomy) 92.dp else 84.dp)) {
                    row.forEach { (label, icon, action) ->
                        ActionTile(Modifier.weight(1f).fillMaxHeight(), label, icon, action)
                    }
                    repeat(tileColumns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun HeroControl(
    modifier: Modifier,
    title: String,
    hint: String,
    icon: Int,
    danger: Boolean,
    onClick: () -> Unit
) {
    val p = LocalPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")
    val shape = RoundedCornerShape(28.dp)
    val brush = if (danger) {
        Brush.linearGradient(listOf(p.alarm, p.alarm.copy(alpha = 0.72f), Color(0xFF7A1024)))
    } else {
        Brush.linearGradient(listOf(p.gold, p.gold.copy(alpha = 0.78f), if (p.dark) Color(0xFF8C6A32) else p.gold))
    }
    val fg = if (danger || !p.dark) Color.White else Color(0xFF1A1408)
    Box(
        modifier
            .scale(scale)
            .shadow(if (pressed) 2.dp else 14.dp, shape, clip = false)
            .clip(shape)
            .background(brush)
            .semantics { contentDescription = title }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(18.dp)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent)))
        )
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.18f))
                    .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(icon), title, Modifier.size(40.dp), contentScale = ContentScale.Crop)
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = fg, fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(hint, color = fg.copy(alpha = 0.82f), fontFamily = Vazir, fontSize = 11.sp, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ActionTile(modifier: Modifier, title: String, icon: Int, onClick: () -> Unit) {
    val p = LocalPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "tile")
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .scale(scale)
            .clip(shape)
            .background(p.surface)
            .border(1.dp, p.stroke, shape)
            .semantics { contentDescription = title }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(painterResource(icon), title, Modifier.size(36.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.height(4.dp))
        Text(title, color = p.ink, fontFamily = Vazir, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
fun AuthorFooter(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val glow by rememberInfiniteTransition(label = "sig").animateFloat(
        0.35f, 0.85f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "g"
    )
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        val narrow = maxWidth < 340.dp
        val nameSize = when {
            maxWidth < 340.dp -> 14.sp
            maxWidth < 420.dp -> 16.sp
            else -> 20.sp
        }
        Row(
            Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .align(Alignment.Center)
                .heightIn(min = if (narrow) 58.dp else 68.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.horizontalGradient(listOf(p.bg2, p.surface, p.bg2)))
                .border(1.dp, p.gold.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MonogramM(if (narrow) 40.dp else 48.dp, glow)
            Column(Modifier.weight(1f)) {
                Text("صاحب اثر", color = p.gold, fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 10.sp, maxLines = 1)
                ExtrudedName("Milad Yaghoobi", nameSize)
            }
        }
    }
}

@Composable
private fun MonogramM(size: Dp, glow: Float) {
    val p = LocalPalette.current
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(p.gold.copy(alpha = 0.22f * glow))
        )
        Image(
            painterResource(ir.g1z4.controlpro.R.drawable.ic_mark),
            "نشان M",
            Modifier
                .fillMaxSize()
                .padding(2.dp)
                .clip(CircleShape)
                .border(1.dp, p.gold.copy(alpha = 0.7f), CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun ExtrudedName(text: String, size: TextUnit) {
    val p = LocalPalette.current
    val depth = if (p.dark) Color(0xFF6A4E1E) else p.gold.copy(alpha = 0.55f)
    Box {
        Text(text, color = depth, fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = size, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp, start = 1.5.dp))
        Text(
            text,
            color = Color.Unspecified,
            fontFamily = Vazir,
            fontWeight = FontWeight.Bold,
            fontSize = size,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = androidx.compose.ui.text.TextStyle(
                brush = Brush.verticalGradient(listOf(Color.White, p.gold, p.gold))
            )
        )
    }
}
