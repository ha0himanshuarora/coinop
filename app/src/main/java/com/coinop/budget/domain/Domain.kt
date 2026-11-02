package com.coinop.budget.domain

import com.coinop.budget.data.BudgetRow
import com.coinop.budget.data.Category
import com.coinop.budget.data.Entry
import com.coinop.budget.data.Rule
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Currency
import java.util.Locale

// ------------------------------------------------------------------ money

object Money {
    fun format(amount: Double, code: String): String {
        val locale = if (code == "INR") Locale("en", "IN") else Locale.US
        return try {
            NumberFormat.getCurrencyInstance(locale).apply {
                currency = Currency.getInstance(code)
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }.format(amount)
        } catch (e: Exception) {
            String.format(Locale.US, "%s %.2f", code, amount)
        }
    }

    fun symbol(code: String): String = try {
        Currency.getInstance(code).getSymbol(if (code == "INR") Locale("en", "IN") else Locale.US)
    } catch (e: Exception) {
        code
    }

    fun name(code: String): String = try {
        Currency.getInstance(code).displayName
    } catch (e: Exception) {
        code
    }

    /** Accepts "1,234.50" style input. Returns null for blank or invalid text. */
    fun parse(text: String): Double? =
        text.replace(",", "").trim().toDoubleOrNull()?.takeIf { it.isFinite() }
}

// ------------------------------------------------------------------ time

object TimeUtil {
    fun zone(id: String): ZoneId = runCatching { ZoneId.of(id) }.getOrDefault(ZoneId.systemDefault())
    fun today(zoneId: String): LocalDate = LocalDate.now(zone(zoneId))
    val dayLabel: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    val shortDay: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)
    val monthLabel: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
}

// ------------------------------------------------------------------ rules

object RuleEngine {
    /** Highest priority first, then longest keyword. Case-insensitive "contains". */
    fun match(text: String?, rules: List<Rule>): String? {
        val t = text?.trim()?.uppercase().orEmpty()
        if (t.isEmpty()) return null
        return rules
            .sortedWith(compareByDescending<Rule> { it.priority }.thenByDescending { it.keyword.length })
            .firstOrNull { it.keyword.isNotBlank() && t.contains(it.keyword.trim().uppercase()) }
            ?.categoryId
    }

    fun uncategorizedId(categories: List<Category>): String? =
        categories.firstOrNull { it.name.equals("Uncategorized", ignoreCase = true) }?.id
}

// ------------------------------------------------------------------ budgets

enum class Period(val db: String, val tab: String) {
    ANNUAL("annual", "YEAR"),
    MONTHLY("monthly", "MONTH"),
    WEEKLY("weekly", "WEEK"),
    DAILY("daily", "DAY");

    val parent: Period? get() = when (this) {
        ANNUAL -> null
        MONTHLY -> ANNUAL
        WEEKLY -> MONTHLY
        DAILY -> WEEKLY
    }
    val child: Period? get() = when (this) {
        ANNUAL -> MONTHLY
        MONTHLY -> WEEKLY
        WEEKLY -> DAILY
        DAILY -> null
    }
}

data class DateRange(val start: LocalDate, val end: LocalDate) {
    fun contains(d: LocalDate) = !d.isBefore(start) && !d.isAfter(end)
}

object Budgets {
    /** Annual = calendar year, weeks run Monday to Sunday. */
    fun rangeOf(period: Period, anchor: LocalDate): DateRange = when (period) {
        Period.ANNUAL -> DateRange(anchor.withDayOfYear(1), anchor.with(TemporalAdjusters.lastDayOfYear()))
        Period.MONTHLY -> DateRange(anchor.withDayOfMonth(1), anchor.with(TemporalAdjusters.lastDayOfMonth()))
        Period.WEEKLY -> {
            val mon = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            DateRange(mon, mon.plusDays(6))
        }
        Period.DAILY -> DateRange(anchor, anchor)
    }

    fun shift(period: Period, anchor: LocalDate, n: Long): LocalDate = when (period) {
        Period.ANNUAL -> anchor.plusYears(n)
        Period.MONTHLY -> anchor.plusMonths(n)
        Period.WEEKLY -> anchor.plusWeeks(n)
        Period.DAILY -> anchor.plusDays(n)
    }

    fun label(period: Period, r: DateRange): String = when (period) {
        Period.ANNUAL -> r.start.year.toString()
        Period.MONTHLY -> r.start.format(TimeUtil.monthLabel)
        Period.WEEKLY -> "${r.start.format(TimeUtil.shortDay)} - ${r.end.format(TimeUtil.dayLabel)}"
        Period.DAILY -> r.start.format(TimeUtil.dayLabel)
    }

    /** The budget this one rolls up into: same category, parent period containing our start. */
    fun parentOf(period: Period, categoryId: String?, start: LocalDate, all: List<BudgetRow>): BudgetRow? {
        val pp = period.parent ?: return null
        return all.firstOrNull {
            it.periodType == pp.db && it.categoryId == categoryId &&
                DateRange(LocalDate.parse(it.startDate), LocalDate.parse(it.endDate)).contains(start)
        }
    }

    fun find(period: Period, categoryId: String?, start: LocalDate, all: List<BudgetRow>): BudgetRow? =
        all.firstOrNull {
            it.periodType == period.db && it.categoryId == categoryId && it.startDate == start.toString()
        }

    /** Suggested starting value when setting a lower-level budget from its parent. */
    fun suggest(period: Period, parent: BudgetRow): Double = when (period) {
        Period.MONTHLY -> parent.amount / 12.0
        Period.WEEKLY -> {
            val ym = YearMonth.from(LocalDate.parse(parent.startDate))
            parent.amount * 7.0 / ym.lengthOfMonth()
        }
        Period.DAILY -> parent.amount / 7.0
        Period.ANNUAL -> parent.amount
    }.let { Math.round(it * 100.0) / 100.0 }

    /** Sum of the budgets directly below [parent]. Used to warn about drift. */
    fun childrenSum(parent: BudgetRow, all: List<BudgetRow>): Double =
        all.filter { it.parentBudgetId == parent.id }.sumOf { it.amount }
}

// ------------------------------------------------------------------ stats

object Stats {
    /** Spending per category id (null = no category). Transfers never count. */
    fun spendByCategory(entries: List<Entry>): Map<String?, Double> =
        entries.filter { it.type == "out" && !it.isTransfer }
            .groupBy { it.categoryId }
            .mapValues { (_, v) -> v.sumOf { it.amount } }

    fun totalSpend(entries: List<Entry>): Double =
        entries.filter { it.type == "out" && !it.isTransfer }.sumOf { it.amount }

    fun totalIncome(entries: List<Entry>): Double =
        entries.filter { it.type == "in" && !it.isTransfer }.sumOf { it.amount }
}
