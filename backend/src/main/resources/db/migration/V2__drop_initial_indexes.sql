-- V2: restore the deliberately naive baseline.
--
-- The four secondary indexes created in V1 are performance optimisations that
-- belong to a later, MEASURED milestone (V4). They were added during initial
-- scaffolding before the project had any instrumentation to justify them.
--
-- Dropping them now makes the V1 baseline genuinely index-free for the
-- query paths we care about, so V4 can:
--   1. measure query latency / IO / statement costs WITHOUT these indexes,
--   2. re-add them (identical DDL), and
--   3. measure the delta against data rown volumes from V2's load tests.
--
-- Primary keys and the unique constraints backing rooms.code / players.token
-- remain: those enforce data integrity, not query tuning, and are not part of
-- this experiment.

DROP INDEX IF EXISTS idx_players_room_id;
DROP INDEX IF EXISTS idx_rounds_room_id;
DROP INDEX IF EXISTS idx_points_round_seq;
DROP INDEX IF EXISTS idx_guesses_round_id;