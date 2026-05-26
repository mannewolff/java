package org.mwolff.api.tools.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mwolff.api.tools.application.ExtractPaletteUseCase;
import org.mwolff.api.tools.domain.InvalidUploadException;
import org.mwolff.api.tools.domain.PaletteResult;
import org.mwolff.api.tools.domain.PythonToolsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaletteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ToolExceptionHandler.class)
class PaletteControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ExtractPaletteUseCase useCase;

  @Test
  void shouldReturnJsonPalette() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new PaletteResult(List.of("#abc123", "#def456")));
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});

    mockMvc
        .perform(multipart("/api/tools/palette").file(file).param("count", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.colors[0]").value("#abc123"))
        .andExpect(jsonPath("$.colors[1]").value("#def456"));
  }

  @Test
  void shouldReturn400WhenFileMissing() throws Exception {
    mockMvc
        .perform(multipart("/api/tools/palette").param("count", "5"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenCountTooLow() throws Exception {
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});
    mockMvc
        .perform(multipart("/api/tools/palette").file(file).param("count", "1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenCountTooHigh() throws Exception {
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});
    mockMvc
        .perform(multipart("/api/tools/palette").file(file).param("count", "20"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenInvalidUpload() throws Exception {
    willThrow(new InvalidUploadException("EMPTY_FILE", "empty"))
        .given(useCase)
        .execute(any(), any());
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});

    mockMvc
        .perform(multipart("/api/tools/palette").file(file).param("count", "5"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("EMPTY_FILE"));
  }

  @Test
  void shouldReturn502WhenPythonServiceFails() throws Exception {
    willThrow(new PythonToolsException("upstream")).given(useCase).execute(any(), any());
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});

    mockMvc
        .perform(multipart("/api/tools/palette").file(file).param("count", "5"))
        .andExpect(status().isBadGateway());
  }
}
