package sena.adso.ace_backend.xp.repository

import com.ace.shared.enums.SportType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import sena.adso.ace_backend.xp.model.XpFormula
import java.util.UUID

@Repository
interface XpFormulaRepository : JpaRepository<XpFormula, UUID> {
    fun findBySportTypeAndIsActiveTrue(sportType: SportType): XpFormula?
}