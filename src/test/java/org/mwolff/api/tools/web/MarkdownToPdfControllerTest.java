package org.mwolff.api.tools.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mwolff.api.tools.application.MarkdownToPdfUseCase;
import org.mwolff.api.tools.domain.ToolImageResult;
import org.mwolff.api.tools.web.dto.MarkdownToPdfRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MarkdownToPdfController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ToolExceptionHandler.class)
class MarkdownToPdfControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private MarkdownToPdfUseCase useCase;

  @Test
  void shouldReturnPdfWithAttachmentHeader() throws Exception {
    given(useCase.execute(any()))
        .willReturn(new ToolImageResult(new byte[] {1, 2, 3}, MediaType.APPLICATION_PDF_VALUE));

    mockMvc
        .perform(
            post("/api/tools/md-to-pdf")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"markdown\":\"# Titel\"}"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF))
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"document.pdf\""));
  }

  @Test
  void shouldReturn400WhenMarkdownBlank() throws Exception {
    mockMvc
        .perform(
            post("/api/tools/md-to-pdf")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"markdown\":\"   \"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenMarkdownTooLong() throws Exception {
    final String tooLong = "x".repeat(MarkdownToPdfRequest.MAX_LENGTH + 1);
    mockMvc
        .perform(
            post("/api/tools/md-to-pdf")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"markdown\":\"" + tooLong + "\"}"))
        .andExpect(status().isBadRequest());
  }
}
