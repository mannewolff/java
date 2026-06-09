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
import org.mwolff.api.tools.application.RasterToPngUseCase;
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

@WebMvcTest(RasterToPngController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ToolExceptionHandler.class)
class RasterToPngControllerTest {

  private static final byte[] JPEG_BYTES = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

  @Autowired private MockMvc mockMvc;

  @MockitoBean private RasterToPngUseCase useCase;

  @Test
  void shouldReturnPngWithFilenameContainingDimensions() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1, 2, 3}, MediaType.IMAGE_PNG_VALUE));

    final MockMultipartFile file =
        new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG_BYTES);

    mockMvc
        .perform(
            multipart("/api/tools/raster-to-png")
                .file(file)
                .param("width", "800")
                .param("height", "600"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=\"photo-800x600.png\""));
  }

  @Test
  void shouldReturnPngWithoutDimensionsInFilenameWhenNotProvided() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1}, MediaType.IMAGE_PNG_VALUE));
    final MockMultipartFile file =
        new MockMultipartFile("file", "image.png", "image/png", JPEG_BYTES);

    mockMvc
        .perform(multipart("/api/tools/raster-to-png").file(file))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"image.png\""));
  }

  @Test
  void shouldKeepLeadingDotFilenameAsBase() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1}, MediaType.IMAGE_PNG_VALUE));
    final MockMultipartFile file = new MockMultipartFile("file", ".jpg", "image/jpeg", JPEG_BYTES);

    mockMvc
        .perform(multipart("/api/tools/raster-to-png").file(file))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", "attachment; filename=\".jpg.png\""));
  }

  @Test
  void shouldUseWidthOnlySuffixWhenHeightMissing() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1}, MediaType.IMAGE_PNG_VALUE));
    final MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", JPEG_BYTES);

    mockMvc
        .perform(multipart("/api/tools/raster-to-png").file(file).param("width", "320"))
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"a-w320.png\""));
  }

  @Test
  void shouldUseHeightOnlySuffixWhenWidthMissing() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1}, MediaType.IMAGE_PNG_VALUE));
    final MockMultipartFile file = new MockMultipartFile("file", "b.jpg", "image/jpeg", JPEG_BYTES);

    mockMvc
        .perform(multipart("/api/tools/raster-to-png").file(file).param("height", "200"))
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"b-h200.png\""));
  }

  @Test
  void shouldFallBackToGenericFilenameWhenOriginalMissing() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1}, MediaType.IMAGE_PNG_VALUE));
    final MockMultipartFile file = new MockMultipartFile("file", "", "image/jpeg", JPEG_BYTES);

    mockMvc
        .perform(multipart("/api/tools/raster-to-png").file(file).param("width", "10"))
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=\"image-w10.png\""));
  }

  @Test
  void shouldFallBackToGenericFilenameWhenOriginalFilenameIsNull() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1}, MediaType.IMAGE_PNG_VALUE));
    final MockMultipartFile file = new MockMultipartFile("file", null, "image/jpeg", JPEG_BYTES);

    mockMvc
        .perform(multipart("/api/tools/raster-to-png").file(file))
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"image.png\""));
  }

  @Test
  void shouldKeepFilenameWhenNoExtensionPresent() throws Exception {
    given(useCase.execute(any(), any()))
        .willReturn(new ToolImageResult(new byte[] {1}, MediaType.IMAGE_PNG_VALUE));
    final MockMultipartFile file = new MockMultipartFile("file", "photo", "image/jpeg", JPEG_BYTES);

    mockMvc
        .perform(multipart("/api/tools/raster-to-png").file(file))
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"photo.png\""));
  }

  @Test
  void shouldReturn400WhenFileMissing() throws Exception {
    mockMvc.perform(multipart("/api/tools/raster-to-png")).andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenWidthZero() throws Exception {
    final MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", JPEG_BYTES);
    mockMvc
        .perform(multipart("/api/tools/raster-to-png").file(file).param("width", "0"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenHeightAboveMax() throws Exception {
    final MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", JPEG_BYTES);
    mockMvc
        .perform(multipart("/api/tools/raster-to-png").file(file).param("height", "99999"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenUploadInvalid() throws Exception {
    willThrow(new InvalidUploadException("UNSUPPORTED_FORMAT", "no"))
        .given(useCase)
        .execute(any(), any());
    final MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", JPEG_BYTES);

    mockMvc
        .perform(multipart("/api/tools/raster-to-png").file(file))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_FORMAT"));
  }

  @Test
  void shouldReturn502WhenServiceFails() throws Exception {
    willThrow(new PythonToolsException("upstream down")).given(useCase).execute(any(), any());
    final MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", JPEG_BYTES);

    mockMvc
        .perform(multipart("/api/tools/raster-to-png").file(file))
        .andExpect(status().isBadGateway());
  }
}
