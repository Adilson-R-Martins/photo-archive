-- =============================================================================
-- Migration: V1 — Add per-photo visibility column
-- =============================================================================
-- Run this script against photoarchive_db BEFORE starting the application.
-- ddl-auto=update cannot safely add a NOT NULL column to a table that already
-- has rows, so this migration must be applied manually.
--
-- Strategy for existing rows:
--   Existing photos are set to 'PUBLIC' so that they retain the same access
--   behaviour they had under the previous global-toggle model (PRIVATE mode
--   required authentication; PUBLIC mode was open — PUBLIC is the safe middle
--   ground that preserves the authenticated-user experience).
--
--   All new uploads default to 'PRIVATE' at the application layer.
-- =============================================================================

ALTER TABLE photos
    ADD COLUMN visibility VARCHAR(10) NOT NULL DEFAULT 'PUBLIC';

-- Explicitly set every existing row to PUBLIC (matches the DEFAULT above,
-- added here for clarity and to make a future Flyway migration idempotent).
UPDATE photos SET visibility = 'PUBLIC' WHERE visibility IS NULL OR visibility = '';
