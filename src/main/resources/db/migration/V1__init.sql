-- Clients (no authentication in this assignment: the caller supplies the client id).
CREATE TABLE clients (
    id         BIGSERIAL PRIMARY KEY,
    client_id  VARCHAR(50)  NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- One balance row per client per currency. Money is NUMERIC, never float.
CREATE TABLE balances (
    id         BIGSERIAL PRIMARY KEY,
    client_id  VARCHAR(50)  NOT NULL REFERENCES clients (client_id),
    currency   VARCHAR(3)      NOT NULL,
    amount     NUMERIC(24,2) NOT NULL DEFAULT 0 CHECK (amount >= 0),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_balance_client_currency UNIQUE (client_id, currency)
);

-- A successful conversion persists here atomically with the balance changes.
CREATE TABLE conversion_records (
    id               BIGSERIAL PRIMARY KEY,
    transaction_id   UUID         NOT NULL UNIQUE,
    client_id        VARCHAR(50)  NOT NULL REFERENCES clients (client_id),
    source_currency  VARCHAR(3)      NOT NULL,
    target_currency  VARCHAR(3)      NOT NULL,
    source_amount    NUMERIC(24,2) NOT NULL,
    target_amount    NUMERIC(24,2) NOT NULL,
    rate             NUMERIC(24,10) NOT NULL,
    idempotency_key  VARCHAR(128),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- NULL keys remain distinct in Postgres, so idempotency stays optional.
    CONSTRAINT uq_conversion_client_idempotency UNIQUE (client_id, idempotency_key)
);

CREATE INDEX idx_conversion_client     ON conversion_records (client_id);
CREATE INDEX idx_conversion_created_at ON conversion_records (created_at);
