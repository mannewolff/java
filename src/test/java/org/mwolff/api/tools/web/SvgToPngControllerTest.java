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
import org.mwolff.api.tools.application.SvgToPngUseCase;
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

@WebMvcTest(SvgToPngController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ToolExceptionHandler.class)
class SvgToPngControllerTest {

  private static final byte[] SVG_BYTES =
      ("<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'/>").getBytes();

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SvgToPngUseCase useCase;

  @Test
  void shouldReturnPngWithFilenameContainingDimensions() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1, 2, 3}, MediaType.IMAGE_PNG_VALUE));

    final MockMultipartFile file =
        new MockMultipartFile("file", "logo.svg", "image/svg+xml", SVG_BYTES);

    mockMvc
        .perform(
            multipart("/api/tools/svg-to-png")
                .file(file)
                .param("width", "256")
                .param("height", "128"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=\"logo-256x128.png\""));
  }

  @Test
  void shouldReturnPngWithoutDimensionsInFilenameWhenNotProvided() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1}, MediaType.IMAGE_PNG_VALUE));
    final MockMultipartFile file =
        new MockMultipartFile("file", "icon.svg", "image/svg+xml", SVG_BYTES);

    mockMvc
        .perform(multipart("/api/tools/svg-to-png").file(file))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"icon.png\""));
  }

  @Test
  void shouldUseWidthOnlySuffixWhenHeightMissing() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1}, MediaType.IMAGE_PNG_VALUE));
    final MockMultipartFile file =
        new MockMultipartFile("file", "a.svg", "image/svg+xml", SVG_BYTES);

    mockMvc
        .perform(multipart("/api/tools/svg-to-png").file(file).param("width", "320"))
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"a-w320.png\""));
  }

  @Test
  void shouldUseHeightOnlySuffixWhenWidthMissing() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1}, MediaType.IMAGE_PNG_VALUE));
    final MockMultipartFile file =
        new MockMultipartFile("file", "b.svg", "image/svg+xml", SVG_BYTES);

    mockMvc
        .perform(multipart("/api/tools/svg-to-png").file(file).param("height", "200"))
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"b-h200.png\""));
  }

  @Test
  void shouldFallBackToGenericFilenameWhenOriginalMissing() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1}, MediaType.IMAGE_PNG_VALUE));
    // Empty original filename triggers the "image" fallback in filenameFor().
    final MockMultipartFile file = new MockMultipartFile("file", "", "image/svg+xml", SVG_BYTES);

    mockMvc
        .perform(multipart("/api/tools/svg-to-png").file(file).param("width", "10"))
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=\"image-w10.png\""));
  }

  @Test
  void shouldFallBackToGenericFilenameWhenOriginalFilenameIsNull() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1}, MediaType.IMAGE_PNG_VALUE));
    // null als originalFilename trifft den ersten Zweig der "|| isBlank"-Bedingung —
    // sonst macht JaCoCo bei dem || einen unbedeckten Branch auf.
    final MockMultipartFile file = new MockMultipartFile("file", null, "image/svg+xml", SVG_BYTES);

    mockMvc
        .perform(multipart("/api/tools/svg-to-png").file(file))
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"image.png\""));
  }

  @Test
  void shouldKeepFilenameWhenNoExtensionPresent() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1}, MediaType.IMAGE_PNG_VALUE));
    final MockMultipartFile file =
        new MockMultipartFile("file", "logo", "image/svg+xml", SVG_BYTES);

    mockMvc
        .perform(multipart("/api/tools/svg-to-png").file(file))
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"logo.png\""));
  }

  @Test
  void shouldReturn400WhenFileMissing() throws Exception {
    mockMvc.perform(multipart("/api/tools/svg-to-png")).andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenWidthZero() throws Exception {
    final MockMultipartFile file =
        new MockMultipartFile("file", "a.svg", "image/svg+xml", SVG_BYTES);
    mockMvc
        .perform(multipart("/api/tools/svg-to-png").file(file).param("width", "0"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenHeightAboveMax() throws Exception {
    final MockMultipartFile file =
        new MockMultipartFile("file", "a.svg", "image/svg+xml", SVG_BYTES);
    mockMvc
        .perform(multipart("/api/tools/svg-to-png").file(file).param("height", "99999"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenBackgroundInvalid() throws Exception {
    final MockMultipartFile file =
        new MockMultipartFile("file", "a.svg", "image/svg+xml", SVG_BYTES);
    mockMvc
        .perform(multipart("/api/tools/svg-to-png").file(file).param("background", "red"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenUploadInvalid() throws Exception {
    willThrow(new InvalidUploadException("UNSUPPORTED_FORMAT", "no"))
        .given(useCase)
        .execute(any(), any());
    final MockMultipartFile file =
        new MockMultipartFile("file", "a.svg", "image/svg+xml", SVG_BYTES);

    mockMvc
        .perform(multipart("/api/tools/svg-to-png").file(file))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_FORMAT"));
  }

  @Test
  void shouldReturn502WhenServiceFails() throws Exception {
    willThrow(new PythonToolsException("upstream down")).given(useCase).execute(any(), any());
    final MockMultipartFile file =
        new MockMultipartFile("file", "a.svg", "image/svg+xml", SVG_BYTES);

    mockMvc
        .perform(multipart("/api/tools/svg-to-png").file(file))
        .andExpect(status().isBadGateway());
  }
}
