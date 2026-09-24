package ir.g1z4.controlpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ir.g1z4.controlpro.ui.screens.AlertsScreen
import ir.g1z4.controlpro.ui.screens.AuthScreen
import ir.g1z4.controlpro.ui.screens.CommScreen
import ir.g1z4.controlpro.ui.screens.DashboardScreen
import ir.g1z4.controlpro.ui.screens.DevicesScreen
import ir.g1z4.controlpro.ui.screens.EventsScreen
import ir.g1z4.controlpro.ui.screens.OutputsScreen
import ir.g1z4.controlpro.ui.screens.ProtocolScreen
import ir.g1z4.controlpro.ui.screens.RemotesScreen
import ir.g1z4.controlpro.ui.screens.SettingsScreen
import ir.g1z4.controlpro.ui.screens.SirenScreen
import ir.g1z4.controlpro.ui.screens.SplashGate
import ir.g1z4.controlpro.ui.screens.ZonesScreen
import ir.g1z4.controlpro.ui.theme.G1Theme
import ir.g1z4.controlpro.ui.theme.LocalPalette
import ir.g1z4.controlpro.ui.theme.Vazir
import ir.g1z4.controlpro.ui.vm.AppModel
import kotlinx.coroutines.delay

@Composable
fun AppRoot(startRoute: String?, model: AppModel = hiltViewModel()) {
    val settings by model.settings.collectAsStateWithLifecycle()
    val ready by model.settingsReady.collectAsStateWithLifecycle()
    val unlocked by model.session.unlocked.collectAsStateWithLifecycle()
    val pinLocal by model.session.pinReadyLocal.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    var splash by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(900); splash = false }
    LaunchedEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) model.touch()
            if (event == Lifecycle.Event.ON_START) model.lockIfNeeded()
        }
        owner.lifecycle.addObserver(observer)
    }
    G1Theme(settings.theme) {
        val pinReady = settings.pinReady || pinLocal
        when {
            splash || !ready -> SplashGate()
            !pinReady -> AuthScreen(setup = true, model = model)
            !unlocked -> AuthScreen(setup = false, model = model)
            else -> MainShell(startRoute, model, settings.developer && ir.g1z4.controlpro.BuildConfig.DEBUG)
        }
    }
}

@Composable
private fun MainShell(startRoute: String?, model: AppModel, developer: Boolean) {
    val nav = rememberNavController()
    val p = LocalPalette.current
    LaunchedEffect(startRoute) {
        when (startRoute) {
            "confirm_arm" -> nav.navigate("act/arm")
            "confirm_disarm" -> nav.navigate("act/disarm")
            "confirm_status" -> nav.navigate("act/status")
            "alerts" -> nav.navigate("alerts")
        }
    }
    val route = nav.currentBackStackEntryAsState().value?.destination?.route.orEmpty()
    val main = route in setOf("dashboard", "zones", "events", "devices", "settings")
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {
            Column(Modifier.navigationBarsPadding()) {
                AuthorFooter()
                if (main) BottomNav(route) { dest -> nav.navigate(dest) { popUpTo("dashboard") { saveState = true }; launchSingleTop = true; restoreState = true } }
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
            if (developer) {
                Text("حالت آزمایشی — این پاسخ سخت‌افزار نیست", color = p.alarm, fontFamily = Vazir, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
            }
            Host(nav, model, Modifier.padding(top = if (developer) 28.dp else 0.dp))
        }
    }
}

@Composable
private fun Host(nav: NavHostController, model: AppModel, modifier: Modifier) {
    NavHost(nav, startDestination = "dashboard", modifier = modifier) {
        composable("dashboard") { DashboardScreen(model, nav, null) }
        composable("act/{action}") { DashboardScreen(model, nav, it.arguments?.getString("action")) }
        composable("zones") { ZonesScreen(model, nav, null) }
        composable("zone/{key}") { ZonesScreen(model, nav, it.arguments?.getString("key")) }
        composable("events") { EventsScreen(model, nav) }
        composable("devices") { DevicesScreen(model, nav, null) }
        composable("device/{id}") { DevicesScreen(model, nav, it.arguments?.getString("id")) }
        composable("settings") { SettingsScreen(model, nav) }
        composable("outputs") { OutputsScreen(model, nav) }
        composable("siren") { SirenScreen(model, nav) }
        composable("alerts") { AlertsScreen(model, nav) }
        composable("remotes") { RemotesScreen(model, nav) }
        composable("comm") { CommScreen(model, nav) }
        composable("protocol") { ProtocolScreen(nav) }
    }
}

@Composable
private fun BottomNav(current: String, onPick: (String) -> Unit) {
    val p = LocalPalette.current
    val items = listOf(
        Triple("dashboard", "خانه", Icons3d.home),
        Triple("zones", "زون‌ها", Icons3d.zone),
        Triple("events", "رویدادها", Icons3d.reports),
        Triple("devices", "دستگاه‌ها", Icons3d.device),
        Triple("settings", "تنظیمات", Icons3d.settings)
    )
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
        val compact = maxWidth < 360.dp
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(p.surface)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { (id, label, icon) ->
                val selected = current == id
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .semantics { contentDescription = label }
                        .clickable { onPick(id) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    androidx.compose.foundation.Image(
                        painterResource(icon),
                        label,
                        Modifier.size(if (compact) 22.dp else 26.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        label,
                        color = if (selected) p.gold else p.muted,
                        fontFamily = Vazir,
                        fontSize = if (compact) 10.sp else 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
