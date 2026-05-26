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
import org.mwolff.api.tools.application.ResizeImageUseCase;
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

@WebMvcTest(ResizeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ToolExceptionHandler.class)
class ResizeControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ResizeImageUseCase useCase;

  @Test
  void shouldReturnResizedImageWithUpstreamContentType() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1, 2, 3}, MediaType.IMAGE_JPEG_VALUE));

    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});

    mockMvc
        .perform(
            multipart("/api/tools/resize")
                .file(file)
                .param("width", "100")
                .param("height", "100")
                .param("output_format", "jpeg")
                .param("quality", "80"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_JPEG))
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=\"resized-100x100.jpg\""));
  }

  @Test
  void shouldUsePngExtensionForPngOutput() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1}, MediaType.IMAGE_PNG_VALUE));
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});

    mockMvc
        .perform(
            multipart("/api/tools/resize")
                .file(file)
                .param("width", "100")
                .param("height", "100")
                .param("quality", "80"))
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=\"resized-100x100.png\""));
  }

  @Test
  void shouldUseWebpExtensionForWebpOutput() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1}, "image/webp"));
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});

    mockMvc
        .perform(
            multipart("/api/tools/resize")
                .file(file)
                .param("width", "100")
                .param("height", "100")
                .param("quality", "80"))
        .andExpect(
            header()
                .string("Content-Disposition", "attachment; filename=\"resized-100x100.webp\""));
  }

  @Test
  void shouldUseBinExtensionForUnknownOutput() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1}, "application/octet-stream"));
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});

    mockMvc
        .perform(
            multipart("/api/tools/resize")
                .file(file)
                .param("width", "100")
                .param("height", "100")
                .param("quality", "80"))
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=\"resized-100x100.bin\""));
  }

  @Test
  void shouldReturn400WhenFileMissing() throws Exception {
    mockMvc
        .perform(multipart("/api/tools/resize").param("width", "100").param("height", "100"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenWidthZero() throws Exception {
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});
    mockMvc
        .perform(
            multipart("/api/tools/resize").file(file).param("width", "0").param("height", "100"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenHeightAboveMax() throws Exception {
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});
    mockMvc
        .perform(
            multipart("/api/tools/resize")
                .file(file)
                .param("width", "100")
                .param("height", "99999"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenQualityOutOfRange() throws Exception {
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});
    mockMvc
        .perform(
            multipart("/api/tools/resize")
                .file(file)
                .param("width", "100")
                .param("height", "100")
                .param("quality", "10"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenOutputFormatInvalid() throws Exception {
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});
    mockMvc
        .perform(
            multipart("/api/tools/resize")
                .file(file)
                .param("width", "100")
                .param("height", "100")
                .param("output_format", "tiff"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenUploadInvalid() throws Exception {
    willThrow(new InvalidUploadException("UNSUPPORTED_FORMAT", "no"))
        .given(useCase)
        .execute(any(), any());
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});

    mockMvc
        .perform(
            multipart("/api/tools/resize").file(file).param("width", "100").param("height", "100"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_FORMAT"));
  }

  @Test
  void shouldReturn502WhenServiceFails() throws Exception {
    willThrow(new PythonToolsException("upstream down")).given(useCase).execute(any(), any());
    final MockMultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {7});

    mockMvc
        .perform(
            multipart("/api/tools/resize").file(file).param("width", "100").param("height", "100"))
        .andExpect(status().isBadGateway());
  }
}
