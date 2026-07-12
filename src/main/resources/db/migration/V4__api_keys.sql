CREATE TABLE api_keys (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id      UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    key_hash    VARCHAR(64)  NOT NULL UNIQUE,  -- SHA-256 hex of the raw key
    key_hint    VARCHAR(12)  NOT NULL,          -- first chars shown in listings
    role        VARCHAR(50)  NOT NULL CHECK (role IN ('ADMIN', 'MEMBER')),
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_api_keys_org_id   ON api_keys (org_id);
CREATE INDEX idx_api_keys_key_hash ON api_keys (key_hash);
