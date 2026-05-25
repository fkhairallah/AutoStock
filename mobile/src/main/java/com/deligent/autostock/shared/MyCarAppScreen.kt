package com.deligent.autostock.shared

import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.ForegroundCarColorSpan
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MyCarAppScreen(carContext: CarContext) : Screen(carContext) {
    private var stockQuotes: List<StockQuote> = emptyList()
    private var errorMessage: String? = null
    private val repository = StockRepository()
    private val symbolStore = SymbolStore(carContext)

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                lifecycleScope.launch {
                    try {
                        stockQuotes = repository.getStockQuotes(symbolStore.getSymbols())
                            .sortedByDescending { quote ->
                                quote.percentChange.replace("+", "").replace("%", "")
                                    .toDoubleOrNull()?.let { kotlin.math.abs(it) } ?: 0.0
                            }
                    } catch (e: Exception) {
                        errorMessage = "Unable to load quotes"
                    }
                    invalidate()
                }
            }
        })
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        if (errorMessage != null) {
            listBuilder.addItem(Row.Builder().setTitle(errorMessage!!).build())
        } else if (stockQuotes.isEmpty()) {
            listBuilder.addItem(Row.Builder().setTitle("Loading...").build())
        } else {
            for (quote in stockQuotes) {
                val color = if (quote.isPositive) CarColor.GREEN else CarColor.RED
                val arrow = if (quote.isPositive) "▲" else "▼"

                val fullText = "$arrow  ${quote.change} (${quote.percentChange})"
                val spannable = SpannableString(fullText)
                spannable.setSpan(
                    ForegroundCarColorSpan.create(color),
                    0,
                    fullText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                listBuilder.addItem(
                    Row.Builder()
                        .setTitle("${quote.symbol}    ${quote.price}")
                        .addText(spannable)
                        .build()
                )
            }
        }

        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setTitle("Current Market")
            .setHeaderAction(Action.APP_ICON)
            .build()
    }
}
