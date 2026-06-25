package com.ace.mobile.feature.stats.domain

import com.ace.mobile.feature.stats.data.StatsRepository
import com.ace.shared.dto.StatsResponseDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetOfficialStatsUseCase @Inject constructor(
    private val statsRepository: StatsRepository
) {
    operator fun invoke(forceRefresh: Boolean = false): Flow<Result<StatsResponseDto>> {
        return statsRepository.getStats(forceRefresh)
    }
}