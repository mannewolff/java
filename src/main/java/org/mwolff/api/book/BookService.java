package org.mwolff.api.book;

import org.mwolff.api.book.dto.BookRequest;
import org.mwolff.api.book.dto.BookResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BookService {

    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<BookResponse> findAll() {
        return repository.findAll().stream().map(BookResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BookResponse findById(Long id) {
        return repository.findById(id)
                .map(BookResponse::from)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    public BookResponse create(BookRequest request) {
        Book book = new Book(request.title(), request.author(), request.isbn());
        return BookResponse.from(repository.save(book));
    }

    public BookResponse update(Long id, BookRequest request) {
        Book book = repository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setIsbn(request.isbn());
        return BookResponse.from(repository.save(book));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new BookNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
