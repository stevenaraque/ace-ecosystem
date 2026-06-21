-- ============================================================
-- A.C.E Backend — V5: Alinear schema con entidades JPA actuales
-- ============================================================

-- 1. Agregar device_id a exercise_blocks
ALTER TABLE exercise_blocks
ADD COLUMN IF NOT EXISTS device_id VARCHAR(255) NOT NULL DEFAULT 'unknown';

-- 2. Agregar bonus_multiplier a xp_formulas
ALTER TABLE xp_formulas
ADD COLUMN IF NOT EXISTS bonus_multiplier DOUBLE PRECISION NOT NULL DEFAULT 1.0;

-- 3. Cambiar min_bpm de INT a DOUBLE PRECISION en xp_formulas
ALTER TABLE xp_formulas
ALTER COLUMN min_bpm TYPE DOUBLE PRECISION USING min_bpm::DOUBLE PRECISION;

-- 4. Agregar schema_version a exercise_blocks
ALTER TABLE exercise_blocks
ADD COLUMN IF NOT EXISTS schema_version INT NOT NULL DEFAULT 1;

-- 5. Recrear exercise_sessions para coincidir con ExerciseSession entity
-- Primero: backup datos si existen
CREATE TABLE IF NOT EXISTS exercise_sessions_backup AS SELECT * FROM exercise_sessions WHERE 1=0;

-- Dropear y recrear con estructura correcta
DROP TABLE IF EXISTS exercise_sessions CASCADE;

CREATE TABLE exercise_sessions (
    session_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id VARCHAR(255) NOT NULL,
    sport_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    timestamp_start TIMESTAMPTZ NOT NULL,
    timestamp_end TIMESTAMPTZ,
    total_blocks INT NOT NULL DEFAULT 0,
    total_xp INT NOT NULL DEFAULT 0,
    schema_version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sessions_user ON exercise_sessions(user_id, timestamp_start DESC);

-- 6. Recrear xp_transactions para coincidir con XpTransaction entity
DROP TABLE IF EXISTS xp_transactions CASCADE;

CREATE TABLE xp_transactions (
    transaction_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    block_id UUID NOT NULL,
    session_id UUID NOT NULL,
    xp_amount INT NOT NULL,
    balance_after INT NOT NULL,
    sport_type VARCHAR(50) NOT NULL,
    duration_seconds INT NOT NULL,
    avg_bpm DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_xp_user_created ON xp_transactions(user_id, created_at DESC);