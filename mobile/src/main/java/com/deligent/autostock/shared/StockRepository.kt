package com.deligent.autostock.shared

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

data class StockQuote(
    val symbol: String,
    val price: String,
    val change: String,
    val percentChange: String,
    val isPositive: Boolean,
    val afterHoursPrice: String = "",
    val afterHoursChange: String = "",
    val afterHoursPercentChange: String = "",
    val afterHoursIsPositive: Boolean = true,
    val hasAfterHours: Boolean = false,
    val extendedHoursLabel: String = "AH"
)

class StockRepository {

    suspend fun getStockQuotes(symbols: List<String>): List<StockQuote> = coroutineScope {
        symbols.map { symbol ->
            async(Dispatchers.IO) {
                try { fetchQuote(symbol) } catch (e: Exception) { null }
            }
        }.awaitAll().filterNotNull()
    }

    private fun fetchQuote(symbol: String): StockQuote {
        val url = URL("https://query2.finance.yahoo.com/v8/finance/chart/$symbol?interval=5m&range=1d&includePrePost=true")
        val connection = url.openConnection() as HttpsURLConnection
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")

        val code = connection.responseCode
        if (code !in 200..299) {
            connection.disconnect()
            throw IOException("HTTP $code fetching $symbol")
        }

        val json = JSONObject(connection.inputStream.bufferedReader().readText())
        connection.disconnect()

        val result = json.getJSONObject("chart").getJSONArray("result").getJSONObject(0)
        val meta = result.getJSONObject("meta")

        val regularPrice = meta.getDouble("regularMarketPrice")
        val prevClose = meta.getDouble("chartPreviousClose")

        if (regularPrice == 0.0) throw IOException("No data for $symbol")

        val regularChange = regularPrice - prevClose
        val regularPct = if (prevClose != 0.0) (regularChange / prevClose) * 100.0 else 0.0
        val isPositive = regularChange >= 0
        val sign = if (isPositive) "+" else ""

        // Determine session using currentTradingPeriod timestamps
        val nowSec = System.currentTimeMillis() / 1000
        val tradingPeriod = meta.optJSONObject("currentTradingPeriod")
        val preStart = tradingPeriod?.optJSONObject("pre")?.optLong("start", 0L) ?: 0L
        val preEnd = tradingPeriod?.optJSONObject("pre")?.optLong("end", 0L) ?: 0L
        val postStart = tradingPeriod?.optJSONObject("post")?.optLong("start", 0L) ?: 0L
        val postEnd = tradingPeriod?.optJSONObject("post")?.optLong("end", 0L) ?: 0L

        val isPreMarket = preEnd > 0L && nowSec in preStart..preEnd
        val isPostMarket = postEnd > 0L && nowSec in postStart..postEnd

        var ahPrice = 0.0
        var label = "AH"

        if (isPreMarket || isPostMarket) {
            label = "AH"
            val closes = result.getJSONObject("indicators")
                .getJSONArray("quote").getJSONObject(0)
                .getJSONArray("close")
            for (i in closes.length() - 1 downTo 0) {
                if (!closes.isNull(i)) {
                    ahPrice = closes.getDouble(i)
                    break
                }
            }
        }

        val hasAH = ahPrice > 0.0
        val ahChange = if (hasAH) ahPrice - regularPrice else 0.0
        val ahPct = if (hasAH && regularPrice != 0.0) (ahChange / regularPrice) * 100.0 else 0.0
        val ahIsPositive = ahChange >= 0
        val ahSign = if (ahIsPositive) "+" else ""

        return StockQuote(
            symbol = symbol,
            price = String.format(Locale.US, "%.2f", regularPrice),
            change = String.format(Locale.US, "%s%.2f", sign, regularChange),
            percentChange = String.format(Locale.US, "%s%.2f%%", sign, regularPct),
            isPositive = isPositive,
            afterHoursPrice = if (hasAH) String.format(Locale.US, "%.2f", ahPrice) else "",
            afterHoursChange = if (hasAH) String.format(Locale.US, "%s%.2f", ahSign, ahChange) else "",
            afterHoursPercentChange = if (hasAH) String.format(Locale.US, "%s%.2f%%", ahSign, ahPct) else "",
            afterHoursIsPositive = ahIsPositive,
            hasAfterHours = hasAH,
            extendedHoursLabel = label
        )
    }
}
