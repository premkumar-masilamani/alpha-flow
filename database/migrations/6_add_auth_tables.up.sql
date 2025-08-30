-- Users
CREATE TABLE users (
    user_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_name VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    updated_by VARCHAR(50) DEFAULT 'admin',
    updated_at TIMESTAMP DEFAULT now() NOT NULL
);

-- Roles
CREATE TABLE roles (
    role_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_name VARCHAR(50) UNIQUE NOT NULL,
    updated_by VARCHAR(50) DEFAULT 'admin',
    updated_at TIMESTAMP DEFAULT now() NOT NULL
);

-- User Roles
CREATE TABLE user_roles (
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    updated_by VARCHAR(50) DEFAULT 'admin',
    updated_at TIMESTAMP DEFAULT now() NOT NULL
);

-- Sessions
CREATE TABLE sessions (
    session_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_token VARCHAR(255) UNIQUE NOT NULL,
    user_id INT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(50) DEFAULT 'admin',
    updated_at TIMESTAMP DEFAULT now() NOT NULL
);
