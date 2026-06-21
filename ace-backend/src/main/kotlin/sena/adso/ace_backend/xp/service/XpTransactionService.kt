package sena.adso.ace_backend.xp.service

import com.ace.shared.dto.ExerciseBlockDto
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sena.adso.ace_backend.xp.model.XpTransaction
import sena.adso.ace_backend.xp.repository.XpTransactionRepository
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class XpTransactionService(
    private val xpTransactionRepository: XpTransactionRepository
) {

    /**
     * Registra una transacción XP append-only.
     * Retorna la transacción creada con el balance actualizado.
     */
    @Transactional
    fun recordXpTransaction(userId: UUID, block: ExerciseBlockDto): XpTransaction {
        val currentBalance = getCurrentBalance(userId)
        val newBalance = currentBalance + block.xpCalculated

        val transaction = XpTransaction(
            transactionId = UUID.randomUUID(),
            userId = userId,
            blockId = UUID.fromString(block.blockId),
            sessionId = UUID.fromString(block.sessionId),
            xpAmount = block.xpCalculated,
            balanceAfter = newBalance,
            sportType = block.sportType,
            durationSeconds = block.durationSeconds,
            avgBpm = block.avgBpm,
            createdAt = Instant.now()
        )

        xpTransactionRepository.save(transaction)

        logger.info {
            "XP transaction recorded: user=$userId, block=${block.blockId}, " +
            "xp=${block.xpCalculated}, balance=$newBalance"
        }

        return transaction
    }

    /**
     * Obtiene el balance actual de XP de un usuario.
     * Si no hay transacciones, retorna 0.
     */
    fun getCurrentBalance(userId: UUID): Int {
        val lastTransaction = xpTransactionRepository
            .findTopByUserIdOrderByCreatedAtDesc(userId)

        return lastTransaction?.balanceAfter ?: 0
    }
}