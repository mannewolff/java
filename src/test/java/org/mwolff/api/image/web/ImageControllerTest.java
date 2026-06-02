package org.mwolff.api.image.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mwolff.api.image.application.CheckImageHashUseCase;
import org.mwolff.api.image.application.DeleteImagesUseCase;
import org.mwolff.api.image.application.DeleteImagesUseCase.DeleteResult;
import org.mwolff.api.image.application.DeleteImagesUseCase.Failure;
import org.mwolff.api.image.application.GetImageThumbnailUseCase;
import org.mwolff.api.image.application.GetImageUseCase;
import org.mwolff.api.image.application.ListImagesUseCase;
import org.mwolff.api.image.application.ListManagedImagesUseCase;
import org.mwolff.api.image.application.UploadImageUseCase;
import org.mwolff.api.image.domain.ImageInUseException;
import org.mwolff.api.image.domain.ImageMetadata;
import org.mwolff.api.image.domain.ImageNotFoundException;
import org.mwolff.api.image.domain.ImagePage;
import org.mwolff.api.image.domain.InvalidImageUploadException;
import org.mwolff.api.image.domain.ManagedImage;
import org.mwolff.api.image.domain.ManagedImagePage;
import org.mwolff.api.image.domain.StoredImage;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ImageControllerTest {

  @Mock private UploadImageUseCase uploadUseCase;
  @Mock private GetImageUseCase getUseCase;
  @Mock private ListImagesUseCase listUseCase;
  @Mock private CheckImageHashUseCase checkHashUseCase;
  @Mock private GetImageThumbnailUseCase thumbnailUseCase;
  @Mock private ListManagedImagesUseCase listManagedUseCase;
  @Mock private DeleteImagesUseCase deleteUseCase;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new ImageController(
                    uploadUseCase,
                    getUseCase,
                    listUseCase,
                    checkHashUseCase,
                    thumbnailUseCase,
                    listManagedUseCase,
                    deleteUseCase))
            .setControllerAdvice(new ImageExceptionHandler())
            .build();
  }

  @Test
  void uploadReturns201WithIdAndUrl() throws Exception {
    final byte[] bytes = {1, 2, 3};
    when(uploadUseCase.execute(eq("image/png"), any()))
        .thenReturn(new StoredImage(5L, "image/png", 3, bytes, Instant.now(), null));

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
    when(getUseCase.execute(5L)).thenReturn(new StoredImage(5L, "image/png", 3, bytes, null, null));

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

  @Test
  void listReturnsMetadataPageWithTotal() throws Exception {
    when(listUseCase.execute(null, null))
        .thenReturn(
            new ImagePage(
                List.of(
                    new ImageMetadata(
                        3L, "image/png", 123, Instant.parse("2026-06-01T00:00:00Z"), null)),
                1L));

    mockMvc
        .perform(get("/api/images"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.images[0].id").value(3))
        .andExpect(jsonPath("$.images[0].contentType").value("image/png"))
        .andExpect(jsonPath("$.images[0].sizeBytes").value(123))
        .andExpect(jsonPath("$.images[0].hash").value(org.hamcrest.Matchers.nullValue()));
  }

  @Test
  void listForwardsLimitAndOffset() throws Exception {
    when(listUseCase.execute(10, 20)).thenReturn(new ImagePage(List.of(), 0L));

    mockMvc
        .perform(get("/api/images?limit=10&offset=20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0));
  }

  private static final String HASH = "a".repeat(64);

  @Test
  void checkHashReturnsExistsTrueWithId() throws Exception {
    when(checkHashUseCase.execute(HASH)).thenReturn(java.util.Optional.of(42L));

    mockMvc
        .perform(
            post("/api/images/check-hash")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hash\":\"" + HASH + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exists").value(true))
        .andExpect(jsonPath("$.id").value(42));
  }

  @Test
  void checkHashReturnsExistsFalseWhenAbsent() throws Exception {
    when(checkHashUseCase.execute(HASH)).thenReturn(java.util.Optional.empty());

    mockMvc
        .perform(
            post("/api/images/check-hash")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hash\":\"" + HASH + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exists").value(false))
        .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.nullValue()));
  }

  @Test
  void checkHashRejectsInvalidHashWith400() throws Exception {
    mockMvc
        .perform(
            post("/api/images/check-hash")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hash\":\"not-a-hash\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void thumbnailReturnsPngWithCacheHeader() throws Exception {
    final byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47};
    when(thumbnailUseCase.execute(5L, null)).thenReturn(png);

    mockMvc
        .perform(get("/api/images/5/thumbnail"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age")))
        .andExpect(content().bytes(png));
  }

  @Test
  void thumbnailForwardsSizeParam() throws Exception {
    when(thumbnailUseCase.execute(5L, 64)).thenReturn(new byte[] {1});

    mockMvc.perform(get("/api/images/5/thumbnail?size=64")).andExpect(status().isOk());

    verify(thumbnailUseCase).execute(5L, 64);
  }

  @Test
  void thumbnailMissingReturns404() throws Exception {
    when(thumbnailUseCase.execute(9L, null)).thenThrow(new ImageNotFoundException(9));

    mockMvc.perform(get("/api/images/9/thumbnail")).andExpect(status().isNotFound());
  }

  // ----- GET /api/images/manage --------------------------------------------

  @Test
  void manageListReturnsImagesWithUsageCount() throws Exception {
    when(listManagedUseCase.execute(null, null))
        .thenReturn(
            new ManagedImagePage(
                List.of(
                    new ManagedImage(
                        new ImageMetadata(
                            3L, "image/png", 123, Instant.parse("2026-06-01T00:00:00Z"), "abc"),
                        2L)),
                1L));

    mockMvc
        .perform(get("/api/images/manage"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.images[0].id").value(3))
        .andExpect(jsonPath("$.images[0].usageCount").value(2))
        .andExpect(jsonPath("$.images[0].hash").value("abc"));
  }

  @Test
  void manageListForwardsLimitAndOffset() throws Exception {
    when(listManagedUseCase.execute(10, 20)).thenReturn(new ManagedImagePage(List.of(), 0L));

    mockMvc.perform(get("/api/images/manage?limit=10&offset=20")).andExpect(status().isOk());

    verify(listManagedUseCase).execute(10, 20);
  }

  // ----- DELETE /api/images/{id} -------------------------------------------

  @Test
  void deleteReturns204WhenUnused() throws Exception {
    mockMvc.perform(delete("/api/images/5")).andExpect(status().isNoContent());

    verify(deleteUseCase).deleteOne(5L);
  }

  @Test
  void deleteReturns409WhenInUse() throws Exception {
    doThrow(new ImageInUseException(5L, 2L)).when(deleteUseCase).deleteOne(5L);

    mockMvc.perform(delete("/api/images/5")).andExpect(status().isConflict());
  }

  @Test
  void deleteReturns404WhenMissing() throws Exception {
    doThrow(new ImageNotFoundException(9)).when(deleteUseCase).deleteOne(9L);

    mockMvc.perform(delete("/api/images/9")).andExpect(status().isNotFound());
  }

  // ----- POST /api/images/batch-delete -------------------------------------

  @Test
  void batchDeleteReturnsDeletedAndFailed() throws Exception {
    when(deleteUseCase.deleteBatch(List.of(1L, 2L)))
        .thenReturn(new DeleteResult(List.of(1L), List.of(new Failure(2L, "IN_USE"))));

    mockMvc
        .perform(
            post("/api/images/batch-delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\":[1,2]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deleted[0]").value(1))
        .andExpect(jsonPath("$.failed[0].id").value(2))
        .andExpect(jsonPath("$.failed[0].reason").value("IN_USE"));
  }

  @Test
  void batchDeleteRejectsEmptyIdsWith400() throws Exception {
    mockMvc
        .perform(
            post("/api/images/batch-delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\":[]}"))
        .andExpect(status().isBadRequest());
  }
}
