package sena.adso.ace_backend.xp.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import sena.adso.ace_backend.xp.model.RankCatalog
import java.util.UUID

@Repository
interface RankCatalogRepository : JpaRepository<RankCatalog, UUID> {
    fun findAllByOrderByMinXpAsc(): List<RankCatalog>
}