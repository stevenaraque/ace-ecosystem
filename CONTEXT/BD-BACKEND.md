-- ============================================================
-- A.C.E Backend — Script completo para Supabase
-- Ejecutar en: Supabase Dashboard → SQL Editor → New Query
-- ============================================================
--
-- ⚠️ NOTA (2026-06-25): Este script es una REFERENCIA TEÓRICA del esquema.
-- La FUENTE DE VERDAD del esquema en producción son las migraciones Flyway en
-- `ace-backend/src/main/resources/db/migration/` (V1..V6, V10).
-- Divergencias conocidas entre este script y las migraciones reales:
--   • `xp_transactions.amount` aquí → la migración/entity real usa la columna `xp_amount`
--     (mapeo `@Column(name="xp_amount")` en `XpTransaction.kt`). Ver fix C4 en DEUDA_TECNICA_v2.md.
--   • Este script no incluye las correcciones de V5/V6/V10 (drop `bonus_multiplier`,
--     PKs de ranking, seed CYCLING/WALKING, `balance_after` BIGINT).
-- Para crear la BD, preferir `flyway migrate` sobre este script.
-- ============================================================

-- 0. Extensión UUID (requerida por Supabase)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- 1. USERS (Apéndice S4)
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- 2. REFRESH TOKENS (Apéndice S4)
-- ============================================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash VARCHAR(255) UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by UUID REFERENCES refresh_tokens(id),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id, device_id);

-- ============================================================
-- 3. USER PROFILES (Apéndice S10)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    username VARCHAR(255),
    nickname VARCHAR(255),
    city_id VARCHAR(255),
    weight_kg DECIMAL(5,2),
    birth_date DATE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- 4. USER STATS (Apéndice S10)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_stats (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    total_xp BIGINT DEFAULT 0,
    total_sessions INT DEFAULT 0,
    total_blocks INT DEFAULT 0,
    total_duration_seconds BIGINT DEFAULT 0,
    avg_bpm_all_time DOUBLE PRECISION DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- 5. USER STREAKS (Apéndice S7)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_streaks (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    current_streak INT DEFAULT 0,
    best_streak INT DEFAULT 0,
    last_exercise_date DATE
);

-- ============================================================
-- 6. EXERCISE SESSIONS (Apéndice S2/S3)
-- ============================================================
CREATE TABLE IF NOT EXISTS exercise_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'ABORTED')),
    sport_type VARCHAR(50) NOT NULL,
    timestamp_start TIMESTAMPTZ NOT NULL,
    timestamp_end TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sessions_user ON exercise_sessions(user_id, timestamp_start DESC);

-- ============================================================
-- 7. EXERCISE BLOCKS (Apéndice S2/S3)
-- ============================================================
CREATE TABLE IF NOT EXISTS exercise_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES exercise_sessions(id) ON DELETE CASCADE,
    block_id UUID UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    timestamp_start TIMESTAMPTZ NOT NULL,
    timestamp_end TIMESTAMPTZ NOT NULL,
    duration_seconds INT NOT NULL,
    avg_bpm DOUBLE PRECISION NOT NULL,
    max_bpm DOUBLE PRECISION NOT NULL,
    min_bpm DOUBLE PRECISION NOT NULL,
    sample_count INT NOT NULL,
    sport_type VARCHAR(50) NOT NULL,
    xp_calculated INT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_blocks_user_time ON exercise_blocks(user_id, timestamp_start DESC);
CREATE INDEX IF NOT EXISTS idx_blocks_session ON exercise_blocks(session_id);

-- ============================================================
-- 8. XP FORMULAS (Apéndice S5)
-- ============================================================
CREATE TABLE IF NOT EXISTS xp_formulas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sport_type VARCHAR(50) UNIQUE NOT NULL,
    min_bpm INT NOT NULL,
    xp_per_minute INT NOT NULL,
    max_xp_per_block INT NOT NULL,
    version INT DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- 9. RANK CATALOG (Apéndice S5)
-- ============================================================
CREATE TABLE IF NOT EXISTS rank_catalog (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rank_name VARCHAR(50) NOT NULL,
    min_xp INT NOT NULL,
    max_xp INT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- 10. XP TRANSACTIONS (Apéndice S5) — Append-only
-- ============================================================
CREATE TABLE IF NOT EXISTS xp_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    block_id UUID REFERENCES exercise_blocks(block_id) ON DELETE SET NULL,
    amount INT NOT NULL,
    balance_after BIGINT NOT NULL,
    reason VARCHAR(50) NOT NULL CHECK (reason IN ('BLOCK_VALIDATED', 'CORRECTION', 'MANUAL_ADJUST')),
    timestamp TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_xp_user_created ON xp_transactions(user_id, created_at DESC);

-- ============================================================
-- 11. RANKING GLOBAL (Apéndice S6)
-- ============================================================
CREATE TABLE IF NOT EXISTS ranking_global (
    position INT PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    total_xp BIGINT NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- 12. RANKING MUNICIPAL (Apéndice S6)
-- ============================================================
CREATE TABLE IF NOT EXISTS ranking_municipal (
    city_id VARCHAR(255) NOT NULL,
    position INT NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    total_xp BIGINT NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (city_id, position),
    UNIQUE (city_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_ranking_municipal_lookup ON ranking_municipal(city_id, position);

-- ============================================================
-- 13. TABLAS DE AUDITORÍA (vacías en MVP)
-- ============================================================
CREATE TABLE IF NOT EXISTS suspicion_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    block_id UUID REFERENCES exercise_blocks(id) ON DELETE SET NULL,
    reason VARCHAR(255) NOT NULL,
    confidence_score DECIMAL(3,2),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS session_gps_points (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES exercise_sessions(id) ON DELETE CASCADE,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    accuracy_meters DECIMAL(6,2),
    timestamp TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_gps_session ON session_gps_points(session_id, timestamp);

-- ============================================================
-- 14. SEED DATA (datos iniciales)
-- ============================================================

-- Rangos
INSERT INTO rank_catalog (rank_name, min_xp, max_xp) VALUES
    ('BRONZE', 0, 99),
    ('SILVER', 100, 249),
    ('GOLD', 250, 499),
    ('PLATINUM', 500, 999),
    ('DIAMOND', 1000, NULL)
ON CONFLICT DO NOTHING;

-- Fórmula RUNNING
INSERT INTO xp_formulas (sport_type, min_bpm, xp_per_minute, max_xp_per_block, version) VALUES
    ('RUNNING', 80, 2, 30, 1)
ON CONFLICT DO NOTHING;

-- ============================================================
-- VERIFICACIÓN: Lista todas las tablas creadas
-- ============================================================
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_type = 'BASE TABLE'
ORDER BY table_name;