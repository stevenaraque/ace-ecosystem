package sena.adso.ace_backend.ranking.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import sena.adso.ace_backend.ranking.model.RankingGlobal
import java.util.UUID

@Repository
interface RankingGlobalRepository : JpaRepository<RankingGlobal, UUID> {

    fun findAllByOrderByPositionAsc(): List<RankingGlobal>

    fun findByUserId(userId: UUID): RankingGlobal?

    @Modifying
    @Query("DELETE FROM RankingGlobal")
    fun deleteAllEntries()
}