-- ============================================================
-- A.C.E Backend — V1: Esquema base (Auth, Exercise, Streak, Formulas)
-- ============================================================

-- Extensión UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. USERS (Apéndice S4)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. REFRESH TOKENS (Apéndice S4) — Stateful, rotación, revocación
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash VARCHAR(255) UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by UUID REFERENCES refresh_tokens(id),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id, device_id);

-- 3. USER PROFILES (Apéndice S10)
CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    username VARCHAR(255),
    city_id VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. USER STATS (Apéndice S10) — Totales oficiales
CREATE TABLE user_stats (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    total_xp BIGINT DEFAULT 0,
    total_sessions INT DEFAULT 0,
    total_blocks INT DEFAULT 0,
    total_duration_seconds BIGINT DEFAULT 0,
    avg_bpm_all_time DOUBLE PRECISION DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. USER STREAKS (Apéndice S7)
CREATE TABLE user_streaks (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    current_streak INT DEFAULT 0,
    best_streak INT DEFAULT 0,
    last_exercise_date DATE
);

-- 6. EXERCISE SESSIONS (Apéndice S2/S3)
CREATE TABLE exercise_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL CHECK (status IN ('ACTIVE', 'COMPLETED', 'ABORTED')),
    sport_type VARCHAR(50) NOT NULL,
    timestamp_start TIMESTAMPTZ NOT NULL,
    timestamp_end TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_sessions_user ON exercise_sessions(user_id, timestamp_start DESC);

-- 7. EXERCISE BLOCKS (Apéndice S2/S3) — Idempotencia por block_id (UUID móvil)
CREATE TABLE exercise_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES exercise_sessions(id) ON DELETE CASCADE,
    block_id UUID UNIQUE NOT NULL,           -- UUID generado por la APK
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    timestamp_start TIMESTAMPTZ NOT NULL,    -- Fuente de verdad temporal
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

CREATE INDEX idx_blocks_user_time ON exercise_blocks(user_id, timestamp_start DESC);
CREATE INDEX idx_blocks_session ON exercise_blocks(session_id);

-- 8. XP FORMULAS (Apéndice S5) — Cache de reglas de negocio
CREATE TABLE xp_formulas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sport_type VARCHAR(50) UNIQUE NOT NULL,
    min_bpm INT NOT NULL,
    xp_per_minute INT NOT NULL,
    max_xp_per_block INT NOT NULL,
    version INT DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 9. RANK CATALOG (Apéndice S5) — Umbrales de rangos
CREATE TABLE rank_catalog (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rank_name VARCHAR(50) NOT NULL,
    min_xp INT NOT NULL,
    max_xp INT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);