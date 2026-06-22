package sena.adso.ace_backend.ranking.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import sena.adso.ace_backend.ranking.model.RankingMunicipal
import java.util.UUID

@Repository
interface RankingMunicipalRepository : JpaRepository<RankingMunicipal, UUID> {

    fun findByCityIdOrderByPositionAsc(cityId: String): List<RankingMunicipal>

    fun findByUserIdAndCityId(userId: UUID, cityId: String): RankingMunicipal?

    @Modifying
    @Query("DELETE FROM RankingMunicipal")
    fun deleteAllEntries()
}