CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_user') THEN
        CREATE ROLE app_user LOGIN PASSWORD 'app_password';
    END IF;
END;
$$;

GRANT ALL PRIVILEGES ON DATABASE teuportal TO app_user;
GRANT ALL PRIVILEGES ON SCHEMA public TO app_user;
