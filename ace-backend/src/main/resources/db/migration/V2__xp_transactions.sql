-- ============================================================
-- A.C.E Backend — V2: Transacciones XP (append-only)
-- ============================================================

CREATE TABLE xp_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    block_id UUID REFERENCES exercise_blocks(block_id) ON DELETE SET NULL,
    amount INT NOT NULL,                     -- positivo o negativo (corrección)
    balance_after BIGINT NOT NULL,             -- total acumulado tras esta tx
    reason VARCHAR(50) NOT NULL CHECK (reason IN ('BLOCK_VALIDATED', 'CORRECTION', 'MANUAL_ADJUST')),
    timestamp TIMESTAMPTZ NOT NULL,            -- momento del esfuerzo, no del procesamiento
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Índice para leer última transacción rápido (balance_after O(1))
CREATE INDEX idx_xp_user_created ON xp_transactions(user_id, created_at DESC);
