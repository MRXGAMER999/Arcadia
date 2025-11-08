# Implementation Plan

- [x] 1. Create ImageLoaderModule with memory and disk cache configuration


  - Create new file `app/src/main/java/com/example/arcadia/di/ImageLoaderModule.kt`
  - Define Koin module that provides singleton ImageLoader instance
  - Configure MemoryCache with 25% of heap memory using `maxSizePercent(0.25)`
  - Configure DiskCache with 2% of disk space, 10MB minimum, 250MB maximum
  - Set disk cache directory to `androidContext().cacheDir.resolve("image_cache")`
  - Inject existing OkHttpClient from networkModule using `get()`
  - Enable crossfade transitions with `crossfade(true)`
  - Enable cache header respect with `respectCacheHeaders(true)`
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 1.1, 1.2_

- [x] 2. Integrate ImageLoaderModule into Koin dependency injection


  - Open `app/src/main/java/com/example/arcadia/di/AppModule.kt`
  - Add `imageLoaderModule` to the `appModule` list after `networkModule`
  - Ensure proper module ordering (networkModule must be before imageLoaderModule)
  - _Requirements: 2.1, 2.5_

- [x] 3. Update GameCard.kt to use ImageRequest.Builder with cache keys


  - Open `app/src/main/java/com/example/arcadia/presentation/screens/home/components/GameCard.kt`
  - Import `coil3.request.ImageRequest` and `coil3.compose.LocalPlatformContext`
  - Update `SmallGameCard` function to get platform context using `LocalPlatformContext.current`
  - Replace `model = game.backgroundImage ?: ""` with `ImageRequest.Builder(context).data(game.backgroundImage ?: "").memoryCacheKey(game.backgroundImage).diskCacheKey(game.backgroundImage).build()`
  - Update `LargeGameCard` function with the same ImageRequest.Builder pattern
  - _Requirements: 3.1, 3.5, 4.1, 4.4_

- [x] 4. Update GameListItem.kt to use ImageRequest.Builder with cache keys


  - Open `app/src/main/java/com/example/arcadia/presentation/screens/home/components/GameListItem.kt`
  - Import `coil3.request.ImageRequest` and `coil3.compose.LocalPlatformContext`
  - Update `GameListItem` function to get platform context using `LocalPlatformContext.current`
  - Replace `model = game.backgroundImage ?: ""` with `ImageRequest.Builder(context).data(game.backgroundImage ?: "").memoryCacheKey(game.backgroundImage).diskCacheKey(game.backgroundImage).build()`
  - _Requirements: 3.3, 3.4, 4.2, 4.4, 4.5_

- [x] 5. Update MyGameCard.kt to use ImageRequest.Builder with cache keys


  - Open `app/src/main/java/com/example/arcadia/presentation/screens/myGames/components/MyGameCard.kt`
  - Import `coil3.request.ImageRequest` and `coil3.compose.LocalPlatformContext`
  - Update `MyGameCard` function to get platform context using `LocalPlatformContext.current`
  - Replace `model = game.backgroundImage ?: ""` with `ImageRequest.Builder(context).data(game.backgroundImage ?: "").memoryCacheKey(game.backgroundImage).diskCacheKey(game.backgroundImage).build()`
  - _Requirements: 3.5, 3.6, 4.3, 4.4, 4.5_

- [x] 6. Verify implementation and cache behavior



  - Build the project to ensure no compilation errors
  - Run the app and navigate to game list screens
  - Verify images load correctly with loading and error states
  - Navigate away and back to verify instant loading from cache
  - Check logcat for any Coil-related errors or warnings
  - _Requirements: 1.3, 1.4, 1.5_
