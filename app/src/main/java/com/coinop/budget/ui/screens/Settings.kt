package com.coinop.budget.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.coinop.budget.ui.theme.family
import com.coinop.budget.ui.theme.onFace
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.Color
import com.coinop.budget.ui.OptionSheet
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coinop.budget.BuildConfig
import com.coinop.budget.data.Category
import com.coinop.budget.domain.Money
import com.coinop.budget.domain.TimeUtil
import com.coinop.budget.ui.Chip
import com.coinop.budget.ui.ChipFlow
import com.coinop.budget.ui.ColorDot
import com.coinop.budget.ui.ConfirmDialog
import com.coinop.budget.ui.DashedDivider
import com.coinop.budget.ui.EmptyState
import com.coinop.budget.ui.IconBox
import com.coinop.budget.ui.MainViewModel
import com.coinop.budget.ui.RText
import com.coinop.budget.ui.RetroButton
import com.coinop.budget.ui.RetroCard
import com.coinop.budget.ui.RetroDialog
import com.coinop.budget.ui.RetroField
import com.coinop.budget.ui.RetroSlider
import com.coinop.budget.ui.RetroTabs
import com.coinop.budget.ui.ScreenTitle
import com.coinop.budget.ui.SectionLabel
import com.coinop.budget.ui.parseHex
import com.coinop.budget.ui.themeSwatches
import com.coinop.budget.ui.theme.AppIcon
import com.coinop.budget.ui.theme.AppIconView
import com.coinop.budget.ui.theme.BackdropStyle
import com.coinop.budget.ui.theme.Face
import com.coinop.budget.ui.theme.FontChoice
import com.coinop.budget.ui.theme.Retro
import com.coinop.budget.ui.theme.ThemeDef
import com.coinop.budget.ui.theme.Themes
import com.coinop.budget.ui.theme.UiStyle
import com.coinop.budget.ui.theme.backdrop
import com.coinop.budget.ui.theme.sticker
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ===================================================================== settings home

private class SettingRowDef(val keywords: String, val content: @Composable () -> Unit)

private class SettingSectionDef(val title: String, val rows: List<SettingRowDef>)

@Composable
fun SettingsScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenCurrency: () -> Unit,
    onOpenTimezone: () -> Unit,
) {
    val a = vm.appearance
    val p = Retro.p
    var query by remember { mutableStateOf("") }
    var fontSheet by remember { mutableStateOf(false) }
    var sizeSheet by remember { mutableStateOf(false) }
    var passwordDialog by remember { mutableStateOf(false) }
    var dataDialog by remember { mutableStateOf(false) }
    var accountDialog by remember { mutableStateOf(false) }
    var updateSheet by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val sections = listOf(
        SettingSectionDef(
            "Appearance",
            listOf(
                SettingRowDef("theme face light dark system mode") {
                    SettingRow(AppIcon.SETTINGS, "Theme", "System follows your phone") {
                        RetroTabs(
                            Face.entries.map { it to it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                            a.face, { vm.updateAppearance(a.copy(face = it)) },
                        )
                    }
                },
                SettingRowDef("ui style arcadia cyberpunk look") {
                    SettingRow(AppIcon.EDIT, "UI style", "Rounded Arcadia or cut-corner Cyberpunk") {
                        RetroTabs(
                            UiStyle.entries.map { it to (if (it == UiStyle.ARCADIA) "Arcadia" else "Cyberpunk") },
                            a.uiStyle, { vm.updateAppearance(a.copy(uiStyle = it)) },
                        )
                    }
                },
                SettingRowDef("font typeface text") {
                    SettingRow(
                        AppIcon.EDIT, "Font", a.font.label,
                        onClick = { fontSheet = true },
                        trailing = { AppIconView(AppIcon.CHEVRON, p.onVariant, 18.dp) },
                    )
                },
                SettingRowDef("text size scale bigger smaller") {
                    SettingRow(
                        AppIcon.SEARCH, "Text size", "${(a.textScale * 100).toInt()}% of normal, across the whole app",
                        onClick = { sizeSheet = true },
                        trailing = { AppIconView(AppIcon.CHEVRON, p.onVariant, 18.dp) },
                    )
                },
                SettingRowDef("colour color theme palette") {
                    SettingRow(AppIcon.SETTINGS, "Colour theme", "25 palettes, each with a light and dark face") {
                        ThemeGrid(selectedId = a.themeId, dark = Retro.dark) { vm.updateAppearance(a.copy(themeId = it)) }
                    }
                },
                SettingRowDef("page background backdrop pattern visibility dots grid scan") {
                    SettingRow(AppIcon.BUDGET, "Page background", "The pattern behind the cards") {
                        ChipFlowBackdrop(a.backdrop) { vm.updateAppearance(a.copy(backdrop = it)) }
                        AnimatedVisibility(
                            visible = a.backdrop != BackdropStyle.PLAIN,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            Column(Modifier.padding(top = 12.dp)) {
                                RText(
                                    "PATTERN VISIBILITY ${(a.backdropStrength * 100).toInt()}%",
                                    size = 12.sp, color = p.onVariant, bold = true,
                                )
                                Spacer(Modifier.height(8.dp))
                                RetroSlider(
                                    a.backdropStrength,
                                    { vm.updateAppearance(a.copy(backdropStrength = it)) },
                                )
                            }
                        }
                    }
                },
            ),
        ),
        SettingSectionDef(
            "Region",
            listOf(
                SettingRowDef("currency money rupee dollar rate") {
                    SettingRow(
                        AppIcon.INVEST, "Currency", "Used across the whole app",
                        onClick = onOpenCurrency,
                        trailing = { SettingValue("${vm.currency} ${Money.symbol(vm.currency)}") },
                    )
                },
                SettingRowDef("timezone time zone today week month") {
                    SettingRow(
                        AppIcon.REFRESH, "Timezone", "Decides what counts as today",
                        onClick = onOpenTimezone,
                        trailing = { SettingValue(vm.settings.timezone) },
                    )
                },
            ),
        ),
        SettingSectionDef(
            "Data",
            listOf(
                SettingRowDef("categories category labels") {
                    SettingRow(
                        AppIcon.BANK, "Categories", "Groups for your spending and income",
                        onClick = onOpenCategories,
                        trailing = { SettingValue("${vm.categories.size}") },
                    )
                },
                SettingRowDef("auto categorize rules keyword") {
                    SettingRow(
                        AppIcon.SWAP, "Auto-categorize rules", "Keywords that pick a category for you",
                        onClick = onOpenRules,
                        trailing = { SettingValue("${vm.rules.size}") },
                    )
                },
            ),
        ),
        SettingSectionDef(
            "About",
            listOf(
                SettingRowDef("about version build release") {
                    SettingRow(
                        AppIcon.SETTINGS, "Version", "Coinop for Android",
                        trailing = { RText("v${BuildConfig.VERSION_NAME}", size = 12.sp, color = p.onVariant, maxLines = 1) },
                    )
                },
                SettingRowDef("update upgrade check new version download install github") {
                    SettingRow(
                        AppIcon.SETTINGS, "Check for updates", "Download the latest release from GitHub",
                        onClick = { updateSheet = true },
                        trailing = { AppIconView(AppIcon.CHEVRON, p.onVariant, 18.dp) },
                    )
                },
            ),
        ),
        SettingSectionDef(
            "Account",
            listOf(
                SettingRowDef("account email signed in") {
                    SettingRow(AppIcon.HOME, "Signed in as", vm.email ?: "")
                },
                SettingRowDef("change password security") {
                    SettingRow(
                        AppIcon.EDIT, "Change password", "Pick a new password for your login",
                        onClick = { passwordDialog = true },
                        trailing = { AppIconView(AppIcon.CHEVRON, p.onVariant, 18.dp) },
                    )
                },
                SettingRowDef("sign out log out logout") {
                    SettingRow(
                        AppIcon.LOGOUT, "Sign out", "Your data stays safe in the cloud",
                        onClick = { vm.signOut() },
                        trailing = { AppIconView(AppIcon.CHEVRON, p.onVariant, 18.dp) },
                    )
                },
                SettingRowDef("delete all my data reset wipe") {
                    SettingRow(
                        AppIcon.TRASH, "Delete all my data", "Banks, entries, budgets, categories and investments",
                        onClick = { dataDialog = true },
                        trailing = { AppIconView(AppIcon.CHEVRON, p.onVariant, 18.dp) },
                        danger = true,
                    )
                },
                SettingRowDef("delete my account remove permanently") {
                    SettingRow(
                        AppIcon.TRASH, "Delete my account", "Removes your login and everything in it",
                        onClick = { accountDialog = true },
                        trailing = { AppIconView(AppIcon.CHEVRON, p.onVariant, 18.dp) },
                        danger = true,
                    )
                },
            ),
        ),
    )

    val q = query.trim().lowercase()
    val visible = sections.mapNotNull { s ->
        val rows = if (q.isEmpty()) s.rows else s.rows.filter { it.keywords.contains(q) || s.title.lowercase().contains(q) }
        if (rows.isEmpty()) null else SettingSectionDef(s.title, rows)
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Settings", onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
        ) {
            SettingsSearch(query) { query = it }
            if (visible.isEmpty()) {
                EmptyState("No settings match \"$query\".")
            }
            visible.forEach { s ->
                SettingsSectionLabel(s.title)
                RetroCard {
                    s.rows.forEachIndexed { i, row ->
                        if (i > 0) DashedDivider()
                        row.content()
                    }
                }
            }
        }
    }

    if (updateSheet) {
        UpdateSheet(vm) { updateSheet = false }
    }

    if (fontSheet) {
        OptionSheet(
            title = "Font",
            subtitle = "The typeface used across the app",
            options = FontChoice.entries.toList(),
            selected = a.font,
            onPick = { vm.updateAppearance(a.copy(font = it)); fontSheet = false },
            onDismiss = { fontSheet = false },
        ) { f ->
            Text(
                "${f.label} (${f.tag})",
                color = Retro.text,
                fontFamily = f.family(Retro.cyber, context),
                fontSize = 16.sp,
            )
        }
    }
    if (sizeSheet) {
        OptionSheet(
            title = "Text size",
            subtitle = "Scales every piece of text in the app",
            options = listOf(0.85f, 1f, 1.15f, 1.3f),
            selected = a.textScale,
            onPick = { vm.updateAppearance(a.copy(textScale = it)); sizeSheet = false },
            onDismiss = { sizeSheet = false },
        ) { v ->
            RText("${(v * 100).toInt()}%" + if (v == 1f) " (normal)" else "", size = 16.sp)
        }
    }
    if (passwordDialog) {
        PasswordDialog(onDismiss = { passwordDialog = false }, onSave = { vm.changePassword(it); passwordDialog = false })
    }
    if (dataDialog) {
        TypedConfirmDialog(
            title = "Delete all my data?",
            text = "This removes every bank, entry, budget, category, rule and investment. Your login, currency and timezone stay. It cannot be undone.",
            confirmLabel = "Delete data",
            onConfirm = { vm.deleteAllData(); dataDialog = false },
            onDismiss = { dataDialog = false },
        )
    }
    if (accountDialog) {
        TypedConfirmDialog(
            title = "Delete my account?",
            text = "This permanently removes your login and all your data. You will be signed out. It cannot be undone.",
            confirmLabel = "Delete account",
            onConfirm = { vm.deleteAccount(); accountDialog = false },
            onDismiss = { accountDialog = false },
        )
    }
}

/** Change-password form: two fields, at least 6 characters, both must match. */
@Composable
private fun PasswordDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var pw by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    val valid = pw.length >= 6 && pw == again
    RetroDialog(
        title = "Change password",
        onDismiss = onDismiss,
        actions = {
            RetroButton("Cancel", onDismiss, face = Retro.p.cream)
            RetroButton("Save", { onSave(pw) }, enabled = valid)
        },
    ) {
        RetroField("New password", pw, { pw = it }, password = true)
        RetroField("Repeat it", again, { again = it }, password = true)
        RText(
            if (pw.isNotEmpty() && pw.length < 6) "Use at least 6 characters."
            else if (again.isNotEmpty() && pw != again) "The two passwords differ."
            else "At least 6 characters.",
            size = 12.sp, color = Retro.p.onVariant,
        )
    }
}

/** Destructive confirmation: the action unlocks only after typing DELETE. */
@Composable
private fun TypedConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var typed by remember { mutableStateOf("") }
    RetroDialog(
        title = title,
        onDismiss = onDismiss,
        actions = {
            RetroButton("Cancel", onDismiss, face = Retro.p.cream)
            RetroButton(confirmLabel, onConfirm, face = Retro.p.red, enabled = typed.trim() == "DELETE")
        },
    ) {
        RText(text, size = 13.sp)
        RetroField("Type DELETE to confirm", typed, { typed = it }, placeholder = "DELETE")
    }
}

@Composable
private fun SettingsSearch(value: String, onChange: (String) -> Unit) {
    val p = Retro.p
    BasicTextField(
        value = value,
        onValueChange = { onChange(it.replace("\n", "")) },
        singleLine = true,
        textStyle = TextStyle(color = Retro.text, fontFamily = Retro.family, fontSize = 15.sp),
        cursorBrush = SolidColor(Retro.primary),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().sticker(p.variant, 8.dp, shadow = false).padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIconView(AppIcon.SEARCH, p.onVariant, 20.dp)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) RText("Search settings", color = p.onVariant, size = 14.sp)
                    inner()
                }
                if (value.isNotEmpty()) {
                    Box(Modifier.clickable { onChange("") }) { AppIconView(AppIcon.CLOSE, p.onVariant, 18.dp) }
                }
            }
        },
    )
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Row(Modifier.padding(top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(Retro.primary))
        Spacer(Modifier.width(8.dp))
        RText(text.uppercase(), size = 12.sp, color = Retro.p.onVariant, bold = true)
    }
}

/** One row inside a section card: icon, title, hint, optional trailing value and optional controls below. */
@Composable
private fun SettingRow(
    icon: AppIcon,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    danger: Boolean = false,
    below: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val p = Retro.p
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIconView(icon, if (danger) p.red else p.onVariant, 22.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                RText(title, bold = true, color = if (danger) p.red else Color.Unspecified)
                if (!subtitle.isNullOrEmpty()) {
                    RText(subtitle, size = 12.sp, color = p.onVariant, maxLines = 2)
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
        if (below != null) {
            Spacer(Modifier.height(12.dp))
            below()
        }
    }
}

@Composable
private fun SettingValue(text: String) {
    val p = Retro.p
    Row(verticalAlignment = Alignment.CenterVertically) {
        RText(
            text, Modifier.widthIn(max = 150.dp), size = 12.sp, color = p.onVariant, maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
        Spacer(Modifier.width(6.dp))
        AppIconView(AppIcon.CHEVRON, p.onVariant, 18.dp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeGrid(selectedId: String, dark: Boolean, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Themes.all.forEach { def -> ThemeSwatch(def, dark, def.id == selectedId) { onSelect(def.id) } }
    }
}

@Composable
private fun ThemeSwatch(def: ThemeDef, dark: Boolean, selected: Boolean, onClick: () -> Unit) {
    val c = if (dark) def.dark else def.light
    Column(Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(60.dp)
                .sticker(c.bg, 10.dp, selected = selected)
                .clickable(onClick = onClick)
                .padding(if (selected) 4.dp else 5.dp),
        ) {
            // the five inks as vertical pixel blocks
            Row(Modifier.fillMaxSize()) {
                listOf(c.surface, c.variant, c.red, c.green, c.mustard).forEach {
                    Box(Modifier.weight(1f).fillMaxHeight().background(it))
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        RText(def.name.uppercase(), size = 12.sp, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlowBackdrop(selected: BackdropStyle, onSelect: (BackdropStyle) -> Unit) {
    val p = Retro.p
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BackdropStyle.entries.forEach { style ->
            Column(Modifier.width(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(60.dp)
                        .sticker(p.bg, 6.dp, selected = style == selected)
                        .backdrop(style, 1f, forceAlpha = 0.4f)
                        .clickable { onSelect(style) },
                )
                RText(style.label.uppercase(), size = 12.sp, maxLines = 1)
            }
        }
    }
}

// ===================================================================== categories

@Composable
fun CategoriesScreen(vm: MainViewModel, onBack: () -> Unit) {
    var editing by remember { mutableStateOf<Category?>(null) }
    var adding by remember { mutableStateOf(false) }
    val p = Retro.p
    val usedColors = vm.categories.mapNotNull { it.color?.uppercase() }.toSet()
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Categories", onBack = onBack) {
            IconBox(AppIcon.PLUS, { adding = true }, face = p.mustard)
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            if (vm.categories.isEmpty()) item { EmptyState("No categories. Tap + to add one.") }
            items(vm.categories, key = { it.id }) { c ->
                Row(
                    Modifier.fillMaxWidth().clickable { editing = c }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ColorDot(c.color, size = 16.dp)
                    Spacer(Modifier.width(12.dp))
                    RText(c.name, Modifier.weight(1f), bold = true, maxLines = 1)
                    AppIconView(AppIcon.EDIT, p.onVariant, 18.dp)
                }
                DashedDivider()
            }
        }
    }
    if (adding) {
        CategoryDialog(
            null, usedColors,
            onDismiss = { adding = false },
            onSave = { n, col -> vm.addCategory(n, col); adding = false },
        )
    }
    editing?.let { c ->
        CategoryDialog(
            c, usedColors,
            onDismiss = { editing = null },
            onSave = { n, col -> vm.updateCategory(c.id, n, col); editing = null },
            onDelete = { vm.deleteCategory(c.id); editing = null },
        )
    }
}

@Composable
private fun CategoryDialog(
    existing: Category?,
    usedColors: Set<String>,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    // colours come from the active theme; a new category starts on the first one no category uses yet
    val swatches = themeSwatches()
    val choices = (listOfNotNull(existing?.color) + swatches).distinct()
    var color by remember {
        mutableStateOf(
            existing?.color
                ?: swatches.firstOrNull { it.uppercase() !in usedColors }
                ?: swatches[usedColors.size % swatches.size],
        )
    }
    var confirm by remember { mutableStateOf(false) }
    RetroDialog(
        title = if (existing == null) "New category" else "Edit category",
        onDismiss = onDismiss,
        actions = {
            if (onDelete != null) RetroButton("Delete", { confirm = true }, face = Retro.p.red)
            RetroButton("Save", { onSave(name, color) }, enabled = name.isNotBlank())
        },
    ) {
        RetroField("Name", name, { name = it })
        RText("COLOUR", size = 12.sp, color = Retro.p.onVariant, bold = true)
        ChipFlow {
            choices.forEach { hex ->
                Box(
                    Modifier
                        .size(32.dp)
                        .sticker(parseHex(hex, Retro.p.onVariant), 4.dp, shadow = false, selected = hex == color)
                        .clickable { color = hex },
                )
            }
        }
    }
    if (confirm && onDelete != null) {
        ConfirmDialog(
            title = "Delete category?",
            text = "Entries in it stay, but show as uncategorized. Rules and budgets using it are removed.",
            confirmLabel = "Delete",
            onConfirm = { confirm = false; onDelete() },
            onDismiss = { confirm = false },
        )
    }
}

// ===================================================================== rules

@Composable
fun RulesScreen(vm: MainViewModel, onBack: () -> Unit) {
    var adding by remember { mutableStateOf(false) }
    val p = Retro.p
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Rules", onBack = onBack) {
            IconBox(AppIcon.PLUS, { adding = true }, face = p.mustard)
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            item {
                RText(
                    "When a note contains a keyword, the entry gets that category. Higher priority wins. " +
                        "A category you pick by hand is never overwritten.",
                    size = 12.sp, color = p.onVariant,
                )
                Spacer(Modifier.height(12.dp))
                RetroButton(
                    "Re-run on uncategorized", { vm.reapplyRules() }, Modifier.fillMaxWidth(),
                    face = p.green, icon = AppIcon.REFRESH,
                )
                Spacer(Modifier.height(12.dp))
            }
            if (vm.rules.isEmpty()) item { EmptyState("No rules yet.") }
            items(vm.rules, key = { it.id }) { r ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        RText(r.keyword.uppercase(), bold = true, maxLines = 1)
                        RText("${vm.categoryName(r.categoryId)}  (priority ${r.priority})", size = 12.sp, color = p.onVariant, maxLines = 1)
                    }
                    IconBox(AppIcon.TRASH, { vm.deleteRule(r.id) }, size = 34.dp)
                }
                DashedDivider()
            }
        }
    }
    if (adding) {
        var keyword by remember { mutableStateOf("") }
        var catId by remember { mutableStateOf<String?>(null) }
        var prio by remember { mutableStateOf("5") }
        RetroDialog(
            title = "New rule",
            onDismiss = { adding = false },
            actions = {
                RetroButton(
                    "Add",
                    { catId?.let { vm.addRule(keyword, it, prio.toIntOrNull() ?: 0); adding = false } },
                    enabled = keyword.isNotBlank() && catId != null,
                )
            },
        ) {
            RetroField("Keyword", keyword, { keyword = it }, placeholder = "e.g. SWIGGY")
            RText("CATEGORY", size = 12.sp, color = p.onVariant, bold = true)
            CategoryChips(vm.categories, catId, { catId = it })
            RetroField("Priority (higher wins)", prio, { prio = it.filter { ch -> ch.isDigit() }.take(3) },
                keyboardType = KeyboardType.Number)
            RText("Tip: give specific keywords (SWIGGY INSTAMART) a higher priority than generic ones (SWIGGY).",
                size = 12.sp, color = p.onVariant)
        }
    }
}

// ===================================================================== currency

private val fallbackCurrencies = listOf("INR", "USD", "EUR", "GBP", "AED", "SGD", "AUD", "CAD", "JPY", "CHF")

@Composable
fun CurrencyScreen(vm: MainViewModel, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<String?>(null) }
    val p = Retro.p
    val codes = (if (vm.fx.isEmpty()) fallbackCurrencies else vm.fx.keys.sorted())
        .filter { query.isBlank() || it.contains(query.trim(), true) || Money.name(it).contains(query.trim(), true) }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Currency", onBack = onBack) {
            IconBox(AppIcon.REFRESH, { vm.refreshRates() })
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            RText("CURRENT: ${vm.currency}  ${Money.name(vm.currency)}", size = 13.sp, bold = true)
            val updated = if (vm.fxTime > 0) {
                "Rates updated " + Instant.ofEpochMilli(vm.fxTime).atZone(TimeUtil.zone(vm.settings.timezone))
                    .format(DateTimeFormatter.ofPattern("dd MMM HH:mm"))
            } else {
                "No live rates yet. Tap refresh."
            }
            RText(updated, size = 12.sp, color = p.onVariant)
            Spacer(Modifier.height(12.dp))
            RetroField("Search", query, { query = it }, placeholder = "USD, euro...")
            Spacer(Modifier.height(8.dp))
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            items(codes, key = { it }) { code ->
                Row(
                    Modifier.fillMaxWidth().clickable { if (code != vm.currency) pending = code }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RText(code, Modifier.width(56.dp), bold = true, color = if (code == vm.currency) Retro.primary else Retro.text)
                    RText(Money.name(code), Modifier.weight(1f), size = 13.sp, color = p.onVariant, maxLines = 1)
                    if (code == vm.currency) AppIconView(AppIcon.CHECK, Retro.primary, 18.dp)
                }
                DashedDivider()
            }
        }
    }

    pending?.let { newCode ->
        val ro = vm.fx[vm.currency]
        val rn = vm.fx[newCode]
        val rate = if (ro != null && rn != null) rn / ro else null
        RetroDialog(
            title = "Switch to $newCode",
            onDismiss = { pending = null },
            actions = { RetroButton("Cancel", { pending = null }, face = p.cream) },
        ) {
            if (rate != null) {
                RText("Live rate: 1 ${vm.currency} = ${String.format(java.util.Locale.US, "%.4f", rate)} $newCode", size = 13.sp, bold = true)
            } else {
                RText("No live rate available right now.", size = 13.sp, color = p.red)
            }
            RText("RELABEL keeps every number as it is and just changes the symbol.", size = 12.sp, color = p.onVariant)
            RetroButton("Relabel only", { vm.changeCurrency(newCode, false); pending = null }, Modifier.fillMaxWidth(), face = p.cream)
            RText("CONVERT multiplies all balances, entries and budgets by the live rate.", size = 12.sp, color = p.onVariant)
            RetroButton(
                "Convert at live rate",
                { vm.changeCurrency(newCode, true); pending = null },
                Modifier.fillMaxWidth(),
                enabled = rate != null,
            )
        }
    }
}

// ===================================================================== timezone

@Composable
fun TimezoneScreen(vm: MainViewModel, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val p = Retro.p
    val zones = remember { ZoneId.getAvailableZoneIds().filter { it.contains("/") && !it.startsWith("Etc/") }.sorted() }
    val shown = zones.filter { query.isBlank() || it.contains(query.trim().replace(' ', '_'), true) }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Timezone", onBack = onBack)
        Column(Modifier.padding(horizontal = 16.dp)) {
            RText("CURRENT: ${vm.settings.timezone}", size = 13.sp, bold = true)
            RText("Decides what counts as today, this week and this month.", size = 12.sp, color = p.onVariant)
            Spacer(Modifier.height(10.dp))
            Chip("Use device timezone", false, { vm.setTimezone(ZoneId.systemDefault().id) })
            Spacer(Modifier.height(10.dp))
            RetroField("Search", query, { query = it }, placeholder = "Kolkata, London...")
            Spacer(Modifier.height(8.dp))
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            items(shown, key = { it }) { z ->
                Row(
                    Modifier.fillMaxWidth().clickable { vm.setTimezone(z) }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RText(z, Modifier.weight(1f), size = 13.sp, maxLines = 1,
                        color = if (z == vm.settings.timezone) Retro.primary else Retro.text,
                        bold = z == vm.settings.timezone)
                    if (z == vm.settings.timezone) AppIconView(AppIcon.CHECK, Retro.primary, 18.dp)
                }
                DashedDivider()
            }
        }
    }
}
