# 🎮 ARCADIA - Complete Project Documentation

<div align="center">

```
    ╔═══════════════════════════════════════════════════════════════╗
    ║     █████╗ ██████╗  ██████╗ █████╗ ██████╗ ██╗ █████╗        ║
    ║    ██╔══██╗██╔══██╗██╔════╝██╔══██╗██╔══██╗██║██╔══██╗       ║
    ║    ███████║██████╔╝██║     ███████║██║  ██║██║███████║       ║
    ║    ██╔══██║██╔══██╗██║     ██╔══██║██║  ██║██║██╔══██║       ║
    ║    ██║  ██║██║  ██║╚██████╗██║  ██║██████╔╝██║██║  ██║       ║
    ║    ╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝╚═╝  ╚═╝╚═════╝ ╚═╝╚═╝  ╚═╝       ║
    ║                                                               ║
    ║           🎯 Your Personal Gaming Companion 🎯                ║
    ╚═══════════════════════════════════════════════════════════════╝
```

**Version 1.0** | **Android SDK 28-36** | **Kotlin + Jetpack Compose**

</div>

---

## 📑 Table of Contents

1. [Executive Summary](#executive-summary)
2. [System Architecture](#system-architecture)
3. [Package Structure](#package-structure)
4. [Data Models](#data-models)
5. [AI Integration System](#ai-integration-system)
6. [UI/UX Design System](#uiux-design-system)
7. [Screen Wireframes & Flow](#screen-wireframes--flow)
8. [Database Schema](#database-schema)
9. [API Integration](#api-integration)
10. [Dependency Injection](#dependency-injection)
11. [State Management](#state-management)
12. [Error Handling](#error-handling)
13. [Performance Optimizations](#performance-optimizations)
14. [Setup & Configuration](#setup--configuration)
15. [Testing Strategy](#testing-strategy)
16. [Appendix: Diagrams](#appendix-diagrams)

---

<a id="executive-summary"></a>
## 🚀 Executive Summary

### What is Arcadia?

**Arcadia** is a cutting-edge Android gaming companion application that revolutionizes how gamers discover, track, and manage their video game collections. Built with Modern Android Development (MAD) principles, it combines the power of AI with a beautiful, intuitive interface.

### 🎯 Core Value Propositions

| Feature | Description |
|---------|-------------|
| **🤖 AI-Powered Discovery** | Dual AI system (Gemini + Groq) provides personalized game recommendations |
| **📚 Smart Library Management** | Track games across all platforms with status, ratings, and reviews |
| **📊 Gaming Analytics** | Deep insights into your gaming habits and personality |
| **☁️ Cloud Sync** | Firebase-powered synchronization across devices |
| **📴 Offline Support** | Room database caching for seamless offline experience |

### 🛠️ Technology Stack Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        ARCADIA TECH STACK                       │
├─────────────────────────────────────────────────────────────────┤
│  UI Layer        │ Jetpack Compose + Material 3                 │
│  Architecture    │ Clean Architecture + MVVM                    │
│  DI Framework    │ Koin                                         │
│  Networking      │ Retrofit + OkHttp (HTTP/2, Brotli)          │
│  Local Storage   │ Room Database + Paging 3                     │
│  Backend         │ Firebase (Auth, Firestore, Storage)          │
│  AI Services     │ Google Gemini + Groq (Llama 3.3)            │
│  Game Data       │ RAWG Video Games Database API                │
│  Image Loading   │ Coil 3                                       │
│  Video Playback  │ ExoPlayer (Media3)                           │
└─────────────────────────────────────────────────────────────────┘
```

---

<a id="system-architecture"></a>
## 🏗 System Architecture

### Clean Architecture Overview

Arcadia follows **Clean Architecture** principles with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           PRESENTATION LAYER                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │
│  │   Screens   │  │  ViewModels │  │  Components │  │    Theme    │   │
│  │  (Compose)  │  │   (State)   │  │ (Reusable)  │  │  (Colors)   │   │
│  └──────┬──────┘  └──────┬──────┘  └─────────────┘  └─────────────┘   │
│         │                │                                              │
│         └────────────────┼──────────────────────────────────────────────┤
│                          ▼                                              │
├─────────────────────────────────────────────────────────────────────────┤
│                            DOMAIN LAYER                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                     │
│  │  Use Cases  │  │   Models    │  │ Repository  │                     │
│  │  (Business  │  │  (Entities) │  │ Interfaces  │                     │
│  │   Logic)    │  │             │  │             │                     │
│  └──────┬──────┘  └─────────────┘  └──────┬──────┘                     │
│         │                                  │                            │
│         └──────────────────────────────────┼────────────────────────────┤
│                                            ▼                            │
├─────────────────────────────────────────────────────────────────────────┤
│                             DATA LAYER                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │
│  │ Repository  │  │   Remote    │  │    Local    │  │   Mappers   │   │
│  │   Impls     │  │   (APIs)    │  │   (Room)    │  │             │   │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

### MVVM Pattern Implementation

```
┌──────────────────────────────────────────────────────────────────┐
│                         VIEW (Compose)                           │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  @Composable fun HomeScreen(viewModel: HomeViewModel)      │ │
│  │  - Observes screenState                                    │ │
│  │  - Calls viewModel methods on user actions                 │ │
│  │  - Renders UI based on state                               │ │
│  └────────────────────────────────────────────────────────────┘ │
│                              │                                   │
│                              ▼                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                    VIEWMODEL                               │ │
│  │  - var screenState by mutableStateOf(HomeScreenState())   │ │
│  │  - Exposes Flows for reactive data                        │ │
│  │  - Handles business logic via Use Cases                   │ │
│  │  - Manages coroutine scopes                               │ │
│  └────────────────────────────────────────────────────────────┘ │
│                              │                                   │
│                              ▼                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                    MODEL (Domain)                          │ │
│  │  - Data classes (Game, Gamer, GameListEntry)              │ │
│  │  - Repository interfaces                                   │ │
│  │  - Use Cases                                               │ │
│  └────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

---

<a id="package-structure"></a>
## 📦 Package Structure

### Complete Package Tree

```
com.example.arcadia/
│
├── 📁 data/                          # Data Layer
│   ├── 📁 background/                # Background workers
│   ├── 📁 local/                     # Local storage
│   │   ├── 📁 dao/                   # Data Access Objects
│   │   │   ├── CachedGamesDao.kt     # Paging 3 DAO for cached games
│   │   │   └── RecommendationFeedbackDao.kt
│   │   ├── 📁 entity/                # Room entities
│   │   │   ├── AIRecommendationRemoteKey.kt
│   │   │   ├── CachedGameEntity.kt
│   │   │   └── RecommendationFeedbackEntity.kt
│   │   ├── GameCacheDatabase.kt      # Room database for game cache
│   │   ├── StudioCacheDatabase.kt    # Room database for studio cache
│   │   ├── StudioCacheManager.kt     # Studio cache operations
│   │   └── HardcodedStudioMappings.kt
│   │
│   ├── 📁 mapper/                    # Data mappers
│   │   ├── AIResponseMapper.kt
│   │   └── StudioMapper.kt
│   │
│   ├── 📁 paging/                    # Paging 3 components
│   │   └── AIRecommendationsRemoteMediator.kt
│   │
│   ├── 📁 remote/                    # Remote data sources
│   │   ├── 📁 dto/                   # Data Transfer Objects
│   │   │   ├── GameDto.kt
│   │   │   ├── GameListEntryDto.kt
│   │   │   ├── MovieDto.kt
│   │   │   └── ScreenshotResponseDto.kt
│   │   ├── 📁 mapper/                # DTO to Domain mappers
│   │   │   ├── GameListEntryMapper.kt
│   │   │   └── GameMapper.kt
│   │   ├── AIClient.kt               # AI client interface
│   │   ├── AIConfig.kt               # AI configuration
│   │   ├── GeminiAIClient.kt         # Google Gemini implementation
│   │   ├── GeminiConfig.kt
│   │   ├── GeminiPrompts.kt          # AI prompt templates
│   │   ├── GroqAIClient.kt           # Groq implementation
│   │   ├── GroqApiService.kt
│   │   ├── GroqConfig.kt
│   │   └── RawgApiService.kt         # RAWG API interface
│   │
│   ├── 📁 repository/                # Repository implementations
│   │   ├── BaseAIRepository.kt       # Base AI repository
│   │   ├── FallbackAIRepository.kt   # Groq → Gemini fallback
│   │   ├── GameListRepositoryImpl.kt # Firebase game list
│   │   ├── GameRepositoryImpl.kt     # RAWG API repository
│   │   ├── GeminiRepository.kt
│   │   ├── GroqRepository.kt
│   │   └── PagedGameRepositoryImpl.kt # Paging 3 repository
│   │
│   └── GamerRepositoryImpl.kt        # User profile repository
│
├── 📁 di/                            # Dependency Injection
│   ├── AppModule.kt                  # Main module aggregator
│   ├── ImageLoaderModule.kt          # Coil configuration
│   ├── NetworkModule.kt              # Retrofit, OkHttp setup
│   ├── RepositoryModule.kt           # Repository bindings
│   ├── UseCaseModule.kt              # Use case bindings
│   ├── UtilModule.kt                 # Utility bindings
│   └── ViewModelModule.kt            # ViewModel bindings
│
├── 📁 domain/                        # Domain Layer (Pure Kotlin)
│   ├── 📁 model/                     # Domain entities
│   │   ├── 📁 ai/                    # AI-related models
│   │   │   ├── AIGameSuggestions.kt
│   │   │   ├── GameInsights.kt
│   │   │   ├── StreamingInsights.kt
│   │   │   ├── StudioExpansionResult.kt
│   │   │   ├── StudioMatch.kt
│   │   │   └── StudioSearchResult.kt
│   │   ├── 📁 ui/                    # UI state models
│   │   │   ├── AddToLibraryState.kt
│   │   │   ├── DetailsUiState.kt
│   │   │   ├── HomeSection.kt
│   │   │   ├── HomeUiState.kt
│   │   │   └── SearchUiState.kt
│   │   ├── AIError.kt
│   │   ├── DiscoveryFilter.kt
│   │   ├── Game.kt                   # Core game entity
│   │   ├── GameListEntry.kt          # User's library entry
│   │   ├── Gamer.kt                  # User profile
│   │   ├── OnBoardingPage.kt
│   │   └── StudioFilter.kt
│   │
│   ├── 📁 repository/                # Repository interfaces
│   │   ├── AIRepository.kt
│   │   ├── GameListRepository.kt
│   │   ├── GameRepository.kt
│   │   ├── GamerRepository.kt
│   │   ├── GeminiRepository.kt
│   │   ├── PagedGameRepository.kt
│   │   └── SortOrder.kt
│   │
│   └── 📁 usecase/                   # Business logic
│       ├── 📁 studio/
│       │   ├── GetLocalStudioSuggestionsUseCase.kt
│       │   ├── GetStudioExpansionUseCase.kt
│       │   └── SearchStudiosUseCase.kt
│       ├── AddGameToLibraryUseCase.kt
│       ├── AnalyzeGamingProfileUseCase.kt
│       ├── CalculateGamingStatsUseCase.kt
│       ├── DetermineGamingPersonalityUseCase.kt
│       ├── FilterGamesUseCase.kt
│       ├── GetAIGameSuggestionsUseCase.kt
│       ├── GetNewReleasesUseCase.kt
│       ├── GetPopularGamesUseCase.kt
│       ├── GetRecommendedGamesUseCase.kt
│       ├── GetUpcomingGamesUseCase.kt
│       ├── ParallelGameFilter.kt
│       ├── RemoveGameFromLibraryUseCase.kt
│       ├── SearchGamesUseCase.kt
│       ├── SortGamesUseCase.kt
│       └── UpdateGameEntryUseCase.kt
│
├── 📁 navigation/                    # Navigation
│   └── NavigationRoot.kt             # Navigation 3 setup
│
├── 📁 presentation/                  # Presentation Layer
│   ├── 📁 base/                      # Base classes
│   │   ├── BaseViewModel.kt
│   │   ├── LibraryAwareViewModel.kt
│   │   └── UndoableViewModel.kt
│   │
│   ├── 📁 components/                # Reusable UI components
│   │   ├── 📁 ai/                    # AI-specific components
│   │   ├── 📁 common/                # Common components
│   │   │   ├── EmptyState.kt
│   │   │   ├── ErrorState.kt
│   │   │   └── LoadingState.kt
│   │   ├── 📁 game_rating/           # Rating components
│   │   │   ├── GameRatingDialogs.kt
│   │   │   ├── GameRatingSections.kt
│   │   │   └── GameRatingSheet.kt
│   │   ├── 📁 sign_in/               # Auth components
│   │   │   ├── GoogleAuthUiClient.kt
│   │   │   ├── SignInResult.kt
│   │   │   ├── SignInState.kt
│   │   │   └── SignInViewModel.kt
│   │   ├── AddGameSnackbar.kt
│   │   ├── DiscoveryFilterDialog.kt
│   │   ├── EmptyState.kt
│   │   ├── FullscreenImageViewer.kt
│   │   ├── GoogleButton.kt
│   │   ├── ListGameCard.kt
│   │   ├── PlatformIcons.kt
│   │   ├── PrimaryButton.kt
│   │   ├── QuickRateDialog.kt
│   │   ├── QuickSettingsDialog.kt
│   │   ├── QuickStatusSheet.kt
│   │   ├── ReorderableGameList.kt
│   │   ├── ScrollToTopFAB.kt
│   │   ├── SearchBar.kt
│   │   ├── StudioFilterBottomSheet.kt
│   │   ├── SwipeToDeleteItem.kt
│   │   ├── TopNotification.kt
│   │   ├── UnsavedChangesSnackbar.kt
│   │   └── VideoPlayer.kt
│   │
│   └── 📁 screens/                   # App screens
│       ├── 📁 analytics/             # Analytics screen
│       │   ├── 📁 components/
│       │   │   └── AIInsightsSection.kt
│       │   ├── 📁 util/
│       │   ├── AnalyticsScreen.kt
│       │   └── AnalyticsViewModel.kt
│       ├── 📁 authScreen/            # Authentication
│       │   ├── 📁 components/
│       │   ├── AuthScreen.kt
│       │   └── AuthViewModel.kt
│       ├── 📁 detailsScreen/         # Game details
│       │   ├── 📁 components/
│       │   ├── DetailsScreen.kt
│       │   └── DetailsScreenViewModel.kt
│       ├── 📁 home/                  # Home/Discovery
│       │   ├── 📁 components/
│       │   ├── 📁 tabs/
│       │   ├── DiscoveryViewModel.kt
│       │   ├── HomeScreen.kt
│       │   └── HomeViewModel.kt
│       ├── 📁 myGames/               # Library
│       │   ├── 📁 components/
│       │   ├── MyGamesScreen.kt
│       │   └── MyGamesViewModel.kt
│       ├── 📁 onBoarding/            # Onboarding
│       │   ├── 📁 components/
│       │   └── OnBoardingScreen.kt
│       ├── 📁 profile/               # User profile
│       │   ├── 📁 components/
│       │   └── 📁 update_profile/
│       │       └── EditProfileViewModel.kt
│       └── 📁 searchScreen/          # Search
│           ├── 📁 components/
│           ├── SearchScreen.kt
│           └── SearchViewModel.kt
│
├── 📁 ui/                            # UI Theme
│   └── 📁 theme/
│       ├── Color.kt                  # Color definitions
│       ├── Dimensions.kt             # Spacing/sizing
│       ├── Theme.kt                  # Material theme
│       └── Type.kt                   # Typography
│
├── 📁 util/                          # Utilities
│   ├── Constants.kt
│   ├── Countries.kt
│   ├── DateUtils.kt
│   ├── NetworkCacheManager.kt
│   ├── PhotoPicker.kt
│   ├── PreferencesManager.kt
│   ├── RequestDeduplicator.kt
│   ├── RequestState.kt
│   └── SafeApiCall.kt
│
└── MainActivity.kt                   # Entry point
```

---

<a id="data-models"></a>
## 💾 Data Models

### Core Domain Entities

#### 🎮 Game Entity

```kotlin
data class Game(
    val id: Int,                      // RAWG game ID
    val slug: String,                 // URL-friendly name
    val name: String,                 // Display name
    val released: String?,            // Release date (YYYY-MM-DD)
    val backgroundImage: String?,     // Cover art URL
    val rating: Double,               // RAWG rating (0-5)
    val ratingTop: Int = 5,
    val ratingsCount: Int = 0,
    val metacritic: Int?,             // Metacritic score
    val playtime: Int,                // Average playtime (hours)
    val platforms: List<String>,      // PC, PlayStation, Xbox, etc.
    val genres: List<String>,         // Action, RPG, etc.
    val tags: List<String>,           // Detailed tags
    val screenshots: List<String>,    // Screenshot URLs
    val trailerUrl: String?,          // Video trailer URL
    val description: String?,         // HTML description
    val developers: List<String>,     // Developer studios
    val publishers: List<String>,     // Publisher companies
    
    // AI Metadata (populated by AI recommendations)
    val aiConfidence: Float?,         // 0-100 confidence score
    val aiReason: String?,            // Why this was recommended
    val aiTier: String?,              // PERFECT_MATCH, STRONG_MATCH, etc.
    val aiBadges: List<String>        // AI-generated tags
)
```

#### 👤 Gamer Entity (User Profile)

```kotlin
@Serializable
data class Gamer(
    val id: String = "",              // Firebase UID
    val name: String = "",
    val email: String = "",
    val username: String = "",
    val country: String? = null,
    val city: String? = null,
    val gender: String? = null,
    val description: String? = "",
    val profileImageUrl: String? = null,
    val profileComplete: Boolean = false
)
```

#### 📚 GameListEntry (Library Entry)

```kotlin
@Immutable
data class GameListEntry(
    val id: String = "",              // Firestore document ID
    val rawgId: Int = 0,              // RAWG game ID
    val name: String = "",
    val backgroundImage: String? = null,
    val genres: List<String> = emptyList(),
    val platforms: List<String> = emptyList(),
    val developers: List<String> = emptyList(),
    val publishers: List<String> = emptyList(),
    val addedAt: Long = 0L,           // Timestamp added
    val updatedAt: Long = 0L,         // Last update timestamp
    val status: GameStatus = GameStatus.WANT,
    val rating: Float? = null,        // User rating (0-10)
    val review: String = "",          // User notes
    val hoursPlayed: Int = 0,
    val aspects: List<String> = emptyList(),  // Best aspects tags
    val releaseDate: String? = null,
    val importance: Int = 0           // Custom ordering
)

enum class GameStatus(val displayName: String) {
    PLAYING("Playing"),
    FINISHED("Finished"),
    DROPPED("Dropped"),
    WANT("Want to Play"),
    ON_HOLD("On Hold")
}
```

### AI-Related Models

#### 🤖 AI Game Suggestions

```kotlin
data class AIGameSuggestions(
    val games: List<String>,                    // Game names
    val recommendations: List<GameRecommendation>,
    val reasoning: String?,                     // AI explanation
    val fromCache: Boolean = false
)

data class GameRecommendation(
    val name: String,
    val confidence: Int = 50,                   // 1-100
    val reason: String? = null,                 // 3-5 sentence explanation
    val badges: List<String> = emptyList(),     // AI-generated tags
    val developer: String? = null,
    val year: Int? = null,
    val similarTo: List<String> = emptyList()   // Similar games in library
)
```

#### 📊 Game Insights (Profile Analysis)

```kotlin
data class GameInsights(
    val personalityAnalysis: String,    // Who you are as a gamer
    val preferredGenres: List<String>,
    val playStyle: String,              // How you approach games
    val funFacts: List<String>,         // Surprising observations
    val recommendations: String         // Personalized suggestions
)
```

### Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        ENTITY RELATIONSHIPS                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   ┌─────────────┐         ┌─────────────────┐         ┌─────────────┐  │
│   │    Gamer    │ 1───────│ * GameListEntry │ *───────│ 1   Game    │  │
│   │  (Firebase) │         │   (Firebase)    │         │   (RAWG)    │  │
│   └─────────────┘         └─────────────────┘         └─────────────┘  │
│         │                        │                          │          │
│         │                        │                          │          │
│         ▼                        ▼                          ▼          │
│   ┌─────────────┐         ┌─────────────────┐         ┌─────────────┐  │
│   │  - id (UID) │         │  - id (doc ID)  │         │  - id       │  │
│   │  - name     │         │  - rawgId (FK)  │         │  - name     │  │
│   │  - email    │         │  - status       │         │  - rating   │  │
│   │  - username │         │  - rating       │         │  - genres   │  │
│   │  - country  │         │  - review       │         │  - platforms│  │
│   │  - avatar   │         │  - hoursPlayed  │         │  - devs     │  │
│   └─────────────┘         │  - aspects      │         │  - pubs     │  │
│                           │  - importance   │         │  - metacrit │  │
│                           └─────────────────┘         └─────────────┘  │
│                                                                         │
│   ┌─────────────────────────────────────────────────────────────────┐  │
│   │                    CachedGameEntity (Room)                      │  │
│   │  - All Game fields + AI metadata + cache timestamp              │  │
│   │  - Used for offline support and Paging 3                        │  │
│   └─────────────────────────────────────────────────────────────────┘  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

<a id="ai-integration-system"></a>
## 🤖 AI Integration System

### Dual AI Architecture

Arcadia implements a sophisticated dual-AI system with automatic fallback:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         AI FALLBACK SYSTEM                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   ┌─────────────────────────────────────────────────────────────────┐  │
│   │                    FallbackAIRepository                         │  │
│   │  - Implements AIRepository interface                            │  │
│   │  - Manages primary/fallback switching                           │  │
│   │  - Logs all fallback events for monitoring                      │  │
│   └─────────────────────────────────────────────────────────────────┘  │
│                              │                                          │
│              ┌───────────────┴───────────────┐                         │
│              ▼                               ▼                          │
│   ┌─────────────────────┐         ┌─────────────────────┐              │
│   │   PRIMARY: Groq     │         │  FALLBACK: Gemini   │              │
│   │   (Llama 3.3 70B)   │         │  (Flash 2.5)        │              │
│   ├─────────────────────┤         ├─────────────────────┤              │
│   │ ✅ Fast (< 2s)      │         │ ✅ Reliable         │              │
│   │ ✅ Cost-effective   │         │ ✅ High quality     │              │
│   │ ⚠️ Rate limits     │         │ ⚠️ Slower (~3-5s)  │              │
│   └─────────────────────┘         └─────────────────────┘              │
│                                                                         │
│   FALLBACK TRIGGERS:                                                    │
│   • Rate limit (429)                                                    │
│   • Network timeout                                                     │
│   • Parse errors                                                        │
│   • Any exception                                                       │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### AI Personas & Prompts

The AI system uses carefully crafted personas defined in `GeminiPrompts.kt`:

#### 1. 🎯 The Expert Curator
**Purpose:** General game suggestions based on natural language queries

```kotlin
fun gameSuggestionPrompt(userQuery: String, count: Int): String {
    return """
    You are a world-class Video Game Curator. The user has a request: "$userQuery"
    
    Your task: Curate a collection of $count games that perfectly answer this request.
    
    GUIDELINES:
    1. Understand the Vibe: Look beyond just keywords
    2. Quality First: Prioritize well-regarded or cult classics
    3. Diverse Selection: Mix big hits with hidden gems
    4. Released Games Only: No upcoming titles
    
    OUTPUT FORMAT (JSON ONLY):
    {
      "games": ["Exact Title 1", "Exact Title 2"],
      "reasoning": "Brief explanation..."
    }
    """.trimIndent()
}
```

#### 2. 🧠 The Gaming Psychologist
**Purpose:** Analyze user's gaming profile and personality

```kotlin
fun profileAnalysisPrompt(gameData: String): String = """
    Act as a "Gaming Psychologist" and analyze the user's gaming history.
    
    ANALYSIS GOALS:
    1. Identify the "Player DNA": What motivates them?
    2. Spot Patterns: Do they binge-play? Drop long games?
    3. Avoid Generic Advice: Be specific based on actual behavior
    4. STRICTLY EXCLUDE OWNED GAMES from recommendations
    
    OUTPUT FORMAT:
    ===PERSONALITY===
    2-3 warm, insightful sentences about their gaming identity.
    
    ===PLAY_STYLE===
    Describe how they approach games.
    
    ===INSIGHTS===
    - Surprising observation
    - Specific strength or quirk
    - Pattern they might not have noticed
    
    ===RECOMMENDATIONS===
    {"games":[{"name":"Title","reason":"Why this fits"}]}
""".trimIndent()
```

#### 3. 📚 The Library Curator
**Purpose:** Personalized recommendations based on user's library

```kotlin
fun libraryBasedRecommendationPromptV3(
    libraryData: String, 
    exclusionList: String, 
    count: Int
): String {
    return """
    You are a gaming expert. Suggest exactly $count games based on the user's library.
    
    RULES:
    1. NO games from exclusion list
    2. Only released games
    3. Match their favorite developers/genres
    4. Mix: 60% modern (2018+), 30% classic (2010-2017), 10% older gems
    
    TIERS:
    - PERFECT_MATCH: Same studio as favorite or spiritual successor
    - STRONG_MATCH: Matches 3+ aspects of taste
    - GOOD_MATCH: Matches genre + quality
    - DECENT_MATCH: Expands horizons
    
    OUTPUT JSON:
    {
      "games": [
        {
          "name": "Exact Game Title",
          "tier": "PERFECT_MATCH",
          "why": "Short 1-2 sentence reason",
          "badges": ["Tag1", "Tag2"],
          "developer": "Studio Name",
          "year": 2022,
          "similarTo": ["Game1", "Game2"]
        }
      ]
    }
    """.trimIndent()
}
```

### AI Recommendation Confidence Tiers

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    RECOMMENDATION CONFIDENCE TIERS                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   PERFECT_MATCH (95%)  ████████████████████████████████████████  🏆    │
│   • Same studio as a 9-10 rated game                                   │
│   • Spiritual successor to a favorite                                   │
│                                                                         │
│   STRONG_MATCH (82%)   ██████████████████████████████████  ⭐          │
│   • Matches 3+ aspects of user's taste                                 │
│   • Same developer as any liked game                                   │
│                                                                         │
│   GOOD_MATCH (68%)     ████████████████████████████  ✓                 │
│   • Matches genre + quality standards                                  │
│   • High Metacritic in preferred genre                                 │
│                                                                         │
│   DECENT_MATCH (55%)   ██████████████████████  ○                       │
│   • Expands horizons                                                   │
│   • Quality game outside comfort zone                                  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### AI Data Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         AI RECOMMENDATION FLOW                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   1. USER'S LIBRARY                                                     │
│      ┌─────────────────────────────────────────────────────────────┐   │
│      │ GameListEntry[] → Format as structured text                 │   │
│      │ Include: name, rating, status, genres, developers, hours    │   │
│      └─────────────────────────────────────────────────────────────┘   │
│                              │                                          │
│                              ▼                                          │
│   2. AI PROCESSING                                                      │
│      ┌─────────────────────────────────────────────────────────────┐   │
│      │ Groq/Gemini → Analyze patterns → Generate recommendations   │   │
│      │ Output: JSON with game names, tiers, reasons, badges        │   │
│      └─────────────────────────────────────────────────────────────┘   │
│                              │                                          │
│                              ▼                                          │
│   3. RAWG ENRICHMENT                                                    │
│      ┌─────────────────────────────────────────────────────────────┐   │
│      │ For each AI suggestion → Search RAWG API → Get full details │   │
│      │ Parallel fetching for speed (coroutineScope + async)        │   │
│      └─────────────────────────────────────────────────────────────┘   │
│                              │                                          │
│                              ▼                                          │
│   4. CACHING (Room + Paging 3)                                         │
│      ┌─────────────────────────────────────────────────────────────┐   │
│      │ CachedGameEntity → Store with AI metadata                   │   │
│      │ RemoteMediator → Handle pagination + offline support        │   │
│      └─────────────────────────────────────────────────────────────┘   │
│                              │                                          │
│                              ▼                                          │
│   5. UI DISPLAY                                                         │
│      ┌─────────────────────────────────────────────────────────────┐   │
│      │ PagingData<Game> → LazyColumn → GameCard with AI badges     │   │
│      │ Filter out games already in library (reactive)              │   │
│      └─────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

<a id="uiux-design-system"></a>
## 🎨 UI/UX Design System

### Color Palette - "Deep Space" Theme

Arcadia features a sleek, dark-themed UI inspired by modern gaming consoles and RGB aesthetics:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         ARCADIA COLOR PALETTE                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   PRIMARY COLORS                                                        │
│   ┌─────────────────────────────────────────────────────────────────┐  │
│   │  ██████  Surface/Background    #00123B  (Dark Navy)             │  │
│   │  ██████  Primary Button        #62B4DA  (Electric Blue)         │  │
│   │  ██████  Accent                #FBB02E  (Neon Gold)             │  │
│   │  ██████  Secondary             #B5A8D5  (Purple Haze)           │  │
│   │  ██████  Text Primary          #DCDCDC  (Light Gray)            │  │
│   └─────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│   GAME STATUS COLORS                                                    │
│   ┌─────────────────────────────────────────────────────────────────┐  │
│   │  ██████  Finished              #FBB02E  (Gold)                  │  │
│   │  ██████  Playing               #D34ECE  (Magenta)               │  │
│   │  ██████  Dropped               #BA5C3E  (Rust)                  │  │
│   │  ██████  On Hold               #62B4DA  (Cyan)                  │  │
│   │  ██████  Want to Play          #3F77CC  (Blue)                  │  │
│   └─────────────────────────────────────────────────────────────────┘  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Dynamic Rating Gradient System

Ratings use a sophisticated color gradient that transitions from warm to cool:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      RATING COLOR GRADIENT                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   0.0 ─────────────────────────────────────────────────────────── 10.0  │
│                                                                         │
│   ████  0-1   Extremely Bad    #CC0000 → #AA0000  (Deep Red)           │
│   ████  1-2   Very Bad         #FF4444 → #DD2222  (Warm Red)           │
│   ████  2-3   Bad              #FF5533 → #EE3322  (Red-Orange)         │
│   ████  3-4   Poor             #FF6B35 → #FF4444  (Orange-Red)         │
│   ████  4-5   Below Average    #FF8844 → #FF7733  (Orange)             │
│   ████  5-6   Average          #FBB02E → #FF8844  (Yellow-Orange)      │
│   ████  6-7   Decent           #FFCC00 → #FBB02E  (Gold-Yellow)        │
│   ████  7-7.5 Good             #FFD700 → #FFCC00  (Gold)               │
│   ████  7.5-8 Very Good        #CCFF00 → #FFD700  (Gold-Lime)          │
│   ████  8-8.5 Great            #00DDAA → #88DD55  (Blue-Green)         │
│   ████  8.5-9 Excellent        #00DDFF → #00DDAA  (Aqua)               │
│   ████  9-9.5 Outstanding      #00D9FF → #00BBDD  (Cyan)               │
│   ████  9.5-10 Masterpiece     #00EEFF → #00CCFF  (Bright Cyan)        │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Typography System

```kotlin
// Custom Fonts
val BebasNeueFont = FontFamily(Font(R.font.bebas_neue_regular))
val RobotoCondensedFont = FontFamily(Font(R.font.roboto_condensed_medium))

// Font Sizes
object FontSize {
    val EXTRA_SMALL = 10.sp
    val SMALL = 12.sp
    val REGULAR = 14.sp
    val EXTRA_REGULAR = 16.sp
    val MEDIUM = 18.sp
    val EXTRA_MEDIUM = 20.sp
    val LARGE = 30.sp
    val EXTRA_LARGE = 40.sp
}
```

### Custom Icons

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         CUSTOM ICON ASSETS                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   PLATFORM ICONS                                                        │
│   🎮 playstation_ic.xml    PlayStation logo                            │
│   🎮 xbox_ic.xml           Xbox logo                                   │
│   🎮 nintendo_switch_ic.xml Nintendo Switch logo                       │
│   💻 pc_ic.xml             PC/Desktop icon                             │
│                                                                         │
│   STATUS ICONS                                                          │
│   ✅ finished_ic.xml       Checkmark/Trophy                            │
│   ▶️ playing_ic.xml        Play button                                 │
│   ❌ dropped_ic.xml        X mark                                      │
│   ⏸️ on_hold_ic.xml        Pause icon                                  │
│   ⭐ want_ic.xml           Star/Wishlist                               │
│                                                                         │
│   RATING ICONS (Emotion-based)                                          │
│   😢 between0and2_ic.xml   Very sad face                               │
│   😕 from2to4_ic.xml       Disappointed face                           │
│   😐 from4to6_ic.xml       Neutral face                                │
│   🙂 from6_5to7_5_ic.xml   Slight smile                                │
│   😊 from7_5to8_5_ic.xml   Happy face                                  │
│   😄 from8_5to9_5_ic.xml   Very happy face                             │
│   🤩 from9_5to10_ic.xml    Star-struck face                            │
│   ❓ no_rating_ic.xml      Question mark                               │
│                                                                         │
│   OTHER                                                                 │
│   🤖 ai_analysis.xml       AI brain icon                               │
│   🎮 ai_controller.xml     AI + controller combo                       │
│   📷 camera.xml            Camera for profile                          │
│   🎮 controller.xml        Generic controller                          │
│   🔷 logo.xml              Arcadia logo                                │
│   🌟 splash_logo.xml       Splash screen logo                          │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

<a id="screen-wireframes--flow"></a>
## 📱 Screen Wireframes & Flow

### Application Navigation Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       ARCADIA NAVIGATION FLOW                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│                        ┌─────────────────┐                              │
│                        │  SPLASH SCREEN  │                              │
│                        │   (Animated)    │                              │
│                        └────────┬────────┘                              │
│                                 │                                       │
│                    ┌────────────┴────────────┐                         │
│                    ▼                         ▼                          │
│         ┌─────────────────┐       ┌─────────────────┐                  │
│         │   ONBOARDING    │       │   AUTH SCREEN   │                  │
│         │  (First Launch) │──────▶│ (Google/Email)  │                  │
│         └─────────────────┘       └────────┬────────┘                  │
│                                            │                            │
│                    ┌───────────────────────┴───────────────────────┐   │
│                    ▼                                               ▼    │
│         ┌─────────────────┐                              ┌──────────┐  │
│         │  EDIT PROFILE   │◀─────────────────────────────│   HOME   │  │
│         │  (New Users)    │                              │  SCREEN  │  │
│         └─────────────────┘                              └────┬─────┘  │
│                                                               │        │
│   ┌───────────────────────────────────────────────────────────┼────┐   │
│   │                    BOTTOM NAVIGATION                      │    │   │
│   │  ┌─────────┐    ┌─────────┐    ┌─────────┐               │    │   │
│   │  │  HOME   │    │ DISCOVER│    │ LIBRARY │               │    │   │
│   │  │  (Tab)  │    │  (Tab)  │    │  (Tab)  │               │    │   │
│   │  └────┬────┘    └────┬────┘    └────┬────┘               │    │   │
│   └───────┼──────────────┼──────────────┼────────────────────┘    │   │
│           │              │              │                          │   │
│           ▼              ▼              ▼                          │   │
│   ┌─────────────┐ ┌─────────────┐ ┌─────────────┐                 │   │
│   │ • Popular   │ │ • AI Recs   │ │ • All Games │                 │   │
│   │ • Upcoming  │ │ • Filters   │ │ • By Status │                 │   │
│   │ • New       │ │ • Studios   │ │ • Reorder   │                 │   │
│   │ • Trending  │ │ • Genres    │ │ • Stats     │                 │   │
│   └──────┬──────┘ └──────┬──────┘ └──────┬──────┘                 │   │
│          │               │               │                         │   │
│          └───────────────┼───────────────┘                         │   │
│                          ▼                                         │   │
│                 ┌─────────────────┐                                │   │
│                 │  GAME DETAILS   │◀───────────────────────────────┘   │
│                 │  • Hero Image   │                                    │
│                 │  • Info/Media   │                                    │
│                 │  • Add/Rate     │                                    │
│                 └────────┬────────┘                                    │
│                          │                                             │
│                          ▼                                             │
│                 ┌─────────────────┐         ┌─────────────────┐       │
│                 │  RATING SHEET   │         │    ANALYTICS    │       │
│                 │  • Status       │         │  • Stats        │       │
│                 │  • Rating       │         │  • AI Insights  │       │
│                 │  • Aspects      │         │  • Personality  │       │
│                 │  • Playtime     │         │  • Trends       │       │
│                 └─────────────────┘         └─────────────────┘       │
│                                                                        │
│                 ┌─────────────────┐                                    │
│                 │  SEARCH SCREEN  │◀──── (From any screen)            │
│                 │  • Text Search  │                                    │
│                 │  • AI Search    │                                    │
│                 │  • Filters      │                                    │
│                 └─────────────────┘                                    │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

### Screen Wireframes

#### 1. Home Screen (Tab 1)

```
┌────────────────────────────────────────┐
│ ┌────────────────────────────────────┐ │
│ │  🔍 Search...          ⚙️  🔔     │ │  ← Top Bar
│ └────────────────────────────────────┘ │
│                                        │
│ ▶ Popular Games                        │
│ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐   │
│ │ 🎮   │ │ 🎮   │ │ 🎮   │ │ 🎮   │   │  ← Horizontal Scroll
│ │ Game │ │ Game │ │ Game │ │ Game │   │
│ │ 9.2  │ │ 8.8  │ │ 9.0  │ │ 8.5  │   │
│ └──────┘ └──────┘ └──────┘ └──────┘   │
│                                        │
│ ▶ Upcoming Releases                    │
│ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐   │
│ │ 🎮   │ │ 🎮   │ │ 🎮   │ │ 🎮   │   │
│ │ TBA  │ │ 2025 │ │ 2025 │ │ TBA  │   │
│ └──────┘ └──────┘ └──────┘ └──────┘   │
│                                        │
│ ▶ New Releases                         │
│ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐   │
│ │ 🎮   │ │ 🎮   │ │ 🎮   │ │ 🎮   │   │
│ └──────┘ └──────┘ └──────┘ └──────┘   │
│                                        │
│ ▶ Recommended For You                  │
│ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐   │
│ │ 🎮   │ │ 🎮   │ │ 🎮   │ │ 🎮   │   │
│ └──────┘ └──────┘ └──────┘ └──────┘   │
│                                        │
├────────────────────────────────────────┤
│  🏠 Home    🔍 Discover    📚 Library  │  ← Bottom Nav
└────────────────────────────────────────┘
```

#### 2. Discover Screen (Tab 2) - AI Recommendations

```
┌────────────────────────────────────────┐
│ ┌────────────────────────────────────┐ │
│ │  🔍 Search...          ⚙️  🔔     │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ 🎯 AI Recommendations  ▼ Filters   │ │  ← Filter Bar
│ │ [Studios] [Genres] [Year] [Sort]   │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ 🏆 PERFECT MATCH                   │ │
│ │ ┌──────────────────────────────┐   │ │
│ │ │ 🎮 Elden Ring                │   │ │
│ │ │ ⭐ 95% Match                 │   │ │
│ │ │ 🏷️ FromSoft • Souls-like    │   │ │
│ │ │ "Similar to Dark Souls III" │   │ │
│ │ └──────────────────────────────┘   │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ ⭐ STRONG MATCH                    │ │
│ │ ┌──────────────────────────────┐   │ │
│ │ │ 🎮 Hollow Knight             │   │ │
│ │ │ ⭐ 82% Match                 │   │ │
│ │ │ 🏷️ Metroidvania • Indie     │   │ │
│ │ └──────────────────────────────┘   │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ ✓ GOOD MATCH                       │ │
│ │ ┌──────────────────────────────┐   │ │
│ │ │ 🎮 Celeste                   │   │ │
│ │ │ ⭐ 68% Match                 │   │ │
│ │ └──────────────────────────────┘   │ │
│ └────────────────────────────────────┘ │
│                                        │
├────────────────────────────────────────┤
│  🏠 Home    🔍 Discover    📚 Library  │
└────────────────────────────────────────┘
```

#### 3. Library Screen (Tab 3)

```
┌────────────────────────────────────────┐
│ ┌────────────────────────────────────┐ │
│ │ ← My Game List              ⋮     │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ [Filters] [Stats] [Analysis 📊]   │ │  ← Quick Actions
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ 📊 STATS CARD (Collapsible)        │ │
│ │ Total: 45 │ Finished: 20 │ Avg: 8.2│ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ ┌────┐ The Witcher 3        ✏️    │ │
│ │ │ 🎮 │ 9.5 ⭐ │ Finished 🏆       │ │
│ │ │    │ RPG, Action                │ │
│ │ └────┘ Added: Nov 15, 2024        │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ ┌────┐ Baldur's Gate 3      ✏️    │ │
│ │ │ 🎮 │ 9.8 ⭐ │ Playing ▶️        │ │
│ │ │    │ RPG, Turn-Based            │ │
│ │ └────┘ Updated: Nov 28, 2024      │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ ┌────┐ Cyberpunk 2077       ✏️    │ │
│ │ │ 🎮 │ Not Rated │ Want ⭐        │ │
│ │ │    │ RPG, Action                │ │
│ │ └────┘ Added: Nov 20, 2024        │ │
│ └────────────────────────────────────┘ │
│                                        │
│                              ⬆️ FAB    │  ← Scroll to Top
├────────────────────────────────────────┤
│  🏠 Home    🔍 Discover    📚 Library  │
└────────────────────────────────────────┘
```

#### 4. Game Details Screen

```
┌────────────────────────────────────────┐
│ ┌────────────────────────────────────┐ │
│ │                                    │ │
│ │     🎮 HERO IMAGE (Parallax)       │ │
│ │                                    │ │
│ │  ← Back              Game Title    │ │  ← Collapsing Toolbar
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ The Witcher 3: Wild Hunt           │ │
│ │ ⭐ 4.5/5 (RAWG) │ 93 Metacritic    │ │
│ │                                    │ │
│ │ 📅 May 19, 2015                    │ │
│ │ 🎮 PC, PS4, Xbox, Switch           │ │
│ │ 🏢 CD Projekt Red                  │ │
│ │ 🎭 Action, RPG, Open World         │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ [➕ Add to Library]                │ │  ← Primary Action
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ 📝 DESCRIPTION                     │ │
│ │ The Witcher 3 is a story-driven... │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ 🎬 TRAILER                         │ │
│ │ ┌──────────────────────────────┐   │ │
│ │ │        ▶️ Video Player       │   │ │
│ │ └──────────────────────────────┘   │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ 📸 SCREENSHOTS                     │ │
│ │ ┌────┐ ┌────┐ ┌────┐ ┌────┐       │ │
│ │ │ 📷 │ │ 📷 │ │ 📷 │ │ 📷 │       │ │
│ │ └────┘ └────┘ └────┘ └────┘       │ │
│ └────────────────────────────────────┘ │
└────────────────────────────────────────┘
```

#### 5. Game Rating Sheet (Bottom Sheet)

```
┌────────────────────────────────────────┐
│ ┌────────────────────────────────────┐ │
│ │ ═══════════════════════════════    │ │  ← Drag Handle
│ │                                    │ │
│ │ 🎮 The Witcher 3                   │ │
│ │ ┌────┐                             │ │
│ │ │ 🖼️ │  CD Projekt Red            │ │
│ │ └────┘  RPG, Action                │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ 📊 SLIDE TO RATE           ▼      │ │
│ │                                    │ │
│ │         🤩  9.5                    │ │  ← Animated Icon
│ │                                    │ │
│ │ ═══════════════════●═══════════   │ │  ← Slider
│ │ 0                              10  │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ 🏷️ CLASSIFICATION          ▼      │ │
│ │                                    │ │
│ │ [Finished🏆] [Playing] [Dropped]   │ │
│ │ [On Hold] [Want to Play]           │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ ⭐ BEST ASPECTS             ▼      │ │
│ │                                    │ │
│ │ [Story✓] [Gameplay✓] [Graphics]    │ │
│ │ [Music] [Characters✓] [+ Add]      │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ ⏱️ PLAYTIME                 ▼      │ │
│ │                                    │ │
│ │ [5h] [10h] [20h] [30h✓] [Custom]   │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │         [💾 SAVE CHANGES]          │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │         [🗑️ Remove from Library]   │ │
│ └────────────────────────────────────┘ │
└────────────────────────────────────────┘
```

#### 6. Analytics Screen

```
┌────────────────────────────────────────┐
│ ┌────────────────────────────────────┐ │
│ │ ← Gaming Analytics                 │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ 📊 YOUR STATS                      │ │
│ │                                    │ │
│ │  45        20        5       8.2   │ │
│ │ Total   Finished  Dropped   Avg    │ │
│ │                                    │ │
│ │ Completion Rate: ████████░░ 80%    │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ 🎭 TOP GENRES                      │ │
│ │                                    │ │
│ │ RPG        ████████████████  15    │ │
│ │ Action     ██████████████    12    │ │
│ │ Adventure  ████████████      10    │ │
│ │ Indie      ████████          8     │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ 🧠 GAMING PERSONALITY              │ │
│ │                                    │ │
│ │ You are: THE EXPLORER 🗺️           │ │
│ │                                    │ │
│ │ "You seek vast worlds to discover  │ │
│ │  and stories to uncover..."        │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ 🤖 AI INSIGHTS                     │ │
│ │                                    │ │
│ │ ═══════════════════════════════    │ │  ← Streaming Text
│ │ "Based on your library, you have   │ │
│ │  a strong preference for story-    │ │
│ │  driven experiences with deep      │ │
│ │  character development..."         │ │
│ │                                    │ │
│ │ 💡 Fun Facts:                      │ │
│ │ • You finish 80% of RPGs you start │ │
│ │ • Your avg session is 3+ hours     │ │
│ │ • You rate FromSoft games highest  │ │
│ └────────────────────────────────────┘ │
└────────────────────────────────────────┘
```

---

<a id="database-schema"></a>
## 🗄️ Database Schema

### Room Database Schema

Arcadia uses two Room databases for local caching:

#### 1. GameCacheDatabase

```sql
-- Table: cached_games
-- Purpose: Cache game data for offline support and Paging 3
CREATE TABLE cached_games (
    id              INTEGER PRIMARY KEY,    -- RAWG game ID
    slug            TEXT NOT NULL,
    name            TEXT NOT NULL,
    released        TEXT,
    backgroundImage TEXT,
    rating          REAL NOT NULL,
    ratingTop       INTEGER NOT NULL,
    ratingsCount    INTEGER NOT NULL,
    metacritic      INTEGER,
    playtime        INTEGER NOT NULL,
    platforms       TEXT NOT NULL,          -- JSON encoded list
    genres          TEXT NOT NULL,          -- JSON encoded list
    tags            TEXT NOT NULL,          -- JSON encoded list
    screenshots     TEXT NOT NULL,          -- JSON encoded list
    trailerUrl      TEXT,
    description     TEXT,
    developers      TEXT NOT NULL,          -- JSON encoded list
    publishers      TEXT NOT NULL,          -- JSON encoded list
    
    -- AI Recommendation Metadata
    isAIRecommendation    INTEGER DEFAULT 0,
    aiConfidence          REAL,
    aiReason              TEXT,
    aiTier                TEXT,
    aiBadges              TEXT DEFAULT '',   -- JSON encoded list
    aiRecommendationOrder INTEGER,
    
    -- Cache Metadata
    cachedAt              INTEGER NOT NULL,
    libraryHashWhenCached INTEGER
);

-- Table: ai_recommendation_remote_keys
-- Purpose: Paging 3 RemoteMediator state
CREATE TABLE ai_recommendation_remote_keys (
    id          INTEGER PRIMARY KEY DEFAULT 0,
    nextPage    INTEGER,
    prevPage    INTEGER,
    lastUpdated INTEGER NOT NULL
);

-- Table: recommendation_feedback
-- Purpose: Track user interactions for AI improvement
CREATE TABLE recommendation_feedback (
    gameId      INTEGER PRIMARY KEY,
    clicked     INTEGER DEFAULT 0,
    addedToLib  INTEGER DEFAULT 0,
    dismissed   INTEGER DEFAULT 0,
    timestamp   INTEGER NOT NULL
);
```

#### 2. StudioCacheDatabase

```sql
-- Table: studio_cache
-- Purpose: Cache studio expansion results from AI
CREATE TABLE studio_cache (
    parentStudio    TEXT PRIMARY KEY,
    displayNames    TEXT NOT NULL,          -- JSON encoded Set<String>
    slugs           TEXT NOT NULL,          -- Comma-separated slugs
    cachedAt        INTEGER NOT NULL
);
```

### Firebase Firestore Schema

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      FIRESTORE COLLECTIONS                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   📁 users/{userId}                                                     │
│   │                                                                     │
│   ├── id: string (Firebase UID)                                        │
│   ├── name: string                                                      │
│   ├── email: string                                                     │
│   ├── username: string                                                  │
│   ├── country: string?                                                  │
│   ├── city: string?                                                     │
│   ├── gender: string?                                                   │
│   ├── description: string?                                              │
│   ├── profileImageUrl: string?                                          │
│   └── profileComplete: boolean                                          │
│                                                                         │
│   📁 users/{userId}/gameList/{entryId}                                 │
│   │                                                                     │
│   ├── id: string (auto-generated)                                       │
│   ├── rawgId: number                                                    │
│   ├── name: string                                                      │
│   ├── backgroundImage: string?                                          │
│   ├── genres: string[]                                                  │
│   ├── platforms: string[]                                               │
│   ├── developers: string[]                                              │
│   ├── publishers: string[]                                              │
│   ├── addedAt: timestamp                                                │
│   ├── updatedAt: timestamp                                              │
│   ├── status: string (PLAYING|FINISHED|DROPPED|WANT|ON_HOLD)           │
│   ├── rating: number? (0.0 - 10.0)                                      │
│   ├── review: string                                                    │
│   ├── hoursPlayed: number                                               │
│   ├── aspects: string[]                                                 │
│   ├── releaseDate: string?                                              │
│   └── importance: number                                                │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

<a id="api-integration"></a>
## 🌐 API Integration

### RAWG Video Games Database API

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         RAWG API ENDPOINTS                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   BASE URL: https://api.rawg.io/api/                                   │
│                                                                         │
│   GET /games                                                            │
│   ├── page: int                                                         │
│   ├── page_size: int (max 40)                                          │
│   ├── ordering: string (-rating, -released, -added, name)              │
│   ├── dates: string (YYYY-MM-DD,YYYY-MM-DD)                            │
│   ├── genres: string (comma-separated IDs or slugs)                    │
│   ├── tags: string (comma-separated)                                   │
│   ├── search: string                                                    │
│   ├── developers: string (comma-separated slugs)                       │
│   └── publishers: string (comma-separated slugs)                       │
│                                                                         │
│   GET /games/{id}                                                       │
│   └── Returns full game details with description                       │
│                                                                         │
│   GET /games/{id}/movies                                                │
│   └── Returns video trailers                                           │
│                                                                         │
│   GET /games/{id}/screenshots                                           │
│   └── Returns screenshot URLs                                          │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Retrofit Service Interface

```kotlin
interface RawgApiService {
    
    @GET("games")
    suspend fun getGames(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("ordering") ordering: String? = null,
        @Query("dates") dates: String? = null,
        @Query("genres") genres: String? = null,
        @Query("tags") tags: String? = null,
        @Query("search") search: String? = null,
        @Query("developers") developers: String? = null,
        @Query("publishers") publishers: String? = null
    ): GamesResponse
    
    @GET("games/{id}")
    suspend fun getGameDetails(@Path("id") gameId: Int): GameDto

    @GET("games/{id}/movies")
    suspend fun getGameVideos(@Path("id") gameId: Int): MovieResponse

    @GET("games/{id}/screenshots")
    suspend fun getGameScreenshots(@Path("id") gameId: Int): ScreenshotResponse

    companion object {
        const val BASE_URL = "https://api.rawg.io/api/"
    }
}
```

### Network Configuration

```kotlin
// OkHttpClient with optimizations
OkHttpClient.Builder()
    // HTTP/2 support for multiplexing
    .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
    // Connection pooling (15 connections, 5 min keep-alive)
    .connectionPool(ConnectionPool(15, 5, TimeUnit.MINUTES))
    // Disk cache (100MB)
    .cache(Cache(cacheDir, 100L * 1024 * 1024))
    // Brotli compression (20-26% smaller than GZIP)
    .addInterceptor(BrotliInterceptor)
    // API key interceptor
    .addInterceptor(apiKeyInterceptor)
    // Cache control (5 min max-age)
    .addNetworkInterceptor(cacheInterceptor)
    // Timeouts
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(10, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .build()
```

---

<a id="dependency-injection"></a>
## 💉 Dependency Injection

### Koin Module Structure

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         KOIN MODULES                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   appModule = listOf(                                                   │
│       viewModelModule,                                                  │
│       repositoryModule,                                                 │
│       useCaseModule,                                                    │
│       networkModule,                                                    │
│       imageLoaderModule,                                                │
│       utilModule                                                        │
│   )                                                                     │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Module Details

#### NetworkModule
```kotlin
val networkModule = module {
    // JSON serialization
    single { Json { ignoreUnknownKeys = true; isLenient = true } }
    
    // Request deduplication
    single { RequestDeduplicator() }
    
    // In-memory cache
    single { NetworkCacheManager() }
    
    // OkHttpClient (optimized)
    single { /* HTTP/2, Brotli, caching */ }
    
    // Retrofit (RAWG)
    single { Retrofit.Builder().baseUrl(RawgApiService.BASE_URL)... }
    
    // RAWG API Service
    single<RawgApiService> { get<Retrofit>().create(...) }
    
    // Groq API Service
    single<GroqApiService> { get<Retrofit>(named("groqRetrofit")).create(...) }
}
```

#### RepositoryModule
```kotlin
val repositoryModule = module {
    // User profile
    single<GamerRepository> { GamerRepositoryImpl() }
    
    // Game data (RAWG)
    single<GameRepository> { 
        GameRepositoryImpl(get(), get(), get()) 
    }
    
    // User's game list (Firebase)
    single<GameListRepository> { GameListRepositoryImpl() }
    
    // Room databases
    single { GameCacheDatabase.getInstance(androidContext()) }
    single { StudioCacheDatabase.getInstance(androidContext()) }
    single { StudioCacheManager(get()) }
    
    // DAOs
    single { get<GameCacheDatabase>().cachedGamesDao() }
    single { get<GameCacheDatabase>().recommendationFeedbackDao() }
    
    // Paging 3 repository
    single<PagedGameRepository> {
        PagedGameRepositoryImpl(get(), get(), get(), get(), get())
    }
    
    // AI Repositories with fallback
    single<AIRepository>(named("groq")) { GroqRepository(get(), get()) }
    single<AIRepository>(named("gemini")) { GeminiRepository(get()) }
    single<AIRepository> { 
        FallbackAIRepository(
            primaryRepository = get(named("groq")),
            fallbackRepository = get(named("gemini"))
        )
    }
}
```

#### ViewModelModule
```kotlin
val viewModelModule = module {
    viewModel { SignInViewModel() }
    viewModel { AuthViewModel(get()) }
    viewModel { EditProfileViewModel(get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { DiscoveryViewModel(get(), get(), get(), get()) }
    viewModel { MyGamesViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { DetailsScreenViewModel(get(), get(), get(), get()) }
    viewModel { SearchViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { AnalyticsViewModel(get(), get(), get(), get()) }
}
```

---

<a id="state-management"></a>
## 🔄 State Management

### ViewModel State Pattern

Arcadia uses Compose's `mutableStateOf` for reactive UI state:

```kotlin
class HomeViewModel(...) : LibraryAwareViewModel(...) {
    
    // Screen state - single source of truth
    var screenState by mutableStateOf(HomeScreenState())
        private set
    
    // Paging 3 flow for AI recommendations
    val aiRecommendationsPaged: Flow<PagingData<Game>> = 
        gamesInLibrary.flatMapLatest { libraryIds ->
            pagedGameRepository.getAIRecommendations()
                .map { pagingData ->
                    pagingData.filter { game -> game.id !in libraryIds }
                }
                .cachedIn(viewModelScope)
        }
    
    // Update state immutably
    private fun updateState(update: HomeScreenState.() -> HomeScreenState) {
        screenState = screenState.update()
    }
}

data class HomeScreenState(
    val popularGames: RequestState<List<Game>> = RequestState.Idle,
    val upcomingGames: RequestState<List<Game>> = RequestState.Idle,
    val recommendedGames: RequestState<List<Game>> = RequestState.Idle,
    val newReleases: RequestState<List<Game>> = RequestState.Idle,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false
)
```

### RequestState Sealed Class

```kotlin
sealed class RequestState<out T> {
    object Idle : RequestState<Nothing>()
    object Loading : RequestState<Nothing>()
    data class Success<T>(val data: T) : RequestState<T>()
    data class Error(val message: String) : RequestState<Nothing>()
}
```

### Base ViewModel Hierarchy

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      VIEWMODEL HIERARCHY                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   ┌─────────────────────────────────────────────────────────────────┐  │
│   │                      BaseViewModel                              │  │
│   │  - launchWithKey(): Cancellable coroutine jobs                 │  │
│   │  - launchWithDebounce(): Debounced operations                  │  │
│   │  - showTemporaryNotification(): Timed notifications            │  │
│   └─────────────────────────────────────────────────────────────────┘  │
│                              │                                          │
│                              ▼                                          │
│   ┌─────────────────────────────────────────────────────────────────┐  │
│   │                  LibraryAwareViewModel                          │  │
│   │  - gamesInLibrary: StateFlow<Set<Int>>                         │  │
│   │  - isGameInLibrary(id): Boolean                                │  │
│   │  - addGameToLibrary(): With snackbar                           │  │
│   │  - onLibraryUpdated(): Override for reactions                  │  │
│   └─────────────────────────────────────────────────────────────────┘  │
│                              │                                          │
│                              ▼                                          │
│   ┌─────────────────────────────────────────────────────────────────┐  │
│   │                    UndoableViewModel                            │  │
│   │  - undoState: StateFlow<UndoState>                             │  │
│   │  - removeGameWithUndo(): Optimistic delete                     │  │
│   │  - undoRemoval(): Restore deleted item                         │  │
│   │  - UNDO_TIMEOUT_MS = 5000                                      │  │
│   └─────────────────────────────────────────────────────────────────┘  │
│                              │                                          │
│              ┌───────────────┼───────────────┐                         │
│              ▼               ▼               ▼                          │
│   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐                  │
│   │HomeViewModel│   │MyGamesVM    │   │DetailsVM   │                  │
│   └─────────────┘   └─────────────┘   └─────────────┘                  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

<a id="error-handling"></a>
## 🚨 Error Handling

### Error Handling Architecture

Arcadia implements a comprehensive error handling strategy across all layers:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      ERROR HANDLING ARCHITECTURE                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   ┌─────────────────────────────────────────────────────────────────┐  │
│   │                         UI LAYER                                │  │
│   │  • ErrorState composable for consistent error display           │  │
│   │  • Snackbar notifications for transient errors                  │  │
│   │  • Retry buttons with exponential backoff                       │  │
│   │  • Graceful degradation (show cached data on network failure)   │  │
│   └─────────────────────────────────────────────────────────────────┘  │
│                              │                                          │
│                              ▼                                          │
│   ┌─────────────────────────────────────────────────────────────────┐  │
│   │                      VIEWMODEL LAYER                            │  │
│   │  • RequestState sealed class for loading/success/error states   │  │
│   │  • Coroutine exception handlers                                 │  │
│   │  • User-friendly error message mapping                          │  │
│   │  • State recovery mechanisms                                    │  │
│   └─────────────────────────────────────────────────────────────────┘  │
│                              │                                          │
│                              ▼                                          │
│   ┌─────────────────────────────────────────────────────────────────┐  │
│   │                     REPOSITORY LAYER                            │  │
│   │  • SafeApiCall wrapper for network requests                     │  │
│   │  • Automatic retry with backoff                                 │  │
│   │  • Fallback to cache on network errors                          │  │
│   │  • AI fallback system (Groq → Gemini)                          │  │
│   └─────────────────────────────────────────────────────────────────┘  │
│                              │                                          │
│                              ▼                                          │
│   ┌─────────────────────────────────────────────────────────────────┐  │
│   │                      NETWORK LAYER                              │  │
│   │  • HTTP error code handling                                     │  │
│   │  • Timeout management                                           │  │
│   │  • Connection failure detection                                 │  │
│   │  • Rate limit handling (429)                                    │  │
│   └─────────────────────────────────────────────────────────────────┘  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### SafeApiCall Wrapper

```kotlin
// util/SafeApiCall.kt
suspend fun <T> safeApiCall(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    apiCall: suspend () -> T
): RequestState<T> {
    return withContext(dispatcher) {
        try {
            RequestState.Success(apiCall())
        } catch (e: HttpException) {
            RequestState.Error(mapHttpError(e.code()))
        } catch (e: IOException) {
            RequestState.Error("Network error. Please check your connection.")
        } catch (e: SocketTimeoutException) {
            RequestState.Error("Request timed out. Please try again.")
        } catch (e: Exception) {
            RequestState.Error(e.localizedMessage ?: "An unexpected error occurred.")
        }
    }
}

private fun mapHttpError(code: Int): String = when (code) {
    400 -> "Invalid request. Please try again."
    401 -> "Authentication required. Please sign in."
    403 -> "Access denied."
    404 -> "Content not found."
    429 -> "Too many requests. Please wait a moment."
    in 500..599 -> "Server error. Please try again later."
    else -> "Something went wrong (Error $code)."
}
```

### AI Error Types

```kotlin
// domain/model/AIError.kt
sealed class AIError : Exception() {
    object RateLimited : AIError()           // 429 - Too many requests
    object InvalidResponse : AIError()       // JSON parse failure
    object NetworkError : AIError()          // Connection issues
    object Timeout : AIError()               // Request timeout
    object ServiceUnavailable : AIError()    // 503 - Service down
    data class Unknown(val cause: Throwable) : AIError()
}
```

### Error State UI Components

```kotlin
// presentation/components/common/ErrorState.kt
@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Dimensions.PADDING_LARGE),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        onRetry?.let {
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                text = "Try Again",
                onClick = it
            )
        }
    }
}
```

### User-Facing Error States

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      USER-FACING ERROR STATES                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   ERROR TYPE              UI TREATMENT                    RECOVERY      │
│   ─────────────────────────────────────────────────────────────────    │
│                                                                         │
│   🌐 Network Error        Full-screen error state         Retry button  │
│                           "No internet connection"        + Pull refresh│
│                           Show cached data if available                 │
│                                                                         │
│   ⏱️ Timeout              Snackbar notification           Auto-retry    │
│                           "Request timed out"             (3 attempts)  │
│                                                                         │
│   🔒 Auth Error           Redirect to login               Re-auth flow  │
│                           "Session expired"                             │
│                                                                         │
│   🤖 AI Unavailable       Graceful degradation            Silent retry  │
│                           Hide AI sections                Groq→Gemini   │
│                           Show non-AI content                           │
│                                                                         │
│   📭 Empty State          Illustrated empty state         Action CTA    │
│                           "No games found"                "Add games"   │
│                                                                         │
│   🔄 Rate Limited         Snackbar with countdown         Auto-retry    │
│                           "Please wait 30s"               after delay   │
│                                                                         │
│   💥 Unexpected Error     Generic error state             Retry + Report│
│                           "Something went wrong"                        │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### ViewModel Error Handling Pattern

```kotlin
class HomeViewModel(...) : LibraryAwareViewModel(...) {
    
    var screenState by mutableStateOf(HomeScreenState())
        private set
    
    fun loadPopularGames() {
        viewModelScope.launch {
            screenState = screenState.copy(popularGames = RequestState.Loading)
            
            when (val result = getPopularGamesUseCase()) {
                is RequestState.Success -> {
                    screenState = screenState.copy(
                        popularGames = RequestState.Success(result.data)
                    )
                }
                is RequestState.Error -> {
                    // Try cache fallback
                    val cached = getCachedPopularGames()
                    screenState = if (cached.isNotEmpty()) {
                        screenState.copy(
                            popularGames = RequestState.Success(cached),
                            showOfflineBanner = true
                        )
                    } else {
                        screenState.copy(
                            popularGames = RequestState.Error(result.message)
                        )
                    }
                }
                else -> Unit
            }
        }
    }
}
```

### Retry Strategy

```kotlin
// Exponential backoff retry
suspend fun <T> retryWithBackoff(
    times: Int = 3,
    initialDelayMs: Long = 1000,
    maxDelayMs: Long = 10000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelayMs
    repeat(times - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w("Retry", "Attempt ${attempt + 1} failed, retrying in ${currentDelay}ms")
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
    }
    return block() // Last attempt, let exception propagate
}
```

### Firebase Error Handling

```kotlin
// Firestore operations with error mapping
suspend fun addGameToList(game: Game, status: GameStatus): RequestState<String> {
    return try {
        val docRef = firestore
            .collection("users")
            .document(currentUserId)
            .collection("gameList")
            .add(game.toEntry(status))
            .await()
        RequestState.Success(docRef.id)
    } catch (e: FirebaseFirestoreException) {
        RequestState.Error(mapFirestoreError(e.code))
    } catch (e: Exception) {
        RequestState.Error("Failed to save game. Please try again.")
    }
}

private fun mapFirestoreError(code: FirebaseFirestoreException.Code): String = when (code) {
    Code.PERMISSION_DENIED -> "You don't have permission to perform this action."
    Code.UNAVAILABLE -> "Service temporarily unavailable. Please try again."
    Code.UNAUTHENTICATED -> "Please sign in to continue."
    Code.NOT_FOUND -> "The requested data was not found."
    Code.ALREADY_EXISTS -> "This item already exists."
    else -> "An error occurred. Please try again."
}
```

---

<a id="performance-optimizations"></a>
## ⚡ Performance Optimizations

### Network Optimizations

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    NETWORK PERFORMANCE FEATURES                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   1. HTTP/2 MULTIPLEXING                                               │
│      • Multiple requests over single connection                        │
│      • Reduced latency for parallel fetches                            │
│                                                                         │
│   2. BROTLI COMPRESSION                                                │
│      • 20-26% smaller than GZIP                                        │
│      • Faster data transfer                                            │
│                                                                         │
│   3. CONNECTION POOLING                                                │
│      • 15 idle connections                                             │
│      • 5 minute keep-alive                                             │
│      • Reuse connections for sequential requests                       │
│                                                                         │
│   4. DISK CACHE (100MB)                                                │
│      • 5 minute max-age for API responses                              │
│      • 7 day stale-while-revalidate for offline                        │
│                                                                         │
│   5. IN-MEMORY CACHE                                                   │
│      • NetworkCacheManager for instant repeated loads                  │
│      • Short (2 min), Standard (5 min), Long (15 min) durations       │
│                                                                         │
│   6. REQUEST DEDUPLICATION                                             │
│      • RequestDeduplicator prevents duplicate simultaneous requests    │
│      • Same request returns cached result                              │
│                                                                         │
│   7. PARALLEL FETCHING                                                 │
│      • coroutineScope + async for concurrent API calls                 │
│      • Game details + videos + screenshots in parallel                 │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Image Loading Optimizations (Coil 3)

```kotlin
// Optimized image loading
SubcomposeAsyncImage(
    model = ImageRequest.Builder(context)
        .data(game.backgroundImage)
        .size(imageSizePx, imageSizePx)      // Exact size for memory efficiency
        .scale(Scale.FILL)
        .memoryCacheKey(game.backgroundImage) // Consistent cache keys
        .diskCacheKey(game.backgroundImage)
        .crossfade(true)                      // Smooth transitions
        .build(),
    contentDescription = game.name,
    contentScale = ContentScale.Crop
)
```

### Paging 3 with RemoteMediator

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    PAGING 3 ARCHITECTURE                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   ┌─────────────────────────────────────────────────────────────────┐  │
│   │                    AIRecommendationsRemoteMediator              │  │
│   │                                                                 │  │
│   │  load(LoadType, PagingState) → MediatorResult                  │  │
│   │                                                                 │  │
│   │  REFRESH:                                                       │  │
│   │  1. Check if cache is stale (library changed)                  │  │
│   │  2. Fetch AI recommendations from Groq/Gemini                  │  │
│   │  3. Enrich with RAWG data (parallel)                           │  │
│   │  4. Store in Room with AI metadata                             │  │
│   │                                                                 │  │
│   │  APPEND:                                                        │  │
│   │  1. Get next page from remote key                              │  │
│   │  2. Fetch more AI recommendations                              │  │
│   │  3. Append to Room cache                                       │  │
│   │                                                                 │  │
│   │  PREPEND: Not supported (AI recommendations are ordered)       │  │
│   └─────────────────────────────────────────────────────────────────┘  │
│                              │                                          │
│                              ▼                                          │
│   ┌─────────────────────────────────────────────────────────────────┐  │
│   │                    CachedGamesDao                               │  │
│   │                                                                 │  │
│   │  getAIRecommendationsPagingSource(): PagingSource<Int, Entity> │  │
│   │  - Ordered by aiRecommendationOrder                            │  │
│   │  - Filtered by isAIRecommendation = 1                          │  │
│   └─────────────────────────────────────────────────────────────────┘  │
│                              │                                          │
│                              ▼                                          │
│   ┌─────────────────────────────────────────────────────────────────┐  │
│   │                    UI (LazyColumn)                              │  │
│   │                                                                 │  │
│   │  val lazyPagingItems = aiRecommendationsPaged.collectAsLazyPagingItems() │
│   │  items(lazyPagingItems) { game -> GameCard(game) }             │  │
│   └─────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│   BENEFITS:                                                             │
│   ✅ Instant app restart (loads from Room cache)                       │
│   ✅ Offline support                                                    │
│   ✅ Progressive loading (10 games at a time)                          │
│   ✅ Automatic refresh when library changes                            │
│   ✅ Memory efficient (only visible items in memory)                   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### UI Performance

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      UI PERFORMANCE FEATURES                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   1. LAZY COMPOSABLES                                                  │
│      • LazyColumn, LazyRow, LazyVerticalGrid                          │
│      • Only compose visible items                                      │
│                                                                         │
│   2. STABLE KEYS                                                       │
│      • items(key = { it.id }) for efficient recomposition             │
│      • Prevents unnecessary item recreation                            │
│                                                                         │
│   3. REMEMBER & DERIVEDSTATEOF                                         │
│      • remember { } for expensive calculations                         │
│      • derivedStateOf { } for derived state                           │
│                                                                         │
│   4. IMMUTABLE DATA CLASSES                                            │
│      • @Immutable annotation on GameListEntry                         │
│      • Compose skips recomposition for unchanged data                 │
│                                                                         │
│   5. ANIMATION OPTIMIZATIONS                                           │
│      • animateFloatAsState with spring() for smooth animations        │
│      • AnimatedContent with proper transition specs                   │
│                                                                         │
│   6. SCROLL STATE PRESERVATION                                         │
│      • rememberSaveable for scroll positions                          │
│      • Custom LazyListStateSaver                                      │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

<a id="setup--configuration"></a>
## ⚙️ Setup & Configuration

### Prerequisites

- Android Studio Ladybug (2024.2.1) or newer
- JDK 21
- Android SDK 28-36
- Kotlin 2.0+

### API Keys Required

| Service | Purpose | Get Key |
|---------|---------|---------|
| RAWG API | Game metadata | [rawg.io/apidocs](https://rawg.io/apidocs) |
| Gemini API | AI recommendations | [aistudio.google.com](https://aistudio.google.com/) |
| Groq API | Fast AI inference | [console.groq.com](https://console.groq.com/) |

### Configuration Steps

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/Arcadia.git
cd Arcadia
```

2. **Configure API Keys** - Create `local.properties`:
```properties
sdk.dir=C\:\\Users\\YourUser\\AppData\\Local\\Android\\Sdk
RAWG_API_KEY=your_rawg_api_key_here
GEMINI_API_KEY=your_gemini_api_key_here
GROQ_API_KEY=your_groq_api_key_here
```

3. **Firebase Setup**
   - Create project at [Firebase Console](https://console.firebase.google.com/)
   - Add Android app with package `com.example.arcadia`
   - Download `google-services.json`
   - Place in `app/google-services.json`
   - Enable Authentication (Google Sign-In, Email/Password)
   - Enable Firestore Database
   - Enable Storage

4. **Build & Run**
```bash
./gradlew assembleDebug
# Or use Android Studio Run button
```

### Build Variants

| Variant | Minify | ProGuard | Use Case |
|---------|--------|----------|----------|
| debug | No | No | Development |
| release | Yes | Yes | Production |

---

<a id="testing-strategy"></a>
## 🧪 Testing Strategy

### Test Structure

```
app/src/
├── test/                    # Unit tests
│   └── java/
│       └── com/example/arcadia/
│           ├── domain/usecase/    # Use case tests
│           ├── data/repository/   # Repository tests
│           └── presentation/      # ViewModel tests
│
└── androidTest/             # Instrumented tests
    └── java/
        └── com/example/arcadia/
            ├── ui/                # UI tests (Compose)
            └── data/local/        # Room database tests
```

### Testing Recommendations

```kotlin
// Unit Test Example - Use Case
class AddGameToLibraryUseCaseTest {
    @Test
    fun `adding game returns success with entry id`() = runTest {
        val mockRepository = mockk<GameListRepository>()
        coEvery { mockRepository.addGameToList(any(), any()) } returns 
            RequestState.Success("entry123")
        
        val useCase = AddGameToLibraryUseCase(mockRepository)
        val result = useCase(testGame, GameStatus.PLAYING)
        
        assertThat(result).isInstanceOf(RequestState.Success::class.java)
    }
}

// Compose UI Test Example
class HomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun homeScreen_displaysPopularGames() {
        composeTestRule.setContent {
            ArcadiaTheme {
                HomeScreen(viewModel = fakeViewModel)
            }
        }
        
        composeTestRule.onNodeWithText("Popular Games").assertIsDisplayed()
    }
}
```

---

<a id="appendix-diagrams"></a>
## 📊 Appendix: Diagrams

### Complete System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              ARCADIA SYSTEM ARCHITECTURE                                │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐  │
│   │                              PRESENTATION LAYER                                 │  │
│   │  ┌─────────────────────────────────────────────────────────────────────────┐   │  │
│   │  │                         JETPACK COMPOSE UI                              │   │  │
│   │  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐          │   │  │
│   │  │  │  Home   │ │Discover │ │ Library │ │ Details │ │Analytics│          │   │  │
│   │  │  │ Screen  │ │ Screen  │ │ Screen  │ │ Screen  │ │ Screen  │          │   │  │
│   │  │  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘          │   │  │
│   │  └───────┼──────────┼──────────┼──────────┼──────────┼───────────────────┘   │  │
│   │          │          │          │          │          │                        │  │
│   │  ┌───────┴──────────┴──────────┴──────────┴──────────┴───────────────────┐   │  │
│   │  │                           VIEWMODELS                                   │   │  │
│   │  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐          │   │  │
│   │  │  │ HomeVM  │ │DiscVM   │ │MyGamesVM│ │DetailsVM│ │AnalytVM │          │   │  │
│   │  │  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘          │   │  │
│   │  └───────┼──────────┼──────────┼──────────┼──────────┼───────────────────┘   │  │
│   └──────────┼──────────┼──────────┼──────────┼──────────┼───────────────────────┘  │
│              │          │          │          │          │                          │
│   ┌──────────┼──────────┼──────────┼──────────┼──────────┼───────────────────────┐  │
│   │          │          │          │          │          │                       │  │
│   │  ┌───────┴──────────┴──────────┴──────────┴──────────┴───────────────────┐  │  │
│   │  │                            USE CASES                                   │  │  │
│   │  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐  │  │  │
│   │  │  │AddGameToLib  │ │GetAISuggest  │ │AnalyzeProf  │ │CalcStats     │  │  │  │
│   │  │  │FilterGames   │ │SearchGames   │ │SortGames    │ │RemoveGame    │  │  │  │
│   │  │  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘  │  │  │
│   │  └───────────────────────────────────────────────────────────────────────┘  │  │
│   │                                      │                                       │  │
│   │  ┌───────────────────────────────────┴───────────────────────────────────┐  │  │
│   │  │                       REPOSITORY INTERFACES                           │  │  │
│   │  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐  │  │  │
│   │  │  │GameRepository│ │GameListRepo  │ │AIRepository  │ │GamerRepo     │  │  │  │
│   │  │  │PagedGameRepo │ │              │ │              │ │              │  │  │  │
│   │  │  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘  │  │  │
│   │  └───────────────────────────────────────────────────────────────────────┘  │  │
│   │                              DOMAIN LAYER                                    │  │
│   └──────────────────────────────────────────────────────────────────────────────┘  │
│                                      │                                              │
│   ┌──────────────────────────────────┴──────────────────────────────────────────┐  │
│   │                              DATA LAYER                                      │  │
│   │  ┌───────────────────────────────────────────────────────────────────────┐  │  │
│   │  │                      REPOSITORY IMPLEMENTATIONS                       │  │  │
│   │  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐  │  │  │
│   │  │  │GameRepoImpl  │ │GameListImpl  │ │FallbackAI   │ │GamerRepoImpl │  │  │  │
│   │  │  │PagedGameImpl │ │              │ │GroqRepo     │ │              │  │  │  │
│   │  │  │              │ │              │ │GeminiRepo   │ │              │  │  │  │
│   │  │  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └──────┬───────┘  │  │  │
│   │  └─────────┼────────────────┼────────────────┼────────────────┼──────────┘  │  │
│   │            │                │                │                │             │  │
│   │  ┌─────────┴────────┐ ┌─────┴─────┐ ┌────────┴────────┐ ┌─────┴─────┐      │  │
│   │  │   REMOTE DATA    │ │  FIREBASE │ │    AI CLIENTS   │ │   LOCAL   │      │  │
│   │  │  ┌────────────┐  │ │           │ │  ┌───────────┐  │ │   DATA    │      │  │
│   │  │  │ RawgAPI    │  │ │ Firestore │ │  │ GroqAPI   │  │ │ ┌───────┐ │      │  │
│   │  │  │ Service    │  │ │ Auth      │ │  │ GeminiAPI │  │ │ │ Room  │ │      │  │
│   │  │  └────────────┘  │ │ Storage   │ │  └───────────┘  │ │ │ DAOs  │ │      │  │
│   │  └──────────────────┘ └───────────┘ └─────────────────┘ │ └───────┘ │      │  │
│   │                                                          └───────────┘      │  │
│   └─────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                     │
│   ┌─────────────────────────────────────────────────────────────────────────────┐  │
│   │                           EXTERNAL SERVICES                                 │  │
│   │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │  │
│   │  │  RAWG API   │  │  Firebase   │  │    Groq     │  │   Gemini    │        │  │
│   │  │ (Game Data) │  │  (Backend)  │  │ (Llama 3.3) │  │ (Flash 2.5) │        │  │
│   │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘        │  │
│   └─────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                     │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

### Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                 DATA FLOW DIAGRAM                                       │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│   USER ACTION                                                                           │
│       │                                                                                 │
│       ▼                                                                                 │
│   ┌─────────────────┐                                                                  │
│   │   UI (Compose)  │ ──────────────────────────────────────────────────────┐          │
│   └────────┬────────┘                                                       │          │
│            │ onClick/onAction                                               │          │
│            ▼                                                                │          │
│   ┌─────────────────┐                                                       │          │
│   │   ViewModel     │ ◀─────────────────────────────────────────────────────┤          │
│   │                 │                                                       │          │
│   │ • Update State  │                                                       │          │
│   │ • Call UseCase  │                                                       │          │
│   └────────┬────────┘                                                       │          │
│            │                                                                │          │
│            ▼                                                                │          │
│   ┌─────────────────┐                                                       │          │
│   │    Use Case     │                                                       │          │
│   │                 │                                                       │          │
│   │ • Business Logic│                                                       │          │
│   │ • Validation    │                                                       │          │
│   └────────┬────────┘                                                       │          │
│            │                                                                │          │
│            ▼                                                                │          │
│   ┌─────────────────┐         ┌─────────────────┐         ┌─────────────┐  │          │
│   │   Repository    │ ───────▶│   Remote API    │ ───────▶│   Server    │  │          │
│   │                 │         │   (Retrofit)    │         │ (RAWG/AI)   │  │          │
│   │ • Cache Check   │         └─────────────────┘         └──────┬──────┘  │          │
│   │ • Deduplication │                                            │         │          │
│   │ • Data Mapping  │◀───────────────────────────────────────────┘         │          │
│   └────────┬────────┘         Response                                     │          │
│            │                                                                │          │
│            │ ┌─────────────────┐                                           │          │
│            ├▶│   Room Cache    │ (Offline Support)                         │          │
│            │ └─────────────────┘                                           │          │
│            │                                                                │          │
│            │ ┌─────────────────┐                                           │          │
│            └▶│   Firebase      │ (User Data Sync)                          │          │
│              └─────────────────┘                                           │          │
│                     │                                                       │          │
│                     │ Flow<RequestState<T>>                                │          │
│                     ▼                                                       │          │
│   ┌─────────────────┐                                                       │          │
│   │   ViewModel     │                                                       │          │
│   │                 │                                                       │          │
│   │ screenState =   │                                                       │          │
│   │   state.copy()  │                                                       │          │
│   └────────┬────────┘                                                       │          │
│            │                                                                │          │
│            │ State Change (mutableStateOf)                                 │          │
│            ▼                                                                │          │
│   ┌─────────────────┐                                                       │          │
│   │   UI Recompose  │ ◀─────────────────────────────────────────────────────┘          │
│   │                 │                                                                  │
│   │ • Show Loading  │                                                                  │
│   │ • Display Data  │                                                                  │
│   │ • Handle Error  │                                                                  │
│   └─────────────────┘                                                                  │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### Component Interaction Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                            COMPONENT INTERACTIONS                                       │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐  │
│   │                              HOME SCREEN FLOW                                   │  │
│   │                                                                                 │  │
│   │   HomeScreen                                                                    │  │
│   │       │                                                                         │  │
│   │       ├──▶ HomeTopBar ──▶ [Search] ──▶ SearchScreen                            │  │
│   │       │                   [Settings] ──▶ EditProfileScreen                     │  │
│   │       │                                                                         │  │
│   │       ├──▶ HomeTabContent                                                       │  │
│   │       │       ├──▶ GameHorizontalList (Popular)                                │  │
│   │       │       │       └──▶ GameCard ──▶ [Click] ──▶ DetailsScreen              │  │
│   │       │       ├──▶ GameHorizontalList (Upcoming)                               │  │
│   │       │       ├──▶ GameHorizontalList (New Releases)                           │  │
│   │       │       └──▶ GameHorizontalList (Recommended)                            │  │
│   │       │                                                                         │  │
│   │       ├──▶ DiscoverTabContent                                                   │  │
│   │       │       ├──▶ DiscoveryFilterDialog                                       │  │
│   │       │       │       ├──▶ StudioFilterBottomSheet                             │  │
│   │       │       │       ├──▶ GenreChips                                          │  │
│   │       │       │       └──▶ SortOptions                                         │  │
│   │       │       └──▶ AIRecommendationsList (Paging 3)                            │  │
│   │       │               └──▶ AIGameCard ──▶ [Click] ──▶ DetailsScreen            │  │
│   │       │                                                                         │  │
│   │       └──▶ LibraryTabContent                                                    │  │
│   │               ├──▶ GameStatsCard                                               │  │
│   │               ├──▶ QuickSettingsDialog                                         │  │
│   │               └──▶ ReorderableGameList                                         │  │
│   │                       └──▶ ListGameCard                                        │  │
│   │                               ├──▶ [Click] ──▶ DetailsScreen                   │  │
│   │                               ├──▶ [Edit] ──▶ GameRatingSheet                  │  │
│   │                               └──▶ [Swipe] ──▶ Delete with Undo                │  │
│   │                                                                                 │  │
│   │       └──▶ HomeBottomBar ──▶ Tab Navigation                                    │  │
│   │                                                                                 │  │
│   └─────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                         │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐  │
│   │                            DETAILS SCREEN FLOW                                  │  │
│   │                                                                                 │  │
│   │   DetailsScreen                                                                 │  │
│   │       │                                                                         │  │
│   │       ├──▶ GameHeaderSection (Parallax Image)                                  │  │
│   │       │                                                                         │  │
│   │       ├──▶ GameDetailsContent                                                   │  │
│   │       │       ├──▶ GameInfo (Title, Rating, Platforms, Genres)                 │  │
│   │       │       ├──▶ AddToLibraryButton                                          │  │
│   │       │       │       └──▶ [Click] ──▶ GameRatingSheet                         │  │
│   │       │       ├──▶ Description                                                 │  │
│   │       │       ├──▶ VideoPlayer (ExoPlayer)                                     │  │
│   │       │       └──▶ ScreenshotGallery                                           │  │
│   │       │               └──▶ [Click] ──▶ FullscreenImageViewer                   │  │
│   │       │                                                                         │  │
│   │       └──▶ GameRatingSheet (Bottom Sheet)                                      │  │
│   │               ├──▶ SlideToRateSection                                          │  │
│   │               ├──▶ ClassificationSection                                       │  │
│   │               ├──▶ GameBestAspectsSection                                      │  │
│   │               ├──▶ PlaytimeSection                                             │  │
│   │               └──▶ [Save] ──▶ Firebase Update                                  │  │
│   │                                                                                 │  │
│   └─────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 📝 Changelog & Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | Nov 2024 | Initial release with core features |
| 0.2.0 | Oct 2024 | Added AI recommendations, Paging 3 |
| 0.1.0 | Sep 2024 | MVP with basic library management |

---

## 📄 License

This project is licensed under the MIT License.

---

<div align="center">

```
╔═══════════════════════════════════════════════════════════════╗
║                                                               ║
║   🎮 ARCADIA - Your Personal Gaming Companion 🎮              ║
║                                                               ║
║   Built with ❤️ using Modern Android Development              ║
║                                                               ║
║   Kotlin • Jetpack Compose • Firebase • AI                    ║
║                                                               ║
╚═══════════════════════════════════════════════════════════════╝
```

**November 30, 2025**

</div>
