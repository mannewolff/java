package org.mwolff.api.kanban.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mwolff.api.auth.infrastructure.SecurityConfig;
import org.mwolff.api.kanban.application.ArchiveItemUseCase;
import org.mwolff.api.kanban.application.CreateItemUseCase;
import org.mwolff.api.kanban.application.ForceDeleteItemUseCase;
import org.mwolff.api.kanban.application.ListItemsUseCase;
import org.mwolff.api.kanban.application.MoveItemUseCase;
import org.mwolff.api.kanban.application.RestoreItemUseCase;
import org.mwolff.api.kanban.application.UpdateItemContentUseCase;
import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(KanbanItemController.class)
@Import({KanbanExceptionHandler.class, SecurityConfig.class})
class KanbanItemControllerTest {

  private static final String SUB = "user-1";

  private static org.springframework.test.web.servlet.request.RequestPostProcessor userJwt() {
    return jwt().jwt(j -> j.subject(SUB)).authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ListItemsUseCase listUseCase;
  @MockitoBean private CreateItemUseCase createUseCase;
  @MockitoBean private UpdateItemContentUseCase updateUseCase;
  @MockitoBean private MoveItemUseCase moveUseCase;
  @MockitoBean private ArchiveItemUseCase archiveUseCase;
  @MockitoBean private ForceDeleteItemUseCase forceDeleteUseCase;
  @MockitoBean private RestoreItemUseCase restoreUseCase;

  @MockitoBean private JwtDecoder jwtDecoder;

  private static KanbanItem item(long id, KanbanColumn column, int position) {
    return new KanbanItem(
        id,
        SUB,
        "T",
        "b",
        column,
        position,
        Instant.EPOCH,
        Instant.EPOCH,
        column == KanbanColumn.DONE ? Instant.EPOCH : null,
        false,
        0);
  }

  @Test
  void listShouldReturnGroupedByColumn() throws Exception {
    final Map<KanbanColumn, List<KanbanItem>> grouped = new EnumMap<>(KanbanColumn.class);
    for (KanbanColumn c : KanbanColumn.values()) grouped.put(c, List.of());
    grouped.put(KanbanColumn.BACKLOG, List.of(item(1L, KanbanColumn.BACKLOG, 0)));
    given(listUseCase.execute(SUB)).willReturn(grouped);

    mockMvc
        .perform(get("/api/kanban/items").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.BACKLOG").isArray())
        .andExpect(jsonPath("$.BACKLOG[0].id").value(1))
        .andExpect(jsonPath("$.READY").isEmpty())
        .andExpect(jsonPath("$.IN_PROGRESS").isEmpty())
        .andExpect(jsonPath("$.IN_REVIEW").isEmpty())
        .andExpect(jsonPath("$.DONE").isEmpty());
  }

  @Test
  void listWithoutJwtShouldReturn401() throws Exception {
    mockMvc.perform(get("/api/kanban/items")).andExpect(status().isUnauthorized());
  }

  @Test
  void createShouldReturn201() throws Exception {
    given(createUseCase.execute(eq(SUB), eq("Neu"), eq("b"), eq(KanbanColumn.BACKLOG)))
        .willReturn(item(7L, KanbanColumn.BACKLOG, 0));

    mockMvc
        .perform(
            post("/api/kanban/items")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Neu\",\"body\":\"b\",\"column\":\"BACKLOG\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(7))
        .andExpect(jsonPath("$.column").value("BACKLOG"));
  }

  @Test
  void createShouldAcceptReadyColumn() throws Exception {
    given(createUseCase.execute(eq(SUB), eq("Neu"), eq("b"), eq(KanbanColumn.READY)))
        .willReturn(item(11L, KanbanColumn.READY, 0));

    mockMvc
        .perform(
            post("/api/kanban/items")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Neu\",\"body\":\"b\",\"column\":\"READY\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.column").value("READY"));
  }

  @Test
  void createShouldDefaultEmptyBodyAndBacklogColumn() throws Exception {
    given(createUseCase.execute(eq(SUB), eq("Title only"), eq(""), eq((KanbanColumn) null)))
        .willReturn(item(8L, KanbanColumn.BACKLOG, 0));

    mockMvc
        .perform(
            post("/api/kanban/items")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Title only\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(8));
  }

  @Test
  void createShouldReturn400WhenTitleMissing() throws Exception {
    mockMvc
        .perform(
            post("/api/kanban/items")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateShouldReturnUpdated() throws Exception {
    given(updateUseCase.execute(SUB, 5L, "Neu", "Body"))
        .willReturn(item(5L, KanbanColumn.BACKLOG, 0));

    mockMvc
        .perform(
            put("/api/kanban/items/5")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Neu\",\"body\":\"Body\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(5));
  }

  @Test
  void updateForeignItemShouldReturn404() throws Exception {
    willThrow(new KanbanItemNotFoundException(5L))
        .given(updateUseCase)
        .execute(any(), eq(5L), any(), any());

    mockMvc
        .perform(
            put("/api/kanban/items/5")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Neu\",\"body\":\"Body\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void moveShouldDelegateToUseCase() throws Exception {
    given(moveUseCase.execute(SUB, 9L, KanbanColumn.DONE, 0))
        .willReturn(item(9L, KanbanColumn.DONE, 0));

    mockMvc
        .perform(
            put("/api/kanban/items/9/move")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"column\":\"DONE\",\"position\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.column").value("DONE"));
  }

  @Test
  void moveShouldReturn400WhenNegativePosition() throws Exception {
    mockMvc
        .perform(
            put("/api/kanban/items/9/move")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"column\":\"DONE\",\"position\":-1}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void archiveShouldReturn204() throws Exception {
    mockMvc
        .perform(delete("/api/kanban/items/3").with(userJwt()))
        .andExpect(status().isNoContent());
  }

  @Test
  void archiveForeignShouldReturn404() throws Exception {
    willThrow(new KanbanItemNotFoundException(3L)).given(archiveUseCase).execute(any(), eq(3L));

    mockMvc.perform(delete("/api/kanban/items/3").with(userJwt())).andExpect(status().isNotFound());
  }

  @Test
  void forceDeleteShouldReturn204() throws Exception {
    mockMvc
        .perform(delete("/api/kanban/items/3/force").with(userJwt()))
        .andExpect(status().isNoContent());
  }

  @Test
  void forceDeleteForeignShouldReturn404() throws Exception {
    willThrow(new KanbanItemNotFoundException(3L)).given(forceDeleteUseCase).execute(any(), eq(3L));

    mockMvc
        .perform(delete("/api/kanban/items/3/force").with(userJwt()))
        .andExpect(status().isNotFound());
  }

  @Test
  void restoreShouldReturn200WithItem() throws Exception {
    given(restoreUseCase.execute(SUB, 5L)).willReturn(item(5L, KanbanColumn.BACKLOG, 0));

    mockMvc
        .perform(patch("/api/kanban/items/5/restore").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(5))
        .andExpect(jsonPath("$.archived").value(false));
  }

  @Test
  void restoreForeignShouldReturn404() throws Exception {
    willThrow(new KanbanItemNotFoundException(5L)).given(restoreUseCase).execute(any(), eq(5L));

    mockMvc
        .perform(patch("/api/kanban/items/5/restore").with(userJwt()))
        .andExpect(status().isNotFound());
  }

  @Test
  void archiveShouldReturn400WhenIdNotPositive() throws Exception {
    // @Min(1) auf @PathVariable id (#268): 0 darf die Geschäftslogik nicht erreichen.
    mockMvc
        .perform(delete("/api/kanban/items/0").with(userJwt()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateShouldReturn400WhenIdNotPositive() throws Exception {
    mockMvc
        .perform(
            put("/api/kanban/items/0")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\",\"body\":\"y\"}"))
        .andExpect(status().isBadRequest());
  }
}
