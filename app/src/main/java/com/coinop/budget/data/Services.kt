package com.coinop.budget.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder

/** Plain HTTP GET helper for the free public APIs (no keys needed). */
object Http {
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
    }

    suspend fun get(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val r = client.get(url) {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Linux; Android) Coinop/1.0")
                header(HttpHeaders.Accept, "application/json")
            }
            if (r.status.isSuccess()) r.bodyAsText() else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}

private val json = Json { ignoreUnknownKeys = true }

private fun JsonElement?.num(): Double? = this?.let {
    runCatching { it.jsonPrimitive.doubleOrNull ?: it.jsonPrimitive.contentOrNull?.toDoubleOrNull() }.getOrNull()
}

/**
 * Live exchange rates from free, keyless APIs. Rates are always fetched as
 * "1 INR = x CODE" because market prices are quoted in INR.
 */
object FxService {
    suspend fun fetchInrRates(): Map<String, Double>? {
        val primary = Http.get("https://open.er-api.com/v6/latest/INR")?.let { parseRates(it) }
        if (!primary.isNullOrEmpty()) return primary
        return Http.get("https://api.frankfurter.app/latest?from=INR")?.let { parseRates(it) }
    }

    fun parseRates(body: String): Map<String, Double>? {
        val rates = runCatching { json.parseToJsonElement(body).jsonObject["rates"]?.jsonObject }.getOrNull()
            ?: return null
        val map = rates.entries.mapNotNull { (k, v) -> v.num()?.let { k to it } }.toMap().toMutableMap()
        map["INR"] = 1.0
        return map
    }
}

/**
 * Live prices.
 *  - Mutual funds: AMFI-based public API (mfapi.in), latest NAV per scheme code.
 *  - Stocks: Yahoo Finance's public chart endpoint (unofficial; may change or rate-limit).
 * Everything is in INR. Swap implementations here if a source stops working.
 */
object PriceService {
    suspend fun mfNav(schemeCode: String): Double? {
        val body = Http.get("https://api.mfapi.in/mf/${Http.enc(schemeCode)}/latest") ?: return null
        return runCatching {
            val data = json.parseToJsonElement(body).jsonObject["data"]?.jsonArray
            data?.firstOrNull()?.jsonObject?.get("nav").num()
        }.getOrNull()
    }

    data class NavPoint(val date: java.time.LocalDate, val nav: Double)

    /** NAV on [date], or the next published NAV after it (weekends and holidays have none). */
    suspend fun navOnOrAfter(schemeCode: String, date: java.time.LocalDate): NavPoint? {
        val body = Http.get("https://api.mfapi.in/mf/${Http.enc(schemeCode)}") ?: return null
        return runCatching {
            val fmt = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")
            val rows = json.parseToJsonElement(body).jsonObject["data"]?.jsonArray ?: return@runCatching null
            val points = rows.mapNotNull { el ->
                val o = el.jsonObject
                val d = o["date"]?.jsonPrimitive?.contentOrNull?.let { java.time.LocalDate.parse(it, fmt) }
                    ?: return@mapNotNull null
                val n = o["nav"].num() ?: return@mapNotNull null
                NavPoint(d, n)
            }
            points.filter { !it.date.isBefore(date) }.minByOrNull { it.date } ?: points.maxByOrNull { it.date }
        }.getOrNull()
    }

    suspend fun stockPrice(symbol: String): Double? {
        val body = Http.get(
            "https://query1.finance.yahoo.com/v8/finance/chart/${Http.enc(symbol)}?range=1d&interval=1d",
        ) ?: return null
        return runCatching {
            val result = json.parseToJsonElement(body).jsonObject["chart"]?.jsonObject
                ?.get("result")?.jsonArray?.firstOrNull()?.jsonObject
            result?.get("meta")?.jsonObject?.get("regularMarketPrice").num()
        }.getOrNull()
    }

    /** Price per holding id; holdings whose price could not be fetched are simply absent. */
    suspend fun pricesFor(holdings: List<Holding>): Map<String, Double> = coroutineScope {
        holdings.map { h ->
            async {
                val p = if (h.kind == "mf") mfNav(h.symbol) else stockPrice(h.symbol)
                if (p != null) h.id to p else null
            }
        }.mapNotNull { it.await() }.toMap()
    }

    suspend fun searchMf(query: String): List<Instrument> {
        val body = Http.get("https://api.mfapi.in/mf/search?q=${Http.enc(query)}") ?: return emptyList()
        return runCatching {
            val arr: JsonArray = json.parseToJsonElement(body).jsonArray
            arr.take(25).mapNotNull { el ->
                val o: JsonObject = el.jsonObject
                val code = o["schemeCode"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val name = o["schemeName"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                Instrument("mf", code, name)
            }
        }.getOrDefault(emptyList())
    }

    suspend fun searchStock(query: String): List<Instrument> {
        val body = Http.get(
            "https://query1.finance.yahoo.com/v1/finance/search?q=${Http.enc(query)}&quotesCount=15&newsCount=0",
        ) ?: return emptyList()
        return runCatching {
            val quotes = json.parseToJsonElement(body).jsonObject["quotes"]?.jsonArray ?: JsonArray(emptyList())
            quotes.mapNotNull { el ->
                val o = el.jsonObject
                val symbol = o["symbol"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                if (!(symbol.endsWith(".NS") || symbol.endsWith(".BO"))) return@mapNotNull null
                val name = o["shortname"]?.jsonPrimitive?.contentOrNull
                    ?: o["longname"]?.jsonPrimitive?.contentOrNull ?: symbol
                Instrument("stock", symbol, name, o["exchange"]?.jsonPrimitive?.contentOrNull)
            }
        }.getOrDefault(emptyList())
    }
}
