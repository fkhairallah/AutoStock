package com.deligent.autostock.shared

import android.content.Context

class SymbolStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSymbols(): List<String> {
        val saved = prefs.getString(KEY_SYMBOLS, null)
        return if (saved.isNullOrBlank()) DEFAULT_SYMBOLS
        else saved.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    fun saveSymbols(symbols: List<String>) {
        prefs.edit().putString(KEY_SYMBOLS, symbols.joinToString(",")).apply()
    }

    companion object {
        private const val PREFS_NAME = "autostock_prefs"
        private const val KEY_SYMBOLS = "symbols"
        val DEFAULT_SYMBOLS = listOf("NVDA", "TSLA", "MSFT", "BRK-B", "HQY")
    }
}
