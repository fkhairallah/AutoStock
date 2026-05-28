# AutoStock

A lightweight Android app for tracking real-time stock quotes.

## Features

- Real-time stock prices via the [Yahoo Finance API](https://finance.yahoo.com) (no API key required)
- After-hours / pre-market prices with change displayed under each symbol
- Add and remove stocks from a persistent watchlist
- Swipe left to reveal a DELETE button for removing a stock
- Color-coded price changes (green / red)
- Sort by default order, alphabetical, or % price movement
- Pull-to-refresh
- Dark-themed Material Design 3 UI

## Screenshots

_Coming soon_

## Requirements

- Android Studio Hedgehog or newer
- Android SDK API 36 (compile) / API 35+ (runtime)
- Java 11+

## Setup

1. Clone the repo:
   ```bash
   git clone https://github.com/fkhairallah/AutoStock.git
   cd AutoStock
   ```

2. Open the project in Android Studio and sync Gradle.

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires keystore.properties — see below)
./gradlew assembleRelease
```

### Release signing

Create `keystore.properties` in the project root (not committed to git):

```properties
storeFile=path/to/your.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

## Default Watchlist

The app starts with these symbols: `NVDA`, `TSLA`, `MSFT`, `BRK-B`, `HQY`. The watchlist is stored in SharedPreferences and persists across restarts.

## Project Structure

```
AutoStock/
├── mobile/                  # Main app module
│   └── src/main/java/com/deligent/autostock/
│       ├── MainActivity.kt          # Phone UI
│       └── shared/
│           ├── StockRepository.kt   # Yahoo Finance API client
│           └── SymbolStore.kt       # Watchlist persistence
└── gradle/
    └── libs.versions.toml           # Dependency version catalog
```

## Tech Stack

- **Kotlin** + Coroutines
- **Material Design 3**
- **SwipeRefreshLayout** for pull-to-refresh
- **Yahoo Finance REST API** for real-time and extended-hours market data

## License

MIT
