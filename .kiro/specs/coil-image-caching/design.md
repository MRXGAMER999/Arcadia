# Design Document: Coil 3 ImageLoader Configuration with Caching

## Overview

This design implements a centralized Coil 3 ImageLoader configuration with optimized memory and disk caching for the Arcadia Android application. The solution provides a singleton ImageLoader instance configured with appropriate cache sizes and integrates it into the existing Koin dependency injection system. All image loading components will be updated to use explicit cache keys through ImageRequest.Builder for consistent and efficient caching behavior.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      MyApplication                          │
│                    (Koin Initialization)                    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ initializes
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    ImageLoaderModule                        │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  ImageLoader (Singleton)                              │  │
│  │  ├─ MemoryCache (25% heap)                           │  │
│  │  ├─ DiskCache (2% disk, 10MB-250MB)                  │  │
│  │  ├─ OkHttpClient (network fetching)                  │  │
│  │  └─ Crossfade enabled                                │  │
│  └───────────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ provides ImageLoader to
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              UI Components (Composables)                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  GameCard    │  │ GameListItem │  │ MyGameCard   │      │
│  │              │  │              │  │              │      │
│  │ ImageRequest │  │ ImageRequest │  │ ImageRequest │      │
│  │ + cacheKeys  │  │ + cacheKeys  │  │ + cacheKeys  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### Component Interaction Flow

1. **Application Startup**: MyApplication initializes Koin with imageLoaderModule
2. **ImageLoader Creation**: Koin creates singleton ImageLoader with configured caches
3. **Image Request**: UI component creates ImageRequest with explicit cache keys
4. **Cache Lookup**: ImageLoader checks memory cache → disk cache → network
5. **Cache Storage**: Loaded images are stored in both memory and disk caches
6. **Image Display**: SubcomposeAsyncImage displays the cached/loaded image

## Components and Interfaces

### 1. ImageLoaderModule (New)

**Location**: `app/src/main/java/com/example/arcadia/di/ImageLoaderModule.kt`

**Purpose**: Provides a singleton ImageLoader instance configured with memory and disk caching.

**Configuration**:
```kotlin
val imageLoaderModule = module {
    single {
        ImageLoader.Builder(androidContext())
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(androidContext(), 0.25) // 25% of heap
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(androidContext().cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // 2% of disk
                    .minimumMaxSizeBytes(10 * 1024 * 1024) // 10MB minimum
                    .maximumMaxSizeBytes(250 * 1024 * 1024) // 250MB maximum
                    .build()
            }
            .okHttpClient(get()) // Reuse existing OkHttpClient from networkModule
            .crossfade(true)
            .respectCacheHeaders(true)
            .build()
    }
}
```

**Dependencies**:
- Requires `OkHttpClient` from existing `networkModule`
- Requires Android `Context` from Koin's `androidContext()`

### 2. AppModule Updates

**Location**: `app/src/main/java/com/example/arcadia/di/AppModule.kt`

**Changes**: Add `imageLoaderModule` to the module list

```kotlin
val appModule = listOf(
    viewModelModule,
    repositoryModule,
    networkModule,
    imageLoaderModule, // NEW
)
```

### 3. UI Component Updates

All three components will be updated to use `ImageRequest.Builder` instead of passing the URL directly to `SubcomposeAsyncImage`.

#### GameCard.kt Updates

**Location**: `app/src/main/java/com/example/arcadia/presentation/screens/home/components/GameCard.kt`

**Changes**:
- Import `ImageRequest` and `LocalPlatformContext`
- Replace `model = game.backgroundImage ?: ""` with ImageRequest.Builder
- Add `memoryCacheKey` and `diskCacheKey` based on image URL

**Pattern**:
```kotlin
val context = LocalPlatformContext.current
SubcomposeAsyncImage(
    model = ImageRequest.Builder(context)
        .data(game.backgroundImage ?: "")
        .memoryCacheKey(game.backgroundImage)
        .diskCacheKey(game.backgroundImage)
        .build(),
    // ... rest of parameters
)
```

**Affected Functions**:
- `SmallGameCard`: 1 SubcomposeAsyncImage call
- `LargeGameCard`: 1 SubcomposeAsyncImage call

#### GameListItem.kt Updates

**Location**: `app/src/main/java/com/example/arcadia/presentation/screens/home/components/GameListItem.kt`

**Changes**: Same pattern as GameCard.kt

**Affected Functions**:
- `GameListItem`: 1 SubcomposeAsyncImage call

#### MyGameCard.kt Updates

**Location**: `app/src/main/java/com/example/arcadia/presentation/screens/myGames/components/MyGameCard.kt`

**Changes**: Same pattern as GameCard.kt

**Affected Functions**:
- `MyGameCard`: 1 SubcomposeAsyncImage call

## Data Models

No new data models are required. The existing `Game` and `UserGame` models already contain the `backgroundImage` field which will be used as the cache key.

### Cache Key Strategy

**Cache Key Format**: The image URL itself serves as the cache key

**Rationale**:
- URLs are unique identifiers for images
- Simple and straightforward implementation
- No additional processing required
- Consistent across memory and disk caches

**Example**:
```
URL: "https://media.rawg.io/media/games/456/456dea5e1c7e3cd07060c14e96612001.jpg"
memoryCacheKey: "https://media.rawg.io/media/games/456/456dea5e1c7e3cd07060c14e96612001.jpg"
diskCacheKey: "https://media.rawg.io/media/games/456/456dea5e1c7e3cd07060c14e96612001.jpg"
```

## Error Handling

### Cache Initialization Errors

**Scenario**: Insufficient disk space or permissions issues

**Handling**:
- Coil will gracefully degrade to memory-only caching
- Network requests will still function normally
- No app crashes or user-facing errors

### Image Loading Errors

**Scenario**: Network failure, invalid URL, or corrupted cache

**Handling**:
- Existing error composables in each component will display fallback UI
- Error state shows game controller emoji (🎮)
- Cache entries for failed loads are not stored

### Cache Eviction

**Scenario**: Cache size limits are reached

**Handling**:
- Coil automatically evicts least recently used (LRU) entries
- Memory cache eviction happens automatically based on heap pressure
- Disk cache eviction happens when size limits are exceeded

## Testing Strategy

### Unit Tests (Optional)

**ImageLoaderModule Tests**:
- Verify ImageLoader singleton is created correctly
- Verify memory cache configuration (25% heap)
- Verify disk cache configuration (2% disk, 10MB-250MB bounds)
- Verify OkHttpClient is injected from networkModule

**Test Location**: `app/src/test/java/com/example/arcadia/di/ImageLoaderModuleTest.kt`

### Integration Tests (Optional)

**Cache Behavior Tests**:
- Verify images are cached after first load
- Verify cached images are retrieved without network calls
- Verify cache keys are correctly applied

**Test Location**: `app/src/androidTest/java/com/example/arcadia/ImageCachingTest.kt`

### Manual Testing

**Test Scenarios**:
1. **First Load**: Open app, navigate to game list, verify images load from network
2. **Cache Hit**: Navigate away and back, verify images load instantly from cache
3. **App Restart**: Close and reopen app, verify images load from disk cache
4. **Offline Mode**: Disable network, verify previously viewed images still display
5. **Memory Pressure**: Open many games, verify older images are evicted gracefully

**Test Devices**:
- Test on devices with varying memory (2GB, 4GB, 8GB RAM)
- Test on devices with limited storage space

## Performance Considerations

### Memory Cache Sizing

**Configuration**: 25% of available heap memory

**Rationale**:
- Balances image caching with app memory needs
- Prevents OutOfMemoryError on low-end devices
- Allows caching of ~20-50 game images depending on device

### Disk Cache Sizing

**Configuration**: 2% of available disk space (10MB-250MB)

**Rationale**:
- Minimum 10MB ensures at least 50-100 images can be cached
- Maximum 250MB prevents excessive storage usage
- 2% is reasonable for most devices (500MB on 25GB storage)

### Network Optimization

**OkHttpClient Reuse**:
- Shares connection pool with existing Retrofit client
- Reduces memory overhead
- Maintains consistent network configuration

**Crossfade Animation**:
- Smooth visual transition when images load
- 300ms default duration (Coil default)
- Minimal performance impact

## Implementation Notes

### Coil 3 API Changes

Coil 3 introduces several API changes from Coil 2:
- `coil3.compose.SubcomposeAsyncImage` (package change)
- `coil3.request.ImageRequest` (package change)
- `coil3.PlatformContext` instead of Android Context in Compose

### Dependency Injection

The ImageLoader will be available for injection in any component that needs it:
```kotlin
@Composable
fun MyComponent(imageLoader: ImageLoader = get()) {
    // Use imageLoader if needed
}
```

However, for SubcomposeAsyncImage, Coil automatically uses the singleton ImageLoader from the context, so explicit injection is not required in most cases.

### Migration Path

This is a non-breaking change:
- Existing SubcomposeAsyncImage calls will continue to work
- Updated calls with ImageRequest.Builder provide better caching
- No changes required to data layer or repositories

## Security Considerations

### Cache Security

**Disk Cache Location**: `app/src/main/java/com/example/arcadia/cache/image_cache`
- Stored in app's private cache directory
- Not accessible to other apps
- Automatically cleared when app is uninstalled

**Network Security**:
- Respects existing network security config
- Uses HTTPS for image URLs from RAWG API
- No sensitive data is cached (only public game images)

### Privacy

**User Data**: No user-specific data is cached
**Analytics**: No tracking of cache hits/misses
**Permissions**: No additional permissions required

## Future Enhancements

### Potential Improvements

1. **Preloading**: Preload images for upcoming games in lists
2. **Cache Metrics**: Add analytics for cache hit rates
3. **Custom Cache Keys**: Use game IDs instead of URLs for more stable keys
4. **Image Transformations**: Add blur or placeholder transformations
5. **Cache Management UI**: Allow users to clear image cache manually

### Scalability

The current design scales well:
- LRU eviction handles growing cache sizes
- Percentage-based sizing adapts to device capabilities
- Singleton pattern ensures single cache instance
