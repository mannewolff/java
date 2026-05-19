package org.mwolff.api.book;

public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(Long id) {
        super("Book not found: " + id);
    }
}
