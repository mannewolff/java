package org.mwolff.api.tools;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ResizeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ResizeControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ResizeService service;
  @MockitoBean private UploadValidator uploadValidator;

  @Test
  void shouldReturnResizedImageWithUpstreamContentType() throws Exception {
    final byte[] processed = "jpeg-bytes".getBytes();
    given(service.resize(any(), anyInt(), anyInt(), anyString(), anyInt()))
        .willReturn(new ResizeResult(processed, MediaType.IMAGE_JPEG));
    final MockMultipartFile upload =
        new MockMultipartFile("file", "photo.png", MediaType.IMAGE_PNG_VALUE, "raw".getBytes());

    mockMvc
        .perform(
            multipart("/api/tools/resize")
                .file(upload)
                .param("width", "400")
                .param("height", "300"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_JPEG))
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=\"resized-400x300.jpg\""))
        .andExpect(content().bytes(processed));
  }

  @Test
  void shouldUsePngExtensionForPngOutput() throws Exception {
    given(service.resize(any(), anyInt(), anyInt(), anyString(), anyInt()))
        .willReturn(new ResizeResult("png".getBytes(), MediaType.IMAGE_PNG));
    final MockMultipartFile upload =
        new MockMultipartFile("file", "photo.png", MediaType.IMAGE_PNG_VALUE, "raw".getBytes());

    mockMvc
        .perform(
            multipart("/api/tools/resize")
                .file(upload)
                .param("width", "100")
                .param("height", "100"))
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=\"resized-100x100.png\""));
  }

  @Test
  void shouldUseWebpExtensionForWebpOutput() throws Exception {
    given(service.resize(any(), anyInt(), anyInt(), anyString(), anyInt()))
        .willReturn(new ResizeResult("webp".getBytes(), MediaType.parseMediaType("image/webp")));
    final MockMultipartFile upload =
        new MockMultipartFile("file", "photo.webp", "image/webp", "raw".getBytes());

    mockMvc
        .perform(
            multipart("/api/tools/resize")
                .file(upload)
                .param("width", "100")
                .param("height", "100"))
        .andExpect(
            header()
                .string("Content-Disposition", "attachment; filename=\"resized-100x100.webp\""));
  }

  @Test
  void shouldUseBinExtensionForUnknownOutput() throws Exception {
    given(service.resize(any(), anyInt(), anyInt(), anyString(), anyInt()))
        .willReturn(new ResizeResult("data".getBytes(), MediaType.APPLICATION_OCTET_STREAM));
    final MockMultipartFile upload =
        new MockMultipartFile("file", "photo.png", MediaType.IMAGE_PNG_VALUE, "raw".getBytes());

    mockMvc
        .perform(
            multipart("/api/tools/resize")
                .file(upload)
                .param("width", "100")
                .param("height", "100"))
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=\"resized-100x100.bin\""));
  }

  @Test
  void shouldReturn400WhenFileMissing() throws Exception {
    mockMvc
        .perform(multipart("/api/tools/resize").param("width", "400").param("height", "300"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenWidthZero() throws Exception {
    final MockMultipartFile upload =
        new MockMultipartFile("file", "photo.png", MediaType.IMAGE_PNG_VALUE, "raw".getBytes());
    mockMvc
        .perform(
            multipart("/api/tools/resize").file(upload).param("width", "0").param("height", "300"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenHeightAboveMax() throws Exception {
    final MockMultipartFile upload =
        new MockMultipartFile("file", "photo.png", MediaType.IMAGE_PNG_VALUE, "raw".getBytes());
    mockMvc
        .perform(
            multipart("/api/tools/resize")
                .file(upload)
                .param("width", "400")
                .param("height", "9000"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenQualityOutOfRange() throws Exception {
    final MockMultipartFile upload =
        new MockMultipartFile("file", "photo.png", MediaType.IMAGE_PNG_VALUE, "raw".getBytes());
    mockMvc
        .perform(
            multipart("/api/tools/resize")
                .file(upload)
                .param("width", "400")
                .param("height", "300")
                .param("quality", "10"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400WhenOutputFormatInvalid() throws Exception {
    final MockMultipartFile upload =
        new MockMultipartFile("file", "photo.png", MediaType.IMAGE_PNG_VALUE, "raw".getBytes());
    mockMvc
        .perform(
            multipart("/api/tools/resize")
                .file(upload)
                .param("width", "400")
                .param("height", "300")
                .param("output_format", "bmp"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn502WhenServiceFails() throws Exception {
    willThrow(new PythonToolsException("upstream down"))
        .given(service)
        .resize(any(), anyInt(), anyInt(), anyString(), anyInt());
    final MockMultipartFile upload =
        new MockMultipartFile("file", "photo.png", MediaType.IMAGE_PNG_VALUE, "raw".getBytes());

    mockMvc
        .perform(
            multipart("/api/tools/resize")
                .file(upload)
                .param("width", "400")
                .param("height", "300"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Tool-Service derzeit nicht erreichbar."));
  }
}
