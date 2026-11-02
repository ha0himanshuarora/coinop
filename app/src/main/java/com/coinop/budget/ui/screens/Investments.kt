package com.coinop.budget.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coinop.budget.data.Holding
import com.coinop.budget.data.Instrument
import com.coinop.budget.domain.Money
import com.coinop.budget.domain.TimeUtil
import com.coinop.budget.ui.Chip
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
import com.coinop.budget.ui.RetroTabs
import com.coinop.budget.ui.ScreenTitle
import com.coinop.budget.ui.SectionLabel
import com.coinop.budget.ui.Skeleton
import com.coinop.budget.ui.Tag
import com.coinop.budget.ui.theme.AppIcon
import com.coinop.budget.ui.theme.Retro
import androidx.compose.runtime.LaunchedEffect
import com.coinop.budget.data.HoldingLot
import java.time.LocalDate
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun fmtQty(q: Double): String =
    if (q % 1.0 == 0.0) q.toLong().toString() else String.format(Locale.US, "%.4f", q).trimEnd('0').trimEnd('.')

@Composable
fun InvestmentsScreen(vm: MainViewModel) {
    var showAddAccount by remember { mutableStateOf(false) }
    var showAddHolding by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Holding?>(null) }
    val p = Retro.p

    val selected = vm.selectedInvest?.takeIf { id -> vm.investAccounts.any { it.id == id } }
    val shown = vm.holdingsOf(selected)
    val invested = shown.sumOf { vm.holdingInvestedInr(it) }
    val current = shown.sumOf { vm.holdingValueInr(it) }
    val pnl = current - invested
    val pnlPct = if (invested > 0) pnl / invested * 100.0 else 0.0
    val selectedAccount = vm.investAccounts.firstOrNull { it.id == selected }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Invest") {
            IconBox(AppIcon.REFRESH, { vm.refreshPricesNow() })
            IconBox(AppIcon.PLUS, {
                if (vm.investAccounts.isEmpty()) showAddAccount = true else showAddHolding = true
            }, face = p.mustard)
        }

        // account switcher: ALL + each demat/MF account
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Chip("ALL", selected == null, { vm.selectedInvest = null })
            vm.investAccounts.forEach { a ->
                Chip(a.name, selected == a.id, { vm.selectedInvest = a.id })
            }
            Chip("+ ACCOUNT", false, { showAddAccount = true })
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (vm.loading && vm.investAccounts.isEmpty()) {
                item { Skeleton(height = 96.dp) }
            } else if (vm.investAccounts.isEmpty()) {
                item {
                    EmptyState(
                        "No demat or fund accounts yet.\nAdd one (Upstox, Groww...) with + ACCOUNT, " +
                            "then add the stocks and funds it holds.",
                    )
                }
            } else {
                item {
                    RetroCard(fill = p.floor) {
                        RText(
                            (selectedAccount?.name ?: "ALL ACCOUNTS").uppercase(),
                            size = 12.sp, color = p.cream, bold = true,
                        )
                        Spacer(Modifier.height(4.dp))
                        RText(vm.fmtInr(current), size = 24.sp, bold = true, color = p.mustard)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column {
                                RText("INVESTED", size = 12.sp, color = p.cream)
                                RText(vm.fmtInr(invested), size = 14.sp, bold = true, color = p.cream)
                            }
                            Column {
                                RText("P&L", size = 12.sp, color = p.cream)
                                RText(
                                    (if (pnl >= 0) "+" else "-") + vm.fmtInr(kotlin.math.abs(pnl)) +
                                        String.format(Locale.US, " (%.1f%%)", pnlPct),
                                    size = 14.sp, bold = true,
                                    color = if (pnl >= 0) p.green else p.red,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        val updated = if (vm.pricesLoading) {
                            "Fetching live prices..."
                        } else if (vm.pricesUpdatedAt > 0) {
                            "Prices updated " + Instant.ofEpochMilli(vm.pricesUpdatedAt)
                                .atZone(TimeUtil.zone(vm.settings.timezone))
                                .format(DateTimeFormatter.ofPattern("HH:mm"))
                        } else {
                            "No live prices yet. Tap refresh."
                        }
                        RText(updated, size = 12.sp, color = p.cream)
                        if (vm.currency != "INR" && vm.inrRate() == null) {
                            RText("No live exchange rate yet, showing INR.", size = 12.sp, color = p.cream)
                        }
                    }
                }

                if (selectedAccount != null) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip("Rename", false, { showRename = true })
                            Chip("Delete account", false, { confirmDelete = true })
                        }
                    }
                } else {
                    item { SectionLabel("Accounts") }
                    items(vm.investAccounts, key = { it.id }) { a ->
                        val hs = vm.holdingsOf(a.id)
                        RetroCard(onClick = { vm.selectedInvest = a.id }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    RText(a.name, bold = true, maxLines = 1)
                                    RText("${hs.size} holdings", size = 12.sp, color = p.onVariant)
                                }
                                RText(vm.fmtInr(hs.sumOf { vm.holdingValueInr(it) }), bold = true, maxLines = 1)
                            }
                        }
                    }
                }

                item { SectionLabel("Holdings") }
                if (shown.isEmpty()) {
                    item { EmptyState("No holdings here yet. Tap + to add one.") }
                } else {
                    items(shown, key = { it.id }) { h ->
                        HoldingCard(vm, h, showAccount = selected == null) { editing = h }
                    }
                }
            }
        }
    }

    if (showAddAccount) {
        var name by remember { mutableStateOf("") }
        RetroDialog(
            title = "New account",
            onDismiss = { showAddAccount = false },
            actions = {
                RetroButton("Add", { vm.addInvestAccount(name); showAddAccount = false }, enabled = name.isNotBlank())
            },
        ) {
            RetroField("Account name", name, { name = it }, placeholder = "e.g. Upstox, Groww")
        }
    }
    if (showRename && selectedAccount != null) {
        var name by remember { mutableStateOf(selectedAccount.name) }
        RetroDialog(
            title = "Rename account",
            onDismiss = { showRename = false },
            actions = {
                RetroButton("Save", { vm.renameInvestAccount(selectedAccount.id, name); showRename = false },
                    enabled = name.isNotBlank())
            },
        ) {
            RetroField("Account name", name, { name = it })
        }
    }
    if (confirmDelete && selectedAccount != null) {
        ConfirmDialog(
            title = "Delete account?",
            text = "This removes ${selectedAccount.name} and every holding in it.",
            confirmLabel = "Delete",
            onConfirm = { vm.deleteInvestAccount(selectedAccount.id); confirmDelete = false },
            onDismiss = { confirmDelete = false },
        )
    }
    if (showAddHolding) {
        AddHoldingSheet(vm, selected, onDismiss = { showAddHolding = false })
    }
    editing?.let { h -> EditHoldingSheet(vm, h, onDismiss = { editing = null }) }
}

@Composable
private fun HoldingCard(vm: MainViewModel, h: Holding, showAccount: Boolean, onClick: () -> Unit) {
    val p = Retro.p
    val price = vm.prices[h.id]
    val value = vm.holdingValueInr(h)
    val cost = vm.holdingInvestedInr(h)
    val pnl = value - cost
    val hasPrice = price != null
    RetroCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RText(h.name, Modifier.weight(1f), bold = true, size = 14.sp, maxLines = 2)
            Spacer(Modifier.width(8.dp))
            Tag(if (h.kind == "mf") "MF" else "STOCK", if (h.kind == "mf") p.green else p.mustard)
        }
        if (showAccount) {
            val an = vm.investAccounts.firstOrNull { it.id == h.investAccountId }?.name
            if (an != null) RText(an, size = 12.sp, color = p.onVariant, maxLines = 1)
        }
        Spacer(Modifier.height(6.dp))
        RText(
            "${fmtQty(h.quantity)} x " + (if (hasPrice) vm.fmtInr(price!!) else "no price"),
            size = 13.sp, color = p.onVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            RText(vm.fmtInr(value), Modifier.weight(1f), bold = true, size = 16.sp, maxLines = 1)
            if (hasPrice && cost > 0) {
                RText(
                    (if (pnl >= 0) "+" else "-") + vm.fmtInr(kotlin.math.abs(pnl)),
                    size = 13.sp, bold = true, color = if (pnl >= 0) p.green else p.red, maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun AddHoldingSheet(vm: MainViewModel, defaultAccountId: String?, onDismiss: () -> Unit) {
    val p = Retro.p
    val scope = rememberCoroutineScope()
    var kind by remember { mutableStateOf("stock") }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Instrument>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<Instrument?>(null) }
    var manual by remember { mutableStateOf(false) }
    var manualSymbol by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(vm.today.toString()) }
    var accountId by remember { mutableStateOf(defaultAccountId ?: vm.investAccounts.firstOrNull()?.id) }

    val instrument: Instrument? = when {
        manual && manualSymbol.isNotBlank() ->
            Instrument(kind, manualSymbol.trim(), manualName.trim().ifEmpty { manualSymbol.trim() })
        else -> picked
    }
    val qty = Money.parse(qtyText)
    val cost = Money.parse(costText)
    val amount = Money.parse(amountText)
    val investDate = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
    val valid = instrument != null && accountId != null && qty != null && qty > 0 && cost != null && cost >= 0
    val mfValid = instrument != null && accountId != null && amount != null && amount > 0 &&
        investDate != null && !investDate.isAfter(vm.today) && !vm.investBusy

    RetroSheet(onDismiss) {
        RText("ADD HOLDING", size = 16.sp, bold = true)
        RText("ACCOUNT", size = 12.sp, color = p.onVariant, bold = true)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            vm.investAccounts.forEach { a -> Chip(a.name, a.id == accountId, { accountId = a.id }) }
        }
        RetroTabs(
            listOf("stock" to "Stock", "mf" to "Mutual fund"), kind,
            { kind = it; results = emptyList(); picked = null; searched = false },
        )

        if (!manual) {
            if (picked == null) {
                RetroField(
                    if (kind == "mf") "Search fund name" else "Search company (NSE/BSE)",
                    query, { query = it },
                    placeholder = if (kind == "mf") "e.g. parag parikh flexi" else "e.g. reliance",
                )
                RetroButton(
                    if (searching) "Searching..." else "Search",
                    {
                        scope.launch {
                            searching = true
                            results = vm.search(kind, query.trim())
                            searched = true
                            searching = false
                        }
                    },
                    Modifier.fillMaxWidth(),
                    enabled = query.trim().length >= 2 && !searching,
                    icon = AppIcon.SEARCH,
                )
                results.forEach { r ->
                    Column(
                        Modifier.fillMaxWidth().clickable { picked = r }.padding(vertical = 8.dp),
                    ) {
                        RText(r.name, size = 14.sp, bold = true, maxLines = 2)
                        RText(r.symbol + (r.exchange?.let { "  $it" } ?: ""), size = 12.sp, color = p.onVariant)
                    }
                }
                if (searched && results.isEmpty() && !searching) {
                    RText("Nothing found, or the price service is unreachable. You can type the symbol yourself.",
                        size = 12.sp, color = p.onVariant)
                }
            } else {
                picked?.let { pk ->
                    RetroCard(fill = p.variant) {
                        RText(pk.name, bold = true, maxLines = 2)
                        RText(pk.symbol, size = 12.sp, color = p.onVariant)
                    }
                    Chip("Change", false, { picked = null })
                }
            }
            Chip("Type symbol manually", false, { manual = true })
        } else {
            RetroField(
                if (kind == "mf") "AMFI scheme code" else "Yahoo symbol",
                manualSymbol, { manualSymbol = it },
                placeholder = if (kind == "mf") "e.g. 122639" else "e.g. RELIANCE.NS",
            )
            RetroField("Display name (optional)", manualName, { manualName = it })
            Chip("Back to search", false, { manual = false })
        }

        if (kind == "mf") {
            RetroField(
                "Amount invested (INR)", amountText, { amountText = it },
                keyboardType = KeyboardType.Decimal, placeholder = "0.00",
            )
            RetroField("Date invested (YYYY-MM-DD)", dateText, { dateText = it }, placeholder = "2025-01-31")
            RText(
                "Units are worked out from the fund's NAV on that date. Add more investments later from the fund's page.",
                size = 12.sp, color = p.onVariant,
            )
            RetroButton(
                if (vm.investBusy) "Saving..." else "Save investment",
                {
                    val ins = instrument
                    val acc = accountId
                    if (ins != null && acc != null && amount != null && investDate != null) {
                        vm.addMfInvestment(acc, ins, amount, investDate)
                        onDismiss()
                    }
                },
                Modifier.fillMaxWidth(),
                enabled = mfValid,
            )
        } else {
            RetroField("Quantity / units", qtyText, { qtyText = it }, keyboardType = KeyboardType.Decimal, placeholder = "0")
            RetroField(
                "Average buy price per unit (INR)", costText, { costText = it },
                keyboardType = KeyboardType.Decimal, placeholder = "0.00",
            )
            RetroButton(
                "Save holding",
                {
                    val ins = instrument
                    val acc = accountId
                    if (ins != null && acc != null && qty != null && cost != null) {
                        vm.addHolding(acc, ins, qty, cost)
                        onDismiss()
                    }
                },
                Modifier.fillMaxWidth(),
                enabled = valid,
            )
        }
    }
}

@Composable
private fun EditHoldingSheet(vm: MainViewModel, h0: Holding, onDismiss: () -> Unit) {
    // always read the live row: investments change units and average cost while the sheet is open
    val h = vm.holdings.firstOrNull { it.id == h0.id }
    if (h == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    if (h.kind == "mf") MfHoldingSheet(vm, h, onDismiss) else StockHoldingSheet(vm, h, onDismiss)
}

@Composable
private fun MfHoldingSheet(vm: MainViewModel, h: Holding, onDismiss: () -> Unit) {
    val p = Retro.p
    val lots = vm.lotsOf(h.id).sortedByDescending { it.investDate }
    var amountText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(vm.today.toString()) }
    var confirmHolding by remember { mutableStateOf(false) }
    var lotToDelete by remember { mutableStateOf<HoldingLot?>(null) }
    val amount = Money.parse(amountText)
    val date = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
    val value = vm.holdingValueInr(h)
    val invested = vm.holdingInvestedInr(h)
    val pnl = value - invested

    RetroSheet(onDismiss) {
        RText(h.name, size = 15.sp, bold = true, maxLines = 2)
        RText(h.symbol, size = 12.sp, color = p.onVariant)
        RetroCard(fill = p.variant) {
            Row {
                Column(Modifier.weight(1f)) {
                    RText("INVESTED", size = 12.sp, color = p.onVariant)
                    RText(vm.fmtInr(invested), size = 14.sp, bold = true, maxLines = 1)
                }
                Column(Modifier.weight(1f)) {
                    RText("VALUE NOW", size = 12.sp, color = p.onVariant)
                    RText(vm.fmtInr(value), size = 14.sp, bold = true, maxLines = 1)
                }
            }
            Spacer(Modifier.height(6.dp))
            RText(
                fmtQty(h.quantity) + " units   " + (if (pnl >= 0) "+" else "-") + vm.fmtInr(kotlin.math.abs(pnl)),
                size = 12.sp, bold = true, color = if (pnl >= 0) p.green else p.red, maxLines = 1,
            )
        }

        RText("INVESTMENTS", size = 12.sp, color = p.onVariant, bold = true)
        if (lots.isEmpty()) {
            RText(
                "${fmtQty(h.quantity)} units added earlier at ${vm.fmtInr(h.avgCost)} average. New investments are listed here.",
                size = 12.sp, color = p.onVariant,
            )
        }
        lots.forEach { lot ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val day = runCatching { LocalDate.parse(lot.investDate).format(TimeUtil.dayLabel) }.getOrDefault(lot.investDate)
                    RText(day, size = 13.sp, bold = true, maxLines = 1)
                    RText(
                        fmtQty(lot.units) + " units at " + vm.fmtInr(lot.nav),
                        size = 12.sp, color = p.onVariant, maxLines = 1,
                    )
                }
                RText(vm.fmtInr(lot.amount), size = 13.sp, bold = true, maxLines = 1)
                Spacer(Modifier.width(8.dp))
                IconBox(AppIcon.TRASH, { lotToDelete = lot }, size = 34.dp)
            }
            DashedDivider()
        }

        RText("ADD INVESTMENT", size = 12.sp, color = p.onVariant, bold = true)
        RetroField(
            "Amount invested (INR)", amountText, { amountText = it },
            keyboardType = KeyboardType.Decimal, placeholder = "0.00",
        )
        RetroField("Date invested (YYYY-MM-DD)", dateText, { dateText = it }, placeholder = "2025-01-31")
        RetroButton(
            if (vm.investBusy) "Saving..." else "Add investment",
            {
                if (amount != null && date != null) {
                    vm.addMfInvestment(h.investAccountId, Instrument("mf", h.symbol, h.name), amount, date)
                    amountText = ""
                }
            },
            Modifier.fillMaxWidth(),
            enabled = amount != null && amount > 0 && date != null && !date.isAfter(vm.today) && !vm.investBusy,
        )
        RetroButton("Delete fund", { confirmHolding = true }, Modifier.fillMaxWidth(), face = p.red, icon = AppIcon.TRASH)
    }

    lotToDelete?.let { lot ->
        ConfirmDialog(
            title = "Delete investment?",
            text = "Remove the ${vm.fmtInr(lot.amount)} investment from ${lot.investDate}?",
            confirmLabel = "Delete",
            onConfirm = { vm.deleteLot(lot); lotToDelete = null },
            onDismiss = { lotToDelete = null },
        )
    }
    if (confirmHolding) {
        ConfirmDialog(
            title = "Delete fund?",
            text = "Remove ${h.name} and all its investments from this account?",
            confirmLabel = "Delete",
            onConfirm = { vm.deleteHolding(h.id); confirmHolding = false; onDismiss() },
            onDismiss = { confirmHolding = false },
        )
    }
}

@Composable
private fun StockHoldingSheet(vm: MainViewModel, h: Holding, onDismiss: () -> Unit) {
    val p = Retro.p
    var qtyText by remember { mutableStateOf(fmtQty(h.quantity)) }
    var costText by remember { mutableStateOf(h.avgCost.toString()) }
    var confirm by remember { mutableStateOf(false) }
    val qty = Money.parse(qtyText)
    val cost = Money.parse(costText)
    RetroSheet(onDismiss) {
        RText(h.name, size = 15.sp, bold = true, maxLines = 2)
        RText(h.symbol, size = 12.sp, color = p.onVariant)
        RetroField("Quantity / units", qtyText, { qtyText = it }, keyboardType = KeyboardType.Decimal)
        RetroField("Average buy price per unit (INR)", costText, { costText = it }, keyboardType = KeyboardType.Decimal)
        RetroButton(
            "Save",
            { if (qty != null && cost != null) { vm.updateHolding(h.id, qty, cost); onDismiss() } },
            Modifier.fillMaxWidth(),
            enabled = qty != null && qty >= 0 && cost != null && cost >= 0,
        )
        RetroButton("Delete holding", { confirm = true }, Modifier.fillMaxWidth(), face = p.red, icon = AppIcon.TRASH)
    }
    if (confirm) {
        ConfirmDialog(
            title = "Delete holding?",
            text = "Remove ${h.name} from this account?",
            confirmLabel = "Delete",
            onConfirm = { vm.deleteHolding(h.id); confirm = false; onDismiss() },
            onDismiss = { confirm = false },
        )
    }
}
