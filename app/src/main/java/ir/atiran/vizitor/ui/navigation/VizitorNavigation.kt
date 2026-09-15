/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | ناوبری اصلی + نوار پایین ۵ تایی با FAB مرکزی
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  تب ۱: پیشخوان من | تب ۲: ویترین کالا | تب ۳: گفتگو | تب ۴: سبد سفارش (FAB مرکزی)
 *  تب ۵: مشتری | تب ۶: گزارشات و تنظیمات
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ir.atiran.vizitor.VizitorViewModel
import ir.atiran.vizitor.perf.VizitorPerf
import ir.atiran.vizitor.ui.components.dashboardBackdrop
import ir.atiran.vizitor.ui.screens.CartScreen
import ir.atiran.vizitor.ui.screens.CatalogScreen
import ir.atiran.vizitor.ui.screens.ChatScreen
import ir.atiran.vizitor.ui.screens.CustomersScreen
import ir.atiran.vizitor.ui.screens.DashboardScreen
import ir.atiran.vizitor.ui.screens.ReportsScreen
import ir.atiran.vizitor.ui.screens.ScannerScreen
import ir.atiran.vizitor.ui.screens.SplashScreen
import ir.atiran.vizitor.ui.theme.DarkSlateElevated
import ir.atiran.vizitor.ui.theme.GlassBorder
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.NeonPurple
import ir.atiran.vizitor.ui.theme.NeonPurpleDark
import ir.atiran.vizitor.ui.theme.NeonPurpleGlow
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette

object Routes {
    const val DASHBOARD = "dashboard"
    const val CATALOG = "catalog"
    const val CART = "cart"
    const val CUSTOMERS = "customers"
    const val REPORTS = "reports"
    const val CHAT = "chat"
    const val SPLASH = "splash"
    const val SCANNER = "scanner"
}

data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val rightTabs = listOf(
    TabItem(Routes.DASHBOARD, "پیشخوان", Icons.Filled.Dashboard),
    TabItem(Routes.CATALOG, "ویترین", Icons.Filled.Storefront),
    TabItem(Routes.CHAT, "گفتگو", Icons.Filled.Message)
)

private val leftTabs = listOf(
    TabItem(Routes.CUSTOMERS, "مشتری", Icons.Filled.Person),
    TabItem(Routes.REPORTS, "گزارشات", Icons.Filled.Receipt)
)

@Composable
fun VizitorRoot(viewModel: VizitorViewModel = viewModel()) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // دوربین برای اسکنر بارکد
    val cameraPermissionGranted = remember {
        androidx.compose.runtime.mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> cameraPermissionGranted.value = granted }

    // نمایش پیام‌های سراسری
    val toast by viewModel.toast.collectAsState()
    LaunchedEffect(toast) {
        toast?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.consumeToast()
        }
    }

    val navBackStack by navController.currentBackStackEntryAsState()
    val isSplash = navBackStack?.destination?.route == Routes.SPLASH

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (isSplash) return@Scaffold
            VizitorBottomBar(
                navController = navController,
                onFabClick = {
                    navController.navigate(Routes.CART) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true; restoreState = true
                    }
                },
                onScanClick = {
                    if (cameraPermissionGranted.value) {
                        navController.navigate(Routes.SCANNER)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .dashboardBackdrop()
        ) {
            // صفحه ورود لوکس — نام پخش + مدیریت + دکمه ورود تم‌دار (v2.5.0)
            composable(Routes.SPLASH) {
                SplashScreen(
                    onEnter = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    },
                    onSoon = { viewModel.showToast(it) }
                )
            }
            composable(Routes.DASHBOARD) { DashboardScreen(viewModel) }
            // ── اتاق گفتگوی ویزیتورها (v2.3.0) ─────────────────────────────
            composable(Routes.CHAT) { ChatScreen(viewModel) }
            composable(Routes.CATALOG) {
                CatalogScreen(
                    viewModel = viewModel,
                    onOpenScanner = {
                        if (cameraPermissionGranted.value) navController.navigate(Routes.SCANNER)
                        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
            composable(Routes.CART) { CartScreen(viewModel) }
            composable(Routes.CUSTOMERS) { CustomersScreen(viewModel) }
            composable(Routes.REPORTS) { ReportsScreen(viewModel) }
            composable(Routes.SCANNER) {
                ScannerScreen(
                    onBarcode = { code ->
                        viewModel.findProductByBarcode(code) { product ->
                            viewModel.showToast(
                                if (product != null) "«${product.name}» با بارکد $code به سبد اضافه شد ✅"
                                else "کالایی با بارکد $code یافت نشد ❌"
                            )
                        }
                        navController.popBackStack()
                    },
                    onClose = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * نوار ناوبری پایین شیشه‌ای:
 * [پیشخوان][ویترین] — (FAB مرکزی: سبد سفارش) — [مشتری][گزارشات]
 */
@Composable
private fun VizitorBottomBar(
    navController: androidx.navigation.NavController,
    onFabClick: () -> Unit,
    onScanClick: () -> Unit
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.hierarchy?.firstOrNull()?.route
    val vm: VizitorViewModel = viewModel()
    val cartCount by vm.cartItems.collectAsState()

    // رنگ‌های تم — کپچر در کانتکست کامپوزبل پیش از ورود به لایه رسم
    val hairlinePrimary = NeonPurple
    val hairlineGold = Gold
    val barSurface = vizitorPalette.surface
    val barSurfaceDeep = vizitorPalette.surfaceDeep
    Box(modifier = Modifier.fillMaxWidth()) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(barSurface.copy(alpha = 0.97f), barSurfaceDeep.copy(alpha = 0.97f))
                    )
                )
                .border(0.5.dp, GlassBorder, RoundedCornerShape(0.dp))
                // خط نور گرادیانی بالای نوار (امضای لوکس)
                .drawBehind {
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, hairlinePrimary, hairlineGold, Color.Transparent)
                        ),
                        start = Offset(0f, 0.5f),
                        end = Offset(size.width, 0.5f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
        ) {
            rightTabs.forEach { tab ->
                BottomTab(tab, currentRoute == tab.route, navController)
            }

            // جایگاه خالی برای FAB مرکزی (سبد سفارش) — بدون برچسب متنی؛ ورود فقط از آیکن سبد
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.weight(1f).height(80.dp),
                contentAlignment = Alignment.Center
            ) { }

            leftTabs.forEach { tab ->
                BottomTab(tab, currentRoute == tab.route, navController)
            }
        }

        // ── دکمه مرکزی شناور (FAB) — تب ۳: سبد سفارش ────────────────────────
        // قوس نور طلایی که دور FAB در مدار می‌چرخد ✨ (در حالت «سبک» ثابت)
        val orbitAngle = if (VizitorPerf.screenFx) {
            val orbitTransition = rememberInfiniteTransition(label = "fabOrbit")
            val a by orbitTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(animation = tween(3000, easing = LinearEasing)),
                label = "orbitAngle"
            )
            a
        } else 35f
        val ringGold = Gold
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-30).dp)
                .size(74.dp),
            contentAlignment = Alignment.Center
        ) {
            // حلقه مداری نور (رویه غیرکلیپ‌شده)
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f - 4.dp.toPx()
                val cx = size.width / 2f
                val cy = size.height / 2f
                val tl = Offset(cx - r, cy - r)
                val arcSize = Size(r * 2f, r * 2f)
                drawArc(
                    color = ringGold.copy(alpha = 0.20f),
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = tl, size = arcSize,
                    style = Stroke(2.5.dp.toPx())
                )
                drawArc(
                    color = ringGold,
                    startAngle = orbitAngle, sweepAngle = 105f, useCenter = false,
                    topLeft = tl, size = arcSize,
                    style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            // دایره اصلی دکمه
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(NeonPurpleDark, NeonPurple))
                    )
                    .border(1.5.dp, ringGold.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.FloatingActionButton(
                    onClick = onFabClick,
                    containerColor = Color.Transparent,
                    elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(0.dp),
                    modifier = Modifier
                        .size(62.dp)
                        .align(Alignment.Center)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.ShoppingCart,
                            contentDescription = "سبد سفارش",
                            tint = Color.White,
                            modifier = Modifier.size(27.dp)
                        )
                        val count = cartCount.sumOf { it.quantity }.toInt()
                        if (count > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 8.dp, y = (-8).dp)
                                    .clip(CircleShape)
                                    .background(ir.atiran.vizitor.ui.theme.NeonGreen)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "$count",
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BottomTab(
    tab: TabItem,
    selected: Boolean,
    navController: androidx.navigation.NavController
) {
    val p = vizitorPalette
    val lift by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "tabLift${tab.route}"
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .height(74.dp)
            .padding(horizontal = 3.dp, vertical = 6.dp)
            .graphicsLayer { translationY = -7.dp.toPx() * lift }
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (selected)
                    Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(p.primary.copy(alpha = 0.32f), p.primaryDark.copy(alpha = 0.24f))
                            )
                        )
                        .border(1.dp, p.gold.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                else
                    Modifier
                        .background(Color.Transparent)
                        .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(16.dp))
            )
            .clickable {
                navController.navigate(tab.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true; restoreState = true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // کپسول سه‌بعدی آیکن — انتخاب: گرادیان تم + حلقه طلایی و سایه نور
            Box(
                modifier = Modifier
                    .size(if (selected) 34.dp else 30.dp)
                    .clip(CircleShape)
                    .then(
                        if (selected)
                            Modifier
                                .background(Brush.verticalGradient(listOf(p.primary, p.primaryDark)))
                                .border(1.dp, p.gold.copy(alpha = 0.75f), CircleShape)
                        else
                            Modifier
                                .background(Color(0x14FFFFFF))
                                .border(1.dp, Color(0x22FFFFFF), CircleShape)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    tab.icon,
                    contentDescription = tab.label,
                    tint = if (selected) p.gold else TextSecondary,
                    modifier = Modifier.size(if (selected) 18.dp else 16.dp)
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                tab.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
                    fontSize = 9.5.sp
                ),
                color = if (selected) p.gold else TextSecondary,
                maxLines = 1
            )
        }
    }
}

