-- ============================================================
-- A.C.E Backend — V10: Consolidación deuda técnica S3-S10
-- IDEMPOTENTE: puede ejecutarse múltiples veces sin error
-- ============================================================

-- ─── C3: Eliminar bonus_multiplier si aún existe ───
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'xp_formulas' AND column_name = 'bonus_multiplier'
    ) THEN
        ALTER TABLE xp_formulas DROP COLUMN bonus_multiplier;
    END IF;
END $$;

-- ─── C4: Asegurar balance_after sea BIGINT ───
DO $$
DECLARE
    current_type TEXT;
BEGIN
    SELECT data_type INTO current_type
    FROM information_schema.columns
    WHERE table_name = 'xp_transactions' AND column_name = 'balance_after';

    IF current_type IN ('integer', 'int', 'int4') THEN
        ALTER TABLE xp_transactions ALTER COLUMN balance_after TYPE BIGINT USING balance_after::BIGINT;
    END IF;
END $$;

-- ─── F5: Seed CYCLING y WALKING (idempotente) ───
INSERT INTO xp_formulas (formula_id, sport_type, min_bpm, xp_per_minute, max_xp_per_block, is_active, created_at)
VALUES
  (gen_random_uuid(), 'CYCLING',  90, 2, 30, true, now()),
  (gen_random_uuid(), 'WALKING',  70, 1, 20, true, now())
ON CONFLICT (sport_type) DO NOTHING;

-- ─── Asegurar RUNNING sigue activo ───
UPDATE xp_formulas SET is_active = true WHERE sport_type = 'RUNNING';