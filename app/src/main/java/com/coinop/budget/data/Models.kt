package com.coinop.budget.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

// Row shapes returned by Supabase. Dates are ISO strings (yyyy-MM-dd).

@Serializable
data class AccountRow(
    val id: String,
    val name: String,
    @SerialName("opening_balance") val openingBalance: Double = 0.0,
    val balance: Double = 0.0,
    val sort: Int = 0,
    @SerialName("is_archived") val isArchived: Boolean = false,
)

@Serializable
data class AccountInsert(
    val name: String,
    @SerialName("opening_balance") val openingBalance: Double,
)

@Serializable
data class Category(
    val id: String,
    val name: String,
    val icon: String? = null,
    val color: String? = null,
)

@Serializable
data class CategoryInsert(
    val name: String,
    val icon: String? = null,
    val color: String? = null,
)

@Serializable
data class Rule(
    val id: String,
    val keyword: String,
    @SerialName("category_id") val categoryId: String,
    val priority: Int = 0,
)

@Serializable
data class RuleInsert(
    val keyword: String,
    @SerialName("category_id") val categoryId: String,
    val priority: Int = 0,
)

@Serializable
data class Entry(
    val id: String,
    @SerialName("account_id") val accountId: String,
    val date: String,
    val amount: Double,
    val type: String, // "in" | "out"
    @SerialName("category_id") val categoryId: String? = null,
    val note: String? = null,
    @SerialName("is_manual_category") val isManualCategory: Boolean = false,
    @SerialName("is_transfer") val isTransfer: Boolean = false,
    @SerialName("transfer_id") val transferId: String? = null,
) {
    val isIn: Boolean get() = type == "in"
    val localDate: LocalDate get() = LocalDate.parse(date)
}

@Serializable
data class EntryInsert(
    @SerialName("account_id") val accountId: String,
    val date: String,
    val amount: Double,
    val type: String,
    @SerialName("category_id") val categoryId: String? = null,
    val note: String? = null,
    @SerialName("is_manual_category") val isManualCategory: Boolean = false,
    @SerialName("is_transfer") val isTransfer: Boolean = false,
    @SerialName("transfer_id") val transferId: String? = null,
)

@Serializable
data class BudgetRow(
    val id: String,
    @SerialName("period_type") val periodType: String,
    @SerialName("parent_budget_id") val parentBudgetId: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    val amount: Double,
)

@Serializable
data class BudgetInsert(
    @SerialName("period_type") val periodType: String,
    @SerialName("parent_budget_id") val parentBudgetId: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    val amount: Double,
)

@Serializable
data class InvestAccount(
    val id: String,
    val name: String,
    val kind: String = "demat",
)

@Serializable
data class InvestAccountInsert(
    val name: String,
    val kind: String = "demat",
)

@Serializable
data class Holding(
    val id: String,
    @SerialName("invest_account_id") val investAccountId: String,
    val kind: String, // "stock" | "mf"
    val symbol: String,
    val name: String,
    val quantity: Double,
    @SerialName("avg_cost") val avgCost: Double = 0.0,
)

@Serializable
data class HoldingInsert(
    @SerialName("invest_account_id") val investAccountId: String,
    val kind: String,
    val symbol: String,
    val name: String,
    val quantity: Double,
    @SerialName("avg_cost") val avgCost: Double,
)

/** One mutual fund investment: money put in on a date, and the units it bought at that day's NAV. */
@Serializable
data class HoldingLot(
    val id: String,
    @SerialName("holding_id") val holdingId: String,
    @SerialName("invest_date") val investDate: String,
    val amount: Double,
    val nav: Double,
    val units: Double,
)

@Serializable
data class HoldingLotInsert(
    @SerialName("holding_id") val holdingId: String,
    @SerialName("invest_date") val investDate: String,
    val amount: Double,
    val nav: Double,
    val units: Double,
)

@Serializable
data class WidgetRow(
    val id: String,
    @SerialName("widget_type") val widgetType: String,
    val position: Int = 0,
    @SerialName("is_visible") val isVisible: Boolean = true,
)

@Serializable
data class WidgetInsert(
    @SerialName("widget_type") val widgetType: String,
    val position: Int,
    @SerialName("is_visible") val isVisible: Boolean = true,
)

@Serializable
data class SettingsRow(
    @SerialName("currency_code") val currencyCode: String = "INR",
    val timezone: String? = null,
)

@Serializable
data class SettingsUpsert(
    @SerialName("user_id") val userId: String,
    @SerialName("currency_code") val currencyCode: String,
    val timezone: String?,
)

/** App-wide settings kept in Supabase and mirrored on-device. */
data class AppSettings(
    val currencyCode: String = "INR",
    val timezone: String = java.time.ZoneId.systemDefault().id,
)

/** A search hit when adding a holding. */
data class Instrument(
    val kind: String, // "stock" | "mf"
    val symbol: String,
    val name: String,
    val exchange: String? = null,
)

object WidgetTypes {
    const val BALANCE = "balance_summary"
    const val NET_WORTH = "net_worth"
    const val BUDGET = "budget_progress"
    const val TOP_CATEGORIES = "top_categories"
    val all = listOf(BALANCE, NET_WORTH, BUDGET, TOP_CATEGORIES)
    fun title(type: String) = when (type) {
        BALANCE -> "Balances"
        NET_WORTH -> "Net worth"
        BUDGET -> "Budget progress"
        TOP_CATEGORIES -> "Top categories"
        else -> type
    }
}
