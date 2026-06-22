package com.ace.mobile.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ace.mobile.data.local.database.dao.UserDao
import com.ace.mobile.data.repository.RankingCacheRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userDao: UserDao,
    private val rankingCacheRepository: RankingCacheRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadHomeData() {
        viewModelScope.launch {
            userDao.observeCurrentUser().collect { user ->
                val hasTrained = hasTrainedToday(user)
                val rankingResult = rankingCacheRepository.getGlobalRanking(forceRefresh = false)
                val myPosition = rankingResult.getOrNull()?.myPosition ?: 0

                _uiState.value = _uiState.value.copy(
                    currentStreak = user?.currentStreak ?: 0,
                    bestStreak = user?.bestStreak ?: 0,
                    totalXp = user?.totalXp ?: 0L,
                    totalSessions = user?.totalSessions ?: 0,
                    globalPosition = myPosition,
                    hasTrainedToday = hasTrained,
                    isLoggedIn = user != null && !user.accessToken.isNullOrEmpty()
                )
            }
        }
    }

    fun refreshRanking() {
        viewModelScope.launch {
            val result = rankingCacheRepository.getGlobalRanking(forceRefresh = true)
            result.onSuccess { data ->
                _uiState.value = _uiState.value.copy(
                    globalPosition = data.myPosition,
                    totalXp = data.myTotalXp
                )
            }
        }
    }

    private fun hasTrainedToday(user: com.ace.mobile.data.local.database.entity.LocalUserEntity?): Boolean {
        if (user?.lastExerciseDate == null) return false
        val today = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
        val lastDay = Instant.ofEpochMilli(user.lastExerciseDate)
            .atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
        return today == lastDay
    }

    data class HomeUiState(
        val currentStreak: Int = 0,
        val bestStreak: Int = 0,
        val totalXp: Long = 0L,
        val totalSessions: Int = 0,
        val globalPosition: Int = 0,
        val hasTrainedToday: Boolean = false,
        val isLoggedIn: Boolean = false
    )
}