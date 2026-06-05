-- ============================================================
-- A.C.E Backend — V3: Tablas materializadas de ranking
-- ============================================================

-- Ranking Global (Apéndice S6)
CREATE TABLE ranking_global (
    position INT PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    total_xp BIGINT NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Ranking Municipal (Apéndice S6)
CREATE TABLE ranking_municipal (
    city_id VARCHAR(255) NOT NULL,
    position INT NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    total_xp BIGINT NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (city_id, position),
    UNIQUE (city_id, user_id)
);

CREATE INDEX idx_ranking_municipal_lookup ON ranking_municipal(city_id, position);
