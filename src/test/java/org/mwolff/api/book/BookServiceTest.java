package org.mwolff.api.book;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mwolff.api.book.dto.BookRequest;
import org.mwolff.api.book.dto.BookResponse;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository repository;

    @InjectMocks
    private BookService service;

    @Test
    void findAll_returnsMappedResponses() {
        Book book = bookWithId(1L, "Effective Java", "Bloch", "978-0134685991");
        when(repository.findAll()).thenReturn(List.of(book));

        List<BookResponse> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).title()).isEqualTo("Effective Java");
    }

    @Test
    void findById_existing_returnsResponse() {
        Book book = bookWithId(42L, "Clean Code", "Martin", "9780132350884");
        when(repository.findById(42L)).thenReturn(Optional.of(book));

        BookResponse result = service.findById(42L);

        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.author()).isEqualTo("Martin");
    }

    @Test
    void findById_missing_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_persistsAndReturnsResponse() {
        BookRequest request = new BookRequest("Refactoring", "Fowler", "9780201485677");
        Book persisted = bookWithId(7L, "Refactoring", "Fowler", "9780201485677");
        when(repository.save(any(Book.class))).thenReturn(persisted);

        BookResponse result = service.create(request);

        assertThat(result.id()).isEqualTo(7L);
        verify(repository, times(1)).save(any(Book.class));
    }

    @Test
    void update_existing_savesUpdatedBook() {
        Book existing = bookWithId(5L, "Old Title", "Old Author", "111");
        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Book.class))).thenReturn(existing);

        BookRequest request = new BookRequest("New Title", "New Author", "222");
        service.update(5L, request);

        assertThat(existing.getTitle()).isEqualTo("New Title");
        assertThat(existing.getAuthor()).isEqualTo("New Author");
        assertThat(existing.getIsbn()).isEqualTo("222");
        verify(repository).save(existing);
    }

    @Test
    void update_missing_throwsNotFound() {
        when(repository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(123L, new BookRequest("t", "a", null)))
                .isInstanceOf(BookNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void delete_existing_callsRepository() {
        when(repository.existsById(3L)).thenReturn(true);

        service.delete(3L);

        verify(repository).deleteById(3L);
    }

    @Test
    void delete_missing_throwsNotFound() {
        when(repository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(404L))
                .isInstanceOf(BookNotFoundException.class);
        verify(repository, never()).deleteById(any());
    }

    private static Book bookWithId(Long id, String title, String author, String isbn) {
        Book book = new Book(title, author, isbn);
        try {
            Field idField = Book.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(book, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return book;
    }
}
