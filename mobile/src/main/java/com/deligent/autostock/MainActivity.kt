package com.deligent.autostock

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.deligent.autostock.shared.StockQuote
import com.deligent.autostock.shared.StockRepository
import com.deligent.autostock.shared.SymbolStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private val repository = StockRepository()
    private lateinit var symbolStore: SymbolStore
    private var loadJob: Job? = null

    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var stockContainer: LinearLayout
    private lateinit var sortButton: TextView

    private enum class SortMode { DEFAULT, NAME, MOVER }
    private var sortMode = SortMode.DEFAULT
    private var currentQuotes = listOf<StockQuote>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        symbolStore = SymbolStore(this)

        progressBar = findViewById(R.id.progressBar)
        errorText = findViewById(R.id.errorText)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        stockContainer = findViewById(R.id.stockContainer)
        sortButton = findViewById(R.id.sortButton)

        swipeRefreshLayout.setOnRefreshListener { loadQuotes() }

        val version = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        findViewById<TextView>(R.id.versionText).text = "v$version"

        sortButton.setOnClickListener {
            sortMode = when (sortMode) {
                SortMode.DEFAULT -> SortMode.NAME
                SortMode.NAME -> SortMode.MOVER
                SortMode.MOVER -> SortMode.DEFAULT
            }
            updateSortButtonLabel()
            renderQuotes()
        }

        findViewById<TextView>(R.id.addIconButton).setOnClickListener {
            showAddDialog()
        }

        loadQuotes()
    }

    private fun updateSortButtonLabel() {
        sortButton.text = when (sortMode) {
            SortMode.DEFAULT -> "⇅"
            SortMode.NAME -> "A-Z"
            SortMode.MOVER -> "% ↓"
        }
    }

    private fun showAddDialog() {
        val input = EditText(this).apply {
            hint = "Symbol (e.g. AAPL)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        }
        val container = LinearLayout(this).apply {
            setPadding(64, 16, 64, 0)
            addView(input)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Add Stock")
            .setView(container)
            .setPositiveButton("Add") { _, _ ->
                val symbol = input.text.toString().trim().uppercase()
                if (symbol.isNotBlank() && !symbolStore.getSymbols().contains(symbol)) {
                    symbolStore.saveSymbols(symbolStore.getSymbols() + symbol)
                    loadQuotes()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadQuotes() {
        loadJob?.cancel()
        if (!swipeRefreshLayout.isRefreshing) {
            progressBar.visibility = View.VISIBLE
            errorText.visibility = View.GONE
            swipeRefreshLayout.visibility = View.GONE
        }
        stockContainer.removeAllViews()

        loadJob = lifecycleScope.launch {
            val quotes = repository.getStockQuotes(symbolStore.getSymbols())
            progressBar.visibility = View.GONE
            swipeRefreshLayout.isRefreshing = false

            if (quotes.isEmpty()) {
                errorText.text = if (symbolStore.getSymbols().isEmpty())
                    "Tap ＋ to add your first stock"
                else
                    "Unable to load quotes"
                errorText.visibility = View.VISIBLE
                swipeRefreshLayout.visibility = View.GONE
                currentQuotes = emptyList()
                return@launch
            }

            currentQuotes = quotes
            renderQuotes()
        }
    }

    private fun renderQuotes() {
        stockContainer.removeAllViews()

        val sorted = when (sortMode) {
            SortMode.DEFAULT -> currentQuotes
            SortMode.NAME -> currentQuotes.sortedBy { it.symbol }
            SortMode.MOVER -> currentQuotes.sortedByDescending {
                abs(it.percentChange.replace("%", "").replace("+", "").toDoubleOrNull() ?: 0.0)
            }
        }

        if (sorted.isEmpty()) return

        for (quote in sorted) {
            val row = LayoutInflater.from(this)
                .inflate(R.layout.item_stock, stockContainer, false)
            row.findViewById<TextView>(R.id.symbol).text = quote.symbol
            row.findViewById<TextView>(R.id.price).text = quote.price
            val color = if (quote.isPositive) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
            row.findViewById<TextView>(R.id.change).apply {
                text = "${quote.change}  (${quote.percentChange})"
                setTextColor(color)
            }
            row.findViewById<TextView>(R.id.removeButton).setOnClickListener {
                symbolStore.saveSymbols(symbolStore.getSymbols().filter { it != quote.symbol })
                loadQuotes()
            }
            stockContainer.addView(row)
        }
        swipeRefreshLayout.visibility = View.VISIBLE
        errorText.visibility = View.GONE
    }
}
