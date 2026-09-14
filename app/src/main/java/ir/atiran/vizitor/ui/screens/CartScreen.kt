/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | تب ۳: سبد سفارش (Smart Cart) — FAB مرکزی
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  لیست اقلام فاکتور + محاسبه خودکار کسورات + پنل امضای دیجیتال (Canvas)
 *  + دستیار هوش مصنوعی + دکمه بزرگ بنفش «صدور و ارسال به آتیران»
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import ir.atiran.vizitor.ui.components.royalBorder
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import ir.atiran.vizitor.util.parseAmount
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.VizitorViewModel
import ir.atiran.vizitor.data.local.CartItemEntity
import ir.atiran.vizitor.ui.components.GlassCard
import ir.atiran.vizitor.ui.components.GoldBurstOverlay
import ir.atiran.vizitor.ui.components.ShimmerGoldText
import ir.atiran.vizitor.ui.components.MilanoFooter
import ir.atiran.vizitor.ui.components.NeonPurpleButton
import ir.atiran.vizitor.ui.components.SectionTitle
import ir.atiran.vizitor.ui.theme.AccentText
import ir.atiran.vizitor.ui.theme.DarkSlateDeep
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.NeonGreen
import ir.atiran.vizitor.ui.theme.NeonPurple
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaNumber
import ir.atiran.vizitor.util.toFaPrice
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(viewModel: VizitorViewModel) {
    val items by viewModel.cartItems.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val gross by viewModel.cartTotal.collectAsState()
    var note by remember { mutableStateOf("") }

    // نسخه ۱٫۶٫۰ — تخفیفات و کسورات حذف شد؛ مبلغ نهایی = جمع اقلام
    val finalAmount = gross

    // امضای دیجیتال — مسیرهای رسم‌شده
    val signaturePaths = remember { mutableStateListOf<Path>() }
    var hasSignature by remember { mutableStateOf(false) }

    var goldBurst by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                ShimmerGoldText("سبد سفارش")
                Text(
                    "صدور فاکتور هوشمند برای مشتریان آتیران",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // ── انتخاب مشتری ────────────────────────────────────────────────────
        item {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selectedCustomer?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("مشتری فاکتور") },
                    placeholder = { Text("انتخاب مشتری…") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedLabelColor = NeonPurple,
                        cursorColor = NeonPurple
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("مشتری متفرقه") },
                        onClick = {
                            viewModel.selectCustomer(null); expanded = false
                        }
                    )
                    customers.forEach { customer ->
                        DropdownMenuItem(
                            text = { Text("${customer.name} — ${customer.groupName}") },
                            onClick = {
                                viewModel.selectCustomer(customer); expanded = false
                            }
                        )
                    }
                }
            }
        }

        // ── توضیحات ویزیتور (درج در پیش‌فاکتور فروش آتیران) ─────────────────
        item {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("توضیحات ویزیتور (برای پیش‌فاکتور)") },
                placeholder = { Text("مثال: تحویل قبل از پنجشنبه، کارتن‌های پسته بدون مغز باز…") },
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonPurple,
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    focusedLabelColor = NeonPurple,
                    cursorColor = NeonPurple
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ── اقلام فاکتور ────────────────────────────────────────────────────
        item { SectionTitle(text = "اقلام فاکتور", icon = Icons.Filled.AutoAwesome) }

        if (items.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "سبد خالی است؛ از «ویترین کالا» یا اسکنر بارکد، کالا اضافه کنید. 🛒",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        items(items, key = { it.productId }) { item ->
            CartLine(
                item = item,
                onAdd = {
                    if (item.quantity < item.stock) {
                        viewModel.addToCart(
                            ir.atiran.vizitor.data.local.ProductEntity(
                                item.productId, "", item.productName, "", item.unitPrice, item.stock
                            )
                        )
                    } else viewModel.showToast("موجودی کافی نیست ❌")
                },
                onRemove = { viewModel.decrement(item.productId) },
                onDelete = { viewModel.removeFromCart(item.productId) },
                onSetQty = { q -> viewModel.setCartQty(item.productId, q) }
            )
        }

        // ── جمع نهایی سفارش (بدون کسورات) ──────────────────────────────────
        item {
            GlassCard(modifier = Modifier.fillMaxWidth().royalBorder()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "مبلغ نهایی سفارش",
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentText,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        finalAmount.toFaPrice(),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = NeonGreen
                    )
                }
            }
        }

        // ── پنل امضای دیجیتال مشتری (Canvas لمسی) ───────────────────────────
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("امضای دیجیتال مشتری", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            signaturePaths.clear(); hasSignature = false
                        }) {
                            Text("پاک کردن", color = DangerColor)
                        }
                    }
                    SignaturePad(
                        paths = signaturePaths,
                        onDrawn = { hasSignature = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSlateDeep)
                    )
                    Text(
                        if (hasSignature) "امضا دریافت شد ✅" else "مشتری با انگشت روی کادر بالا امضا می‌کند",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasSignature) NeonGreen else TextSecondary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        // ── دکمه بزرگ بنفش: صدور و ارسال به آتیران ──────────────────────────
        item {
            NeonPurpleButton(
                text = "صدور و ارسال به آتیران",
                icon = Icons.Filled.Send,
                enabled = items.isNotEmpty(),
                onClick = {
                    val png = if (hasSignature) renderSignature(signaturePaths) else null
                    viewModel.issueInvoice(png, false, note) { invoice ->
                        signaturePaths.clear()
                        hasSignature = false
                        note = ""
                        goldBurst = true // ❄️✨ افکت یخ/باران طلایی
                        viewModel.showToast("فاکتور ${invoice.id.toFaNumber()} صادر شد ✅")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item { MilanoFooter() }
    }

    // جلوه یک‌باره یخ طلایی هنگام ثبت فاکتور
    GoldBurstOverlay(active = goldBurst) { goldBurst = false }
    }
}

private val DangerColor = Color(0xFFFF4D6D)

@Composable
private fun CartLine(
    item: CartItemEntity,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit,
    onSetQty: (Double) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth().royalBorder()) {
        Column(Modifier.fillMaxWidth()) {
            // ردیف اول: نام + قیمت واحد | کنترل تعداد
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1.25f)) {
                    Text(
                        item.productName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "قیمت واحد: ${item.unitPrice.toFaPrice()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(NeonPurple.copy(alpha = 0.25f)),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = NeonPurple)
                ) { Icon(Icons.Filled.Remove, contentDescription = "کمتر", modifier = Modifier.size(16.dp)) }
                var qtyText by remember(item.quantity) { mutableStateOf(item.quantity.toFaNumber()) }
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { v ->
                        qtyText = v
                        v.parseAmount()?.let { q -> onSetQty(q) }
                    },
                    modifier = Modifier
                        .width(62.dp)
                        .padding(horizontal = 4.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleSmall.copy(
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
                    onClick = onAdd,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(NeonGreen.copy(alpha = 0.9f)),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Black)
                ) { Icon(Icons.Filled.Add, contentDescription = "بیشتر", modifier = Modifier.size(16.dp)) }
            }

            Spacer(Modifier.height(8.dp))

            // ردیف دوم: مبلغ نهایی آیتم + حذف
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "مبلغ نهایی آیتم",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    (item.quantity.toLong() * item.unitPrice).toFaPrice(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonGreen
                    )
                )
                Spacer(Modifier.width(10.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = DangerColor.copy(alpha = 0.15f),
                        contentColor = DangerColor
                    )
                ) { Icon(Icons.Filled.Delete, contentDescription = "حذف", modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

/**
 * پنل لمسی اخذ امضای دیجیتال — رسم مسیر با انگشت روی Canvas.
 */
@Composable
private fun SignaturePad(
    paths: androidx.compose.runtime.snapshots.SnapshotStateList<Path>,
    onDrawn: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPath by remember { mutableStateOf<Path?>(null) }
    // رنگ‌های تم — خوانده شده پیش از ورود به Canvas
    val inkColor = NeonGreen
    val guideColor = vizitorPalette.glassBorder

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    val p = Path().apply { moveTo(offset.x, offset.y) }
                    currentPath = p
                },
                onDrag = { change, _ ->
                    change.consume()
                    currentPath?.lineTo(change.position.x, change.position.y)
                },
                onDragEnd = {
                    currentPath?.let {
                        paths.add(it)
                        onDrawn()
                    }
                    currentPath = null
                }
            )
        }
    ) {
        // خطوط راهنمای امضا
        drawLine(
            color = guideColor,
            start = Offset(size.width * 0.1f, size.height * 0.75f),
            end = Offset(size.width * 0.9f, size.height * 0.75f),
            strokeWidth = 1.dp.toPx()
        )
        paths.forEach { path ->
            drawPath(path = path, color = inkColor, style = Stroke(width = 3.dp.toPx()))
        }
        currentPath?.let {
            drawPath(path = it, color = inkColor, style = Stroke(width = 3.dp.toPx()))
        }
    }
}

/** رستر کردن امضا به PNG برای ذخیره در دیتابیس و ارسال به سرور. */
private fun renderSignature(
    paths: androidx.compose.runtime.snapshots.SnapshotStateList<Path>,
    width: Int = 800,
    height: Int = 300
): ByteArray? {
    if (paths.isEmpty()) return null
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint().apply {
        color = android.graphics.Color.argb(255, 43, 255, 136)
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    paths.forEach { uiPath ->
        canvas.drawPath(uiPath.asAndroidPath(), paint)
    }
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
    bitmap.recycle()
    return out.toByteArray()
}
