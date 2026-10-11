-- V1 baseline schema.
--
-- Deliberately portable between PostgreSQL (runtime) and H2 in PostgreSQL mode
-- (integration tests) so the same migration runs in both. Keep DDL free of
-- engine-specific defaults (no gen_random_uuid(), no jsonb, no partial indexes).
-- UUIDs and timestamps are generated in application code.

CREATE TABLE rooms (
    id                   BIGSERIAL                PRIMARY KEY,
    code                 VARCHAR(8)               NOT NULL UNIQUE,
    status               VARCHAR(16)              NOT NULL,
    host_player_id       BIGINT,
    total_rounds         INTEGER                  NOT NULL DEFAULT 3,
    current_round_number INTEGER                  NOT NULL DEFAULT 0,
    current_drawer_id    BIGINT,
    current_word         VARCHAR(64),
    round_ends_at        TIMESTAMP WITH TIME ZONE,
    version              BIGINT                   NOT NULL DEFAULT 0,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE players (
    id           BIGSERIAL                PRIMARY KEY,
    room_id      BIGINT                   NOT NULL,
    name         VARCHAR(32)              NOT NULL,
    score        INTEGER                  NOT NULL DEFAULT 0,
    is_host      BOOLEAN                  NOT NULL DEFAULT FALSE,
    connected    BOOLEAN                  NOT NULL DEFAULT TRUE,
    token        VARCHAR(36)              NOT NULL UNIQUE,
    joined_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    last_seen_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_players_room FOREIGN KEY (room_id) REFERENCES rooms (id)
);

CREATE INDEX idx_players_room_id ON players (room_id);

CREATE TABLE rounds (
    id               BIGSERIAL                PRIMARY KEY,
    room_id          BIGINT                   NOT NULL,
    round_number     INTEGER                  NOT NULL,
    drawer_player_id BIGINT,
    word             VARCHAR(64)              NOT NULL,
    status           VARCHAR(16)              NOT NULL,
    started_at       TIMESTAMP WITH TIME ZONE,
    ended_at         TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_rounds_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT uq_rounds_room_number UNIQUE (room_id, round_number)
);

CREATE INDEX idx_rounds_room_id ON rounds (room_id);

CREATE TABLE drawing_points (
    id         BIGSERIAL                PRIMARY KEY,
    room_id    BIGINT                   NOT NULL,
    round_id   BIGINT                   NOT NULL,
    player_id  BIGINT                   NOT NULL,
    seq        BIGINT                   NOT NULL,
    x          REAL                     NOT NULL,
    y          REAL                     NOT NULL,
    color      VARCHAR(9)               NOT NULL,
    size       INTEGER                  NOT NULL,
    tool       VARCHAR(8)               NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_points_room  FOREIGN KEY (room_id)  REFERENCES rooms (id),
    CONSTRAINT fk_points_round FOREIGN KEY (round_id) REFERENCES rounds (id)
);

-- Primary read path: fetch points for the active round in draw order,
-- supporting the `?sinceSeq=N` incremental polling contract. (The explicit
-- index here is cheap insurance; V4 will measure whether it is even needed.)
CREATE INDEX idx_points_round_seq ON drawing_points (round_id, seq);

CREATE TABLE guesses (
    id         BIGSERIAL                PRIMARY KEY,
    room_id    BIGINT                   NOT NULL,
    round_id   BIGINT                   NOT NULL,
    player_id  BIGINT                   NOT NULL,
    text       VARCHAR(255)             NOT NULL,
    is_correct BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_guesses_room  FOREIGN KEY (room_id)  REFERENCES rooms (id),
    CONSTRAINT fk_guesses_round FOREIGN KEY (round_id) REFERENCES rounds (id)
);

CREATE INDEX idx_guesses_round_id ON guesses (round_id);
