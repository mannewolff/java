package org.mwolff.api.book;

import org.junit.jupiter.api.Test;
import org.mwolff.api.AbstractIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private BookRepository repository;

    @Test
    void saveAndFindById_persistsBookWithGeneratedId() {
        Book book = new Book("Domain-Driven Design", "Evans", "9780321125217");

        Book saved = repository.save(book);

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).hasValueSatisfying(found -> {
            assertThat(found.getTitle()).isEqualTo("Domain-Driven Design");
            assertThat(found.getAuthor()).isEqualTo("Evans");
            assertThat(found.getIsbn()).isEqualTo("9780321125217");
        });
    }

    @Test
    void findAll_returnsAllPersistedBooks() {
        repository.save(new Book("A", "Author A", "isbn-a"));
        repository.save(new Book("B", "Author B", "isbn-b"));

        assertThat(repository.findAll()).extracting(Book::getTitle).contains("A", "B");
    }
}
