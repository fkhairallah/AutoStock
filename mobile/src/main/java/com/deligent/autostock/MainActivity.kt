package com.deligent.autostock

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import kotlin.math.abs
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.deligent.autostock.shared.QuoteState
import com.deligent.autostock.shared.StockDataStore
import com.deligent.autostock.shared.StockQuote
import com.deligent.autostock.shared.SymbolStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var symbolStore: SymbolStore

    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var stockContainer: LinearLayout
    private lateinit var sortButton: TextView

    private enum class SortMode { DEFAULT, NAME, MOVER }
    private val prefs by lazy { getSharedPreferences("autostock_prefs", Context.MODE_PRIVATE) }
    private var sortMode = SortMode.DEFAULT
    private var currentQuotes = listOf<StockQuote>()
    private var openSwipeCard: View? = null

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

        sortMode = SortMode.valueOf(prefs.getString("sort_mode", SortMode.DEFAULT.name) ?: SortMode.DEFAULT.name)
        updateSortButtonLabel()

        sortButton.setOnClickListener {
            sortMode = when (sortMode) {
                SortMode.DEFAULT -> SortMode.NAME
                SortMode.NAME -> SortMode.MOVER
                SortMode.MOVER -> SortMode.DEFAULT
            }
            prefs.edit().putString("sort_mode", sortMode.name).apply()
            updateSortButtonLabel()
            renderQuotes()
        }

        findViewById<TextView>(R.id.addIconButton).setOnClickListener {
            showAddDialog()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                StockDataStore.state.collect { state ->
                    when (state) {
                        is QuoteState.Idle -> Unit
                        is QuoteState.Loading -> {
                            if (!swipeRefreshLayout.isRefreshing) {
                                progressBar.visibility = View.VISIBLE
                                errorText.visibility = View.GONE
                                swipeRefreshLayout.visibility = View.GONE
                                stockContainer.removeAllViews()
                            }
                        }
                        is QuoteState.Success -> {
                            progressBar.visibility = View.GONE
                            swipeRefreshLayout.isRefreshing = false
                            val symbols = symbolStore.getSymbols()
                            if (state.quotes.isEmpty() || state.quotes.all { it.isError } && symbols.isEmpty()) {
                                errorText.text = if (symbols.isEmpty())
                                    "Tap ＋ to add your first stock"
                                else
                                    "Unable to load quotes"
                                errorText.visibility = View.VISIBLE
                                swipeRefreshLayout.visibility = View.GONE
                                currentQuotes = emptyList()
                            } else {
                                currentQuotes = state.quotes
                                renderQuotes()
                            }
                        }
                    }
                }
            }
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
        StockDataStore.refresh(symbolStore.getSymbols())
    }

    private fun renderQuotes() {
        openSwipeCard = null
        stockContainer.removeAllViews()

        val base = when (sortMode) {
            SortMode.DEFAULT -> currentQuotes
            SortMode.NAME -> currentQuotes.sortedBy { it.symbol }
            SortMode.MOVER -> currentQuotes.sortedByDescending {
                abs(it.percentChange.replace("%", "").replace("+", "").toDoubleOrNull() ?: 0.0)
            }
        }
        // Error rows always sink to the bottom regardless of sort
        val sorted = base.filter { !it.isError } + base.filter { it.isError }

        if (sorted.isEmpty()) return

        for (quote in sorted) {
            val row = LayoutInflater.from(this)
                .inflate(R.layout.item_stock, stockContainer, false)
            row.findViewById<TextView>(R.id.symbol).text = quote.symbol

            when {
                quote.isError -> {
                    row.findViewById<TextView>(R.id.price).apply {
                        text = "—"
                        setTextColor(0x88FFFFFF.toInt())
                    }
                    row.findViewById<TextView>(R.id.change).text = ""
                }
                else -> {
                    row.findViewById<TextView>(R.id.price).text = quote.price
                    val color = if (quote.isPositive) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
                    row.findViewById<TextView>(R.id.change).apply {
                        text = "${quote.change}  (${quote.percentChange})"
                        setTextColor(color)
                    }
                    row.findViewById<TextView>(R.id.afterHoursInfo).apply {
                        when {
                            quote.hasAfterHours -> {
                                isVisible = true
                                val ahColor = if (quote.afterHoursIsPositive) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
                                text = "AH  ${quote.afterHoursPrice}  ${quote.afterHoursChange} (${quote.afterHoursPercentChange})"
                                setTextColor(ahColor)
                            }
                            quote.isExtendedHours -> {
                                isVisible = true
                                text = "AH  N/A"
                                setTextColor(0x88FFFFFF.toInt())
                            }
                            else -> isVisible = false
                        }
                    }
                }
            }
            attachSwipeToDelete(row) {
                symbolStore.saveSymbols(symbolStore.getSymbols().filter { it != quote.symbol })
                loadQuotes()
            }
            stockContainer.addView(row)
        }
        swipeRefreshLayout.visibility = View.VISIBLE
        errorText.visibility = View.GONE
    }

    private fun attachSwipeToDelete(row: View, onDelete: () -> Unit) {
        val contentCard = row.findViewById<View>(R.id.contentCard)
        val deleteButton = row.findViewById<View>(R.id.deleteButton)
        val deleteBtnWidth = 80 * resources.displayMetrics.density
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop.toFloat()

        var downX = 0f
        var downY = 0f
        var startTranslation = 0f
        var swipeConfirmed = false

        deleteButton.setOnClickListener { onDelete() }

        contentCard.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (openSwipeCard != null && openSwipeCard !== contentCard) {
                        openSwipeCard?.animate()?.translationX(0f)?.setDuration(200)?.start()
                        openSwipeCard = null
                    }
                    downX = event.rawX
                    downY = event.rawY
                    startTranslation = contentCard.translationX
                    swipeConfirmed = false
                    true  // must claim DOWN to receive MOVE/UP
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!swipeConfirmed) {
                        when {
                            abs(dx) > touchSlop && abs(dx) > abs(dy) -> {
                                swipeConfirmed = true
                                contentCard.parent.requestDisallowInterceptTouchEvent(true)
                            }
                            abs(dy) > touchSlop -> {
                                // vertical — release to scroll
                                contentCard.parent.requestDisallowInterceptTouchEvent(false)
                                return@setOnTouchListener false
                            }
                        }
                    }
                    if (swipeConfirmed) {
                        contentCard.translationX = (startTranslation + dx).coerceIn(-deleteBtnWidth, 0f)
                        true
                    } else false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (swipeConfirmed) {
                        if (contentCard.translationX < -deleteBtnWidth / 2f) {
                            contentCard.animate().translationX(-deleteBtnWidth).setDuration(200).start()
                            openSwipeCard = contentCard
                        } else {
                            contentCard.animate().translationX(0f).setDuration(200).start()
                            if (openSwipeCard === contentCard) openSwipeCard = null
                        }
                        true
                    } else {
                        // tap with no swipe: close if open
                        if (contentCard.translationX < 0f) {
                            contentCard.animate().translationX(0f).setDuration(200).start()
                            if (openSwipeCard === contentCard) openSwipeCard = null
                            true
                        } else false
                    }
                }
                else -> false
            }
        }
    }
}
