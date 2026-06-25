package com.ace.mobile.feature.stats.domain

import com.ace.mobile.feature.stats.data.StatsRepository
import com.ace.shared.dto.ClientStatsDto
import com.ace.shared.dto.StatsReconcileResponseDto
import javax.inject.Inject

class ReconcileStatsUseCase @Inject constructor(
    private val statsRepository: StatsRepository
) {
    suspend operator fun invoke(clientStats: ClientStatsDto): Result<StatsReconcileResponseDto> {
        return statsRepository.reconcile(clientStats)
    }
}