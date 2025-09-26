-- Enforce tenant context presence by requiring app.company_id to be set

CREATE OR REPLACE FUNCTION app.require_company_id()
RETURNS uuid
LANGUAGE plpgsql
AS $$
DECLARE
    tenant uuid;
BEGIN
    tenant := app.current_company_id();
    IF tenant IS NULL THEN
        RAISE EXCEPTION 'tenant context is required (app.company_id not set)' USING ERRCODE = 'P0001';
    END IF;
    RETURN tenant;
END;
$$;

ALTER POLICY company_isolation ON company
    USING (id = app.require_company_id())
    WITH CHECK (id = app.require_company_id());

ALTER POLICY company_user_isolation ON company_user
    USING (company_id = app.require_company_id())
    WITH CHECK (
        company_id = app.require_company_id()
        AND app.company_user_can_manage(company_id)
    );

ALTER POLICY company_user_self_update ON company_user
    USING (
        id = app.current_user_id()
        AND company_id = app.require_company_id()
    )
    WITH CHECK (
        id = app.current_user_id()
        AND company_id = app.require_company_id()
    );

ALTER POLICY client_isolation ON client
    USING (company_id = app.require_company_id())
    WITH CHECK (
        company_id = app.require_company_id()
        AND app.company_user_can_manage(company_id)
    );

ALTER POLICY client_user_isolation ON client_user
    USING (company_id = app.require_company_id())
    WITH CHECK (company_id = app.require_company_id());

ALTER POLICY folder_isolation ON folder
    USING (company_id = app.require_company_id())
    WITH CHECK (company_id = app.require_company_id());

ALTER POLICY file_isolation ON file
    USING (company_id = app.require_company_id())
    WITH CHECK (company_id = app.require_company_id());

ALTER POLICY file_share_isolation ON file_share
    USING (company_id = app.require_company_id())
    WITH CHECK (company_id = app.require_company_id());

ALTER POLICY audit_event_isolation ON audit_event
    USING (company_id = app.require_company_id())
    WITH CHECK (company_id = app.require_company_id());

ALTER POLICY settings_isolation ON settings
    USING (company_id = app.require_company_id())
    WITH CHECK (company_id = app.require_company_id());

CREATE OR REPLACE FUNCTION app.enforce_company_tenant()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    tenant uuid;
BEGIN
    tenant := app.require_company_id();

    IF TG_OP = 'INSERT' THEN
        IF NEW.company_id IS DISTINCT FROM tenant THEN
            RAISE EXCEPTION 'tenant mismatch for insert on %', TG_TABLE_NAME USING ERRCODE = '42501';
        END IF;
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        IF NEW.company_id IS DISTINCT FROM tenant OR OLD.company_id IS DISTINCT FROM tenant THEN
            RAISE EXCEPTION 'tenant mismatch for update on %', TG_TABLE_NAME USING ERRCODE = '42501';
        END IF;
        RETURN NEW;
    ELSE -- DELETE
        IF OLD.company_id IS DISTINCT FROM tenant THEN
            RAISE EXCEPTION 'tenant mismatch for delete on %', TG_TABLE_NAME USING ERRCODE = '42501';
        END IF;
        RETURN OLD;
    END IF;
END;
$$;

DROP TRIGGER IF EXISTS trg_company_user_enforce_tenant ON company_user;
CREATE TRIGGER trg_company_user_enforce_tenant
    BEFORE INSERT OR UPDATE OR DELETE ON company_user
    FOR EACH ROW EXECUTE FUNCTION app.enforce_company_tenant();

DROP TRIGGER IF EXISTS trg_client_enforce_tenant ON client;
CREATE TRIGGER trg_client_enforce_tenant
    BEFORE INSERT OR UPDATE OR DELETE ON client
    FOR EACH ROW EXECUTE FUNCTION app.enforce_company_tenant();

DROP TRIGGER IF EXISTS trg_client_user_enforce_tenant ON client_user;
CREATE TRIGGER trg_client_user_enforce_tenant
    BEFORE INSERT OR UPDATE OR DELETE ON client_user
    FOR EACH ROW EXECUTE FUNCTION app.enforce_company_tenant();

DROP TRIGGER IF EXISTS trg_folder_enforce_tenant ON folder;
CREATE TRIGGER trg_folder_enforce_tenant
    BEFORE INSERT OR UPDATE OR DELETE ON folder
    FOR EACH ROW EXECUTE FUNCTION app.enforce_company_tenant();

DROP TRIGGER IF EXISTS trg_file_enforce_tenant ON file;
CREATE TRIGGER trg_file_enforce_tenant
    BEFORE INSERT OR UPDATE OR DELETE ON file
    FOR EACH ROW EXECUTE FUNCTION app.enforce_company_tenant();

DROP TRIGGER IF EXISTS trg_file_share_enforce_tenant ON file_share;
CREATE TRIGGER trg_file_share_enforce_tenant
    BEFORE INSERT OR UPDATE OR DELETE ON file_share
    FOR EACH ROW EXECUTE FUNCTION app.enforce_company_tenant();

DROP TRIGGER IF EXISTS trg_audit_event_enforce_tenant ON audit_event;
CREATE TRIGGER trg_audit_event_enforce_tenant
    BEFORE INSERT OR UPDATE OR DELETE ON audit_event
    FOR EACH ROW EXECUTE FUNCTION app.enforce_company_tenant();

DROP TRIGGER IF EXISTS trg_settings_enforce_tenant ON settings;
CREATE TRIGGER trg_settings_enforce_tenant
    BEFORE INSERT OR UPDATE OR DELETE ON settings
    FOR EACH ROW EXECUTE FUNCTION app.enforce_company_tenant();
