package org.mwolff.api.kanban.web;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import org.mwolff.api.kanban.domain.KanbanItemType;
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

  private static KanbanItem archivedItem(long id, KanbanColumn column, int position) {
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
        true,
        0);
  }

  @Test
  void listShouldReturnGroupedByColumn() throws Exception {
    final Map<KanbanColumn, List<KanbanItem>> grouped = new EnumMap<>(KanbanColumn.class);
    for (KanbanColumn c : KanbanColumn.values()) grouped.put(c, List.of());
    grouped.put(KanbanColumn.BACKLOG, List.of(item(1L, KanbanColumn.BACKLOG, 0)));
    given(listUseCase.execute(SUB, false)).willReturn(grouped);

    mockMvc
        .perform(get("/api/kanban/items").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.BACKLOG").isArray())
        .andExpect(jsonPath("$.BACKLOG[0].id").value(1))
        // Epics-Fundament (#321): Typ und Epic-Zuordnung sind Teil des Wire-Formats.
        .andExpect(jsonPath("$.BACKLOG[0].type").value("ITEM"))
        .andExpect(jsonPath("$.BACKLOG[0].parentId").value(nullValue()))
        .andExpect(jsonPath("$.READY").isEmpty())
        .andExpect(jsonPath("$.IN_PROGRESS").isEmpty())
        .andExpect(jsonPath("$.IN_REVIEW").isEmpty())
        .andExpect(jsonPath("$.DONE").isEmpty());
  }

  @Test
  void listWithIncludeArchivedTrueShouldPassFlagToUseCase() throws Exception {
    final Map<KanbanColumn, List<KanbanItem>> grouped = new EnumMap<>(KanbanColumn.class);
    for (KanbanColumn c : KanbanColumn.values()) grouped.put(c, List.of());
    grouped.put(KanbanColumn.DONE, List.of(archivedItem(2L, KanbanColumn.DONE, 0)));
    given(listUseCase.execute(SUB, true)).willReturn(grouped);

    mockMvc
        .perform(get("/api/kanban/items?includeArchived=true").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.DONE[0].id").value(2))
        .andExpect(jsonPath("$.DONE[0].archived").value(true));
  }

  @Test
  void listWithoutIncludeArchivedParamShouldDefaultToFalse() throws Exception {
    final Map<KanbanColumn, List<KanbanItem>> grouped = new EnumMap<>(KanbanColumn.class);
    for (KanbanColumn c : KanbanColumn.values()) grouped.put(c, List.of());
    given(listUseCase.execute(SUB, false)).willReturn(grouped);

    mockMvc.perform(get("/api/kanban/items").with(userJwt())).andExpect(status().isOk());
  }

  @Test
  void listWithInvalidIncludeArchivedValueShouldReturnConsistentErrorFormat() throws Exception {
    // Issue #297: ein von Spring nicht als boolean interpretierbarer Wert fiel bisher auf
    // Springs generisches Default-Fehlerformat zurueck statt auf das API-eigene JSON-Format.
    // "yes"/"no"/"on"/"off"/"1"/"0" werden von Springs StringToBooleanConverter bereits als
    // true/false erkannt — erst ein wirklich unbekannter Wert loest den Type-Mismatch aus.
    mockMvc
        .perform(get("/api/kanban/items?includeArchived=maybe").with(userJwt()))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message").value("Ungültiger Wert 'maybe' für Parameter 'includeArchived'"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void listWithoutJwtShouldReturn401() throws Exception {
    mockMvc.perform(get("/api/kanban/items")).andExpect(status().isUnauthorized());
  }

  @Test
  void createShouldReturn201() throws Exception {
    given(
            createUseCase.execute(
                eq(SUB),
                eq("Neu"),
                eq("b"),
                eq(KanbanColumn.BACKLOG),
                eq(KanbanItemType.ITEM),
                isNull(),
                isNull()))
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
  void createShouldReturn409OnConcurrentConstraintViolation() throws Exception {
    // Paralleler Create kollidiert am Unique-Constraint (#309) — der Handler mappt das auf 409
    // Conflict statt eines intransparenten 500.
    given(
            createUseCase.execute(
                eq(SUB),
                eq("Neu"),
                eq("b"),
                eq(KanbanColumn.BACKLOG),
                eq(KanbanItemType.ITEM),
                isNull(),
                isNull()))
        .willThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate"));

    mockMvc
        .perform(
            post("/api/kanban/items")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Neu\",\"body\":\"b\",\"column\":\"BACKLOG\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void createShouldAcceptReadyColumn() throws Exception {
    given(
            createUseCase.execute(
                eq(SUB),
                eq("Neu"),
                eq("b"),
                eq(KanbanColumn.READY),
                eq(KanbanItemType.ITEM),
                isNull(),
                isNull()))
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
    given(
            createUseCase.execute(
                eq(SUB),
                eq("Title only"),
                eq(""),
                eq((KanbanColumn) null),
                eq(KanbanItemType.ITEM),
                isNull(),
                isNull()))
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
  void createShouldPassTypeAndParentId() throws Exception {
    given(
            createUseCase.execute(
                eq(SUB),
                eq("Story"),
                eq(""),
                eq((KanbanColumn) null),
                eq(KanbanItemType.ITEM),
                eq(42L),
                isNull()))
        .willReturn(item(12L, KanbanColumn.BACKLOG, 0));

    mockMvc
        .perform(
            post("/api/kanban/items")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Story\",\"type\":\"ITEM\",\"parentId\":42}"))
        .andExpect(status().isCreated());
  }

  @Test
  void createEpicShouldPassShortcode() throws Exception {
    given(
            createUseCase.execute(
                eq(SUB),
                eq("Workshop"),
                eq(""),
                eq((KanbanColumn) null),
                eq(KanbanItemType.EPIC),
                isNull(),
                eq("ITB")))
        .willReturn(item(13L, KanbanColumn.BACKLOG, 0));

    mockMvc
        .perform(
            post("/api/kanban/items")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Workshop\",\"type\":\"EPIC\",\"shortcode\":\"ITB\"}"))
        .andExpect(status().isCreated());
  }

  @Test
  void createShouldReturn400WhenParentInvalid() throws Exception {
    // Use-Case lehnt eine ungültige Epic-Zuordnung ab → 400 (KanbanExceptionHandler, #321/#322).
    willThrow(new IllegalArgumentException("parent 42 is not an epic"))
        .given(createUseCase)
        .execute(eq(SUB), any(), any(), any(), eq(KanbanItemType.ITEM), eq(42L), isNull());

    mockMvc
        .perform(
            post("/api/kanban/items")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Story\",\"parentId\":42}"))
        .andExpect(status().isBadRequest());
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
  void moveShouldReturn400WhenTargetIsEpic() throws Exception {
    // Epics nehmen nicht am Spalten-Workflow teil (#321): der Use-Case-Guard wird vom
    // KanbanExceptionHandler auf 400 gemappt (statt 500).
    willThrow(new IllegalArgumentException("epics cannot be moved on the board"))
        .given(moveUseCase)
        .execute(SUB, 9L, KanbanColumn.DONE, 0);

    mockMvc
        .perform(
            put("/api/kanban/items/9/move")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"column\":\"DONE\",\"position\":0}"))
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
