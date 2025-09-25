-- Baseline tenant schema and core structures
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SCHEMA IF NOT EXISTS app;

CREATE OR REPLACE FUNCTION app.current_company_id()
RETURNS uuid
LANGUAGE plpgsql
AS $$
DECLARE
    company_setting text;
BEGIN
    company_setting := current_setting('app.company_id', true);
    IF company_setting IS NULL OR company_setting = '' THEN
        RETURN NULL;
    END IF;
    RETURN company_setting::uuid;
END;
$$;

CREATE OR REPLACE FUNCTION app.current_user_id()
RETURNS uuid
LANGUAGE plpgsql
AS $$
DECLARE
    user_setting text;
BEGIN
    user_setting := current_setting('app.user_id', true);
    IF user_setting IS NULL OR user_setting = '' THEN
        RETURN NULL;
    END IF;
    RETURN user_setting::uuid;
END;
$$;

CREATE TABLE company (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name text NOT NULL,
    slug text NOT NULL,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (slug)
);

CREATE TABLE company_user (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES company (id) ON DELETE CASCADE,
    email text NOT NULL,
    display_name text NOT NULL,
    role text NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    invited_at timestamptz,
    joined_at timestamptz,
    last_sign_in_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_company_user_company_email ON company_user (company_id, lower(email));
CREATE INDEX idx_company_user_company_role ON company_user (company_id, role);

CREATE TABLE client (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES company (id) ON DELETE CASCADE,
    name text NOT NULL,
    description text,
    contact_email text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_client_company_name ON client (company_id, lower(name));
CREATE INDEX idx_client_company_name ON client (company_id, name);

CREATE TABLE client_user (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES company (id) ON DELETE CASCADE,
    client_id uuid NOT NULL REFERENCES client (id) ON DELETE CASCADE,
    email text NOT NULL,
    display_name text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_client_user_company_client_email ON client_user (company_id, client_id, lower(email));
CREATE INDEX idx_client_user_company_client ON client_user (company_id, client_id);

CREATE TABLE folder (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES company (id) ON DELETE CASCADE,
    name text NOT NULL,
    parent_id uuid REFERENCES folder (id) ON DELETE SET NULL,
    created_by uuid REFERENCES company_user (id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK (parent_id IS NULL OR parent_id <> id)
);
CREATE INDEX idx_folder_company_parent ON folder (company_id, parent_id);
CREATE UNIQUE INDEX ux_folder_company_parent_name ON folder (company_id, COALESCE(parent_id, '00000000-0000-0000-0000-000000000000'::uuid), lower(name));

CREATE TABLE file (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES company (id) ON DELETE CASCADE,
    client_id uuid REFERENCES client (id) ON DELETE SET NULL,
    folder_id uuid REFERENCES folder (id) ON DELETE SET NULL,
    name text NOT NULL,
    size_bytes bigint NOT NULL CHECK (size_bytes >= 0),
    mime_type text NOT NULL,
    checksum text NOT NULL,
    storage_key text NOT NULL,
    uploaded_by uuid REFERENCES company_user (id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_file_company_storage ON file (company_id, storage_key);
CREATE INDEX idx_file_company_folder ON file (company_id, folder_id);
CREATE INDEX idx_file_company_client ON file (company_id, client_id);

CREATE TABLE file_share (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES company (id) ON DELETE CASCADE,
    file_id uuid NOT NULL REFERENCES file (id) ON DELETE CASCADE,
    token text NOT NULL,
    expires_at timestamptz,
    passcode_hash text,
    created_by uuid REFERENCES company_user (id),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_file_share_company_token ON file_share (company_id, token);
CREATE INDEX idx_file_share_company_file ON file_share (company_id, file_id);
CREATE INDEX idx_file_share_company_expires ON file_share (company_id, expires_at);

CREATE TABLE audit_event (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES company (id) ON DELETE CASCADE,
    type text NOT NULL,
    actor_company_user_id uuid REFERENCES company_user (id),
    actor_client_user_id uuid REFERENCES client_user (id),
    ip inet,
    user_agent text,
    meta jsonb NOT NULL DEFAULT '{}'::jsonb,
    occurred_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_event_company_time ON audit_event (company_id, occurred_at DESC);
CREATE INDEX idx_audit_event_company_type ON audit_event (company_id, type);

CREATE TABLE settings (
    company_id uuid PRIMARY KEY REFERENCES company (id) ON DELETE CASCADE,
    configured boolean NOT NULL DEFAULT false,
    preferences jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE login_token (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nonce text NOT NULL,
    email text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    used_at timestamptz,
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    UNIQUE (nonce)
);
CREATE INDEX idx_login_token_email ON login_token (lower(email));
CREATE INDEX idx_login_token_expires ON login_token (expires_at);

-- Spring Session tables (global scope)
CREATE TABLE spring_session (
    primary_id char(36) NOT NULL,
    session_id char(36) NOT NULL,
    creation_time bigint NOT NULL,
    last_access_time bigint NOT NULL,
    max_inactive_interval int NOT NULL,
    expiry_time bigint NOT NULL,
    principal_name varchar(100),
    CONSTRAINT spring_session_pk PRIMARY KEY (primary_id)
);
CREATE UNIQUE INDEX spring_session_ix1 ON spring_session (session_id);
CREATE INDEX spring_session_ix2 ON spring_session (expiry_time);
CREATE INDEX spring_session_ix3 ON spring_session (principal_name);

CREATE TABLE spring_session_attributes (
    session_primary_id char(36) NOT NULL,
    attribute_name varchar(200) NOT NULL,
    attribute_bytes bytea NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id) REFERENCES spring_session (primary_id) ON DELETE CASCADE
);



CREATE OR REPLACE FUNCTION app.first_company_id()
RETURNS uuid
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT id FROM company ORDER BY created_at ASC LIMIT 1;
$$;

-- Trigger helpers to keep updated_at fresh
CREATE OR REPLACE FUNCTION app.touch_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_company_set_updated
    BEFORE UPDATE ON company
    FOR EACH ROW EXECUTE FUNCTION app.touch_updated_at();

CREATE TRIGGER trg_company_user_set_updated
    BEFORE UPDATE ON company_user
    FOR EACH ROW EXECUTE FUNCTION app.touch_updated_at();

CREATE TRIGGER trg_client_set_updated
    BEFORE UPDATE ON client
    FOR EACH ROW EXECUTE FUNCTION app.touch_updated_at();

CREATE TRIGGER trg_client_user_set_updated
    BEFORE UPDATE ON client_user
    FOR EACH ROW EXECUTE FUNCTION app.touch_updated_at();

CREATE TRIGGER trg_folder_set_updated
    BEFORE UPDATE ON folder
    FOR EACH ROW EXECUTE FUNCTION app.touch_updated_at();

CREATE TRIGGER trg_file_set_updated
    BEFORE UPDATE ON file
    FOR EACH ROW EXECUTE FUNCTION app.touch_updated_at();

CREATE TRIGGER trg_settings_set_updated
    BEFORE UPDATE ON settings
    FOR EACH ROW EXECUTE FUNCTION app.touch_updated_at();












