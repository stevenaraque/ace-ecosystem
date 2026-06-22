-- ============================================================
-- A.C.E Backend — V6: Hacer position actualizable en ranking
-- NOTA: Esta migración se aplicó MANUALMENTE en Supabase.
-- En local/test (H2), Flyway la ejecuta normalmente.
-- ============================================================

-- 1. RANKING_GLOBAL
ALTER TABLE ranking_global DROP CONSTRAINT IF EXISTS ranking_global_pkey;
ALTER TABLE ranking_global ADD PRIMARY KEY (user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ranking_global_position ON ranking_global(position);

-- 2. RANKING_MUNICIPAL
ALTER TABLE ranking_municipal DROP CONSTRAINT IF EXISTS ranking_municipal_pkey;
ALTER TABLE ranking_municipal DROP CONSTRAINT IF EXISTS ranking_municipal_city_id_user_id_key;
ALTER TABLE ranking_municipal ADD PRIMARY KEY (city_id, user_id);
CREATE INDEX IF NOT EXISTS idx_ranking_municipal_position ON ranking_municipal(city_id, position);

-- 3. Campos nuevos
ALTER TABLE ranking_global ADD COLUMN IF NOT EXISTS username VARCHAR(255);
ALTER TABLE ranking_global ADD COLUMN IF NOT EXISTS rank_name VARCHAR(100);
ALTER TABLE ranking_municipal ADD COLUMN IF NOT EXISTS username VARCHAR(255);
ALTER TABLE ranking_municipal ADD COLUMN IF NOT EXISTS rank_name VARCHAR(100);

-- 4. Defaults (solo para H2, en Supabase ya se hizo manual)
UPDATE ranking_global SET username = 'Usuario ' || CAST(user_id AS VARCHAR) WHERE username IS NULL;
UPDATE ranking_global SET rank_name = 'BRONZE' WHERE rank_name IS NULL;
UPDATE ranking_municipal SET username = 'Usuario ' || CAST(user_id AS VARCHAR) WHERE username IS NULL;
UPDATE ranking_municipal SET rank_name = 'BRONZE' WHERE rank_name IS NULL;

-- 5. NOT NULL
ALTER TABLE ranking_global ALTER COLUMN username SET NOT NULL;
ALTER TABLE ranking_global ALTER COLUMN rank_name SET NOT NULL;
ALTER TABLE ranking_municipal ALTER COLUMN username SET NOT NULL;
ALTER TABLE ranking_municipal ALTER COLUMN rank_name SET NOT NULL;