-- ============================================================
-- A.C.E Backend — V4: Seed data (rangos y fórmulas iniciales)
-- ============================================================

-- Seed: Rank Catalog (Apéndice S5)
INSERT INTO rank_catalog (rank_name, min_xp, max_xp) VALUES
    ('BRONZE', 0, 99),
    ('SILVER', 100, 249),
    ('GOLD', 250, 499),
    ('PLATINUM', 500, 999),
    ('DIAMOND', 1000, NULL);

-- Seed: XP Formula for RUNNING (Apéndice S5)
INSERT INTO xp_formulas (sport_type, min_bpm, xp_per_minute, max_xp_per_block, version) VALUES
    ('RUNNING', 80, 2, 30, 1);
