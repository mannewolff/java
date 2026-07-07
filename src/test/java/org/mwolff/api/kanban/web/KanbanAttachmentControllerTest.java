package org.mwolff.api.kanban.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mwolff.api.auth.infrastructure.SecurityConfig;
import org.mwolff.api.kanban.application.DeleteAttachmentUseCase;
import org.mwolff.api.kanban.application.GetAttachmentUseCase;
import org.mwolff.api.kanban.application.ListAttachmentsUseCase;
import org.mwolff.api.kanban.application.UploadAttachmentUseCase;
import org.mwolff.api.kanban.domain.KanbanAttachment;
import org.mwolff.api.kanban.domain.KanbanAttachmentLimitExceededException;
import org.mwolff.api.kanban.domain.KanbanAttachmentNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(KanbanAttachmentController.class)
@Import({KanbanExceptionHandler.class, SecurityConfig.class})
class KanbanAttachmentControllerTest {

  private static final String SUB = "user-1";
  private static final long ITEM = 5L;
  private static final byte[] BYTES = {1, 2, 3};

  private static RequestPostProcessor userJwt() {
    return jwt().jwt(j -> j.subject(SUB)).authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  private static KanbanAttachment attachment(long id, String filename, String contentType) {
    return new KanbanAttachment(
        id, ITEM, filename, contentType, BYTES.length, BYTES, "hash", SUB, Instant.EPOCH);
  }

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ListAttachmentsUseCase listUseCase;
  @MockitoBean private UploadAttachmentUseCase uploadUseCase;
  @MockitoBean private GetAttachmentUseCase getUseCase;
  @MockitoBean private DeleteAttachmentUseCase deleteUseCase;
  @MockitoBean private JwtDecoder jwtDecoder;

  private MockMultipartFile file() {
    return new MockMultipartFile("file", "orig.txt", "text/plain", BYTES);
  }

  private KanbanAttachmentController controller() {
    return new KanbanAttachmentController(listUseCase, uploadUseCase, getUseCase, deleteUseCase);
  }

  @Test
  void uploadEmptyFileIsBadRequest() throws Exception {
    mockMvc
        .perform(
            multipart("/api/kanban/items/5/attachments")
                .file(new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]))
                .with(userJwt()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void uploadNullFileThrows() {
    // Deckt den file==null-Zweig ab (auth wird davor nicht dereferenziert).
    assertThatThrownBy(() -> controller().upload(null, ITEM, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void uploadUnreadableFileThrows() throws Exception {
    // Deckt den defensiven catch(IOException)-Zweig von file.getBytes() ab.
    final org.springframework.web.multipart.MultipartFile broken =
        mock(org.springframework.web.multipart.MultipartFile.class);
    when(broken.isEmpty()).thenReturn(false);
    when(broken.getBytes()).thenThrow(new java.io.IOException("boom"));

    assertThatThrownBy(() -> controller().upload(null, ITEM, broken))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void uploadWithoutJwtIsUnauthorized() throws Exception {
    mockMvc
        .perform(multipart("/api/kanban/items/5/attachments").file(file()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void uploadForeignItemIsNotFound() throws Exception {
    given(uploadUseCase.execute(eq(SUB), eq(ITEM), any(), any()))
        .willThrow(new KanbanItemNotFoundException(ITEM));

    mockMvc
        .perform(multipart("/api/kanban/items/5/attachments").file(file()).with(userJwt()))
        .andExpect(status().isNotFound());
  }

  @Test
  void uploadOwnItemReturns201WithMetadata() throws Exception {
    given(uploadUseCase.execute(eq(SUB), eq(ITEM), any(), any()))
        .willReturn(attachment(7L, "doc.pdf", "application/pdf"));

    mockMvc
        .perform(multipart("/api/kanban/items/5/attachments").file(file()).with(userJwt()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(7))
        .andExpect(jsonPath("$.filename").value("doc.pdf"))
        .andExpect(jsonPath("$.contentType").value("application/pdf"))
        .andExpect(jsonPath("$.uploadedBy").value(SUB));
  }

  @Test
  void uploadBeyondLimitIsConflict() throws Exception {
    given(uploadUseCase.execute(eq(SUB), eq(ITEM), any(), any()))
        .willThrow(new KanbanAttachmentLimitExceededException(ITEM, 5));

    mockMvc
        .perform(multipart("/api/kanban/items/5/attachments").file(file()).with(userJwt()))
        .andExpect(status().isConflict());
  }

  @Test
  void listReturnsAttachments() throws Exception {
    given(listUseCase.execute(SUB, ITEM))
        .willReturn(
            java.util.List.of(
                new org.mwolff.api.kanban.domain.KanbanAttachmentMeta(
                    1L, ITEM, "a.txt", "text/plain", 3, SUB, Instant.EPOCH)));

    mockMvc
        .perform(get("/api/kanban/items/5/attachments").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].filename").value("a.txt"));
  }

  @Test
  void downloadSetsAttachmentDispositionAndContentType() throws Exception {
    given(getUseCase.execute(SUB, ITEM, 7L))
        .willReturn(attachment(7L, "doc.pdf", "application/pdf"));

    mockMvc
        .perform(get("/api/kanban/items/5/attachments/7").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
        .andExpect(
            header().string("Content-Disposition", org.hamcrest.Matchers.containsString("doc.pdf")))
        .andExpect(header().string("Content-Type", "application/pdf"));
  }

  @Test
  void downloadUnknownAttachmentIsNotFound() throws Exception {
    given(getUseCase.execute(SUB, ITEM, 7L)).willThrow(new KanbanAttachmentNotFoundException(7L));

    mockMvc
        .perform(get("/api/kanban/items/5/attachments/7").with(userJwt()))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteOwnAttachmentIsNoContent() throws Exception {
    mockMvc
        .perform(delete("/api/kanban/items/5/attachments/7").with(userJwt()))
        .andExpect(status().isNoContent());
    verify(deleteUseCase).execute(SUB, ITEM, 7L);
  }

  @Test
  void deleteForeignAttachmentIsNotFound() throws Exception {
    doThrow(new KanbanAttachmentNotFoundException(7L)).when(deleteUseCase).execute(SUB, ITEM, 7L);

    mockMvc
        .perform(delete("/api/kanban/items/5/attachments/7").with(userJwt()))
        .andExpect(status().isNotFound());
  }
}
