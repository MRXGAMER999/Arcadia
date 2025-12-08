# Gaming Roast Feature - Implementation Documentation

## Overview

The Gaming Roast feature is a viral engagement feature that uses AI (Gemini/Groq) to generate personalized, humorous roasts based on users' gaming statistics. This document details all files created/modified and known issues.

## Quick Summary

| Category | Count |
|----------|-------|
| New files created | 17 |
| Existing files modified | 11 |
| Known issues | 3 |

### New Files (17)
- **Domain:** 7 files (models, repository interfaces, use case)
- **Data:** 6 files (entity, dao, converters, repositories, mappers)
- **Presentation:** 9 files (screen, viewmodel, components, util)

### Modified Files (11)
- GameCacheDatabase, GeminiPrompts, BaseAIRepository, AIRepository
- NavigationRoot, AnalyticsScreen, ProfileScreen, ProfileViewModel
- RepositoryModule, UseCaseModule, ViewModelModule

---

## NEW FILES CREATED

### 1. Domain Models

#### `domain/model/ai/RoastInsights.kt`
```kotlin
data class RoastInsights(
    val headline: String,              // Main roast line
    val couldHaveList: List<String>,   // 5 things they could have done
    val prediction: String,            // Future prediction
    val wholesomeCloser: String,       // Positive ending
    val roastTitle: String,            // Badge title (2-4 words)
    val roastTitleEmoji: String        // Single emoji
)
```

#### `domain/model/ai/Badge.kt`
```kotlin
data class Badge(
    val title: String,   // "Side Quest Specialist"
    val emoji: String,   // "🗺️"
    val reason: String   // "Why they earned this"
)
```

#### `domain/model/ai/RoastStats.kt`
```kotlin
data class RoastStats(
    val hoursPlayed: Int,
    val totalGames: Int,
    val completionRate: Float,
    val completedGames: Int,
    val droppedGames: Int,
    val topGenres: List<Pair<String, Int>>,
    val gamingPersonality: String,
    val averageRating: Float
)
```

#### `domain/model/ai/RoastValidation.kt`
```kotlin
object RoastValidation {
    const val MIN_GAMES = 3
    const val MIN_HOURS = 5
    
    fun hasInsufficientStats(stats: RoastStats): Boolean =
        stats.totalGames < MIN_GAMES || stats.hoursPlayed < MIN_HOURS
}
```

---

### 2. Domain Repository Interfaces

#### `domain/repository/RoastRepository.kt`
```kotlin
data class RoastWithTimestamp(
    val roast: RoastInsights,
    val generatedAt: Long
)

interface RoastRepository {
    fun getLastRoast(): Flow<RoastInsights?>
    fun getLastRoastWithTimestamp(): Flow<RoastWithTimestamp?>
    suspend fun saveRoast(roast: RoastInsights)
    suspend fun clearRoast()
}
```

#### `domain/repository/FeaturedBadgesRepository.kt`
```kotlin
interface FeaturedBadgesRepository {
    suspend fun saveFeaturedBadges(userId: String, badges: List<Badge>): Result<Unit>
    fun getFeaturedBadges(userId: String): Flow<List<Badge>>
}
```

---

### 3. Domain Use Cases

#### `domain/usecase/ExtractRoastStatsUseCase.kt`
```kotlin
class ExtractRoastStatsUseCase {
    operator fun invoke(analyticsState: AnalyticsState): RoastStats {
        return RoastStats(
            hoursPlayed = analyticsState.hoursPlayed,
            totalGames = analyticsState.totalGames,
            completionRate = analyticsState.completionRate,
            completedGames = analyticsState.completedGames,
            droppedGames = analyticsState.droppedGames,
            topGenres = analyticsState.topGenres,
            gamingPersonality = analyticsState.gamingPersonality.title,
            averageRating = analyticsState.averageRating
        )
    }
}
```

---

### 4. Data Layer - Room Storage

#### `data/local/entity/RoastEntity.kt`
```kotlin
@Entity(tableName = "roast_table")
data class RoastEntity(
    @PrimaryKey val id: Int = 1,  // Single row pattern
    val headline: String,
    val couldHaveList: String,    // JSON string (manual conversion)
    val prediction: String,
    val wholesomeCloser: String,
    val roastTitle: String,
    val roastTitleEmoji: String,
    val generatedAt: Long
)

// Extension functions for conversion
fun RoastEntity.toRoastInsights(): RoastInsights { ... }
fun RoastInsights.toEntity(generatedAt: Long): RoastEntity { ... }
```

#### `data/local/converter/RoastTypeConverters.kt` ⚠️ UNUSED
```kotlin
class RoastTypeConverters {
    @TypeConverter
    fun fromStringList(list: List<String>): String = json.encodeToString(list)
    
    @TypeConverter
    fun toStringList(jsonString: String): List<String> = json.decodeFromString(jsonString)
}
```
**Note:** This class is registered but never used because RoastEntity stores couldHaveList as String with manual JSON conversion.

#### `data/local/dao/RoastDao.kt`
```kotlin
@Dao
interface RoastDao {
    @Query("SELECT * FROM roast_table WHERE id = 1")
    fun getLastRoast(): Flow<RoastEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRoast(roast: RoastEntity)
    
    @Query("DELETE FROM roast_table")
    suspend fun clearRoast()
}
```

---

### 5. Data Layer - Repository Implementations

#### `data/repository/RoastRepositoryImpl.kt`
```kotlin
class RoastRepositoryImpl(private val roastDao: RoastDao) : RoastRepository {
    override fun getLastRoast(): Flow<RoastInsights?> =
        roastDao.getLastRoast().map { it?.toRoastInsights() }
    
    override fun getLastRoastWithTimestamp(): Flow<RoastWithTimestamp?> =
        roastDao.getLastRoast().map { entity ->
            entity?.let { RoastWithTimestamp(it.toRoastInsights(), it.generatedAt) }
        }
    
    override suspend fun saveRoast(roast: RoastInsights) =
        roastDao.saveRoast(roast.toEntity())
    
    override suspend fun clearRoast() = roastDao.clearRoast()
}
```

#### `data/repository/FeaturedBadgesRepositoryImpl.kt`
```kotlin
class FeaturedBadgesRepositoryImpl : FeaturedBadgesRepository {
    private val firestore = Firebase.firestore
    private val usersCollection = firestore.collection("users")
    
    override suspend fun saveFeaturedBadges(userId: String, badges: List<Badge>): Result<Unit> {
        // Converts badges to maps and saves to Firestore
        // Enforces max 3 badges
    }
    
    override fun getFeaturedBadges(userId: String): Flow<List<Badge>> = callbackFlow {
        // Real-time listener on user document
        // Parses featuredBadges field
    }
}
```

---

### 6. Data Layer - Response Mappers

#### `data/mapper/RoastResponseMapper.kt`
```kotlin
object RoastResponseMapper {
    fun parseRoastResponse(response: String): Result<RoastInsights> {
        // Extracts sections using regex: ===SECTION_NAME===
        // Sections: HEADLINE, COULD_HAVE, PREDICTION, WHOLESOME, ROAST_TITLE, ROAST_EMOJI
        // Returns failure if required sections missing
    }
    
    private fun extractSection(text: String, section: String): String? {
        val pattern = "===${section}===\\s*([\\s\\S]*?)(?====|$)".toRegex()
        return pattern.find(text)?.groupValues?.get(1)?.trim()
    }
}
```

#### `data/mapper/BadgeResponseMapper.kt`
```kotlin
object BadgeResponseMapper {
    fun parseBadgesResponse(response: String): Result<List<Badge>> {
        // Cleans markdown code blocks
        // Parses JSON: { "badges": [{ "title", "emoji", "reason" }] }
    }
}

@Serializable
internal data class BadgesResponseDto(val badges: List<BadgeDto>)

@Serializable
internal data class BadgeDto(val title: String, val emoji: String, val reason: String)
```

---

### 7. Presentation Layer - ViewModel

#### `presentation/screens/roast/RoastViewModel.kt`
```kotlin
data class RoastScreenState(
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val roast: RoastInsights? = null,
    val generatedAt: Long? = null,
    val badges: List<Badge> = emptyList(),
    val selectedBadges: List<Badge> = emptyList(),
    val error: String? = null,
    val isRegenerateDialogVisible: Boolean = false,
    val targetUserId: String? = null,
    val hasInsufficientStats: Boolean = false,
    val isSavingBadges: Boolean = false,
    val badgesSaved: Boolean = false
) {
    companion object { const val MAX_FEATURED_BADGES = 3 }
}

class RoastViewModel(
    private val aiRepository: AIRepository,
    private val roastRepository: RoastRepository,
    private val gameListRepository: GameListRepository,
    private val calculateGamingStatsUseCase: CalculateGamingStatsUseCase,
    private val determineGamingPersonalityUseCase: DetermineGamingPersonalityUseCase,
    private val extractRoastStatsUseCase: ExtractRoastStatsUseCase,
    private val featuredBadgesRepository: FeaturedBadgesRepository,
    private val gamerRepository: GamerRepository,
    private val targetUserId: String? = null
) : BaseViewModel() {
    
    // Key methods:
    fun loadCachedRoast()      // Load from Room (self-roast only)
    fun generateRoast()        // Call AI, save to Room (self-roast only)
    fun regenerateRoast()      // Show confirmation dialog
    fun confirmRegenerate()    // Replace existing roast
    fun selectBadge(badge)     // Toggle badge selection (max 3)
    fun saveFeaturedBadges()   // Save to Firestore
    fun retry()                // Retry after error
}
```

---

### 8. Presentation Layer - UI Components

#### `presentation/screens/roast/RoastScreen.kt`
- Main screen composable
- States: Empty, Loading, Results, Error
- Regenerate confirmation dialog
- Integrates all sub-components

#### `presentation/screens/roast/components/RoastLoadingState.kt`
- Animated loading with rotating fun messages
- Messages like "Analyzing your gaming sins...", "Consulting the roast gods..."

#### `presentation/screens/roast/components/RoastResultCard.kt`
- Displays: Roast title badge, headline, timestamp
- "Could Have Done" section with numbered list
- "Your Gaming Future" prediction
- "But Seriously..." wholesome closer

#### `presentation/screens/roast/components/RoastErrorState.kt`
- Error types: NETWORK, INSUFFICIENT_STATS, AI_FAILURE, UNKNOWN
- Custom messages and emojis per error type
- Retry button

#### `presentation/screens/roast/components/BadgeSelector.kt`
- FlowRow of selectable badges
- Count indicator "X/3 selected"
- Save to Profile button
- Disabled for friend roasts

#### `presentation/screens/roast/util/RoastShareHelper.kt`
```kotlin
object RoastShareHelper {
    fun shareRoast(context: Context, roast: RoastInsights) {
        // Opens Android share sheet with formatted text
    }
    
    fun formatRoastForSharing(roast: RoastInsights): String {
        // Format: emoji + title, headline, wholesome closer, hashtags
    }
    
    fun formatRoastForTwitter(roast: RoastInsights): String {
        // Truncated version for 280 char limit
    }
}
```

#### `presentation/screens/analytics/components/RoastHeroCard.kt`
- Dark gradient (deep red to black)
- Fire emoji, "Get Roasted" title
- Dynamic subtext with actual hours played
- Clickable to navigate to RoastScreen

#### `presentation/screens/profile/components/BadgesSection.kt`
- Card with "FEATURED BADGES" header
- FlowRow of badges with emoji + title
- Hidden when no badges

---

## EXISTING FILES MODIFIED

### 1. `data/local/GameCacheDatabase.kt`

**Changes:**
```kotlin
// BEFORE
@Database(
    entities = [CachedGameEntity::class, AIRecommendationRemoteKey::class, RecommendationFeedbackEntity::class],
    version = 3,
    exportSchema = false
)
abstract class GameCacheDatabase : RoomDatabase() {
    abstract fun cachedGamesDao(): CachedGamesDao
    abstract fun recommendationFeedbackDao(): RecommendationFeedbackDao
}

// AFTER
@Database(
    entities = [
        CachedGameEntity::class,
        AIRecommendationRemoteKey::class,
        RecommendationFeedbackEntity::class,
        RoastEntity::class  // ADDED
    ],
    version = 4,  // BUMPED
    exportSchema = false
)
@TypeConverters(RoastTypeConverters::class)  // ADDED (but unused)
abstract class GameCacheDatabase : RoomDatabase() {
    abstract fun cachedGamesDao(): CachedGamesDao
    abstract fun recommendationFeedbackDao(): RecommendationFeedbackDao
    abstract fun roastDao(): RoastDao  // ADDED
}
```

---

### 2. `data/remote/GeminiPrompts.kt`

**Added two new functions:**

```kotlin
fun gamingRoastPrompt(stats: RoastStats): String {
    // Builds prompt with:
    // - Stats section (hours, games, completion rate, genres, personality, rating)
    // - Rules (be specific, be absurd, no personal attacks, end wholesome)
    // - Output format with section markers (===HEADLINE===, etc.)
}

fun badgeGenerationPrompt(stats: RoastStats): String {
    // Builds prompt with:
    // - Stats section
    // - Rules (unique, 2-4 word titles, funny reasons)
    // - JSON output format: { "badges": [...] }
}
```

---

### 3. `data/repository/BaseAIRepository.kt`

**Added two new method implementations (lines ~1340-1420):**

```kotlin
override suspend fun generateRoast(stats: RoastStats): Result<RoastInsights> {
    val prompt = GeminiPrompts.gamingRoastPrompt(stats)
    val response = aiClient.generateTextContent(prompt)
    return RoastResponseMapper.parseRoastResponse(response)
}

override suspend fun generateBadges(stats: RoastStats): Result<List<Badge>> {
    val prompt = GeminiPrompts.badgeGenerationPrompt(stats)
    val response = aiClient.generateJsonContent(prompt)
    return BadgeResponseMapper.parseBadgesResponse(response)
}
```

---

### 4. `domain/repository/AIRepository.kt`

**Added two new method signatures:**

```kotlin
interface AIRepository {
    // ... existing methods ...
    
    suspend fun generateRoast(stats: RoastStats): Result<RoastInsights>
    suspend fun generateBadges(stats: RoastStats): Result<List<Badge>>
}
```

---

### 5. `navigation/NavigationRoot.kt`

**Added:**

```kotlin
// New navigation key
@Serializable
data class RoastScreenKey(val targetUserId: String? = null) : NavKey

// In entryProvider when block:
is RoastScreenKey -> {
    NavEntry(key = key) {
        RoastScreen(
            targetUserId = key.targetUserId,
            onNavigateBack = { backStack.remove(key) }
        )
    }
}
```

---

### 6. `presentation/screens/analytics/AnalyticsScreen.kt`

**Added import and RoastHeroCard after GamingDNACard:**

```kotlin
import com.example.arcadia.presentation.screens.analytics.components.RoastHeroCard

// In AnalyticsContent composable, after GamingDNACard:
RoastHeroCard(
    hoursPlayed = state.hoursPlayed,
    onTap = onNavigateToRoast
)
```

**Added callback parameter:**
```kotlin
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSearch: (String) -> Unit,
    onNavigateToRoast: () -> Unit  // ADDED
)
```

---

### 7. `presentation/screens/profile/ProfileScreen.kt`

**Added roast friend button in top bar:**

```kotlin
// Added callback parameter
onNavigateToRoast: (targetUserId: String) -> Unit = {}

// In TopAppBar actions, added:
if (viewModel.shouldShowRoastButton()) {
    IconButton(
        onClick = { 
            profileState.id.takeIf { it.isNotEmpty() }?.let { targetId ->
                onNavigateToRoast(targetId)
            }
        }
    ) {
        Text("🔥", fontSize = 20.sp)
    }
}
```

---

### 8. `presentation/screens/profile/ProfileViewModel.kt`

**Added:**

```kotlin
// New dependency
private val featuredBadgesRepository: FeaturedBadgesRepository

// New state field
val featuredBadges: StateFlow<List<Badge>>

// New method
fun shouldShowRoastButton(): Boolean {
    val currentUserId = gamerRepository.getCurrentUserId()
    return profileState.value.id != currentUserId && 
           profileState.value.isPublic
}

// In init block, load featured badges:
viewModelScope.launch {
    featuredBadgesRepository.getFeaturedBadges(userId).collect { badges ->
        _featuredBadges.value = badges
    }
}
```

---

### 9. `di/RepositoryModule.kt`

**Added:**

```kotlin
// RoastDao singleton
single { get<GameCacheDatabase>().roastDao() }

// RoastRepository binding
single<RoastRepository> { RoastRepositoryImpl(get()) }

// FeaturedBadgesRepository binding
single<FeaturedBadgesRepository> { FeaturedBadgesRepositoryImpl() }
```

---

### 10. `di/UseCaseModule.kt`

**Added:**

```kotlin
factory { ExtractRoastStatsUseCase() }
```

---

### 11. `di/ViewModelModule.kt`

**Added:**

```kotlin
// RoastViewModel with optional targetUserId parameter
viewModel { (targetUserId: String?) -> 
    RoastViewModel(get(), get(), get(), get(), get(), get(), get(), get(), targetUserId) 
}

// Updated ProfileViewModel to include FeaturedBadgesRepository
viewModel { ProfileViewModel(get(), get(), get()) }  // Added 3rd param
```

---

## Known Issues & Technical Debt

### 1. Unused RoastTypeConverters (⚠️ Should be removed)

**File:** `data/local/converter/RoastTypeConverters.kt`

**Problem:** The TypeConverters class is registered with Room but never used because:
- `RoastEntity.couldHaveList` is stored as `String` (not `List<String>`)
- JSON conversion is done manually in `toEntity()` and `toRoastInsights()`

**Recommendation:** Delete `RoastTypeConverters.kt` and remove `@TypeConverters(RoastTypeConverters::class)` from `GameCacheDatabase.kt`

### 2. Database Migration Strategy

**Current:** Uses `fallbackToDestructiveMigration()` which wipes data on schema changes.

**Impact:** Users lose cached roasts when the app updates with schema changes.

**Recommendation:** For production, implement proper Room migrations.

### 3. Error Handling in toRoastInsights()

**File:** `data/local/entity/RoastEntity.kt`

**Issue:** Added fallback parsing for corrupted data (plain text instead of JSON array). This was a hotfix for a crash.

**Root Cause:** The `couldHaveList` field was sometimes stored as plain text instead of JSON array, likely from an earlier bug or manual database manipulation.

## Architecture Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                         User Interaction                         │
│  Analytics Screen → RoastHeroCard → tap → RoastScreen           │
│  Profile Screen → Roast Friend Button → RoastScreen(targetId)   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        RoastViewModel                            │
│  - loadCachedRoast() → RoastRepository                          │
│  - generateRoast() → AIRepository.generateRoast()               │
│  - generateBadges() → AIRepository.generateBadges()             │
│  - saveFeaturedBadges() → FeaturedBadgesRepository              │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ RoastRepository │ │  AIRepository   │ │FeaturedBadges   │
│ (Room local)    │ │ (Gemini/Groq)   │ │Repository       │
│                 │ │                 │ │(Firestore)      │
└─────────────────┘ └─────────────────┘ └─────────────────┘
```

## AI Prompt Structure

### Roast Prompt (GeminiPrompts.gamingRoastPrompt)
- Input: RoastStats (hours, games, completion rate, genres, personality, rating)
- Output sections: HEADLINE, COULD_HAVE, PREDICTION, WHOLESOME, ROAST_TITLE, ROAST_EMOJI
- Parsed by: RoastResponseMapper using regex section extraction

### Badge Prompt (GeminiPrompts.badgeGenerationPrompt)
- Input: RoastStats
- Output: JSON with `badges` array containing title, emoji, reason
- Parsed by: BadgeResponseMapper using kotlinx.serialization

## Testing Checklist

- [ ] Generate roast from Analytics screen
- [ ] View roast results with all sections
- [ ] Regenerate roast (confirmation dialog)
- [ ] Share roast via share sheet
- [ ] Select badges (max 3 enforcement)
- [ ] Save badges to profile
- [ ] View badges on profile
- [ ] Roast friend from their public profile
- [ ] Error states (network, insufficient stats)
- [ ] Offline cached roast display

## Cleanup Completed

✅ Removed unused `RoastTypeConverters.kt`
✅ Removed `@TypeConverters` annotation from `GameCacheDatabase.kt`
✅ Removed unused methods from `RoastViewModel`:
   - `isBadgeSelected()`
   - `getRoastTitleAsBadge()`
   - `clearBadgeSavedState()`
   - `clearError()`
✅ Removed unused methods from `RoastShareHelper`:
   - `formatRoastForTwitter()`
   - `createTruncatedFormat()`
   - Made `formatRoastForSharing()` private

## Remaining Tasks

1. Consider adding proper Room migrations for production
2. Add unit tests for RoastResponseMapper and BadgeResponseMapper
