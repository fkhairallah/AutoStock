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
    val isPositive: Boolean
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
        val url = URL("https://finnhub.io/api/v1/quote?symbol=$symbol&token=$API_KEY")
        val connection = url.openConnection() as HttpsURLConnection

        val code = connection.responseCode
        if (code !in 200..299) {
            connection.disconnect()
            throw IOException("HTTP $code fetching $symbol")
        }

        val json = JSONObject(connection.inputStream.bufferedReader().readText())
        connection.disconnect()

        val price = json.getDouble("c")
        val change = json.getDouble("d")
        val pct = json.getDouble("dp")

        if (price == 0.0) throw IOException("No data for $symbol")

        val isPositive = change >= 0
        val sign = if (isPositive) "+" else ""

        return StockQuote(
            symbol = symbol,
            price = String.format(Locale.US, "%.2f", price),
            change = String.format(Locale.US, "%s%.2f", sign, change),
            percentChange = String.format(Locale.US, "%s%.2f%%", sign, pct),
            isPositive = isPositive
        )
    }

    companion object {
        private const val API_KEY = "d89od4pr01qhi7rtcg40d89od4pr01qhi7rtcg4g"
    }
}
