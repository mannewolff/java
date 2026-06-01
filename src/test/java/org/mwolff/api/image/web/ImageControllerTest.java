package org.mwolff.api.image.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mwolff.api.image.application.GetImageUseCase;
import org.mwolff.api.image.application.UploadImageUseCase;
import org.mwolff.api.image.domain.ImageNotFoundException;
import org.mwolff.api.image.domain.InvalidImageUploadException;
import org.mwolff.api.image.domain.StoredImage;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ImageControllerTest {

  @Mock private UploadImageUseCase uploadUseCase;
  @Mock private GetImageUseCase getUseCase;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ImageController(uploadUseCase, getUseCase))
            .setControllerAdvice(new ImageExceptionHandler())
            .build();
  }

  @Test
  void uploadReturns201WithIdAndUrl() throws Exception {
    final byte[] bytes = {1, 2, 3};
    when(uploadUseCase.execute(eq("image/png"), any()))
        .thenReturn(new StoredImage(5L, "image/png", 3, bytes, Instant.now()));

    mockMvc
        .perform(
            multipart("/api/images")
                .file(new MockMultipartFile("file", "x.png", "image/png", bytes)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/images/5"))
        .andExpect(jsonPath("$.id").value(5))
        .andExpect(jsonPath("$.url").value("/api/images/5"));
  }

  @Test
  void uploadEmptyFileReturns400() throws Exception {
    mockMvc
        .perform(
            multipart("/api/images")
                .file(new MockMultipartFile("file", "x.png", "image/png", new byte[0])))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("EMPTY_FILE"));
  }

  @Test
  void uploadUnsupportedTypeReturns415() throws Exception {
    when(uploadUseCase.execute(any(), any()))
        .thenThrow(new InvalidImageUploadException("UNSUPPORTED_TYPE", "bad"));

    mockMvc
        .perform(
            multipart("/api/images")
                .file(new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[] {1})))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_TYPE"));
  }

  @Test
  void getReturnsBytesWithContentType() throws Exception {
    final byte[] bytes = {1, 2, 3};
    when(getUseCase.execute(5L)).thenReturn(new StoredImage(5L, "image/png", 3, bytes, null));

    mockMvc
        .perform(get("/api/images/5"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(content().bytes(bytes));
  }

  @Test
  void getMissingReturns404() throws Exception {
    when(getUseCase.execute(9L)).thenThrow(new ImageNotFoundException(9));

    mockMvc.perform(get("/api/images/9")).andExpect(status().isNotFound());
  }
}
