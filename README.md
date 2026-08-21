# Piggy Ledger

Piggy Ledger is a modern, Android-native financial management application designed to help users track their expenses, income, and debts effortlessly. Built with a focus on performance, clean architecture, and a seamless user experience.

## Tech Stack & Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture principles
- **Local Persistence**: Room Database
- **Networking**: Retrofit & Ktor
- **Asynchronous Operations**: Kotlin Coroutines & Flow
- **Data Serialization**: kotlinx.serialization
- **Dependency Injection**: Constructor Injection

## Key Features

- **Expense Tracking**: Easily log and categorize financial transactions.
- **Debt Management**: Visualize and manage money owed and money borrowed.
- **AI-Powered Insights**: Integrated AI chat to provide financial advice and data summaries.
- **Secure & Offline-First**: Data persistence powered by Room, ensuring functionality even without network connectivity.

## Getting Started

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   ```
2. **Open in Android Studio**: Ensure you have the latest version of Android Studio installed.
3. **Build & Run**: The project uses Gradle (Kotlin DSL). Simply sync the project and run on an emulator or physical device.

## Project Structure

- `/app`: Main application module.
  - `/src/main/java/com/oryno/piggy_ledger`: Core application code.
    - `/ui`: Compose screens and UI components.
    - `/data`: Room entities, DAOs, and repository implementations.
    - `/ai`: AI integration logic, chat models, and repository.
- `/gradle`: Build configuration and version catalog.

## Contributing

Contributions are welcome! Please fork the repository and submit pull requests for any improvements or new features.

---
*Built with ❤️ for better financial management.*
