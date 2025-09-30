-- Authentication support tables

-- External OAuth account links (tenant scoped)
CREATE TABLE external_account (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES company (id) ON DELETE CASCADE,
    user_id uuid NOT NULL REFERENCES company_user (id) ON DELETE CASCADE,
    provider text NOT NULL,
    provider_user_id text NOT NULL,
    email text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_user_id),
    UNIQUE (company_id, user_id, provider)
);

CREATE INDEX idx_external_account_company_user ON external_account (company_id, user_id);
CREATE INDEX idx_external_account_company_provider_user ON external_account (company_id, provider, provider_user_id);
CREATE INDEX idx_external_account_email ON external_account (company_id, lower(email));

ALTER TABLE external_account ENABLE ROW LEVEL SECURITY;
ALTER TABLE external_account FORCE ROW LEVEL SECURITY;
CREATE POLICY external_account_isolation ON external_account
    USING (company_id = app.require_company_id())
    WITH CHECK (company_id = app.require_company_id());

