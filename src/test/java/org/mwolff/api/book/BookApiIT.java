package org.mwolff.api.book;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mwolff.api.AbstractIntegrationTest;
import org.mwolff.api.book.dto.BookRequest;
import org.mwolff.api.book.dto.BookResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookApiIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private BookRepository repository;

    @BeforeEach
    void cleanDb() {
        repository.deleteAll();
    }

    @Test
    void postThenGet_roundTripsBook() {
        BookRequest request = new BookRequest("Effective Java", "Bloch", "978-0134685991");

        ResponseEntity<BookResponse> created = rest.postForEntity("/api/books", request, BookResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        Long id = created.getBody().id();

        ResponseEntity<BookResponse> fetched = rest.getForEntity("/api/books/" + id, BookResponse.class);

        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody()).isNotNull();
        assertThat(fetched.getBody().title()).isEqualTo("Effective Java");
        assertThat(fetched.getBody().author()).isEqualTo("Bloch");
    }

    @Test
    void get_unknownId_returns404() {
        ResponseEntity<String> response = rest.getForEntity("/api/books/9999", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void post_invalidRequest_returns400() {
        BookRequest invalid = new BookRequest("", "Author", null);
        ResponseEntity<String> response = rest.postForEntity("/api/books", invalid, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void delete_removesBook() {
        Book stored = repository.save(new Book("Temp", "Author", "tmp-isbn"));

        rest.delete("/api/books/" + stored.getId());

        assertThat(repository.findById(stored.getId())).isEmpty();
    }
}
