-- Historischer Stand. Tabelle gehoert zum entfernten Book-CRUD-Scaffold (#54)
-- und wird durch V3__cleanup_book_remnants.sql idempotent entfernt. Datei
-- bleibt im Repo, weil sie in flyway_schema_history der laufenden DB steht.
CREATE TABLE book (
    id     BIGINT       NOT NULL AUTO_INCREMENT,
    title  VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    isbn   VARCHAR(32),
    PRIMARY KEY (id),
    UNIQUE KEY uk_book_isbn (isbn)
);
