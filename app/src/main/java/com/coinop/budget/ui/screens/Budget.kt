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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coinop.budget.data.BudgetRow
import com.coinop.budget.data.Entry
import com.coinop.budget.domain.Budgets
import com.coinop.budget.domain.DateRange
import com.coinop.budget.domain.Money
import com.coinop.budget.domain.Period
import com.coinop.budget.domain.RuleEngine
import com.coinop.budget.domain.Stats
import com.coinop.budget.ui.BlockBar
import com.coinop.budget.ui.Chip
import com.coinop.budget.ui.ColorDot
import com.coinop.budget.ui.IconBox
import com.coinop.budget.ui.MainViewModel
import com.coinop.budget.ui.RText
import com.coinop.budget.ui.RetroButton
import com.coinop.budget.ui.RetroCard
import com.coinop.budget.ui.RetroDialog
import com.coinop.budget.ui.RetroField
import com.coinop.budget.ui.RetroTabs
import com.coinop.budget.ui.ScreenTitle
import com.coinop.budget.ui.Skeleton
import com.coinop.budget.ui.Tag
import com.coinop.budget.ui.theme.AppIcon
import com.coinop.budget.ui.theme.Retro
import kotlinx.coroutines.CancellationException

/** What one row of the budget list is about: overall (null category) or one category. */
private data class BudgetTarget(val categoryId: String?, val name: String, val color: String?)

@Composable
fun BudgetScreen(vm: MainViewModel) {
    var period by rememberSaveable { mutableStateOf(Period.MONTHLY) }
    var anchor by remember { mutableStateOf(vm.today) }
    var editing by remember { mutableStateOf<BudgetTarget?>(null) }

    val range = Budgets.rangeOf(period, anchor)
    val entries by produceState(initialValue = emptyList<Entry>(), range, vm.dataVersion) {
        value = try {
            vm.entriesBetween(range)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }
    val spendMap = Stats.spendByCategory(entries)
    val uncatId = RuleEngine.uncategorizedId(vm.categories)
    fun spentFor(categoryId: String?): Double =
        if (categoryId == null) {
            Stats.totalSpend(entries)
        } else {
            (spendMap[categoryId] ?: 0.0) + (if (categoryId == uncatId) (spendMap[null] ?: 0.0) else 0.0)
        }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Budget")
        Column(Modifier.padding(horizontal = 16.dp)) {
            RetroTabs(Period.entries.map { it to it.tab }, period, { period = it })
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBox(AppIcon.BACK, { anchor = Budgets.shift(period, anchor, -1) })
                Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    RText(Budgets.label(period, range).uppercase(), bold = true, size = 13.sp, textAlign = TextAlign.Center)
                    if (!range.contains(vm.today)) {
                        Chip("Jump to today", false, { anchor = vm.today })
                    }
                }
                IconBox(AppIcon.CHEVRON, { anchor = Budgets.shift(period, anchor, 1) })
            }
            Spacer(Modifier.height(12.dp))
        }

        val targets = vm.categories.map { BudgetTarget(it.id, it.name, it.color) }
        // overall is never typed in: it is the total of the category budgets for this period
        val categoryBudgets = vm.categories.mapNotNull { Budgets.find(period, it.id, range.start, vm.budgets) }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (vm.loading && vm.categories.isEmpty()) {
                item { Skeleton(height = 80.dp) }
                item { Skeleton(height = 80.dp) }
            }
            item(key = "overall") {
                OverallCard(vm, categoryBudgets.sumOf { it.amount }, categoryBudgets.size, Stats.totalSpend(entries))
            }
            for (t in targets) {
                item(key = t.categoryId ?: "overall") {
                    BudgetRowCard(vm, period, range, t, spentFor(t.categoryId)) { editing = t }
                }
            }
        }
    }

    editing?.let { t ->
        BudgetDialog(vm, period, range, t, onDismiss = { editing = null })
    }
}

/** Overall = the sum of the category budgets for the period; there is nothing to edit here. */
@Composable
private fun OverallCard(vm: MainViewModel, total: Double, count: Int, spent: Double) {
    val p = Retro.p
    RetroCard(fill = p.floor) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RText("OVERALL", Modifier.weight(1f), bold = true, color = p.cream, maxLines = 1)
            if (count > 0) RText(vm.fmt(total), size = 13.sp, bold = true, color = p.mustard, maxLines = 1)
        }
        Spacer(Modifier.height(8.dp))
        if (count == 0) {
            RText("Spent ${vm.fmt(spent)}", size = 13.sp, color = p.cream)
            RText(
                "Set a budget on a category and the overall total is worked out for you.",
                size = 12.sp, color = p.cream,
            )
        } else {
            val frac = if (total > 0) (spent / total).toFloat() else if (spent > 0) 1f else 0f
            BlockBar(frac, when { frac >= 1f -> p.red; frac >= 0.8f -> p.mustard; else -> p.green })
            Spacer(Modifier.height(6.dp))
            val left = total - spent
            Row {
                RText("Spent ${vm.fmt(spent)}", Modifier.weight(1f), size = 12.sp, color = p.cream, maxLines = 1)
                RText(
                    if (left >= 0) "${vm.fmt(left)} left" else "${vm.fmt(-left)} over",
                    size = 12.sp, bold = true, color = if (left >= 0) p.green else p.red, maxLines = 1,
                )
            }
            Spacer(Modifier.height(6.dp))
            RText("Total of $count category budget" + (if (count == 1) "" else "s"), size = 12.sp, color = p.cream)
        }
    }
}

@Composable
private fun BudgetRowCard(
    vm: MainViewModel,
    period: Period,
    range: DateRange,
    t: BudgetTarget,
    spent: Double,
    onClick: () -> Unit,
) {
    val p = Retro.p
    val budget = Budgets.find(period, t.categoryId, range.start, vm.budgets)
    val parent = Budgets.parentOf(period, t.categoryId, range.start, vm.budgets)
    val locked = period.parent != null && parent == null
    val childSum = budget?.let { Budgets.childrenSum(it, vm.budgets) } ?: 0.0
    val drift = budget != null && childSum > budget.amount * 1.05

    RetroCard(onClick = if (locked) null else onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (t.categoryId != null) {
                ColorDot(t.color)
                Spacer(Modifier.width(8.dp))
            }
            RText(t.name, Modifier.weight(1f), bold = true, maxLines = 1)
            if (budget != null) RText(vm.fmt(budget.amount), size = 13.sp, color = p.onVariant, maxLines = 1)
        }
        Spacer(Modifier.height(8.dp))
        when {
            locked -> RText(
                "Set the ${period.parent?.tab?.lowercase()} budget for ${t.name} first.",
                size = 12.sp, color = p.onVariant,
            )
            budget == null -> {
                RText("Spent ${vm.fmt(spent)}", size = 13.sp)
                RText("No budget set. Tap to set one.", size = 12.sp, color = p.onVariant)
            }
            else -> {
                val frac = if (budget.amount > 0) (spent / budget.amount).toFloat() else if (spent > 0) 1f else 0f
                val face = when {
                    frac >= 1f -> p.red
                    frac >= 0.8f -> p.mustard
                    else -> p.green
                }
                BlockBar(frac, face)
                Spacer(Modifier.height(6.dp))
                val left = budget.amount - spent
                Row {
                    RText("Spent ${vm.fmt(spent)}", Modifier.weight(1f), size = 12.sp, maxLines = 1)
                    RText(
                        if (left >= 0) "${vm.fmt(left)} left" else "${vm.fmt(-left)} over",
                        size = 12.sp, bold = true, color = if (left >= 0) p.green else p.red, maxLines = 1,
                    )
                }
                if (drift) {
                    Spacer(Modifier.height(6.dp))
                    Tag("Lower budgets add up to ${vm.fmt(childSum)}", p.mustard)
                }
            }
        }
    }
}

@Composable
private fun BudgetDialog(
    vm: MainViewModel,
    period: Period,
    range: DateRange,
    t: BudgetTarget,
    onDismiss: () -> Unit,
) {
    val existing: BudgetRow? = Budgets.find(period, t.categoryId, range.start, vm.budgets)
    val parent = Budgets.parentOf(period, t.categoryId, range.start, vm.budgets)
    val suggestion = parent?.let { Budgets.suggest(period, it) }
    var text by remember { mutableStateOf(existing?.amount?.let { trimmed(it) } ?: "") }
    val amount = Money.parse(text)
    RetroDialog(
        title = "${period.tab.lowercase()} budget: ${t.name}",
        onDismiss = onDismiss,
        actions = {
            if (existing != null) {
                RetroButton("Remove", { vm.deleteBudget(existing.id); onDismiss() }, face = Retro.p.red)
            }
            RetroButton(
                "Save",
                { if (amount != null) { vm.saveBudget(period, t.categoryId, range, amount); onDismiss() } },
                enabled = amount != null && amount >= 0,
            )
        },
    ) {
        RText(Budgets.label(period, range).uppercase(), size = 12.sp, color = Retro.p.onVariant, bold = true)
        RetroField("Budget amount", text, { text = it }, keyboardType = KeyboardType.Decimal, placeholder = "0.00")
        if (suggestion != null) {
            Chip("Suggest ${vm.fmt(suggestion)} from ${period.parent?.tab?.lowercase()}", false, { text = trimmed(suggestion) })
        }
        if (parent != null) {
            RText(
                "${period.parent?.tab?.lowercase()} budget is ${vm.fmt(parent.amount)}. " +
                    "Lower budgets can add up to more; you will just see a warning.",
                size = 12.sp, color = Retro.p.onVariant,
            )
        }
    }
}

private fun trimmed(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else String.format(java.util.Locale.US, "%.2f", v)
