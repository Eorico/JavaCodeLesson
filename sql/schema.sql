-- =====================================================================
-- DEIHS LIBRARY SYSTEM - DATABASE SCHEMA
--
-- Run this once as a MySQL administrator:
--
--   "C:\Program Files\MySQL\MySQL Server 9.4\bin\mysql.exe" -u root -p < sql\schema.sql
--
-- It creates the database, a dedicated application account and the three
-- tables. The first launch of the program fills the catalogue and creates
-- the librarian login, because those passwords have to be hashed by the
-- application rather than stored here in clear text.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS deihs_library
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Application account.
-- CHANGE THE PASSWORD BELOW, then put the same value in
-- config/app.properties as db.password.
-- ---------------------------------------------------------------------
CREATE USER IF NOT EXISTS 'deihs_app'@'localhost'
    IDENTIFIED BY 'change-this-password';

GRANT SELECT, INSERT, UPDATE, DELETE ON deihs_library.*
    TO 'deihs_app'@'localhost';

FLUSH PRIVILEGES;

USE deihs_library;

-- ---------------------------------------------------------------------
-- Accounts. A row is unverified until the member types the code that
-- was emailed to them, and sign-in is refused while verified = FALSE.
-- Only the salted hash of a password is ever stored.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    username      VARCHAR(32)  NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    email         VARCHAR(190) NOT NULL,
    password_hash VARCHAR(190) NOT NULL,
    role          ENUM('MEMBER', 'LIBRARIAN') NOT NULL DEFAULT 'MEMBER',
    verified      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (username),
    UNIQUE KEY uq_users_email (email)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Catalogue. A book is on loan exactly when borrowed_by is not null, so
-- the two columns can never disagree with each other.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS books (
    id          INT          NOT NULL AUTO_INCREMENT,
    title       VARCHAR(200) NOT NULL,
    author      VARCHAR(120) NOT NULL,
    category    VARCHAR(80)  NOT NULL,
    borrowed_by VARCHAR(32)  NULL,
    borrowed_on DATE         NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_books_title_author (title, author),
    KEY idx_books_borrower (borrowed_by),
    CONSTRAINT fk_books_borrower
        FOREIGN KEY (borrowed_by) REFERENCES users (username)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- One-time codes emailed for sign-up verification and password resets.
-- The code itself is hashed, expires, and is burned after a few wrong
-- guesses so it cannot be brute forced.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS verification_codes (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    username   VARCHAR(32) NOT NULL,
    purpose    ENUM('SIGN_UP', 'PASSWORD_RESET') NOT NULL,
    code_hash  VARCHAR(190) NOT NULL,
    expires_at DATETIME    NOT NULL,
    attempts   INT         NOT NULL DEFAULT 0,
    consumed   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_codes_lookup (username, purpose, consumed),
    CONSTRAINT fk_codes_user
        FOREIGN KEY (username) REFERENCES users (username)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;
