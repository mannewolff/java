package org.mwolff.api.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mwolff.api.book.dto.BookRequest;
import org.mwolff.api.book.dto.BookResponse;
import org.mwolff.api.common.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookController.class)
@Import(GlobalExceptionHandler.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookService service;

    @Test
    void list_returnsBooks() throws Exception {
        when(service.findAll()).thenReturn(List.of(new BookResponse(1L, "Effective Java", "Bloch", "978-0134685991")));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Effective Java"));
    }

    @Test
    void get_existing_returns200() throws Exception {
        when(service.findById(1L)).thenReturn(new BookResponse(1L, "Refactoring", "Fowler", "9780201485677"));

        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author").value("Fowler"));
    }

    @Test
    void get_missing_returns404() throws Exception {
        when(service.findById(99L)).thenThrow(new BookNotFoundException(99L));

        mockMvc.perform(get("/api/books/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void create_validRequest_returns201WithLocation() throws Exception {
        BookRequest request = new BookRequest("Refactoring", "Fowler", "9780201485677");
        when(service.create(any(BookRequest.class)))
                .thenReturn(new BookResponse(10L, "Refactoring", "Fowler", "9780201485677"));

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/books/10")))
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void create_blankTitle_returns400() throws Exception {
        BookRequest invalid = new BookRequest("", "Fowler", "9780201485677");

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void update_validRequest_returns200() throws Exception {
        BookRequest request = new BookRequest("New", "Author", "1");
        when(service.update(eq(5L), any(BookRequest.class)))
                .thenReturn(new BookResponse(5L, "New", "Author", "1"));

        mockMvc.perform(put("/api/books/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New"));
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/books/5"))
                .andExpect(status().isNoContent());
        verify(service).delete(5L);
    }
}
