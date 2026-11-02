package com.coinop.budget.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.coinop.budget.ui.RetroTabs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coinop.budget.data.AccountRow
import com.coinop.budget.data.Entry
import com.coinop.budget.domain.Money
import com.coinop.budget.domain.RuleEngine
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
import com.coinop.budget.ui.RetroSheet
import com.coinop.budget.ui.ScreenTitle
import com.coinop.budget.ui.SectionLabel
import com.coinop.budget.ui.Skeleton
import com.coinop.budget.ui.Tag
import com.coinop.budget.ui.theme.AppIcon
import com.coinop.budget.ui.theme.Retro

// ===================================================================== account list

@Composable
fun BankingScreen(vm: MainViewModel, onOpenAccount: (String) -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }
    var showQuick by remember { mutableStateOf(false) }
    val p = Retro.p

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Banking") {
            if (vm.accounts.size >= 2) IconBox(AppIcon.SWAP, { showTransfer = true })
            IconBox(AppIcon.PLUS, { showAdd = true }, face = p.mustard)
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (vm.loading && vm.accounts.isEmpty()) {
                item { Skeleton(height = 80.dp) }
                item { Skeleton(height = 64.dp) }
            } else if (vm.accounts.isEmpty()) {
                item {
                    EmptyState("No bank accounts yet.\nTap + to add one with the money you have in it today.")
                }
            } else {
                item {
                    RetroButton(
                        "Add money in / out", { showQuick = true }, Modifier.fillMaxWidth(),
                        face = p.mustard, icon = AppIcon.PLUS,
                    )
                }
                item {
                    RetroCard(fill = p.floor) {
                        RText("TOTAL IN BANKS", size = 12.sp, color = p.cream, bold = true)
                        Spacer(Modifier.height(4.dp))
                        RText(vm.fmt(vm.totalBank), size = 24.sp, bold = true, color = p.mustard)
                    }
                }
                items(vm.accounts, key = { it.id }) { a ->
                    RetroCard(onClick = { onOpenAccount(a.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                RText(a.name, bold = true, size = 15.sp, maxLines = 1)
                                Spacer(Modifier.height(4.dp))
                                RText(vm.fmt(a.balance), size = 18.sp, bold = true,
                                    color = if (a.balance < 0) p.red else Retro.text)
                            }
                            com.coinop.budget.ui.theme.AppIconView(AppIcon.CHEVRON, p.onVariant, 20.dp)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AccountDialog(
            title = "New account",
            initialName = "",
            initialOpening = "",
            onDismiss = { showAdd = false },
            onSave = { name, opening -> vm.addAccount(name, opening); showAdd = false },
        )
    }
    if (showTransfer) {
        TransferDialog(vm, onDismiss = { showTransfer = false })
    }
    if (showQuick) {
        QuickAddSheet(vm, onDismiss = { showQuick = false })
    }
}

@Composable
private fun AccountDialog(
    title: String,
    initialName: String,
    initialOpening: String,
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initialName) }
    var opening by remember { mutableStateOf(initialOpening) }
    val amount = Money.parse(opening)
    val valid = name.isNotBlank() && amount != null
    RetroDialog(
        title = title,
        onDismiss = onDismiss,
        actions = {
            if (onDelete != null) RetroButton("Delete", onDelete, face = Retro.p.red)
            RetroButton("Save", { if (amount != null) onSave(name, amount) }, enabled = valid)
        },
    ) {
        RetroField("Bank name", name, { name = it }, placeholder = "e.g. Kotak, ICICI")
        RetroField(
            if (onDelete == null) "Money in it today" else "Opening balance",
            opening, { opening = it },
            keyboardType = KeyboardType.Decimal,
            placeholder = "0.00",
        )
    }
}

@Composable
private fun TransferDialog(vm: MainViewModel, onDismiss: () -> Unit) {
    var from by remember { mutableStateOf(vm.accounts.firstOrNull()?.id) }
    var to by remember { mutableStateOf(vm.accounts.getOrNull(1)?.id) }
    var amountText by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(vm.today) }
    var note by remember { mutableStateOf("") }
    val amount = Money.parse(amountText)
    val valid = from != null && to != null && from != to && amount != null && amount > 0
    RetroDialog(
        title = "Move money",
        onDismiss = onDismiss,
        actions = {
            RetroButton("Cancel", onDismiss, face = Retro.p.cream)
            RetroButton("Move", {
                val f = from
                val t = to
                if (f != null && t != null && amount != null) {
                    vm.transfer(f, t, amount, date, note)
                    onDismiss()
                }
            }, enabled = valid)
        },
    ) {
        RText("FROM", size = 12.sp, color = Retro.p.onVariant, bold = true)
        ChipFlow { vm.accounts.forEach { a -> Chip(a.name, a.id == from, { from = a.id }) } }
        RText("TO", size = 12.sp, color = Retro.p.onVariant, bold = true)
        ChipFlow { vm.accounts.forEach { a -> Chip(a.name, a.id == to, { to = a.id }) } }
        RetroField("Amount", amountText, { amountText = it }, keyboardType = KeyboardType.Decimal, placeholder = "0.00")
        DateStepper(date, { date = it })
        RetroField("Note (optional)", note, { note = it })
        RText("Transfers between your own accounts never count as spending.", size = 12.sp, color = Retro.p.onVariant)
    }
}

// ===================================================================== account detail

@Composable
fun AccountScreen(vm: MainViewModel, accountId: String, onBack: () -> Unit) {
    val account: AccountRow? = vm.accounts.firstOrNull { it.id == accountId }
    LaunchedEffect(accountId) { vm.loadEntries(accountId) }
    LaunchedEffect(account == null) { if (account == null) onBack() }
    if (account == null) return

    var sheetIn by remember { mutableStateOf<Boolean?>(null) } // null = closed, true = in, false = out
    var detail by remember { mutableStateOf<Entry?>(null) }
    var editing by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }
    val p = Retro.p

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(account.name, onBack = onBack) {
            IconBox(AppIcon.EDIT, { editing = true })
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                RetroCard(fill = p.floor) {
                    RText("BALANCE", size = 12.sp, color = p.cream, bold = true)
                    Spacer(Modifier.height(4.dp))
                    RText(vm.fmt(account.balance), size = 26.sp, bold = true, color = p.mustard)
                }
                Spacer(Modifier.height(12.dp))
                ButtonRow {
                    RetroButton("In", { sheetIn = true }, Modifier.weight(1f), face = p.green, icon = AppIcon.PLUS)
                    RetroButton("Out", { sheetIn = false }, Modifier.weight(1f), face = p.red, icon = AppIcon.MINUS)
                    if (vm.accounts.size >= 2) {
                        RetroButton("Move", { showTransfer = true }, Modifier.weight(1f), face = p.cream, icon = AppIcon.SWAP)
                    }
                }
                SectionLabel("History")
            }
            if (vm.accountEntries.isEmpty()) {
                item { EmptyState("No entries yet. Log money in or out as it happens.") }
            } else {
                items(vm.accountEntries, key = { it.id }) { e ->
                    EntryRow(vm, e) { detail = e }
                    DashedDivider()
                }
            }
        }
    }

    sheetIn?.let { isIn ->
        EntrySheet(vm, accountId, isIn, onDismiss = { sheetIn = null })
    }
    detail?.let { e ->
        EntryDetailSheet(vm, e, onDismiss = { detail = null })
    }
    if (showTransfer) TransferDialog(vm, onDismiss = { showTransfer = false })
    if (editing) {
        var confirmDelete by remember { mutableStateOf(false) }
        AccountDialog(
            title = "Edit account",
            initialName = account.name,
            initialOpening = account.openingBalance.toString(),
            onDismiss = { editing = false },
            onSave = { name, opening -> vm.updateAccount(account.id, name, opening); editing = false },
            onDelete = { confirmDelete = true },
        )
        if (confirmDelete) {
            ConfirmDialog(
                title = "Delete account?",
                text = "This removes ${account.name} and all of its entries. This cannot be undone.",
                confirmLabel = "Delete",
                onConfirm = { vm.deleteAccount(account.id); editing = false; confirmDelete = false },
                onDismiss = { confirmDelete = false },
            )
        }
    }
}

@Composable
private fun EntryRow(vm: MainViewModel, e: Entry, onClick: () -> Unit) {
    val p = Retro.p
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            RText(e.note ?: if (e.isTransfer) "Transfer" else vm.categoryName(e.categoryId), bold = true, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (e.isTransfer) {
                    Tag("Transfer", p.cream)
                } else {
                    ColorDot(vm.categoryColor(e.categoryId))
                    Spacer(Modifier.width(6.dp))
                    RText(vm.categoryName(e.categoryId), size = 12.sp, color = p.onVariant, maxLines = 1)
                }
                Spacer(Modifier.width(8.dp))
                RText(e.localDate.format(TimeUtil.shortDay), size = 12.sp, color = p.onVariant, maxLines = 1)
            }
        }
        Spacer(Modifier.width(8.dp))
        RText(
            (if (e.isIn) "+" else "-") + vm.fmt(e.amount),
            bold = true,
            color = if (e.isIn) p.green else p.red,
            maxLines = 1,
        )
    }
}

// ===================================================================== add entry

@Composable
private fun EntrySheet(vm: MainViewModel, accountId: String, isIn: Boolean, onDismiss: () -> Unit) {
    RetroSheet(onDismiss) { EntryForm(vm, accountId, isIn, onDismiss, showHeader = true) }
}

/** Add money in or out without opening a bank first: pick the bank here, then fill the same form. */
@Composable
fun QuickAddSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    val p = Retro.p
    var accountId by remember { mutableStateOf(vm.accounts.firstOrNull()?.id) }
    var isIn by remember { mutableStateOf(false) }
    RetroSheet(onDismiss) {
        RText("ADD ENTRY", size = 16.sp, bold = true)
        val acc = accountId
        if (acc == null) {
            RText(
                "Add a bank account first. Open the Banking tab, tap + and enter the money you have in it.",
                size = 13.sp, color = p.onVariant,
            )
        } else {
            RetroTabs(listOf(false to "Money out", true to "Money in"), isIn, { isIn = it })
            RText("BANK", size = 12.sp, color = p.onVariant, bold = true)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                vm.accounts.forEach { a -> Chip(a.name, a.id == acc, { accountId = a.id }) }
            }
            EntryForm(vm, acc, isIn, onDismiss, showHeader = false)
        }
    }
}

@Composable
private fun ColumnScope.EntryForm(
    vm: MainViewModel,
    accountId: String,
    isIn: Boolean,
    onDismiss: () -> Unit,
    showHeader: Boolean,
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(vm.today) }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var manual by remember { mutableStateOf(false) }
    var suggested by remember { mutableStateOf(false) }
    val amount = Money.parse(amountText)
    val p = Retro.p

    // Keyword rules suggest a category from the note until the user picks one by hand.
    LaunchedEffect(note) {
        if (!manual) {
            val match = RuleEngine.match(note, vm.rules)
            categoryId = match
            suggested = match != null
        }
    }

    run {
        if (showHeader) {
            RText(if (isIn) "MONEY IN" else "MONEY OUT", size = 16.sp, bold = true, color = if (isIn) p.green else p.red)
        }
        RetroField("Amount", amountText, { amountText = it }, keyboardType = KeyboardType.Decimal, placeholder = "0.00")
        RetroField("Note", note, { note = it }, placeholder = "e.g. Swiggy dinner")
        DateStepper(date, { date = it })
        Row(verticalAlignment = Alignment.CenterVertically) {
            RText("CATEGORY", size = 12.sp, color = p.onVariant, bold = true)
            Spacer(Modifier.width(8.dp))
            if (suggested && !manual) Tag("auto", p.mustard)
        }
        CategoryChips(vm.categories, categoryId, onSelect = { categoryId = it; manual = true })
        RetroButton(
            "Save",
            {
                if (amount != null && amount > 0) {
                    val cat = categoryId ?: RuleEngine.uncategorizedId(vm.categories)
                    vm.addEntry(accountId, date, amount, isIn, cat, note, manual)
                    onDismiss()
                }
            },
            Modifier.fillMaxWidth(),
            enabled = amount != null && amount > 0,
            face = if (isIn) p.green else p.red,
        )
    }
}

@Composable
private fun EntryDetailSheet(vm: MainViewModel, e: Entry, onDismiss: () -> Unit) {
    val p = Retro.p
    var confirm by remember { mutableStateOf(false) }
    RetroSheet(onDismiss) {
        RText(
            (if (e.isIn) "+" else "-") + vm.fmt(e.amount),
            size = 22.sp, bold = true, color = if (e.isIn) p.green else p.red,
        )
        RText(e.note ?: "No note")
        RText(e.localDate.format(TimeUtil.dayLabel).uppercase(), size = 12.sp, color = p.onVariant)
        if (e.isTransfer) {
            RText("A transfer between your accounts. Deleting it removes both sides.", size = 13.sp, color = p.onVariant)
        } else {
            RText("CATEGORY", size = 12.sp, color = p.onVariant, bold = true)
            CategoryChips(vm.categories, e.categoryId, onSelect = { vm.setEntryCategory(e, it); onDismiss() })
            RText("Picking a category here is permanent for this entry; rules never overwrite it.",
                size = 12.sp, color = p.onVariant)
        }
        RetroButton("Delete entry", { confirm = true }, Modifier.fillMaxWidth(), face = p.red, icon = AppIcon.TRASH)
    }
    if (confirm) {
        ConfirmDialog(
            title = "Delete entry?",
            text = "This changes the account balance.",
            confirmLabel = "Delete",
            onConfirm = { vm.deleteEntry(e); confirm = false; onDismiss() },
            onDismiss = { confirm = false },
        )
    }
}
