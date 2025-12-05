# Arcadia Project Instructions

## Tech Stack Overview
- **Language**: Kotlin (JVM 21)
- **Framework**: Android (MinSdk 28, TargetSdk 36)
- **UI**: Jetpack Compose (Material 3)
- **Navigation**: AndroidX Navigation 3 (`androidx.navigation3`)
- **DI**: Koin
- **Async**: Coroutines, Flow
- **Data**: Room, Retrofit, Paging 3, Firebase (Auth, Firestore, Storage)
- **AI**: Google Gemini, Groq

## Architecture
Follow **Clean Architecture** principles:
- **Presentation**: `app/src/main/java/com/example/arcadia/presentation` (Screens, ViewModels)
- **Domain**: `app/src/main/java/com/example/arcadia/domain` (Repositories, UseCases, Models)
- **Data**: `app/src/main/java/com/example/arcadia/data` (Repository Impls, Data Sources, DTOs)
- **DI**: `app/src/main/java/com/example/arcadia/di` (Koin Modules)

## Critical Patterns

### Navigation (Navigation 3)
**DO NOT** use `NavHost` or string-based routes. This project uses **Navigation 3**.
- Define screens as `Serializable` objects implementing `NavKey` in `NavigationRoot.kt`.
- Use `NavDisplay` and `rememberNavBackStack`.
- Pass navigation lambdas to screens (e.g., `onNavigateToHome: () -> Unit`).
- Example:
  ```kotlin
  @Serializable
  object HomeScreenKey : NavKey
  
  // In NavigationRoot
  NavEntry(key = HomeScreenKey) { HomeScreen(...) }
  ```

### State Management & ViewModels
- All ViewModels must extend `BaseViewModel` (`presentation/base/BaseViewModel.kt`).
- Use `RequestState<T>` wrapper for async data operations.
- Use `launchWithState` helper to automatically handle Loading/Success/Error states.
- Example:
  ```kotlin
  private val _data = MutableStateFlow<RequestState<List<Game>>>(RequestState.Idle)
  val data = _data.asStateFlow()
  
  fun loadData() {
      launchWithState(_data) { repository.getGames() }
  }
  ```

### Dependency Injection (Koin)
- Register new components in the appropriate module in `di/` (e.g., `ViewModelModule.kt`, `RepositoryModule.kt`).
- Inject into ViewModels using constructor injection.
- Inject into Composables using `koinViewModel()`.

### AI Integration
- Use `AIRepository` interface for AI operations.
- Implementations include `GeminiRepository` and `GroqRepository`.
- Use `FallbackAIRepository` to handle multiple providers.

## Developer Workflow
- **API Keys**: Managed in `local.properties` (RAWG_API_KEY, GEMINI_API_KEY, GROQ_API_KEY) and exposed via `BuildConfig`.
- **Build**: Uses Gradle Version Catalogs (`libs.versions.toml`).
- **Formatting**: Follow Kotlin coding conventions.
