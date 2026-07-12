-- Clear dev data so we can add NOT NULL org_id column
TRUNCATE TABLE projects;

-- App-layer isolation: every project belongs to an org
ALTER TABLE projects
    ADD COLUMN org_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE;

CREATE INDEX idx_projects_org_id ON projects (org_id);

-- PostgreSQL Row-Level Security — defense-in-depth second layer
-- FORCE applies the policy even to the table owner (the app user)
ALTER TABLE projects ENABLE ROW LEVEL SECURITY;
ALTER TABLE projects FORCE ROW LEVEL SECURITY;

-- Policy reads app.current_org, which the app sets via SET LOCAL at the start of each transaction.
-- current_setting(..., true) returns NULL instead of raising an error when the var isn't set,
-- so unauthenticated connections simply see no rows.
CREATE POLICY tenant_isolation ON projects
    USING  (org_id::text = current_setting('app.current_org', true))
    WITH CHECK (org_id::text = current_setting('app.current_org', true));
