ALTER TABLE sessions DROP CONSTRAINT fk_sessions_users;
ALTER TABLE user_roles DROP CONSTRAINT fk_user_roles_roles;
ALTER TABLE user_roles DROP CONSTRAINT fk_user_roles_users;
ALTER TABLE user_roles DROP CONSTRAINT uq_user_id_role_id;
