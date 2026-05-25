package org.mwolff.api.tools;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mwolff.api.common.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BackgroundRemovalController.class)
@Import(GlobalExceptionHandler.class)
class BackgroundRemovalControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private BackgroundRemovalService service;

  @Test
  void shouldReturnPngOnSuccess() throws Exception {
    // Given
    byte[] processed = "png-bytes".getBytes();
    given(service.removeBackground(any())).willReturn(processed);
    MockMultipartFile upload =
        new MockMultipartFile("file", "icon.png", MediaType.IMAGE_PNG_VALUE, "raw".getBytes());

    // When / Then
    mockMvc
        .perform(multipart("/api/tools/remove-background").file(upload))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=\"transparent.png\""))
        .andExpect(content().bytes(processed));
  }

  @Test
  void shouldReturn400WhenFileMissing() throws Exception {
    mockMvc.perform(multipart("/api/tools/remove-background")).andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn502WhenPythonServiceFails() throws Exception {
    // Given
    willThrow(new PythonToolsException("upstream down")).given(service).removeBackground(any());
    MockMultipartFile upload =
        new MockMultipartFile("file", "x.png", MediaType.IMAGE_PNG_VALUE, "raw".getBytes());

    // When / Then
    mockMvc
        .perform(multipart("/api/tools/remove-background").file(upload))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.status").value(502))
        .andExpect(jsonPath("$.message").value("upstream down"));
  }
}
