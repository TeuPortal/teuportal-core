-- Enable row level security and policies for tenant scoped tables

-- Only owners/admins can manage tenant-wide data
CREATE OR REPLACE FUNCTION app.company_user_can_manage(target_company uuid)
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    actor uuid;
BEGIN
    actor := app.current_user_id();
    IF actor IS NULL THEN
        RETURN FALSE;
    END IF;
    RETURN EXISTS (
        SELECT 1
        FROM company_user cu
        WHERE cu.id = actor
          AND cu.company_id = target_company
          AND cu.role IN ('OWNER', 'ADMIN')
    );
END;
$$;

-- Hard fail if app.company_id isn’t set before any tenant query
CREATE OR REPLACE FUNCTION app.require_company_id()
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    tenant_id uuid;
BEGIN
    tenant_id := app.current_company_id();
    IF tenant_id IS NULL THEN
        RAISE EXCEPTION 'app.company_id must be set for this transaction';
    END IF;
    RETURN tenant_id;
END;
$$;

ALTER TABLE company ENABLE ROW LEVEL SECURITY;
ALTER TABLE company FORCE ROW LEVEL SECURITY;
-- Every tenant table policy must check the required company id helper
CREATE POLICY company_isolation ON company
    USING (id = app.require_company_id())
    WITH CHECK (id = app.require_company_id());

ALTER TABLE company_user ENABLE ROW LEVEL SECURITY;
ALTER TABLE company_user FORCE ROW LEVEL SECURITY;
CREATE POLICY company_user_isolation ON company_user
    USING (company_id = app.require_company_id())
    WITH CHECK (
        company_id = app.require_company_id()
        AND app.company_user_can_manage(company_id)
    );
CREATE POLICY company_user_self_update ON company_user
    FOR UPDATE USING (
        id = app.current_user_id()
        AND company_id = app.require_company_id()
    )
    WITH CHECK (
        id = app.current_user_id()
        AND company_id = app.require_company_id()
    );

ALTER TABLE client ENABLE ROW LEVEL SECURITY;
ALTER TABLE client FORCE ROW LEVEL SECURITY;
CREATE POLICY client_isolation ON client
    USING (company_id = app.require_company_id())
    WITH CHECK (
        company_id = app.require_company_id()
        AND app.company_user_can_manage(company_id)
    );

ALTER TABLE client_user ENABLE ROW LEVEL SECURITY;
ALTER TABLE client_user FORCE ROW LEVEL SECURITY;
CREATE POLICY client_user_isolation ON client_user
    USING (company_id = app.require_company_id())
    WITH CHECK (company_id = app.require_company_id());

ALTER TABLE folder ENABLE ROW LEVEL SECURITY;
ALTER TABLE folder FORCE ROW LEVEL SECURITY;
CREATE POLICY folder_isolation ON folder
    USING (company_id = app.require_company_id())
    WITH CHECK (company_id = app.require_company_id());

CREATE POLICY file_isolation ON file
    USING (company_id = app.require_company_id())
    WITH CHECK (company_id = app.require_company_id());

CREATE POLICY file_share_isolation ON file_share
    USING (company_id = app.require_company_id())
    WITH CHECK (company_id = app.require_company_id());

CREATE POLICY audit_event_isolation ON audit_event
    USING (company_id = app.require_company_id())
    WITH CHECK (company_id = app.require_company_id());

CREATE POLICY settings_isolation ON settings
    USING (company_id = app.require_company_id())
    WITH CHECK (company_id = app.require_company_id());

-- Prevent PUBLIC from bypassing the policies
REVOKE ALL ON company, company_user, client, client_user, folder, file, file_share, audit_event, settings FROM PUBLIC;
