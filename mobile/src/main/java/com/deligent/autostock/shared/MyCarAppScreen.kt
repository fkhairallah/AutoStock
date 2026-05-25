package com.deligent.autostock.shared

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
                    isLoading = false
                    invalidate()
                }
            }
        })
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
            errorMessage != null -> {
                listBuilder.addItem(
                    GridItem.Builder()
                        .setTitle(errorMessage!!)
                        .setImage(CarIcon.ALERT)
                        .build()
                )
            }
            else -> {
                for (quote in stockQuotes) {
                    val color = if (quote.isPositive) CarColor.GREEN else CarColor.RED
                    val iconRes = if (quote.isPositive) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
                    val icon = CarIcon.Builder(
                        IconCompat.createWithResource(carContext, iconRes)
                    ).setTint(color).build()

                    listBuilder.addItem(
                        GridItem.Builder()
                            .setImage(icon, GridItem.IMAGE_TYPE_ICON)
                            .setTitle("${quote.symbol}  ${quote.price}")
                            .setText("${quote.change}  (${quote.percentChange})")
                            .build()
                    )
                }
            }
        }

        return GridTemplate.Builder()
            .setTitle("Current Market")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(listBuilder.build())
            .build()
    }
}
