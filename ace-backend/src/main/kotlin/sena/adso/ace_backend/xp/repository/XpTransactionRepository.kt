package sena.adso.ace_backend.xp.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import sena.adso.ace_backend.xp.model.XpTransaction
import java.util.UUID

@Repository
interface XpTransactionRepository : JpaRepository<XpTransaction, UUID> {
    fun findTopByUserIdOrderByCreatedAtDesc(userId: UUID): XpTransaction?
}