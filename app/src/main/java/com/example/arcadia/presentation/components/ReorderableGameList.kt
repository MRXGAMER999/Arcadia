package com.example.arcadia.presentation.components

/**
 * This file previously contained custom drag-and-drop reordering logic.
 * 
 * The implementation has been replaced with the org.burnoutcrew.composereorderable library
 * which provides better performance and smoother animations.
 * 
 * See MyGamesScreen.kt for the new implementation using:
 * - rememberReorderableLazyListState for list view
 * - rememberReorderableLazyGridState for grid view
 * - ReorderableItem for individual items
 * - detectReorderAfterLongPress for drag gesture detection
 * 
 * This file is kept for backwards compatibility but can be safely deleted.
 */
