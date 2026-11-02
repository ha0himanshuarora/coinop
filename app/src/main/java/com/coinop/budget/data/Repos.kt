package com.coinop.budget.data

import com.coinop.budget.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object Db {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        ) {
            defaultSerializer = KotlinXSerializer(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                },
            )
            install(Auth)
            install(Postgrest)
        }
    }
}

object AuthRepo {
    val status: StateFlow<SessionStatus> get() = Db.client.auth.sessionStatus

    fun email(): String? = Db.client.auth.currentUserOrNull()?.email
    fun userId(): String? = Db.client.auth.currentUserOrNull()?.id

    suspend fun signIn(emailAddress: String, pass: String) {
        Db.client.auth.signInWith(Email) {
            this.email = emailAddress
            this.password = pass
        }
    }

    suspend fun signUp(emailAddress: String, pass: String) {
        Db.client.auth.signUpWith(Email) {
            this.email = emailAddress
            this.password = pass
        }
    }

    suspend fun signOut() {
        Db.client.auth.signOut()
    }

    suspend fun changePassword(newPassword: String) {
        Db.client.auth.updateUser { password = newPassword }
    }
}

object AccountsRepo {
    suspend fun list(): List<AccountRow> =
        Db.client.from("account_balances").select {
            filter { eq("is_archived", false) }
            order("sort", Order.ASCENDING)
            order("name", Order.ASCENDING)
        }.decodeList<AccountRow>()

    suspend fun add(name: String, openingBalance: Double) {
        Db.client.from("accounts").insert(AccountInsert(name, openingBalance))
    }

    suspend fun update(id: String, name: String, openingBalance: Double) {
        Db.client.from("accounts").update({
            set("name", name)
            set("opening_balance", openingBalance)
        }) {
            filter { eq("id", id) }
        }
    }

    suspend fun delete(id: String) {
        Db.client.from("accounts").delete { filter { eq("id", id) } }
    }
}

object EntriesRepo {
    suspend fun forAccount(accountId: String, maxRows: Long = 500): List<Entry> =
        Db.client.from("entries").select {
            filter { eq("account_id", accountId) }
            order("date", Order.DESCENDING)
            order("created_at", Order.DESCENDING)
            limit(maxRows)
        }.decodeList<Entry>()

    /** All entries with start <= date <= end, paged past the 1000-row API default. */
    suspend fun between(start: String, end: String): List<Entry> {
        val out = mutableListOf<Entry>()
        var from = 0L
        val page = 1000L
        while (true) {
            val chunk = Db.client.from("entries").select {
                filter {
                    gte("date", start)
                    lte("date", end)
                }
                order("date", Order.ASCENDING)
                order("id", Order.ASCENDING)
                range(from..(from + page - 1))
            }.decodeList<Entry>()
            out += chunk
            if (chunk.size < page) break
            from += page
        }
        return out
    }

    suspend fun uncategorizedAutomatic(): List<Entry> =
        Db.client.from("entries").select {
            filter {
                eq("is_manual_category", false)
                eq("is_transfer", false)
            }
            limit(2000)
        }.decodeList<Entry>()

    suspend fun add(entry: EntryInsert) {
        Db.client.from("entries").insert(entry)
    }

    suspend fun addAll(entries: List<EntryInsert>) {
        Db.client.from("entries").insert(entries)
    }

    suspend fun setCategory(id: String, categoryId: String, manual: Boolean) {
        Db.client.from("entries").update({
            set("category_id", categoryId)
            set("is_manual_category", manual)
        }) {
            filter { eq("id", id) }
        }
    }

    suspend fun delete(entry: Entry) {
        val tid = entry.transferId
        if (entry.isTransfer && tid != null) {
            Db.client.from("entries").delete { filter { eq("transfer_id", tid) } }
        } else {
            Db.client.from("entries").delete { filter { eq("id", entry.id) } }
        }
    }
}

object CategoriesRepo {
    suspend fun list(): List<Category> =
        Db.client.from("categories").select {
            order("name", Order.ASCENDING)
        }.decodeList<Category>()

    suspend fun add(name: String, color: String?) {
        Db.client.from("categories").insert(CategoryInsert(name = name, color = color))
    }

    suspend fun update(id: String, name: String, color: String?) {
        Db.client.from("categories").update({
            set("name", name)
            set("color", color ?: "")
        }) {
            filter { eq("id", id) }
        }
    }

    suspend fun delete(id: String) {
        Db.client.from("categories").delete { filter { eq("id", id) } }
    }
}

object RulesRepo {
    suspend fun list(): List<Rule> =
        Db.client.from("category_rules").select {
            order("priority", Order.DESCENDING)
        }.decodeList<Rule>()

    suspend fun add(keyword: String, categoryId: String, priority: Int) {
        Db.client.from("category_rules").insert(RuleInsert(keyword, categoryId, priority))
    }

    suspend fun delete(id: String) {
        Db.client.from("category_rules").delete { filter { eq("id", id) } }
    }
}

object BudgetsRepo {
    suspend fun list(): List<BudgetRow> =
        Db.client.from("budgets").select {
            order("start_date", Order.ASCENDING)
            limit(5000)
        }.decodeList<BudgetRow>()

    suspend fun save(
        existing: BudgetRow?,
        period: String,
        categoryId: String?,
        start: String,
        end: String,
        amount: Double,
        parentId: String?,
    ) {
        if (existing != null) {
            Db.client.from("budgets").update({ set("amount", amount) }) {
                filter { eq("id", existing.id) }
            }
        } else {
            Db.client.from("budgets").insert(
                BudgetInsert(period, parentId, categoryId, start, end, amount),
            )
        }
    }

    suspend fun delete(id: String) {
        Db.client.from("budgets").delete { filter { eq("id", id) } }
    }
}

object InvestRepo {
    suspend fun accounts(): List<InvestAccount> =
        Db.client.from("invest_accounts").select {
            order("name", Order.ASCENDING)
        }.decodeList<InvestAccount>()

    suspend fun addAccount(name: String) {
        Db.client.from("invest_accounts").insert(InvestAccountInsert(name))
    }

    suspend fun renameAccount(id: String, name: String) {
        Db.client.from("invest_accounts").update({ set("name", name) }) {
            filter { eq("id", id) }
        }
    }

    suspend fun deleteAccount(id: String) {
        Db.client.from("invest_accounts").delete { filter { eq("id", id) } }
    }

    suspend fun holdings(): List<Holding> =
        Db.client.from("holdings").select {
            order("name", Order.ASCENDING)
            limit(5000)
        }.decodeList<Holding>()

    suspend fun addHolding(h: HoldingInsert) {
        Db.client.from("holdings").insert(h)
    }

    suspend fun addHoldingReturning(h: HoldingInsert): Holding =
        Db.client.from("holdings").insert(h) { select() }.decodeSingle<Holding>()

    suspend fun lots(): List<HoldingLot> =
        Db.client.from("holding_lots").select {
            order("invest_date", Order.ASCENDING)
            limit(5000)
        }.decodeList<HoldingLot>()

    suspend fun addLot(l: HoldingLotInsert) {
        Db.client.from("holding_lots").insert(l)
    }

    suspend fun deleteLot(id: String) {
        Db.client.from("holding_lots").delete { filter { eq("id", id) } }
    }

    suspend fun updateHolding(id: String, quantity: Double, avgCost: Double) {
        Db.client.from("holdings").update({
            set("quantity", quantity)
            set("avg_cost", avgCost)
        }) {
            filter { eq("id", id) }
        }
    }

    suspend fun deleteHolding(id: String) {
        Db.client.from("holdings").delete { filter { eq("id", id) } }
    }
}

object WidgetsRepo {
    suspend fun list(): List<WidgetRow> =
        Db.client.from("home_widgets").select {
            order("position", Order.ASCENDING)
        }.decodeList<WidgetRow>()

    suspend fun save(rows: List<WidgetRow>) {
        rows.forEachIndexed { index, w ->
            Db.client.from("home_widgets").update({
                set("position", index)
                set("is_visible", w.isVisible)
            }) {
                filter { eq("id", w.id) }
            }
        }
    }
}

object SettingsRepo {
    suspend fun load(): SettingsRow? =
        Db.client.from("app_settings").select().decodeSingleOrNull<SettingsRow>()

    suspend fun save(s: AppSettings) {
        val uid = AuthRepo.userId() ?: return
        Db.client.from("app_settings").upsert(
            SettingsUpsert(userId = uid, currencyCode = s.currencyCode, timezone = s.timezone),
        )
    }

    /** Wipes banks, entries, budgets, categories and investments (see supabase/account_functions.sql). */
    suspend fun deleteAllData() {
        Db.client.postgrest.rpc("delete_my_data")
    }

    /** Deletes the login itself; every table cascades from it. */
    suspend fun deleteAccount() {
        Db.client.postgrest.rpc("delete_my_account")
    }

    suspend fun convertAllAmounts(rate: Double) {
        Db.client.postgrest.rpc(
            "convert_all_amounts",
            buildJsonObject { put("rate", rate) },
        )
    }
}

/** First sign-in: default categories, starter rules and home widgets. */
object SeedRepo {
    private data class Seed(val name: String, val color: String)

    private val categories = listOf(
        Seed("Food & Dining", "#F59E0B"),
        Seed("Groceries", "#10B981"),
        Seed("Shopping", "#8B5CF6"),
        Seed("Bills & Utilities", "#EF4444"),
        Seed("Rent", "#F97316"),
        Seed("Transport", "#3B82F6"),
        Seed("Entertainment", "#EC4899"),
        Seed("Health", "#14B8A6"),
        Seed("Investments", "#22C55E"),
        Seed("Salary", "#84CC16"),
        Seed("Other Income", "#06B6D4"),
        Seed("Uncategorized", "#9CA3AF"),
    )

    // keyword, category name, priority (specific keywords get higher priority)
    private val rules = listOf(
        Triple("SWIGGY INSTAMART", "Groceries", 10),
        Triple("ZEPTO", "Groceries", 8),
        Triple("BLINKIT", "Groceries", 8),
        Triple("BIGBASKET", "Groceries", 8),
        Triple("SWIGGY", "Food & Dining", 5),
        Triple("ZOMATO", "Food & Dining", 5),
        Triple("AMAZON", "Shopping", 5),
        Triple("FLIPKART", "Shopping", 5),
        Triple("MYNTRA", "Shopping", 5),
        Triple("UBER", "Transport", 5),
        Triple("OLA", "Transport", 3),
        Triple("METRO", "Transport", 3),
        Triple("NETFLIX", "Entertainment", 5),
        Triple("SPOTIFY", "Entertainment", 5),
        Triple("ELECTRICITY", "Bills & Utilities", 5),
        Triple("RECHARGE", "Bills & Utilities", 5),
        Triple("RENT", "Rent", 4),
        Triple("PHARMACY", "Health", 5),
        Triple("HOSPITAL", "Health", 5),
        Triple("SALARY", "Salary", 8),
    )

    /**
     * Default categories and starter rules, for a brand-new user only (the caller runs this just
     * when no settings row exists yet). It never runs again, so categories you delete stay deleted.
     */
    suspend fun seedCategoriesOnce() {
        val existing = CategoriesRepo.list()
        if (existing.isEmpty()) {
            val created = Db.client.from("categories").insert(
                categories.map { CategoryInsert(name = it.name, color = it.color) },
            ) { select() }.decodeList<Category>()
            val byName = created.associateBy { it.name }
            val ruleRows = rules.mapNotNull { (kw, cat, pr) ->
                byName[cat]?.let { RuleInsert(kw, it.id, pr) }
            }
            if (ruleRows.isNotEmpty()) {
                Db.client.from("category_rules").insert(ruleRows)
            }
        }
    }

    suspend fun ensureWidgets() {
        if (WidgetsRepo.list().isEmpty()) {
            Db.client.from("home_widgets").insert(
                WidgetTypes.all.mapIndexed { i, t -> WidgetInsert(t, i, true) },
            )
        }
    }
}
