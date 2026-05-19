CREATE TABLE book (
    id     BIGINT       NOT NULL AUTO_INCREMENT,
    title  VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    isbn   VARCHAR(32),
    PRIMARY KEY (id),
    UNIQUE KEY uk_book_isbn (isbn)
);
