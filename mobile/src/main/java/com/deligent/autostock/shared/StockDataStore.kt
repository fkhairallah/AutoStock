package com.deligent.autostock.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class QuoteState {
    object Idle : QuoteState()
    object Loading : QuoteState()
    data class Success(val quotes: List<StockQuote>, val lastUpdated: String) : QuoteState()
}

object StockDataStore {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val repository = StockRepository()

    private val _state = MutableStateFlow<QuoteState>(QuoteState.Idle)
    val state: StateFlow<QuoteState> = _state.asStateFlow()

    private var refreshJob: Job? = null

    fun refresh(symbols: List<String>) {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            _state.value = QuoteState.Loading
            val quotes = repository.getStockQuotes(symbols)
            val timestamp = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            _state.value = QuoteState.Success(quotes, timestamp)
        }
    }
}
