# Requirements Document

## Introduction

This feature implements a centralized Coil 3 ImageLoader configuration with memory and disk caching capabilities for the Arcadia Android application. The system will optimize image loading performance by caching game cover images and reducing redundant network requests. All existing image loading components will be updated to utilize explicit cache keys for consistent caching behavior.

## Glossary

- **ImageLoader**: The Coil 3 component responsible for loading, caching, and displaying images
- **MemoryCache**: In-memory storage for recently accessed images to enable instant retrieval
- **DiskCache**: Persistent storage on device for images to survive app restarts
- **ImageRequest**: A Coil 3 request object that specifies how an image should be loaded
- **CacheKey**: A unique identifier used to store and retrieve cached images
- **SubcomposeAsyncImage**: A Compose component that asynchronously loads and displays images
- **Application Context**: The global Android application context used for singleton initialization

## Requirements

### Requirement 1

**User Story:** As a user, I want game images to load quickly from cache when I revisit screens, so that I have a smooth browsing experience without waiting for images to reload.

#### Acceptance Criteria

1. WHEN the Application starts, THE ImageLoader SHALL initialize with memory cache configured to use 25% of available heap memory
2. WHEN the Application starts, THE ImageLoader SHALL initialize with disk cache configured to use 2% of available disk space with a minimum of 10MB and maximum of 250MB
3. WHEN an image is loaded for the first time, THE ImageLoader SHALL store the image in both memory cache and disk cache
4. WHEN an image is requested that exists in memory cache, THE ImageLoader SHALL retrieve the image from memory cache without network access
5. WHEN an image is requested that exists in disk cache but not memory cache, THE ImageLoader SHALL retrieve the image from disk cache without network access

### Requirement 2

**User Story:** As a developer, I want a centralized ImageLoader configuration, so that all image loading behavior is consistent across the application.

#### Acceptance Criteria

1. THE Application SHALL provide a singleton ImageLoader instance accessible throughout the application
2. THE ImageLoader configuration SHALL use OkHttp network client for image fetching
3. THE ImageLoader configuration SHALL enable crossfade transitions for smooth image appearance
4. THE ImageLoader configuration SHALL respect system-level cache headers from image servers
5. THE Application SHALL inject the ImageLoader instance through the dependency injection container

### Requirement 3

**User Story:** As a user, I want game images to be cached efficiently using their unique identifiers, so that the same game image is not downloaded multiple times.

#### Acceptance Criteria

1. WHEN GameCard component loads an image, THE component SHALL specify a memory cache key based on the game's background image URL
2. WHEN GameCard component loads an image, THE component SHALL specify a disk cache key based on the game's background image URL
3. WHEN GameListItem component loads an image, THE component SHALL specify a memory cache key based on the game's background image URL
4. WHEN GameListItem component loads an image, THE component SHALL specify a disk cache key based on the game's background image URL
5. WHEN MyGameCard component loads an image, THE component SHALL specify a memory cache key based on the game's background image URL
6. WHEN MyGameCard component loads an image, THE component SHALL specify a disk cache key based on the game's background image URL

### Requirement 4

**User Story:** As a developer, I want all SubcomposeAsyncImage calls to use ImageRequest.Builder, so that I have fine-grained control over caching behavior.

#### Acceptance Criteria

1. THE GameCard component SHALL replace direct model usage with ImageRequest.Builder for all SubcomposeAsyncImage calls
2. THE GameListItem component SHALL replace direct model usage with ImageRequest.Builder for all SubcomposeAsyncImage calls
3. THE MyGameCard component SHALL replace direct model usage with ImageRequest.Builder for all SubcomposeAsyncImage calls
4. WHEN an ImageRequest is built, THE request SHALL include the image URL as the data source
5. WHEN an ImageRequest is built, THE request SHALL include explicit memory and disk cache keys
