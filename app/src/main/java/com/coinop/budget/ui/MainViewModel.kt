package com.coinop.budget.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coinop.budget.data.AccountRow
import com.coinop.budget.data.AccountsRepo
import com.coinop.budget.data.AppSettings
import com.coinop.budget.data.AuthRepo
import com.coinop.budget.data.BudgetRow
import com.coinop.budget.data.BudgetsRepo
import com.coinop.budget.data.CategoriesRepo
import com.coinop.budget.data.Category
import com.coinop.budget.data.Entry
import com.coinop.budget.data.EntriesRepo
import com.coinop.budget.data.EntryInsert
import com.coinop.budget.data.FxService
import com.coinop.budget.data.Holding
import com.coinop.budget.data.HoldingInsert
import com.coinop.budget.data.HoldingLot
import com.coinop.budget.data.HoldingLotInsert
import com.coinop.budget.data.Instrument
import com.coinop.budget.data.InvestAccount
import com.coinop.budget.data.InvestRepo
import com.coinop.budget.data.LocalPrefs
import com.coinop.budget.data.PriceService
import com.coinop.budget.data.Rule
import com.coinop.budget.data.RulesRepo
import com.coinop.budget.data.SeedRepo
import com.coinop.budget.data.SettingsRepo
import com.coinop.budget.data.WidgetRow
import com.coinop.budget.data.WidgetsRepo
import com.coinop.budget.domain.Budgets
import com.coinop.budget.domain.DateRange
import com.coinop.budget.domain.Money
import com.coinop.budget.domain.Period
import com.coinop.budget.domain.RuleEngine
import com.coinop.budget.domain.TimeUtil
import com.coinop.budget.ui.theme.Appearance
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

enum class AuthState { LOADING, SIGNED_OUT, SIGNED_IN }

/** One ViewModel holds all app state. Screens read it directly and call its actions. */
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = LocalPrefs(app)

    // ---- global
    var appearance by mutableStateOf(Appearance()); private set
    var settings by mutableStateOf(AppSettings()); private set
    var authState by mutableStateOf(AuthState.LOADING); private set
    var email by mutableStateOf<String?>(null); private set
    var authBusy by mutableStateOf(false); private set
    var loading by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null)

    /** Bumped after every data change so screens re-run their own queries. */
    var dataVersion by mutableIntStateOf(0); private set

    // ---- data
    var accounts by mutableStateOf<List<AccountRow>>(emptyList()); private set
    var categories by mutableStateOf<List<Category>>(emptyList()); private set
    var rules by mutableStateOf<List<Rule>>(emptyList()); private set
    var budgets by mutableStateOf<List<BudgetRow>>(emptyList()); private set
    var widgets by mutableStateOf<List<WidgetRow>>(emptyList()); private set
    var investAccounts by mutableStateOf<List<InvestAccount>>(emptyList()); private set
    var holdings by mutableStateOf<List<Holding>>(emptyList()); private set
    var lots by mutableStateOf<List<HoldingLot>>(emptyList()); private set
    var investBusy by mutableStateOf(false); private set
    var accountEntries by mutableStateOf<List<Entry>>(emptyList()); private set
    private var currentAccountId: String? = null

    // ---- prices and FX
    var prices by mutableStateOf<Map<String, Double>>(emptyMap()); private set
    var pricesLoading by mutableStateOf(false); private set
    var pricesUpdatedAt by mutableLongStateOf(0L); private set
    var selectedInvest by mutableStateOf<String?>(null)
    var fx by mutableStateOf<Map<String, Double>>(emptyMap()); private set
    var fxTime by mutableLongStateOf(0L); private set

    init {
        viewModelScope.launch {
            appearance = prefs.appearance.first()
            settings = prefs.settings.first()
            loadFxCache()
        }
        viewModelScope.launch {
            AuthRepo.status.collect { st ->
                when (st) {
                    is SessionStatus.Authenticated -> {
                        if (authState != AuthState.SIGNED_IN) {
                            authState = AuthState.SIGNED_IN
                            email = AuthRepo.email()
                            viewModelScope.launch { onSignedIn() }
                        }
                    }
                    is SessionStatus.Initializing -> authState = AuthState.LOADING
                    else -> {
                        authState = AuthState.SIGNED_OUT
                        clearData()
                    }
                }
            }
        }
    }

    // ================================================================ helpers

    private fun launchCatching(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = e.message?.take(200) ?: "Something went wrong"
            }
        }
    }

    val currency: String get() = settings.currencyCode
    fun fmt(amount: Double): String = Money.format(amount, currency)

    /** 1 INR in the app currency, or null when live rates are not available yet. */
    fun inrRate(): Double? = if (currency == "INR") 1.0 else fx[currency]

    /** Market prices are in INR; show them in the app currency when a rate is known. */
    fun fmtInr(amountInr: Double): String {
        val r = inrRate()
        return if (r != null) Money.format(amountInr * r, currency) else Money.format(amountInr, "INR")
    }

    fun inrToApp(amountInr: Double): Double = amountInr * (inrRate() ?: 1.0)

    val today: LocalDate get() = TimeUtil.today(settings.timezone)

    fun categoryName(id: String?): String = categories.firstOrNull { it.id == id }?.name ?: "Uncategorized"
    fun categoryColor(id: String?): String? = categories.firstOrNull { it.id == id }?.color

    val totalBank: Double get() = accounts.sumOf { it.balance }

    fun holdingValueInr(h: Holding): Double = (prices[h.id] ?: h.avgCost) * h.quantity
    fun holdingInvestedInr(h: Holding): Double = h.avgCost * h.quantity
    fun lotsOf(holdingId: String): List<HoldingLot> = lots.filter { it.holdingId == holdingId }
    fun holdingsOf(investAccountId: String?): List<Holding> =
        if (investAccountId == null) holdings else holdings.filter { it.investAccountId == investAccountId }

    val investValueInr: Double get() = holdings.sumOf { holdingValueInr(it) }
    val netWorth: Double get() = totalBank + inrToApp(investValueInr)

    // ================================================================ appearance and settings

    fun updateAppearance(a: Appearance) {
        appearance = a
        viewModelScope.launch { prefs.saveAppearance(a) }
    }

    fun setTimezone(zoneId: String) = launchCatching {
        val s = settings.copy(timezone = zoneId)
        settings = s
        prefs.saveSettings(s)
        SettingsRepo.save(s)
        dataVersion++
    }

    fun changeCurrency(newCode: String, convert: Boolean) = launchCatching {
        val old = settings.currencyCode
        if (newCode == old) return@launchCatching
        if (convert) {
            if (fx.isEmpty()) refreshFx()
            val ro = fx[old]
            val rn = fx[newCode]
            if (ro == null || rn == null) {
                message = "Live rates unavailable. Try again later, or relabel only."
                return@launchCatching
            }
            SettingsRepo.convertAllAmounts(rn / ro)
        }
        val s = settings.copy(currencyCode = newCode)
        settings = s
        prefs.saveSettings(s)
        SettingsRepo.save(s)
        refreshAll()
    }

    private suspend fun loadFxCache() {
        val cached = prefs.loadFx() ?: return
        val map = cached.first.split(",").mapNotNull {
            val parts = it.split(":")
            if (parts.size == 2) parts[1].toDoubleOrNull()?.let { v -> parts[0] to v } else null
        }.toMap()
        if (map.isNotEmpty()) {
            fx = map
            fxTime = cached.second
        }
    }

    /** Fetch live rates (1 INR = x CODE). Returns false when the free APIs are unreachable. */
    suspend fun refreshFx(): Boolean {
        val r = FxService.fetchInrRates() ?: return false
        fx = r
        fxTime = System.currentTimeMillis()
        prefs.saveFx(r.entries.joinToString(",") { "${it.key}:${it.value}" }, fxTime)
        return true
    }

    fun refreshRates() = launchCatching {
        if (!refreshFx()) message = "Could not reach the exchange-rate service"
    }

    // ================================================================ auth

    fun signIn(emailAddress: String, password: String) {
        viewModelScope.launch {
            authBusy = true
            try {
                AuthRepo.signIn(emailAddress.trim(), password)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = e.message?.take(200) ?: "Sign-in failed"
            } finally {
                authBusy = false
            }
        }
    }

    fun signUp(emailAddress: String, password: String) {
        viewModelScope.launch {
            authBusy = true
            try {
                AuthRepo.signUp(emailAddress.trim(), password)
                if (authState != AuthState.SIGNED_IN) {
                    message = "Account created. If sign-in does not happen automatically, confirm the email first."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = e.message?.take(200) ?: "Sign-up failed"
            } finally {
                authBusy = false
            }
        }
    }

    fun signOut() = launchCatching { AuthRepo.signOut() }

    fun changePassword(newPassword: String) = launchCatching {
        AuthRepo.changePassword(newPassword)
        message = "Password updated"
    }

    fun deleteAllData() = launchCatching {
        SettingsRepo.deleteAllData()
        selectedInvest = null
        currentAccountId = null
        accountEntries = emptyList()
        prices = emptyMap()
        refreshAll()
        message = "All your data was deleted"
    }

    fun deleteAccount() = launchCatching {
        SettingsRepo.deleteAccount()
        try {
            AuthRepo.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // the login is already gone; the local session is cleared on the next start
        }
        message = "Account deleted"
    }

    private suspend fun onSignedIn() {
        loading = true
        try {
            val row = SettingsRepo.load()
            SeedRepo.ensureWidgets()
            if (row != null) {
                val s = AppSettings(row.currencyCode, row.timezone ?: settings.timezone)
                settings = s
                prefs.saveSettings(s)
            } else {
                // first sign-in ever: starter categories once, then the settings row marks it done
                SeedRepo.seedCategoriesOnce()
                SettingsRepo.save(settings)
            }
            refreshAll()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            message = e.message?.take(200) ?: "Could not load your data"
        } finally {
            loading = false
        }
        // slow network calls run after the UI already has its data
        try {
            if (fx.isEmpty() || System.currentTimeMillis() - fxTime > 6 * 3600_000L) refreshFx()
            refreshPrices()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private fun clearData() {
        accounts = emptyList(); categories = emptyList(); rules = emptyList(); budgets = emptyList()
        widgets = emptyList(); investAccounts = emptyList(); holdings = emptyList(); lots = emptyList()
        accountEntries = emptyList(); prices = emptyMap(); selectedInvest = null; currentAccountId = null
        email = null
    }

    suspend fun refreshAll() {
        coroutineScope {
            val a = async { AccountsRepo.list() }
            val c = async { CategoriesRepo.list() }
            val r = async { RulesRepo.list() }
            val b = async { BudgetsRepo.list() }
            val w = async { WidgetsRepo.list() }
            val ia = async { InvestRepo.accounts() }
            val h = async { InvestRepo.holdings() }
            val l = async { loadLotsSafely() }
            accounts = a.await()
            categories = c.await()
            rules = r.await()
            budgets = b.await()
            widgets = w.await()
            investAccounts = ia.await()
            holdings = h.await()
            lots = l.await()
        }
        currentAccountId?.let { accountEntries = EntriesRepo.forAccount(it) }
        dataVersion++
    }

    fun reload() = launchCatching { refreshAll() }

    // ================================================================ banking

    fun addAccount(name: String, opening: Double) = launchCatching {
        AccountsRepo.add(name.trim(), opening)
        accounts = AccountsRepo.list()
        dataVersion++
    }

    fun updateAccount(id: String, name: String, opening: Double) = launchCatching {
        AccountsRepo.update(id, name.trim(), opening)
        accounts = AccountsRepo.list()
        dataVersion++
    }

    fun deleteAccount(id: String) = launchCatching {
        AccountsRepo.delete(id)
        if (currentAccountId == id) {
            currentAccountId = null
            accountEntries = emptyList()
        }
        accounts = AccountsRepo.list()
        dataVersion++
    }

    fun loadEntries(accountId: String) = launchCatching {
        if (currentAccountId != accountId) accountEntries = emptyList()
        currentAccountId = accountId
        accountEntries = EntriesRepo.forAccount(accountId)
    }

    private suspend fun afterEntryChange() {
        accounts = AccountsRepo.list()
        currentAccountId?.let { accountEntries = EntriesRepo.forAccount(it) }
        dataVersion++
    }

    fun addEntry(
        accountId: String,
        date: LocalDate,
        amount: Double,
        isIn: Boolean,
        categoryId: String?,
        note: String?,
        manualCategory: Boolean,
    ) = launchCatching {
        EntriesRepo.add(
            EntryInsert(
                accountId = accountId,
                date = date.toString(),
                amount = amount,
                type = if (isIn) "in" else "out",
                categoryId = categoryId,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                isManualCategory = manualCategory,
            ),
        )
        afterEntryChange()
    }

    /** Moving money between your own accounts: two linked rows that never count as spending. */
    fun transfer(fromId: String, toId: String, amount: Double, date: LocalDate, note: String?) = launchCatching {
        val tid = UUID.randomUUID().toString()
        val n = note?.trim()?.takeIf { it.isNotEmpty() }
        val fromName = accounts.firstOrNull { it.id == fromId }?.name ?: "account"
        val toName = accounts.firstOrNull { it.id == toId }?.name ?: "account"
        EntriesRepo.addAll(
            listOf(
                EntryInsert(fromId, date.toString(), amount, "out", null, n ?: "Transfer to $toName", false, true, tid),
                EntryInsert(toId, date.toString(), amount, "in", null, n ?: "Transfer from $fromName", false, true, tid),
            ),
        )
        afterEntryChange()
    }

    fun setEntryCategory(entry: Entry, categoryId: String) = launchCatching {
        EntriesRepo.setCategory(entry.id, categoryId, manual = true)
        afterEntryChange()
    }

    fun deleteEntry(entry: Entry) = launchCatching {
        EntriesRepo.delete(entry)
        afterEntryChange()
    }

    suspend fun entriesBetween(range: DateRange): List<Entry> =
        EntriesRepo.between(range.start.toString(), range.end.toString())

    // ================================================================ categories and rules

    fun addCategory(name: String, color: String?) = launchCatching {
        CategoriesRepo.add(name.trim(), color)
        categories = CategoriesRepo.list()
        dataVersion++
    }

    fun updateCategory(id: String, name: String, color: String?) = launchCatching {
        CategoriesRepo.update(id, name.trim(), color)
        categories = CategoriesRepo.list()
        dataVersion++
    }

    fun deleteCategory(id: String) = launchCatching {
        CategoriesRepo.delete(id)
        refreshAll()
    }

    fun addRule(keyword: String, categoryId: String, priority: Int) = launchCatching {
        RulesRepo.add(keyword.trim(), categoryId, priority)
        rules = RulesRepo.list()
    }

    fun deleteRule(id: String) = launchCatching {
        RulesRepo.delete(id)
        rules = RulesRepo.list()
    }

    /** Re-run rules over entries that are uncategorised. Manual picks are never touched. */
    fun reapplyRules() = launchCatching {
        val uncat = RuleEngine.uncategorizedId(categories)
        var changed = 0
        for (e in EntriesRepo.uncategorizedAutomatic()) {
            if (e.categoryId != null && e.categoryId != uncat) continue
            val match = RuleEngine.match(e.note, rules) ?: continue
            if (match != e.categoryId) {
                EntriesRepo.setCategory(e.id, match, manual = false)
                changed++
            }
        }
        message = "$changed entries categorised"
        dataVersion++
    }

    // ================================================================ budgets and widgets

    fun saveBudget(period: Period, categoryId: String?, range: DateRange, amount: Double) = launchCatching {
        val existing = Budgets.find(period, categoryId, range.start, budgets)
        val parent = Budgets.parentOf(period, categoryId, range.start, budgets)
        BudgetsRepo.save(
            existing, period.db, categoryId, range.start.toString(), range.end.toString(), amount, parent?.id,
        )
        budgets = BudgetsRepo.list()
        dataVersion++
    }

    fun deleteBudget(id: String) = launchCatching {
        BudgetsRepo.delete(id)
        budgets = BudgetsRepo.list()
        dataVersion++
    }

    fun saveWidgets(list: List<WidgetRow>) = launchCatching {
        // Home sorts by `position`, so renumber to match the new order before showing it.
        val ordered = list.mapIndexed { i, w -> w.copy(position = i) }
        widgets = ordered
        WidgetsRepo.save(ordered)
    }

    // ================================================================ investments

    fun addInvestAccount(name: String) = launchCatching {
        InvestRepo.addAccount(name.trim())
        investAccounts = InvestRepo.accounts()
        dataVersion++
    }

    fun renameInvestAccount(id: String, name: String) = launchCatching {
        InvestRepo.renameAccount(id, name.trim())
        investAccounts = InvestRepo.accounts()
    }

    fun deleteInvestAccount(id: String) = launchCatching {
        InvestRepo.deleteAccount(id)
        if (selectedInvest == id) selectedInvest = null
        investAccounts = InvestRepo.accounts()
        holdings = InvestRepo.holdings()
        dataVersion++
    }

    fun addHolding(accountId: String, item: Instrument, quantity: Double, avgCost: Double) = launchCatching {
        InvestRepo.addHolding(HoldingInsert(accountId, item.kind, item.symbol, item.name, quantity, avgCost))
        holdings = InvestRepo.holdings()
        dataVersion++
        refreshPrices()
    }

    /** Lots live in their own table; if it has not been created yet the app keeps working without them. */
    private suspend fun loadLotsSafely(): List<HoldingLot> = try {
        InvestRepo.lots()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptyList()
    }

    /**
     * Record money put into a mutual fund. Units come from the NAV on [date] (or the next published
     * NAV), and the fund's total units and average cost are updated to include them.
     */
    fun addMfInvestment(accountId: String, item: Instrument, amount: Double, date: LocalDate) = launchCatching {
        investBusy = true
        try {
            val point = PriceService.navOnOrAfter(item.symbol, date)
            if (point == null) {
                message = "Could not get the NAV for this fund. Check your connection and the scheme code."
                return@launchCatching
            }
            val units = amount / point.nav
            val existing = holdings.firstOrNull {
                it.investAccountId == accountId && it.kind == "mf" && it.symbol == item.symbol
            }
            val holdingId: String
            var created = false
            if (existing == null) {
                holdingId = InvestRepo.addHoldingReturning(
                    HoldingInsert(accountId, "mf", item.symbol, item.name, units, amount / units),
                ).id
                created = true
            } else {
                val newQty = existing.quantity + units
                val newInvested = existing.avgCost * existing.quantity + amount
                InvestRepo.updateHolding(existing.id, newQty, newInvested / newQty)
                holdingId = existing.id
            }
            try {
                InvestRepo.addLot(HoldingLotInsert(holdingId, point.date.toString(), amount, point.nav, units))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // keep the fund list consistent: undo what this call changed, then report the error
                if (created) {
                    InvestRepo.deleteHolding(holdingId)
                } else if (existing != null) {
                    InvestRepo.updateHolding(existing.id, existing.quantity, existing.avgCost)
                }
                throw e
            }
            holdings = InvestRepo.holdings()
            lots = loadLotsSafely()
            dataVersion++
            if (point.date != date) message = "No NAV on $date, used ${point.date}"
            refreshPrices()
        } finally {
            investBusy = false
        }
    }

    fun deleteLot(lot: HoldingLot) = launchCatching {
        val h = holdings.firstOrNull { it.id == lot.holdingId }
        InvestRepo.deleteLot(lot.id)
        if (h != null) {
            val newQty = h.quantity - lot.units
            if (newQty <= 1e-6) {
                InvestRepo.deleteHolding(h.id)
            } else {
                InvestRepo.updateHolding(h.id, newQty, (h.avgCost * h.quantity - lot.amount) / newQty)
            }
        }
        holdings = InvestRepo.holdings()
        lots = loadLotsSafely()
        dataVersion++
    }

    fun updateHolding(id: String, quantity: Double, avgCost: Double) = launchCatching {
        InvestRepo.updateHolding(id, quantity, avgCost)
        holdings = InvestRepo.holdings()
        dataVersion++
    }

    fun deleteHolding(id: String) = launchCatching {
        InvestRepo.deleteHolding(id)
        holdings = InvestRepo.holdings()
        dataVersion++
    }

    /** Pull live prices for every holding (mutual fund NAVs and stock quotes). */
    suspend fun refreshPrices() {
        if (holdings.isEmpty()) return
        pricesLoading = true
        try {
            val fresh = PriceService.pricesFor(holdings)
            // keep older prices for holdings whose fetch failed this time
            prices = prices + fresh
            if (fresh.isNotEmpty()) pricesUpdatedAt = System.currentTimeMillis()
            if (fresh.size < holdings.size) message = "Some prices could not be fetched"
        } finally {
            pricesLoading = false
        }
    }

    fun refreshPricesNow() = launchCatching {
        refreshFx()
        refreshPrices()
    }

    suspend fun search(kind: String, query: String): List<Instrument> =
        if (kind == "mf") PriceService.searchMf(query) else PriceService.searchStock(query)
}
