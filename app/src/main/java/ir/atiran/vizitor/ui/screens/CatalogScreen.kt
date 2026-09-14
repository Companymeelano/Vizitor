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
import ir.atiran.vizitor.ui.components.MilanoFooter
import ir.atiran.vizitor.ui.components.goldBorder
import ir.atiran.vizitor.ui.components.tilt3D
import ir.atiran.vizitor.ui.theme.DangerRed
import ir.atiran.vizitor.ui.theme.Gold
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
                    Text("ویترین کالا", style = MaterialTheme.typography.displaySmall)
                    Text(
                        "کاتالوگ زنده با موجودی لحظه‌ای",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    SearchField(query) { query = it }
                }
            }

            items(filtered, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    onAdd = { viewModel.addToCart(product) },
                    onRemove = { viewModel.decrement(product.id) }
                )
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                MilanoFooter()
            }
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
    onRemove: () -> Unit
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
            // تصویر کالا با جلوه سه‌بعدی
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x22B04BF8), Color(0x08FFFFFF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    product.imageEmoji,
                    fontSize = 40.sp,
                    textAlign = TextAlign.Center
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
                style = MaterialTheme.typography.titleSmall,
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
                product.price.toFaPrice(),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonGreen
                )
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
