package com.coinop.budget.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coinop.budget.data.Entry
import com.coinop.budget.data.WidgetTypes
import com.coinop.budget.domain.Budgets
import com.coinop.budget.domain.Period
import com.coinop.budget.domain.Stats
import com.coinop.budget.ui.BlockBar
import com.coinop.budget.ui.ColorDot
import com.coinop.budget.ui.DashedDivider
import com.coinop.budget.ui.IconBox
import com.coinop.budget.ui.MainViewModel
import com.coinop.budget.ui.RText
import com.coinop.budget.ui.RetroButton
import com.coinop.budget.ui.RetroCard
import com.coinop.budget.ui.RetroSheet
import com.coinop.budget.ui.RetroSwitch
import com.coinop.budget.ui.ScreenTitle
import com.coinop.budget.ui.Skeleton
import com.coinop.budget.ui.theme.AppIcon
import com.coinop.budget.ui.theme.Retro
import kotlinx.coroutines.CancellationException

@Composable
fun HomeScreen(vm: MainViewModel, onOpenSettings: () -> Unit) {
    var manage by remember { mutableStateOf(false) }
    var quickAdd by remember { mutableStateOf(false) }
    val monthRange = Budgets.rangeOf(Period.MONTHLY, vm.today)
    val entries by produceState(initialValue = emptyList<Entry>(), monthRange, vm.dataVersion) {
        value = try {
            vm.entriesBetween(monthRange)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }
    val visible = vm.widgets.sortedBy { it.position }.filter { it.isVisible }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Coinop") {
            IconBox(AppIcon.PLUS, { quickAdd = true }, face = Retro.p.mustard)
            IconBox(AppIcon.EDIT, { manage = true })
            IconBox(AppIcon.SETTINGS, onOpenSettings)
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (vm.loading && vm.widgets.isEmpty()) {
                item { Skeleton(height = 110.dp) }
                item { Skeleton(height = 110.dp) }
                item { Skeleton(height = 110.dp) }
            }
            items(visible, key = { it.id }) { w ->
                when (w.widgetType) {
                    WidgetTypes.BALANCE -> BalanceWidget(vm, entries)
                    WidgetTypes.NET_WORTH -> NetWorthWidget(vm)
                    WidgetTypes.BUDGET -> BudgetWidget(vm, entries)
                    WidgetTypes.TOP_CATEGORIES -> TopCategoriesWidget(vm, entries)
                    else -> Unit
                }
            }
        }
    }

    if (manage) ManageWidgetsSheet(vm, onDismiss = { manage = false })
    if (quickAdd) QuickAddSheet(vm, onDismiss = { quickAdd = false })
}

@Composable
private fun BalanceWidget(vm: MainViewModel, entries: List<Entry>) {
    val p = Retro.p
    RetroCard {
        RText("BALANCES", size = 12.sp, color = p.onVariant, bold = true)
        Spacer(Modifier.height(4.dp))
        RText(vm.fmt(vm.totalBank), size = 22.sp, bold = true)
        if (vm.accounts.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            DashedDivider()
            vm.accounts.forEach { a ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    RText(a.name, Modifier.weight(1f), size = 13.sp, maxLines = 1)
                    RText(vm.fmt(a.balance), size = 13.sp, bold = true, maxLines = 1,
                        color = if (a.balance < 0) p.red else Retro.text)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        DashedDivider()
        Spacer(Modifier.height(8.dp))
        Row {
            Column(Modifier.weight(1f)) {
                RText("IN THIS MONTH", size = 12.sp, color = p.onVariant)
                RText("+" + vm.fmt(Stats.totalIncome(entries)), size = 13.sp, bold = true, color = p.green, maxLines = 1)
            }
            Column(Modifier.weight(1f)) {
                RText("OUT THIS MONTH", size = 12.sp, color = p.onVariant)
                RText("-" + vm.fmt(Stats.totalSpend(entries)), size = 13.sp, bold = true, color = p.red, maxLines = 1)
            }
        }
    }
}

@Composable
private fun NetWorthWidget(vm: MainViewModel) {
    val p = Retro.p
    RetroCard(fill = p.floor) {
        RText("NET WORTH", size = 12.sp, color = p.cream, bold = true)
        Spacer(Modifier.height(4.dp))
        RText(vm.fmt(vm.netWorth), size = 24.sp, bold = true, color = p.mustard)
        Spacer(Modifier.height(8.dp))
        Row {
            Column(Modifier.weight(1f)) {
                RText("BANKS", size = 12.sp, color = p.cream)
                RText(vm.fmt(vm.totalBank), size = 13.sp, bold = true, color = p.cream, maxLines = 1)
            }
            Column(Modifier.weight(1f)) {
                RText("INVESTMENTS", size = 12.sp, color = p.cream)
                RText(vm.fmtInr(vm.investValueInr), size = 13.sp, bold = true, color = p.cream, maxLines = 1)
            }
        }
    }
}

@Composable
private fun BudgetWidget(vm: MainViewModel, entries: List<Entry>) {
    val p = Retro.p
    val range = Budgets.rangeOf(Period.MONTHLY, vm.today)
    val spent = Stats.totalSpend(entries)
    val spendMap = Stats.spendByCategory(entries)
    val allCatBudgets = vm.categories.mapNotNull { c ->
        Budgets.find(Period.MONTHLY, c.id, range.start, vm.budgets)?.let { c to it }
    }
    // the overall figure is the total of the category budgets, never set by hand
    val overallAmount = allCatBudgets.sumOf { it.second.amount }
    val hasOverall = allCatBudgets.isNotEmpty()
    val catBudgets = allCatBudgets.take(3)

    RetroCard {
        RText("BUDGET THIS MONTH", size = 12.sp, color = p.onVariant, bold = true)
        Spacer(Modifier.height(6.dp))
        if (!hasOverall) {
            RText("Spent ${vm.fmt(spent)}", bold = true)
            RText("No monthly budget yet. Set one in the Budget tab.", size = 12.sp, color = p.onVariant)
        }
        if (hasOverall) {
            val frac = if (overallAmount > 0) (spent / overallAmount).toFloat() else 1f
            BlockBar(frac, when { frac >= 1f -> p.red; frac >= 0.8f -> p.mustard; else -> p.green })
            Spacer(Modifier.height(4.dp))
            RText("${vm.fmt(spent)} of ${vm.fmt(overallAmount)}", size = 13.sp, bold = true)
        }
        catBudgets.forEach { (c, b) ->
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorDot(c.color)
                Spacer(Modifier.width(6.dp))
                RText(c.name, Modifier.weight(1f), size = 12.sp, maxLines = 1)
                RText("${vm.fmt(spendMap[c.id] ?: 0.0)} / ${vm.fmt(b.amount)}", size = 12.sp, maxLines = 1)
            }
            Spacer(Modifier.height(4.dp))
            val f = if (b.amount > 0) ((spendMap[c.id] ?: 0.0) / b.amount).toFloat() else 1f
            BlockBar(f, when { f >= 1f -> p.red; f >= 0.8f -> p.mustard; else -> p.green })
        }
    }
}

@Composable
private fun TopCategoriesWidget(vm: MainViewModel, entries: List<Entry>) {
    val p = Retro.p
    val top = Stats.spendByCategory(entries).entries
        .sortedByDescending { it.value }
        .take(5)
    val max = top.firstOrNull()?.value ?: 0.0
    RetroCard {
        RText("TOP SPENDING THIS MONTH", size = 12.sp, color = p.onVariant, bold = true)
        if (top.isEmpty()) {
            Spacer(Modifier.height(6.dp))
            RText("Nothing spent yet this month.", size = 13.sp, color = p.onVariant)
        }
        top.forEach { (catId, amount) ->
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorDot(vm.categoryColor(catId))
                Spacer(Modifier.width(6.dp))
                RText(vm.categoryName(catId), Modifier.weight(1f), size = 13.sp, maxLines = 1)
                RText(vm.fmt(amount), size = 13.sp, bold = true, maxLines = 1)
            }
            Spacer(Modifier.height(4.dp))
            BlockBar(if (max > 0) (amount / max).toFloat() else 0f, p.mustard)
        }
    }
}

@Composable
private fun ManageWidgetsSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    val p = Retro.p
    var list by remember { mutableStateOf(vm.widgets.sortedBy { it.position }) }

    fun move(index: Int, delta: Int) {
        val to = index + delta
        if (to !in list.indices) return
        val m = list.toMutableList()
        val item = m.removeAt(index)
        m.add(to, item)
        list = m
    }

    RetroSheet(onDismiss) {
        RText("HOME WIDGETS", size = 16.sp, bold = true)
        RText("Show, hide and reorder the cards on your Home tab.", size = 12.sp, color = p.onVariant)
        list.forEachIndexed { i, w ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RText(WidgetTypes.title(w.widgetType), Modifier.weight(1f), bold = true)
                IconBox(AppIcon.UP, { move(i, -1) }, size = 34.dp)
                Spacer(Modifier.width(6.dp))
                IconBox(AppIcon.DOWN, { move(i, 1) }, size = 34.dp)
                Spacer(Modifier.width(10.dp))
                RetroSwitch(w.isVisible) { on ->
                    list = list.map { if (it.id == w.id) it.copy(isVisible = on) else it }
                }
            }
        }
        RetroButton("Save", { vm.saveWidgets(list); onDismiss() }, Modifier.fillMaxWidth())
    }
}
