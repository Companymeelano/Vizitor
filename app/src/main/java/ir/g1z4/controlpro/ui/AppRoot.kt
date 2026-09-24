package ir.g1z4.controlpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        bottomBar = { if (main) BottomNav(route) { dest -> nav.navigate(dest) { popUpTo("dashboard") { saveState = true }; launchSingleTop = true; restoreState = true } } }
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
    val items = listOf("dashboard" to "خانه", "zones" to "زون‌ها", "events" to "رویدادها", "devices" to "دستگاه‌ها", "settings" to "تنظیمات")
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(p.surface)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEach { (id, label) ->
            Text(
                label,
                color = if (current == id) p.gold else p.muted,
                fontFamily = Vazir,
                fontSize = 13.sp,
                modifier = Modifier
                    .semantics { contentDescription = label }
                    .clickable { onPick(id) }
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            )
        }
    }
}
