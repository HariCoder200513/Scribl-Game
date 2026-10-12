-- V3: drawing actions.
--
-- V1 models drawing as an append-only stream of one row per stroke event.
-- A canvas clear must be part of that same seq-ordered stream so clients can
-- reproduce the image exactly by replaying events. Clears carry no geometry,
-- so coordinates/style become nullable and an `action` discriminator labels
-- each event (POINT | CLEAR).
--
-- No secondary index is added: the (round_id, seq) read path deliberately stays
-- unindexed as the V1 baseline for the V4 index experiment, and per-round seq
-- integrity is guaranteed by the application under the room row lock (the max+1
-- sequence is not exposed to the database). See V2__drop_initial_indexes.sql
-- and docs/architecture.md.

ALTER TABLE drawing_points ADD COLUMN action VARCHAR(8) NOT NULL DEFAULT 'POINT';

ALTER TABLE drawing_points ALTER COLUMN x DROP NOT NULL;
ALTER TABLE drawing_points ALTER COLUMN y DROP NOT NULL;
ALTER TABLE drawing_points ALTER COLUMN color DROP NOT NULL;
ALTER TABLE drawing_points ALTER COLUMN size DROP NOT NULL;
ALTER TABLE drawing_points ALTER COLUMN tool DROP NOT NULL;