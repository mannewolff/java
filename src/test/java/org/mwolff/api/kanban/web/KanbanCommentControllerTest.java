package org.mwolff.api.kanban.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mwolff.api.auth.infrastructure.SecurityConfig;
import org.mwolff.api.kanban.application.AddCommentUseCase;
import org.mwolff.api.kanban.application.DeleteCommentUseCase;
import org.mwolff.api.kanban.application.ListCommentsUseCase;
import org.mwolff.api.kanban.application.UpdateCommentUseCase;
import org.mwolff.api.kanban.domain.KanbanComment;
import org.mwolff.api.kanban.domain.KanbanCommentForbiddenException;
import org.mwolff.api.kanban.domain.KanbanCommentNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(KanbanCommentController.class)
@Import({KanbanExceptionHandler.class, SecurityConfig.class})
class KanbanCommentControllerTest {

  private static final String SUB = "user-1";
  private static final String USERNAME = "alice";
  private static final long ITEM_ID = 5L;
  private static final long COMMENT_ID = 9L;

  private static RequestPostProcessor userJwt() {
    return jwt()
        .jwt(j -> j.subject(SUB).claim("preferred_username", USERNAME))
        .authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ListCommentsUseCase listUseCase;
  @MockitoBean private AddCommentUseCase addUseCase;
  @MockitoBean private UpdateCommentUseCase updateUseCase;
  @MockitoBean private DeleteCommentUseCase deleteUseCase;

  @MockitoBean private JwtDecoder jwtDecoder;

  private static RequestPostProcessor userJwtWithoutUsername() {
    return jwt().jwt(j -> j.subject(SUB)).authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  private static RequestPostProcessor userJwtBlankUsername() {
    return jwt()
        .jwt(j -> j.subject(SUB).claim("preferred_username", "  "))
        .authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  private static KanbanComment comment(long id, String body) {
    return new KanbanComment(id, ITEM_ID, SUB, USERNAME, body, Instant.EPOCH, Instant.EPOCH);
  }

  @Test
  void listShouldReturnComments() throws Exception {
    given(listUseCase.execute(SUB, ITEM_ID)).willReturn(List.of(comment(COMMENT_ID, "Hallo")));

    mockMvc
        .perform(get("/api/kanban/items/5/comments").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(9))
        .andExpect(jsonPath("$[0].author").value(USERNAME))
        .andExpect(jsonPath("$[0].body").value("Hallo"));
  }

  @Test
  void listWithoutJwtShouldReturn401() throws Exception {
    mockMvc.perform(get("/api/kanban/items/5/comments")).andExpect(status().isUnauthorized());
  }

  @Test
  void listForeignItemShouldReturn404() throws Exception {
    given(listUseCase.execute(SUB, ITEM_ID)).willThrow(new KanbanItemNotFoundException(ITEM_ID));

    mockMvc
        .perform(get("/api/kanban/items/5/comments").with(userJwt()))
        .andExpect(status().isNotFound());
  }

  @Test
  void createShouldReturn201AndPassAuthorFromUsername() throws Exception {
    given(addUseCase.execute(eq(SUB), eq(USERNAME), eq(ITEM_ID), eq("Hallo")))
        .willReturn(comment(COMMENT_ID, "Hallo"));

    mockMvc
        .perform(
            post("/api/kanban/items/5/comments")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Hallo\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(9))
        .andExpect(jsonPath("$.author").value(USERNAME));
  }

  @Test
  void createShouldFallBackToSubAsAuthorWhenUsernameBlank() throws Exception {
    // Leerer preferred_username zählt wie fehlend → Fallback auf sub als Anzeigename.
    given(addUseCase.execute(eq(SUB), eq(SUB), eq(ITEM_ID), eq("Hallo")))
        .willReturn(comment(COMMENT_ID, "Hallo"));

    mockMvc
        .perform(
            post("/api/kanban/items/5/comments")
                .with(userJwtBlankUsername())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Hallo\"}"))
        .andExpect(status().isCreated());
  }

  @Test
  void createShouldFallBackToSubAsAuthorWhenUsernameMissing() throws Exception {
    // Fehlt preferred_username (z. B. Service-Account), wird der stabile sub als Anzeigename
    // benutzt — nie null, sonst NPE/500 in der Domain.
    given(addUseCase.execute(eq(SUB), eq(SUB), eq(ITEM_ID), eq("Hallo")))
        .willReturn(comment(COMMENT_ID, "Hallo"));

    mockMvc
        .perform(
            post("/api/kanban/items/5/comments")
                .with(userJwtWithoutUsername())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Hallo\"}"))
        .andExpect(status().isCreated());
  }

  @Test
  void createShouldReturn400WhenBodyBlank() throws Exception {
    mockMvc
        .perform(
            post("/api/kanban/items/5/comments")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createForeignItemShouldReturn404() throws Exception {
    willThrow(new KanbanItemNotFoundException(ITEM_ID))
        .given(addUseCase)
        .execute(any(), any(), eq(ITEM_ID), any());

    mockMvc
        .perform(
            post("/api/kanban/items/5/comments")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Hallo\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateShouldReturnUpdated() throws Exception {
    given(updateUseCase.execute(SUB, ITEM_ID, COMMENT_ID, "neu"))
        .willReturn(comment(COMMENT_ID, "neu"));

    mockMvc
        .perform(
            put("/api/kanban/items/5/comments/9")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"neu\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.body").value("neu"));
  }

  @Test
  void updateUnknownCommentShouldReturn404() throws Exception {
    willThrow(new KanbanCommentNotFoundException(COMMENT_ID))
        .given(updateUseCase)
        .execute(any(), eq(ITEM_ID), eq(COMMENT_ID), any());

    mockMvc
        .perform(
            put("/api/kanban/items/5/comments/9")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"neu\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateForeignAuthorShouldReturn403() throws Exception {
    willThrow(new KanbanCommentForbiddenException(COMMENT_ID))
        .given(updateUseCase)
        .execute(any(), eq(ITEM_ID), eq(COMMENT_ID), any());

    mockMvc
        .perform(
            put("/api/kanban/items/5/comments/9")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"neu\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void deleteShouldReturn204() throws Exception {
    mockMvc
        .perform(delete("/api/kanban/items/5/comments/9").with(userJwt()))
        .andExpect(status().isNoContent());
  }

  @Test
  void deleteForeignAuthorShouldReturn403() throws Exception {
    willThrow(new KanbanCommentForbiddenException(COMMENT_ID))
        .given(deleteUseCase)
        .execute(any(), eq(ITEM_ID), eq(COMMENT_ID));

    mockMvc
        .perform(delete("/api/kanban/items/5/comments/9").with(userJwt()))
        .andExpect(status().isForbidden());
  }

  @Test
  void listShouldReturn400WhenItemIdNotPositive() throws Exception {
    // @Min(1) auf @PathVariable itemId (#268).
    mockMvc
        .perform(get("/api/kanban/items/0/comments").with(userJwt()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deleteShouldReturn400WhenCommentIdNotPositive() throws Exception {
    mockMvc
        .perform(delete("/api/kanban/items/5/comments/0").with(userJwt()))
        .andExpect(status().isBadRequest());
  }
}
