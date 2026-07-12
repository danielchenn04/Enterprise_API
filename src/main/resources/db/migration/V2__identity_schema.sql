CREATE TABLE organizations (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE memberships (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id     UUID        NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role       VARCHAR(50) NOT NULL CHECK (role IN ('ADMIN', 'MEMBER')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (org_id, user_id)
);

CREATE INDEX idx_memberships_user_id ON memberships (user_id);
CREATE INDEX idx_memberships_org_id  ON memberships (org_id);
