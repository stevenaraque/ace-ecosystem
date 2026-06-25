package com.ace.mobile.feature.history.domain

import com.ace.mobile.feature.history.data.HistoryRepository
import com.ace.shared.dto.SessionHistoryEntryDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSessionHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    operator fun invoke(limit: Int = 20, forceRefresh: Boolean = false): Flow<Result<List<SessionHistoryEntryDto>>> {
        return historyRepository.getHistory(limit, forceRefresh)
    }
}