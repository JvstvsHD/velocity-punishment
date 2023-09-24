CREATE TABLE IF NOT EXISTS velocity_punishment
(
    uuid          VARCHAR(36),
    name          VARCHAR(16),
    type          VARCHAR(1000),
    expiration    TIMESTAMP(6),
    reason        VARCHAR(1000),
    punishment_id VARCHAR(36)
);

CREATE TABLE IF NOT EXISTS velocity_punishment_whitelist
(
    uuid VARCHAR(36)
);