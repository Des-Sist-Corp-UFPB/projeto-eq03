-- Security schema: roles, permissions, users and join tables

CREATE TABLE tb_role (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE tb_permission (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    endpoint    VARCHAR(255) NOT NULL,
    http_method VARCHAR(10)  NOT NULL,
    classe      VARCHAR(100) NOT NULL
);

CREATE TABLE tb_role_permissions (
    role_id       BIGINT NOT NULL REFERENCES tb_role(id),
    permission_id BIGINT NOT NULL REFERENCES tb_permission(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE tb_user (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    phone      VARCHAR(20),
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    role_id    BIGINT       NOT NULL REFERENCES tb_role(id),
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
