package com.coinop.budget.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import com.coinop.budget.ui.theme.rShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coinop.budget.ui.AuthState
import com.coinop.budget.ui.MainViewModel
import com.coinop.budget.ui.RText
import com.coinop.budget.ui.RetroButton
import com.coinop.budget.ui.RetroCard
import com.coinop.budget.ui.RetroField
import com.coinop.budget.ui.Skeleton
import com.coinop.budget.ui.theme.AppIcon
import com.coinop.budget.ui.theme.AppIconView
import com.coinop.budget.ui.theme.Appearance
import com.coinop.budget.ui.theme.CoinopTheme
import com.coinop.budget.ui.theme.Retro
import com.coinop.budget.ui.theme.backdrop
import com.coinop.budget.ui.theme.onFace
import com.coinop.budget.ui.theme.sticker
import kotlinx.coroutines.delay

sealed interface Screen {
    data class Account(val id: String) : Screen
    data object Settings : Screen
    data object Categories : Screen
    data object Rules : Screen
    data object Currency : Screen
    data object Timezone : Screen
}

enum class Tab(val label: String, val icon: AppIcon) {
    HOME("Home", AppIcon.HOME),
    BANKING("Banking", AppIcon.BANK),
    INVEST("Invest", AppIcon.INVEST),
    BUDGET("Budget", AppIcon.BUDGET),
}

@Composable
fun CoinopRoot(vm: MainViewModel) {
    CoinopTheme(vm.appearance) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Retro.p.bg)
                .backdrop(vm.appearance.backdrop, vm.appearance.backdropStrength),
        ) {
            when (vm.authState) {
                AuthState.LOADING -> LoadingScreen()
                AuthState.SIGNED_OUT -> AuthScreen(vm)
                AuthState.SIGNED_IN -> MainShell(vm)
            }
            MessageBanner(vm)
        }
    }
}

/** Shown instead of crashing when Supabase keys are missing from local.properties. */
@Composable
fun ConfigMissingScreen() {
    CoinopTheme(Appearance()) {
        Box(Modifier.fillMaxSize().background(Retro.p.bg).backdrop(Appearance().backdrop, 0.4f)) {
            Column(
                Modifier.align(Alignment.Center).statusBarsPadding().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RText("COINOP", size = 28.sp, bold = true)
                RetroCard {
                    RText("SETUP NEEDED", bold = true)
                    Spacer(Modifier.height(8.dp))
                    RText(
                        "Add SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY to local.properties in the project root, " +
                            "run supabase/schema.sql in your Supabase project, then rebuild.",
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RText("COINOP", size = 24.sp, bold = true)
        Skeleton(height = 96.dp)
        Skeleton(height = 64.dp)
        Skeleton(height = 64.dp)
    }
}

@Composable
private fun BoxScope.MessageBanner(vm: MainViewModel) {
    val msg = vm.message ?: return
    LaunchedEffect(msg) {
        delay(5000)
        if (vm.message == msg) vm.message = null
    }
    val p = Retro.p
    Row(
        Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 88.dp)
            .fillMaxWidth()
            .sticker(p.mustard, 10.dp)
            .clickable { vm.message = null }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RText(msg, color = p.onFace(p.mustard), size = 13.sp)
    }
}

@Composable
private fun AuthScreen(vm: MainViewModel) {
    var email by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    val p = Retro.p
    val canSubmit = email.contains("@") && pass.length >= 6 && !vm.authBusy

    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Column(
            Modifier
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RText("COINOP", size = 32.sp, bold = true, color = Retro.primary)
            RText("INSERT COIN. TRACK MONEY.", size = 12.sp, color = p.onVariant)
            RetroCard(padding = 16.dp) {
                RetroField("Email", email, { email = it }, keyboardType = KeyboardType.Email)
                Spacer(Modifier.height(12.dp))
                RetroField("Password (6+ chars)", pass, { pass = it }, password = true)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RetroButton(
                        "Sign in",
                        { vm.signIn(email, pass) },
                        Modifier.weight(1f),
                        enabled = canSubmit,
                    )
                    RetroButton(
                        "Create",
                        { vm.signUp(email, pass) },
                        Modifier.weight(1f),
                        enabled = canSubmit,
                        face = p.green,
                    )
                }
                if (vm.authBusy) {
                    Spacer(Modifier.height(8.dp))
                    RText("Working...", size = 12.sp, color = p.onVariant, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun MainShell(vm: MainViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var stack by remember { mutableStateOf(listOf<Screen>()) }
    val push: (Screen) -> Unit = { stack = stack + it }
    val back: () -> Unit = { stack = stack.dropLast(1) }

    BackHandler(enabled = stack.isNotEmpty()) { back() }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Box(Modifier.weight(1f)) {
            when (val top = stack.lastOrNull()) {
                null -> when (tab) {
                    Tab.HOME -> HomeScreen(vm, onOpenSettings = { push(Screen.Settings) })
                    Tab.BANKING -> BankingScreen(vm, onOpenAccount = { push(Screen.Account(it)) })
                    Tab.INVEST -> InvestmentsScreen(vm)
                    Tab.BUDGET -> BudgetScreen(vm)
                }
                is Screen.Account -> AccountScreen(vm, top.id, back)
                Screen.Settings -> SettingsScreen(
                    vm,
                    onBack = back,
                    onOpenCategories = { push(Screen.Categories) },
                    onOpenRules = { push(Screen.Rules) },
                    onOpenCurrency = { push(Screen.Currency) },
                    onOpenTimezone = { push(Screen.Timezone) },
                )
                Screen.Categories -> CategoriesScreen(vm, back)
                Screen.Rules -> RulesScreen(vm, back)
                Screen.Currency -> CurrencyScreen(vm, back)
                Screen.Timezone -> TimezoneScreen(vm, back)
            }
        }
        if (stack.isEmpty()) BottomBar(tab) { tab = it }
    }
}

/** Floating tab bar: one sticker pill holds the tabs, and a highlight block slides to the chosen one. */
@Composable
private fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val p = Retro.p
    val cyber = Retro.cyber
    val tabs = Tab.entries
    val barShape = rShape(16.dp)
    val chipShape = rShape(11.dp)

    // end/bottom are 3 dp short of start/top: the sticker adds its own 3 dp of shadow room there.
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 12.dp, end = 9.dp, top = 6.dp, bottom = 5.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().sticker(p.surface, 16.dp, shape = barShape).padding(6.dp)) {
            val tabWidth = maxWidth / tabs.size
            val slide by animateDpAsState(
                targetValue = tabWidth * tabs.indexOf(selected),
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                label = "tabSlide",
            )
            // The moving highlight. Cyberpunk adds a green edge to the mustard face.
            Box(
                Modifier
                    .offset(x = slide)
                    .width(tabWidth)
                    .height(BAR_TAB_HEIGHT)
                    .sticker(p.mustard, 11.dp, shadow = false, shape = chipShape)
                    .then(if (cyber) Modifier.border(2.dp, p.green, chipShape) else Modifier),
            )
            Row(Modifier.fillMaxWidth()) {
                tabs.forEach { t ->
                    val sel = t == selected
                    val content by animateColorAsState(
                        targetValue = if (sel) p.onFace(p.mustard) else p.onFace(p.surface),
                        animationSpec = tween(durationMillis = 200),
                        label = "tabInk",
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .height(BAR_TAB_HEIGHT)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onSelect(t) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        AppIconView(t.icon, content, 24.dp)
                        Spacer(Modifier.height(4.dp))
                        RText(t.label.uppercase(), size = 11.sp, color = content, bold = true, maxLines = 1)
                    }
                }
            }
        }
    }
}

private val BAR_TAB_HEIGHT = 56.dp
