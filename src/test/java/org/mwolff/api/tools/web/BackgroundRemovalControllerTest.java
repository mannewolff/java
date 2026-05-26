package org.mwolff.api.tools.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mwolff.api.tools.application.RemoveBackgroundUseCase;
import org.mwolff.api.tools.domain.InvalidUploadException;
import org.mwolff.api.tools.domain.PythonToolsException;
import org.mwolff.api.tools.domain.ToolImageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BackgroundRemovalController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ToolExceptionHandler.class)
class BackgroundRemovalControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private RemoveBackgroundUseCase useCase;

  @Test
  void shouldReturnPngOnSuccess() throws Exception {
    given(useCase.execute(any()))
        .willReturn(new ToolImageResult(new byte[] {1, 2}, MediaType.IMAGE_PNG_VALUE));
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});

    mockMvc
        .perform(multipart("/api/tools/remove-background").file(file))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=\"transparent.png\""));
  }

  @Test
  void shouldReturn400WhenInvalidUpload() throws Exception {
    willThrow(new InvalidUploadException("EMPTY_FILE", "empty")).given(useCase).execute(any());
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});

    mockMvc
        .perform(multipart("/api/tools/remove-background").file(file))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("EMPTY_FILE"));
  }

  @Test
  void shouldReturn502WhenPythonServiceFails() throws Exception {
    willThrow(new PythonToolsException("upstream")).given(useCase).execute(any());
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});

    mockMvc
        .perform(multipart("/api/tools/remove-background").file(file))
        .andExpect(status().isBadGateway());
  }
}
