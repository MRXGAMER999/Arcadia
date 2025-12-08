package com.example.arcadia.presentation.screens.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.model.ProfileSection
import com.example.arcadia.domain.model.ProfileSectionType
import com.example.arcadia.domain.model.ai.Badge
import com.example.arcadia.domain.repository.FeaturedBadgesRepository
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.presentation.base.BaseViewModel
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import java.util.UUID

data class ProfileState(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val username: String = "",
    val country: String? = null,
    val city: String? = null,
    val gender: String? = null,
    val description: String? = null,
    val profileImageUrl: String? = null,
    val steamId: String? = null,
    val xboxGamertag: String? = null,
    val psnId: String? = null,
    val isProfilePublic: Boolean = true
)

data class ProfileStatsState(
    val totalGames: Int = 0,
    val finishedGames: Int = 0,
    val playingGames: Int = 0,
    val droppedGames: Int = 0,
    val wantToPlayGames: Int = 0,
    val onHoldGames: Int = 0,
    val hoursPlayed: Int = 0,
    val avgRating: Float = 0f,
    val completionRate: Float = 0f
)

class ProfileViewModel(
    private val gamerRepository: GamerRepository,
    private val gameListRepository: GameListRepository,
    private val featuredBadgesRepository: FeaturedBadgesRepository
) : BaseViewModel() {

    var screenReady: RequestState<Unit> by mutableStateOf(RequestState.Loading)
        private set

    var profileState: ProfileState by mutableStateOf(ProfileState())
        private set

    var statsState: ProfileStatsState by mutableStateOf(ProfileStatsState())
        private set

    var isCurrentUser: Boolean by mutableStateOf(true)
        private set

    private val _libraryGames = MutableStateFlow<List<GameListEntry>>(emptyList())
    val libraryGames: StateFlow<List<GameListEntry>> = _libraryGames.asStateFlow()

    private val _featuredBadges = MutableStateFlow<List<Badge>>(emptyList())
    val featuredBadges: StateFlow<List<Badge>> = _featuredBadges.asStateFlow()

    var customSections: List<ProfileSection> by mutableStateOf(emptyList())
        private set

    init {
        // Data loading is now triggered from UI via loadProfile(userId)
    }

    fun loadProfile(userId: String? = null) {
        screenReady = RequestState.Loading
        val currentUserId = gamerRepository.getCurrentUserId()
        isCurrentUser = userId == null || userId == currentUserId
        
        loadProfileData(userId)
        loadGamingStats(userId)
        loadFeaturedBadges(userId ?: currentUserId)
    }

    private fun loadProfileData(userId: String? = null) {
        launchWithKey("load_profile") {
            val flow = if (userId != null) {
                gamerRepository.getGamer(userId)
            } else {
                gamerRepository.readCustomerFlow()
            }

            flow.collectLatest { result ->
                if (result.isSuccess()) {
                    val gamer = result.getSuccessData()
                    profileState = ProfileState(
                        id = gamer.id,
                        name = gamer.name,
                        email = gamer.email,
                        username = gamer.username,
                        country = gamer.country,
                        city = gamer.city,
                        gender = gamer.gender,
                        description = gamer.description,
                        profileImageUrl = gamer.profileImageUrl,
                        steamId = gamer.steamId,
                        xboxGamertag = gamer.xboxGamertag,
                        psnId = gamer.psnId,
                        isProfilePublic = gamer.isProfilePublic
                    )
                    customSections = gamer.customSections
                    screenReady = RequestState.Success(Unit)
                } else if (result.isError()) {
                    screenReady = RequestState.Error(result.getErrorMessage())
                }
            }
        }
    }

    private fun loadGamingStats(userId: String? = null) {
        launchWithKey("load_stats") {
            val flow = if (userId != null) {
                gameListRepository.getGameListForUser(userId)
            } else {
                gameListRepository.getGameList()
            }

            flow.collectLatest { result ->
                if (result.isSuccess()) {
                    val games = result.getSuccessData()
                    
                    // Update library games for profile display
                    _libraryGames.value = games
                    
                    val totalGames = games.size
                    val finishedGames = games.count { it.status == GameStatus.FINISHED }
                    val playingGames = games.count { it.status == GameStatus.PLAYING }
                    val droppedGames = games.count { it.status == GameStatus.DROPPED }
                    val wantToPlayGames = games.count { it.status == GameStatus.WANT }
                    val onHoldGames = games.count { it.status == GameStatus.ON_HOLD }
                    val hoursPlayed = games.sumOf { it.hoursPlayed }
                    
                    val ratedGames = games.filter { it.rating != null && it.rating > 0 }
                    val avgRating = if (ratedGames.isNotEmpty()) {
                        ratedGames.mapNotNull { it.rating }.average().toFloat()
                    } else 0f
                    
                    val totalGamesForCompletion = totalGames - wantToPlayGames
                    val completionRate = if (totalGamesForCompletion > 0) {
                        (finishedGames.toFloat() / totalGamesForCompletion) * 100
                    } else 0f

                    statsState = ProfileStatsState(
                        totalGames = totalGames,
                        finishedGames = finishedGames,
                        playingGames = playingGames,
                        droppedGames = droppedGames,
                        wantToPlayGames = wantToPlayGames,
                        onHoldGames = onHoldGames,
                        hoursPlayed = hoursPlayed,
                        avgRating = avgRating,
                        completionRate = completionRate
                    )
                }
            }
        }
    }

    /**
     * Loads featured badges for the specified user.
     * Requirements: 9.1, 9.3
     */
    private fun loadFeaturedBadges(userId: String?) {
        if (userId == null) {
            _featuredBadges.value = emptyList()
            return
        }
        
        launchWithKey("load_badges") {
            featuredBadgesRepository.getFeaturedBadges(userId).collectLatest { badges ->
                _featuredBadges.value = badges
            }
        }
    }

    fun addCustomSection(title: String, type: ProfileSectionType, gameIds: List<Int>) {
        val newSection = ProfileSection(
            id = UUID.randomUUID().toString(),
            title = title,
            type = type,
            gameIds = gameIds,
            order = customSections.size
        )
        customSections = customSections + newSection
        saveCustomSections()
    }

    fun updateCustomSection(sectionId: String, title: String, type: ProfileSectionType, gameIds: List<Int>) {
        customSections = customSections.map { section ->
            if (section.id == sectionId) {
                section.copy(title = title, type = type, gameIds = gameIds)
            } else {
                section
            }
        }
        saveCustomSections()
    }

    fun deleteCustomSection(sectionId: String) {
        customSections = customSections.filter { it.id != sectionId }
        saveCustomSections()
    }

    private fun saveCustomSections() {
        launchWithKey("save_sections") {
            val result = gamerRepository.updateGamer(customSections = customSections)
            if (result.isError()) {
                // Reload profile to revert to server state on error
                loadProfileData(null)
            }
        }
    }

    fun refreshData(userId: String? = null) {
        screenReady = RequestState.Loading
        loadProfile(userId)
    }

    /**
     * Determines if the roast button should be visible.
     * The roast button is shown only when viewing a public profile of another user.
     * 
     * Requirements: 14.1, 14.2, 14.3
     */
    fun shouldShowRoastButton(): Boolean {
        // Don't show on own profile (Requirement 14.1)
        if (isCurrentUser) return false
        
        // Don't show on private profiles (Requirement 14.2)
        if (!profileState.isProfilePublic) return false
        
        // Show on public profiles of other users (Requirement 14.3)
        return true
    }
}
