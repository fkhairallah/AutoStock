# AutoStock

A lightweight Android app for tracking real-time stock quotes.

## Features

- Real-time stock prices via the [Finnhub API](https://finnhub.io)
- Add and remove stocks from a persistent watchlist
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
- A [Finnhub](https://finnhub.io) API key (free tier available)

## Setup

1. Clone the repo:
   ```bash
   git clone https://github.com/fkhairallah/AutoStock.git
   cd AutoStock
   ```

2. Add your Finnhub API key in `mobile/src/main/java/com/deligent/autostock/shared/StockRepository.kt`:
   ```kotlin
   private const val API_KEY = "your_finnhub_api_key_here"
   ```

3. Open the project in Android Studio and sync Gradle.

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
│           ├── StockRepository.kt   # Finnhub API client
│           └── SymbolStore.kt       # Watchlist persistence
└── gradle/
    └── libs.versions.toml           # Dependency version catalog
```

## Tech Stack

- **Kotlin** + Coroutines
- **Material Design 3**
- **SwipeRefreshLayout** for pull-to-refresh
- **Finnhub REST API** for market data

## License

MIT
