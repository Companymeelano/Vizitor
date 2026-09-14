/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | تب ۴: گشت‌زنی (CRM & Routing)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  لیست مشتریان با نشانگر وضعیت اعتباری (سبز/قرمز)، تماس، مسیریابی
 *  شهری از طریق API نقشه‌ها و بهینه‌سازی مسیر توزیع بر اساس فاصله
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import ir.atiran.vizitor.ui.components.rememberVoiceSearch
import ir.atiran.vizitor.ui.components.ShimmerGoldText
import ir.atiran.vizitor.ui.components.MiniRouteMap
import ir.atiran.vizitor.ui.components.MicButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.atiran.vizitor.VizitorViewModel
import ir.atiran.vizitor.data.local.CustomerEntity
import ir.atiran.vizitor.ui.components.GlassCard
import ir.atiran.vizitor.ui.components.MilanoFooter
import ir.atiran.vizitor.ui.components.NeonGreenButton
import ir.atiran.vizitor.ui.components.SectionTitle
import ir.atiran.vizitor.ui.components.StatusChip
import ir.atiran.vizitor.ui.components.StatusDot
import ir.atiran.vizitor.ui.components.goldBorder
import ir.atiran.vizitor.ui.theme.DangerRed
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.NeonGreen
import ir.atiran.vizitor.ui.theme.NeonPurple
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.util.toFaNumber
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun CustomersScreen(viewModel: VizitorViewModel) {
    val customers by viewModel.customers.collectAsState()
    val context = LocalContext.current
    var optimized by remember { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    val startVoice = rememberVoiceSearch(
        onResult = { query = it; viewModel.showToast("جستجوی صوتی مشتری: «$it»") },
        onUnavailable = { viewModel.showToast("ورودی صوتی روی این دستگاه در دسترس نیست 🎙️") }
    )

    // موقعیت فرضی ویزیتور (در نسخه عملیاتی از FusedLocation استفاده می‌شود)
    val myLat = 35.7219; val myLng = 51.3815

    val list = remember(customers, optimized, query) {
        val base = if (query.isBlank()) customers
        else customers.filter {
            it.name.contains(query) || it.code.contains(query) || it.city.contains(query)
        }
        if (optimized) base.sortedBy { distanceKm(myLat, myLng, it.lat, it.lng) }
        else base
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                ShimmerGoldText("گشت‌زنی")
                Text(
                    "مدیریت مسیر ویزیت و وضعیت اعتباری مشتریان",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { CustomerSearchField(query) { query = it } }
                    Spacer(Modifier.width(8.dp))
                    MicButton(onClick = { startVoice() })
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeonGreenButton(
                        text = if (optimized) "مسیر بهینه شد ✅ (ترتیب پیش‌فرض)" else "بهینه‌سازی مسیر توزیع",
                        icon = Icons.Filled.Route,
                        onClick = { optimized = !optimized },
                        modifier = Modifier.weight(1f)
                    )
                    NeonGreenButton(
                        text = if (showMap) "پنهان‌کردن نقشه" else "نقشه داخلی",
                        icon = Icons.Filled.NearMe,
                        onClick = { showMap = !showMap }
                    )
                }
            }
        }

        // ── نقشه داخلی با نشانگرهای طلایی و مسیر بهینه ────────────────────────
        if (showMap) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    MiniRouteMap(
                        customers = list,
                        route = list,
                        myLat = myLat,
                        myLng = myLng
                    )
                }
            }
        }

        item {
            SectionTitle(text = "مشتریان منطقه", icon = Icons.Filled.NearMe)
        }

        items(list, key = { it.id }) { customer ->
            CustomerCard(
                customer = customer,
                order = if (optimized) list.indexOf(customer) + 1 else null,
                onCall = {
                    context.startActivity(
                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                    )
                },
                onNavigate = {
                    // اتصال به API نقشه‌ها: گوگل‌مپ/نشان/بلد با Intent استاندارد
                    val gmm = Uri.parse(
                        "google.navigation:q=${customer.lat},${customer.lng}&mode=d"
                    )
                    val intent = Intent(Intent.ACTION_VIEW, gmm).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("geo:${customer.lat},${customer.lng}?q=${customer.lat},${customer.lng}(${customer.name})")
                            )
                        )
                    }
                },
                onPick = {
                    viewModel.selectCustomer(customer)
                    viewModel.showToast(
                        "مشتری «${customer.name}» برای فاکتور انتخاب شد؛ از دکمه مرکزی سبد استفاده کنید 🛒"
                    )
                }
            )
        }

        item { MilanoFooter() }
    }
}

/** فاصله هاورساین (کیلومتر) برای بهینه‌سازی مسیر توزیع. */
private fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
    return 2 * r * atan2(sqrt(a), sqrt(1 - a))
}

@Composable
private fun CustomerCard(
    customer: CustomerEntity,
    order: Int?,
    onCall: () -> Unit,
    onNavigate: () -> Unit,
    onPick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (customer.isVip) Modifier.goldBorder() else Modifier)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // نشانگر وضعیت اعتباری (سبز/قرمز)
                StatusDot(if (customer.creditOk) NeonGreen else DangerRed)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (order != null) {
                            Text(
                                "${order.toFaNumber()}. ",
                                style = MaterialTheme.typography.titleSmall,
                                color = Gold,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            customer.name,
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (customer.isVip) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Filled.MilitaryTech,
                                contentDescription = "VIP",
                                tint = Gold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        "${customer.city}، ${customer.address}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                    Text(
                        "گروه: ${customer.groupName} | کد: ${customer.code}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                StatusChip(
                    text = if (customer.creditOk) "مجاز" else "مسدود",
                    color = if (customer.creditOk) NeonGreen else DangerRed
                )
            }
            Spacer(Modifier.height(10.dp))
            Row {
                NeonGreenButton(
                    text = "صدور فاکتور برای این مشتری",
                    onClick = onPick,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onCall,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = NeonPurple.copy(alpha = 0.2f),
                        contentColor = NeonPurple
                    )
                ) { Icon(Icons.Filled.Call, contentDescription = "تماس") }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onNavigate,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Gold.copy(alpha = 0.2f),
                        contentColor = Gold
                    )
                ) { Icon(Icons.Filled.NearMe, contentDescription = "مسیریابی") }
            }
        }
    }
}

/** فیلد جستجوی مشتری (نام/کد/شهر) با سبک شیشه‌ای. */
@Composable
private fun CustomerSearchField(value: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x14FFFFFF))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Search, contentDescription = null,
            tint = TextSecondary, modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        androidx.compose.material3.TextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text("جستجوی مشتری (نام/کد/شهر)…", color = TextSecondary) },
            singleLine = true,
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = NeonPurple
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
