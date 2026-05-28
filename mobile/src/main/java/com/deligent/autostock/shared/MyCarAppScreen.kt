package com.deligent.autostock.shared

import android.content.Context
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.deligent.autostock.R
import kotlinx.coroutines.launch

class MyCarAppScreen(carContext: CarContext) : Screen(carContext) {
    private var stockQuotes: List<StockQuote> = emptyList()
    private var isLoading = true
    private var lastUpdated: String? = null
    private val symbolStore = SymbolStore(carContext)

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                if (StockDataStore.state.value is QuoteState.Idle) {
                    StockDataStore.refresh(symbolStore.getSymbols())
                }
                lifecycleScope.launch {
                    StockDataStore.state.collect { state ->
                        when (state) {
                            is QuoteState.Idle -> Unit
                            is QuoteState.Loading -> {
                                isLoading = true
                                invalidate()
                            }
                            is QuoteState.Success -> {
                                stockQuotes = state.quotes
                                lastUpdated = state.lastUpdated
                                isLoading = false
                                invalidate()
                            }
                        }
                    }
                }
            }
        })
    }

    private fun loadQuotes() {
        StockDataStore.refresh(symbolStore.getSymbols())
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        when {
            isLoading -> {
                listBuilder.addItem(
                    GridItem.Builder()
                        .setTitle("Loading...")
                        .setLoading(true)
                        .build()
                )
            }
            else -> {
                val sortMode = carContext.getSharedPreferences("autostock_prefs", Context.MODE_PRIVATE)
                    .getString("sort_mode", "DEFAULT") ?: "DEFAULT"
                val sorted = when (sortMode) {
                    "NAME" -> stockQuotes.sortedBy { it.symbol }
                    "MOVER" -> stockQuotes.sortedByDescending { quote ->
                        quote.percentChange.replace("+", "").replace("%", "")
                            .toDoubleOrNull()?.let { kotlin.math.abs(it) } ?: 0.0
                    }
                    else -> stockQuotes
                }

                for (quote in sorted) {
                    val isPositive = if (quote.hasAfterHours) quote.afterHoursIsPositive else quote.isPositive
                    val color = if (isPositive) CarColor.GREEN else CarColor.RED
                    val iconRes = if (isPositive) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
                    val icon = CarIcon.Builder(
                        IconCompat.createWithResource(carContext, iconRes)
                    ).setTint(color).build()

                    val displayPrice = if (quote.hasAfterHours) quote.afterHoursPrice else quote.price
                    val displayChange = if (quote.hasAfterHours)
                        "${quote.afterHoursChange}  (${quote.afterHoursPercentChange})"
                    else
                        "${quote.change}  (${quote.percentChange})"

                    listBuilder.addItem(
                        GridItem.Builder()
                            .setImage(icon, GridItem.IMAGE_TYPE_ICON)
                            .setTitle("${quote.symbol}  $displayPrice")
                            .setText(displayChange)
                            .build()
                    )
                }

                val versionName = try {
                    carContext.packageManager.getPackageInfo(carContext.packageName, 0).versionName ?: ""
                } catch (e: Exception) { "" }

                val refreshIcon = CarIcon.Builder(
                    IconCompat.createWithResource(carContext, R.drawable.ic_refresh)
                ).build()
                listBuilder.addItem(
                    GridItem.Builder()
                        .setImage(refreshIcon, GridItem.IMAGE_TYPE_LARGE)
                        .setTitle("Refresh")
                        .setText("v$versionName")
                        .setOnClickListener { loadQuotes() }
                        .build()
                )
            }
        }

        val afterHours = stockQuotes.any { it.hasAfterHours }
        val title = when {
            lastUpdated == null -> "Current Market"
            afterHours -> "After Hours Trading as of $lastUpdated"
            else -> "Markets as of $lastUpdated"
        }

        return GridTemplate.Builder()
            .setTitle(title)
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(listBuilder.build())
            .build()
    }
}
