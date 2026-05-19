package org.mwolff.api.book.dto;

import org.mwolff.api.book.Book;

public record BookResponse(Long id, String title, String author, String isbn) {

    public static BookResponse from(Book book) {
        return new BookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn());
    }
}
