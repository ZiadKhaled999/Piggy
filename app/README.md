# 🐷 Piggy Ledger — Android Personal Finance & AI Co-Pilot

**Piggy Ledger** is a modern, native Android financial tracking application built with **Kotlin**, **Jetpack Compose (Material 3)**, **Room Database**, and **Groq AI**. It provides users with full offline control over their personal ledger, accounts, loans, savings goals, and real-time revenue analytics, backed by an intelligent financial co-pilot.

---

## 🚀 Key Features

* **📊 Interactive Dashboard & Financial Analytics**: Real-time revenue charts, period filters (Weekly, Monthly, Yearly), expense/income breakdowns, and balance tracking across multiple accounts.
* **🏦 Multi-Account Management**: Track bank accounts, credit cards, digital wallets, and cash reserves with live balance calculations.
* **🎯 Savings Goals**: Define target savings goals, monitor progress bars, and log contributions.
* **🤝 Loans & Debt Tracker**: Record personal loans, borrowed funds, interest details, and settlement histories.
* **🤖 Sovereign Groq Financial Co-Pilot**: Context-aware AI assistant powered by Groq LLM API to analyze financial habits, provide budgeting recommendations, and give actionable insights.
* **🎨 Expressive Material 3 Design**: Features fluid custom animations, tailored color palettes, expressive loading indicators, and edge-to-edge layouts.

---

## 🏗️ Architecture & Tech Stack

Piggy Ledger follows modern Android development best practices with **MVVM (Model-View-ViewModel)** architecture and unidirectional data flow (UDF).

| Layer / Technology | Details |
| :--- | :--- |
| **Language** | Kotlin 1.9+ |
| **UI Framework** | Jetpack Compose (Material Design 3) |
| **State Management** | `ViewModel`, `StateFlow`, `collectAsStateWithLifecycle` |
| **Local Persistence** | Room Database (SQLite) + KSP Symbol Processor |
| **Asynchrony** | Kotlin Coroutines & Flow |
| **AI Integration** | Groq AI API (`llama-3.3-70b-versatile` / OpenAI-compatible REST endpoint) |
| **Build System** | Gradle (Kotlin DSL `.gradle.kts`) |

---

## 📁 Project Directory Structure

```text
app/src/main/java/com/oryno/piggy_ledger/
├── ai/
│   ├── AiChatViewModel.kt      # Manages AI Chat state, prompts, and context injection
│   ├── AiChatApi.kt            # Groq API Retrofit interface
│   ├── AiChatModels.kt         # Groq request/response data classes
│   └── AiChatRepository.kt     # Groq API repository handler
├── data/
│   ├── AppDatabase.kt          # Room database definition & migrations
│   ├── AccountDao.kt           # Account database operations
│   ├── TransactionDao.kt       # Expense/Income transaction DAO
│   ├── GoalDao.kt              # Savings goals persistence
│   └── LoanDao.kt              # Loans & debt tracking DAO
├── ui/
│   ├── PiggyLedgerApp.kt       # Main entry point, Scaffold, Floating Navbar & Navigation
│   ├── DashboardScreen.kt      # Home overview, balance cards & quick actions
│   ├── AnalyticsScreen.kt      # Revenue charts & financial metrics breakdown
│   ├── AiChatScreen.kt         # Sovereign AI Assistant chat screen
│   ├── AccountsScreen.kt       # Account management & transaction history
│   ├── GoalsScreen.kt          # Savings targets & milestone progress
│   ├── LoansScreen.kt          # Loan ledger & settlement controls
│   ├── ExpressiveLoadingIndicator.kt # Custom Material 3 loading indicator
│   └── theme/                  # Theme colors, typography, shapes, and dynamic styling
```

---

## 🛠️ Getting Started & Setup

### Prerequisites
* **Android Studio** (Jellyfish / Koala or newer recommended)
* **JDK 17** configured for Gradle builds
* **Android SDK**: `compileSdk = 34`, `targetSdk = 34`, `minSdk = 24`

### Configuration & API Keys
1. Piggy Ledger uses Groq API keys managed securely via environment variables or settings.
2. In production / development, pass your `GROQ_API_KEY` through the environment or platform secrets panel into `BuildConfig.GROQ_API_KEY`.
3. Do **not** commit credentials or `local.properties` to version control.

---

## 💡 Guidelines for Contributors & AI Co-Pilot Role

* **Assistant Role Boundaries**: The AI chat screen functions strictly as an **advisor and co-pilot**. It provides advice, analysis, and screen guidance, but does not execute direct background database mutations without explicit user interaction in the dedicated UI forms.
* **No Hardcoded Mock Data in Production Analytics**: Financial metrics and revenue charts consume real-time Room database records.
* **UI & Styling**: Always use `MaterialTheme.colorScheme` tokens and centralized theme colors (`PinkPrimary`, `AiSurface`, `AiBackground`) rather than hardcoded hex values.

---

## 📄 License
This repository is maintained for Piggy Ledger. All rights reserved.
