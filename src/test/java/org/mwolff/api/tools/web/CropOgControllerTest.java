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
import org.mwolff.api.tools.application.CropOgImageUseCase;
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

@WebMvcTest(CropOgController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ToolExceptionHandler.class)
class CropOgControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CropOgImageUseCase useCase;

  @Test
  void shouldReturnJpegOnSuccess() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1, 2}, MediaType.IMAGE_JPEG_VALUE));
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});

    mockMvc
        .perform(multipart("/api/tools/crop-og").file(file))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_JPEG))
        .andExpect(
            header()
                .string("Content-Disposition", "attachment; filename=\"featured-1200x630.jpg\""));
  }

  @Test
  void shouldUseCustomDimensionsInDownloadFilename() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1}, MediaType.IMAGE_JPEG_VALUE));
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});

    mockMvc
        .perform(
            multipart("/api/tools/crop-og").file(file).param("width", "800").param("height", "400"))
        .andExpect(
            header()
                .string("Content-Disposition", "attachment; filename=\"featured-800x400.jpg\""));
  }

  @Test
  void shouldReturn400WhenFileMissing() throws Exception {
    mockMvc.perform(multipart("/api/tools/crop-og")).andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenYOffsetOutOfRange() throws Exception {
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});
    mockMvc
        .perform(multipart("/api/tools/crop-og").file(file).param("y_offset", "1.5"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenXOffsetNegative() throws Exception {
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});
    mockMvc
        .perform(multipart("/api/tools/crop-og").file(file).param("x_offset", "-0.1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenQualityTooLow() throws Exception {
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});
    mockMvc
        .perform(multipart("/api/tools/crop-og").file(file).param("quality", "10"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenWidthBelowMin() throws Exception {
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});
    mockMvc
        .perform(multipart("/api/tools/crop-og").file(file).param("width", "10"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenHeightAboveMax() throws Exception {
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});
    mockMvc
        .perform(multipart("/api/tools/crop-og").file(file).param("height", "9999"))
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
        .perform(multipart("/api/tools/crop-og").file(file))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("EMPTY_FILE"));
  }

  @Test
  void shouldReturn502WhenPythonServiceFails() throws Exception {
    willThrow(new PythonToolsException("upstream")).given(useCase).execute(any(), any());
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});

    mockMvc.perform(multipart("/api/tools/crop-og").file(file)).andExpect(status().isBadGateway());
  }

  @Test
  void shouldUseDefaultsForOmittedParams() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1}, MediaType.IMAGE_JPEG_VALUE));
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});

    // Nur file, alles andere via Defaults — sollte 200 liefern
    mockMvc.perform(multipart("/api/tools/crop-og").file(file)).andExpect(status().isOk());
  }
}
