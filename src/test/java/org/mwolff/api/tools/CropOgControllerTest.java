package org.mwolff.api.tools;

import org.junit.jupiter.api.Test;
import org.mwolff.api.common.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CropOgController.class)
@Import(GlobalExceptionHandler.class)
class CropOgControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CropOgService service;

    @Test
    void shouldReturnJpegOnSuccess() throws Exception {
        // Given
        final byte[] processed = "jpeg-bytes".getBytes();
        given(service.crop(any(), anyDouble(), anyDouble(), anyInt(), anyInt(), anyInt())).willReturn(processed);
        final MockMultipartFile upload = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "raw".getBytes());

        // When / Then
        mockMvc.perform(multipart("/api/tools/crop-og").file(upload))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"featured-1200x630.jpg\""))
                .andExpect(content().bytes(processed));
    }

    @Test
    void shouldReturn400WhenFileMissing() throws Exception {
        mockMvc.perform(multipart("/api/tools/crop-og"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenYOffsetAboveOne() throws Exception {
        final MockMultipartFile upload = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "raw".getBytes());
        mockMvc.perform(multipart("/api/tools/crop-og").file(upload).param("y_offset", "1.5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenYOffsetBelowZero() throws Exception {
        final MockMultipartFile upload = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "raw".getBytes());
        mockMvc.perform(multipart("/api/tools/crop-og").file(upload).param("y_offset", "-0.1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenQualityBelow50() throws Exception {
        final MockMultipartFile upload = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "raw".getBytes());
        mockMvc.perform(multipart("/api/tools/crop-og").file(upload).param("quality", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenQualityAbove95() throws Exception {
        final MockMultipartFile upload = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "raw".getBytes());
        mockMvc.perform(multipart("/api/tools/crop-og").file(upload).param("quality", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenXOffsetOutOfRange() throws Exception {
        final MockMultipartFile upload = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "raw".getBytes());
        mockMvc.perform(multipart("/api/tools/crop-og").file(upload).param("x_offset", "1.5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenWidthBelowMin() throws Exception {
        final MockMultipartFile upload = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "raw".getBytes());
        mockMvc.perform(multipart("/api/tools/crop-og").file(upload).param("width", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenHeightAboveMax() throws Exception {
        final MockMultipartFile upload = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "raw".getBytes());
        mockMvc.perform(multipart("/api/tools/crop-og").file(upload).param("height", "5000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUseCustomDimensionsInDownloadFilename() throws Exception {
        // Given
        given(service.crop(any(), anyDouble(), anyDouble(), anyInt(), anyInt(), anyInt())).willReturn("jpeg".getBytes());
        final MockMultipartFile upload = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "raw".getBytes());

        // When / Then
        mockMvc.perform(multipart("/api/tools/crop-og")
                        .file(upload)
                        .param("width", "1080")
                        .param("height", "1080"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"featured-1080x1080.jpg\""));
    }

    @Test
    void shouldReturn502WhenPythonServiceFails() throws Exception {
        // Given
        willThrow(new PythonToolsException("upstream down"))
                .given(service).crop(any(), anyDouble(), anyDouble(), anyInt(), anyInt(), anyInt());
        final MockMultipartFile upload = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "raw".getBytes());

        // When / Then
        mockMvc.perform(multipart("/api/tools/crop-og").file(upload))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value("upstream down"));
    }
}
