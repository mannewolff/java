package org.mwolff.api.tools;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mwolff.api.common.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaletteController.class)
@Import(GlobalExceptionHandler.class)
class PaletteControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PaletteService service;
  @MockitoBean private UploadValidator uploadValidator;

  @Test
  void shouldReturnJsonPalette() throws Exception {
    // Given
    given(service.extractPalette(any(), anyInt()))
        .willReturn(new PaletteResponse(List.of("#aabbcc", "#001122")));
    final MockMultipartFile upload =
        new MockMultipartFile("file", "photo.png", MediaType.IMAGE_PNG_VALUE, "raw".getBytes());

    // When / Then
    mockMvc
        .perform(multipart("/api/tools/palette").file(upload))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.colors[0]").value("#aabbcc"))
        .andExpect(jsonPath("$.colors[1]").value("#001122"));
  }

  @Test
  void shouldReturn400WhenFileMissing() throws Exception {
    mockMvc.perform(multipart("/api/tools/palette")).andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenCountTooLow() throws Exception {
    final MockMultipartFile upload =
        new MockMultipartFile("file", "photo.png", MediaType.IMAGE_PNG_VALUE, "raw".getBytes());
    mockMvc
        .perform(multipart("/api/tools/palette").file(upload).param("count", "1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenCountTooHigh() throws Exception {
    final MockMultipartFile upload =
        new MockMultipartFile("file", "photo.png", MediaType.IMAGE_PNG_VALUE, "raw".getBytes());
    mockMvc
        .perform(multipart("/api/tools/palette").file(upload).param("count", "11"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn502WhenPythonServiceFails() throws Exception {
    // Given
    willThrow(new PythonToolsException("upstream down"))
        .given(service)
        .extractPalette(any(), anyInt());
    final MockMultipartFile upload =
        new MockMultipartFile("file", "photo.png", MediaType.IMAGE_PNG_VALUE, "raw".getBytes());

    // When / Then
    mockMvc
        .perform(multipart("/api/tools/palette").file(upload))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Tool-Service derzeit nicht erreichbar."));
  }
}
