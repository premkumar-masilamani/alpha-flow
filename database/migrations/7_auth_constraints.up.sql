-- Foreign Key Constraints
ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_users
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_roles
  FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE;

ALTER TABLE sessions ADD CONSTRAINT fk_sessions_users
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

-- Unique Constraints
ALTER TABLE user_roles ADD CONSTRAINT uq_user_id_role_id UNIQUE (user_id, role_id);
