/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | تب ۲: ویترین کالا (3D Catalog)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  کارت‌های شیشه‌ای با افکت سه‌بعدی، دکمه‌های شناور + و -،
 *  موجودی زنده (Live Stock) و اسکنر بارکد دوربین
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.input.KeyboardType
import ir.atiran.vizitor.data.local.CustomerEntity
import ir.atiran.vizitor.ui.components.RoyalHeader
import ir.atiran.vizitor.ui.components.RoyalSurfaceBrush
import ir.atiran.vizitor.ui.components.auroraFrame
import ir.atiran.vizitor.ui.components.PriceTag3D
import ir.atiran.vizitor.ui.components.royalBorder
import ir.atiran.vizitor.ui.theme.NeonPurpleDark
import ir.atiran.vizitor.util.parseAmount
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Inventory2
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.VizitorViewModel
import ir.atiran.vizitor.data.local.ProductEntity
import ir.atiran.vizitor.ui.components.GlassCard
import ir.atiran.vizitor.ui.components.NeonGreenButton
import ir.atiran.vizitor.ui.components.MicButton
import ir.atiran.vizitor.ui.components.MilanoFooter
import ir.atiran.vizitor.ui.components.ShimmerGoldText
import ir.atiran.vizitor.ui.components.goldBorder
import ir.atiran.vizitor.ui.components.rememberVoiceSearch
import ir.atiran.vizitor.ui.components.tilt3D
import ir.atiran.vizitor.ui.theme.AccentText
import ir.atiran.vizitor.ui.theme.DangerRed
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.GoldDark
import ir.atiran.vizitor.ui.theme.NeonGreen
import ir.atiran.vizitor.ui.theme.NeonPurple
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.util.toFaNumber
import ir.atiran.vizitor.util.toFaPrice

@Composable
fun CatalogScreen(
    viewModel: VizitorViewModel,
    onOpenScanner: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    var query by remember { mutableStateOf("") }
    var zoomProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var addProduct by remember { mutableStateOf<ProductEntity?>(null) }
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()

    // آخرین قیمت فروش کالای انتخابی به مشتری انتخابی
    var lastPrice by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(addProduct) {
        lastPrice = null
        val pr = addProduct
        val cu = selectedCustomer
        if (pr != null && cu != null) {
            viewModel.lastSalePrice(cu.id, pr.id) { lastPrice = it }
        }
    }
    val startVoice = rememberVoiceSearch(
        onResult = { query = it; viewModel.showToast("جستجوی صوتی: «$it»") },
        onUnavailable = { viewModel.showToast("ورودی صوتی روی این دستگاه در دسترس نیست 🎙️") }
    )
    val filtered = remember(products, query) {
        if (query.isBlank()) products
        else products.filter { it.name.contains(query) || it.code.contains(query) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Column {
                    ShimmerGoldText("ویترین کالا")
                    Text(
                        "کاتالوگ زنده با موجودی لحظه‌ای",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { SearchField(query) { query = it } }
                        Spacer(Modifier.width(8.dp))
                        MicButton(onClick = { startVoice() })
                    }
                }
            }

            items(filtered, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    onAdd = { addProduct = product },
                    onRemove = { viewModel.decrement(product.id) },
                    onZoom = { zoomProduct = product }
                )
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                MilanoFooter()
            }
        }

        // ── دیالوگ بزرگنمایی تصویر کالا ────────────────────────────────────
        zoomProduct?.let { p ->
            ProductZoomDialog(p) { zoomProduct = null }
        }

        // ── دیالوگ افزودن هوشمند به سبد (تعداد دستی + انتخاب قیمت) ─────────
        addProduct?.let { pr ->
            AddToCartDialog(
                product = pr,
                customer = selectedCustomer,
                initialLevel = viewModel.priceLevelFor(selectedCustomer),
                lastPrice = lastPrice,
                onSaveVisitorLevel = { viewModel.setDefaultPriceLevel(it) },
                onConfirm = { qty, price ->
                    viewModel.addToCart(pr, qty, price)
                    viewModel.showToast(
                        "«${pr.name}» × ${qty.toFaNumber()} با قیمت انتخابی به سبد اضافه شد ✅"
                    )
                    addProduct = null
                },
                onDismiss = { addProduct = null }
            )
        }

        // ── دکمه فعال‌سازی دوربین — اسکنر بارکد ─────────────────────────────
        androidx.compose.material3.FloatingActionButton(
            onClick = onOpenScanner,
            containerColor = NeonPurple,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 20.dp)
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = "اسکنر بارکد")
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x14FFFFFF))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        androidx.compose.material3.TextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text("جستجوی نام یا بارکد کالا…", color = TextSecondary) },
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

/** کارت کالای شیشه‌ای با افکت سه‌بعدی و دکمه‌های شناور + و -. */
@Composable
private fun ProductCard(
    product: ProductEntity,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onZoom: () -> Unit
) {
    val inStock = product.stock > 0
    val lowStock = product.stock in 0.0..10.0

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .tilt3D(maxTilt = 12f)
            .then(if (product.isVip) Modifier.goldBorder() else Modifier),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // تصویر کالا با جلوه سه‌بعدی — کلیک = بزرگنمایی
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x22B04BF8), Color(0x08FFFFFF))
                        )
                    )
                    .clickable(onClick = onZoom)
                    .auroraFrame(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    product.imageEmoji,
                    fontSize = 40.sp,
                    textAlign = TextAlign.Center
                )
                // نشان بزرگنمایی
                Icon(
                    Icons.Filled.ZoomIn,
                    contentDescription = "بزرگنمایی",
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                )
                if (product.isVip) {
                    Text(
                        "VIP",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(Gold)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Color.Black,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                product.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.2.sp,
                    lineHeight = 21.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )
            Text(
                product.groupName,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(Modifier.height(6.dp))

            // موجودی زنده (Live Stock)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Inventory2,
                    contentDescription = null,
                    tint = if (inStock) if (lowStock) Gold else NeonGreen else DangerRed,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (inStock) "موجودی: ${product.stock.toFaNumber()}" else "ناموجود",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (inStock) if (lowStock) Gold else NeonGreen else DangerRed
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "واحد شمارش: ${product.unit}  •  هر بسته: ${product.packSize.toFaNumber()} عدد",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(6.dp))
            PriceTag3D(
                label = "فروش ۱",
                price = product.price,
                face = Brush.linearGradient(listOf(Color(0xFF8CFFCB), Color(0xFF17D877))),
                edge = Color(0xFF0B7A44),
                textColor = Color.Black,
                modifier = Modifier.fillMaxWidth()
            )
            PriceTag3D(
                label = "فروش ۲",
                price = if (product.price2 > 0) product.price2 else product.price,
                face = Brush.linearGradient(listOf(Color(0xFFFFE29A), Color(0xFFF0B23C))),
                edge = Color(0xFF8F6414),
                textColor = Color.Black,
                modifier = Modifier.fillMaxWidth()
            )
            PriceTag3D(
                label = "مصرف‌کننده",
                price = if (product.consumerPrice > 0) product.consumerPrice else product.price,
                face = Brush.linearGradient(listOf(NeonPurple, NeonPurpleDark)),
                edge = Color(0xFF2C0B4E),
                textColor = Color.White,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // دکمه‌های شناور + و -
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onRemove,
                    enabled = inStock,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(NeonPurple.copy(alpha = 0.25f)),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = NeonPurple)
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "کاهش", modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onAdd,
                    enabled = inStock,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(NeonGreen.copy(alpha = 0.9f)),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Black)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "افزودن به سبد", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/**
 * دیالوگ بزرگنمایی کالا — تصویر بزرگ و مشخصات کامل (واحد/بسته/قیمت‌ها).
 */
@Composable
private fun ProductZoomDialog(product: ProductEntity, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(ir.atiran.vizitor.ui.theme.DarkSlateElevated)
                .goldBorder(RoundedCornerShape(28.dp))
        ) {
            Column(Modifier.padding(18.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x33B04BF8), Color(0x0AFFFFFF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(product.imageEmoji, fontSize = 110.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text(product.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${product.groupName} | بارکد: ${product.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text("واحد شمارش: ${product.unit}", modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("تعداد در بسته: ${product.packSize.toFaNumber()}",
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text("قیمت فروش ۱: ${product.price.toFaPrice()}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall.copy(color = NeonGreen, fontWeight = FontWeight.ExtraBold))
                    Text("قیمت فروش ۲: ${(if (product.price2 > 0) product.price2 else product.price).toFaPrice()}",
                        style = MaterialTheme.typography.titleSmall.copy(color = Gold, fontWeight = FontWeight.ExtraBold))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "قیمت مصرف‌کننده: ${(if (product.consumerPrice > 0) product.consumerPrice else product.price).toFaPrice()}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = AccentText,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (product.stock > 0) "موجودی زنده: ${product.stock.toFaNumber()} ${product.unit}" else "ناموجود",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.stock > 0) NeonGreen else DangerRed
                )
                Spacer(Modifier.height(12.dp))
                NeonGreenButton(text = "بستن", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** چیپ انتخاب سطح قیمت (فروش ۱ / فروش ۲) با ظاهر سلطنتی بنفش. */
@Composable
private fun PriceChip(
    label: String,
    price: Long,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (selected) Modifier.background(Brush.linearGradient(listOf(NeonPurple, NeonPurpleDark)))
                else Modifier.background(Color(0x14FFFFFF))
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Gold else Color(0x33FFFFFF),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                color = if (!enabled) Color(0xFF5A6270) else if (selected) Color.White else TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                price.toFaPrice(),
                color = if (!enabled) Color(0xFF5A6270) else if (selected) Gold else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

/**
 * دیالوگ افزودن هوشمند به سبد:
 *  — ورود دستی تعداد (تایپ عدد) + دکمه‌های ±
 *  — انتخاب قیمت فروش ۱ یا ۲ با پیش‌فرض هوشمند (گروه مشتری / تنظیم ویزیتور)
 *  — نمایش آخرین قیمت فروش به مشتری انتخاب‌شده و امکان اعمال آن
 *  — ویرایش دستی قیمت نهایی
 */
@Composable
private fun AddToCartDialog(
    product: ProductEntity,
    customer: CustomerEntity?,
    initialLevel: Int,
    lastPrice: Long?,
    onSaveVisitorLevel: (Int) -> Unit,
    onConfirm: (Double, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var qtyText by remember { mutableStateOf("0") }
    var lockedQty by remember { mutableStateOf(0.0) }
    var level by remember { mutableStateOf(initialLevel) }
    var manual by remember { mutableStateOf(false) }
    var manualText by remember { mutableStateOf("") }

    val hasPrice2 = product.price2 > 0
    val levelPrice = if (level == 2 && hasPrice2) product.price2 else product.price
    val qty = qtyText.parseAmount() ?: 0.0
    val effectiveQty = if (lockedQty > 0) lockedQty else qty
    val finalPrice: Long = if (manual) (manualText.parseAmount()?.toLong() ?: levelPrice) else levelPrice

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(RoyalSurfaceBrush)
                .royalBorder(RoundedCornerShape(26.dp))
        ) {
            Column(Modifier.padding(18.dp)) {
                RoyalHeader(text = "افزودن به سبد فروش", icon = Icons.Filled.Add)
                Spacer(Modifier.height(10.dp))
                Text(
                    "${product.imageEmoji} ${product.name}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    "واحد: ${product.unit} • هر بسته: ${product.packSize.toFaNumber()} عدد • موجودی: ${product.stock.toFaNumber()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )

                Spacer(Modifier.height(12.dp))
                Text("تعداد / مقدار:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            qtyText = ((qty + 1).coerceAtMost(product.stock)).toLong().toString()
                            lockedQty = 0.0
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = NeonPurple.copy(alpha = 0.25f),
                            contentColor = NeonPurple
                        )
                    ) { Icon(Icons.Filled.Add, contentDescription = "بیشتر") }
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = {
                            qtyText = it
                            lockedQty = 0.0
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            cursorColor = NeonPurple
                        )
                    )
                    IconButton(
                        onClick = {
                            qtyText = (qty - 1).coerceAtLeast(0.0).toLong().toString()
                            lockedQty = 0.0
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0x1AFFFFFF),
                            contentColor = TextSecondary
                        )
                    ) { Icon(Icons.Filled.Remove, contentDescription = "کمتر") }
                }
                Spacer(Modifier.height(6.dp))
                // درج سریع بر اساس بسته‌بندی + دکمه درج تعداد
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PackChip("۱ بسته (${product.packSize.toFaNumber()})") {
                        qtyText = product.packSize.toString()
                        lockedQty = product.packSize.toDouble()
                    }
                    PackChip("۲ بسته") {
                        val v = product.packSize * 2
                        qtyText = v.toString()
                        lockedQty = v.toDouble()
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (qty > 0) Modifier.background(Brush.linearGradient(listOf(Gold, GoldDark)))
                                else Modifier.background(Color(0x14FFFFFF))
                            )
                            .clickable(enabled = qty > 0) {
                                lockedQty = qty.coerceAtMost(product.stock)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            if (lockedQty > 0) "تعداد درج شد ✓" else "درج تعداد",
                            color = if (qty > 0) Color.Black else TextSecondary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("سطح قیمت:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Row {
                    PriceChip(
                        label = "قیمت فروش ۱",
                        price = product.price,
                        selected = level == 1,
                        enabled = true
                    ) { level = 1; manual = false }
                    PriceChip(
                        label = "قیمت فروش ۲",
                        price = if (hasPrice2) product.price2 else product.price,
                        selected = level == 2,
                        enabled = hasPrice2
                    ) { level = 2; manual = false }
                }
                if (!hasPrice2) {
                    Text(
                        "این کالا قیمت فروش ۲ ندارد؛ همان فروش ۱ محاسبه می‌شود.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                TextButton(onClick = { onSaveVisitorLevel(level) }) {
                    Text(
                        "ذخیره «فروش $level» به‌عنوان پیش‌فرض همیشگی من",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonPurple
                    )
                }

                if (lastPrice != null && customer != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Gold.copy(alpha = 0.10f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "آخرین قیمت فروش به ${customer.name}:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                lastPrice.toFaPrice(),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Gold
                                )
                            )
                        }
                        TextButton(onClick = { manual = true; manualText = lastPrice.toString() }) {
                            Text("اعمال همین قیمت", color = Gold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ویرایش دستی قیمت واحد", style = MaterialTheme.typography.labelMedium)
                        Text(
                            if (manual) "قیمت دلخواه فعال است" else "بر اساس سطح انتخابی بالا",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = manual,
                        onCheckedChange = {
                            manual = it
                            if (it && manualText.isBlank()) manualText = levelPrice.toString()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonPurple
                        )
                    )
                }
                if (manual) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = manualText,
                        onValueChange = { manualText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("قیمت دلخواه هر واحد (ریال)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            cursorColor = Gold,
                            focusedLabelColor = Gold
                        )
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "قیمت نهایی هر واحد:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        finalPrice.toFaPrice(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonGreen
                        )
                    )
                }
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "جمع این قلم:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        (finalPrice.toBigDecimal() * effectiveQty.toBigDecimal()).toLong().toFaPrice(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Gold
                        )
                    )
                }

                Spacer(Modifier.height(14.dp))
                NeonGreenButton(
                    text = if (lockedQty > 0) "افزودن ${lockedQty.toFaNumber()} عدد به سبد 🛒" else "افزودن به سبد 🛒",
                    onClick = { if (lockedQty > 0) onConfirm(lockedQty.coerceAtMost(product.stock), finalPrice) },
                    enabled = lockedQty > 0,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("انصراف", color = TextSecondary)
                }
            }
        }
    }
}

/** چیپ درج سریع تعداد بر اساس بسته‌بندی کالا. */
@Composable
private fun PackChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(NeonPurple.copy(alpha = 0.18f))
            .border(1.dp, NeonPurple.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, color = AccentText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}
