package org.mwolff.api.kanban.web;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.mwolff.api.kanban.application.ArchiveItemUseCase;
import org.mwolff.api.kanban.application.CreateItemUseCase;
import org.mwolff.api.kanban.application.ForceDeleteItemUseCase;
import org.mwolff.api.kanban.application.ListItemsUseCase;
import org.mwolff.api.kanban.application.MoveItemUseCase;
import org.mwolff.api.kanban.application.RestoreItemUseCase;
import org.mwolff.api.kanban.application.UpdateItemContentUseCase;
import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.web.dto.CreateKanbanItemRequest;
import org.mwolff.api.kanban.web.dto.KanbanItemResponse;
import org.mwolff.api.kanban.web.dto.MoveKanbanItemRequest;
import org.mwolff.api.kanban.web.dto.UpdateKanbanItemRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Adapter für Kanban-Items. Alle Endpoints sind durch {@code
 * SecurityConfig#requestMatchers("/api/kanban/**").hasRole("USER")} geschützt. Owner-Check erfolgt
 * in den Use-Cases via JWT-{@code sub}.
 *
 * <p>Löschen = Soft-Delete (archivieren). Physisches Löschen nur über {@code DELETE /{id}/force}.
 */
@RestController
@RequestMapping("/api/kanban/items")
@Validated
public class KanbanItemController {

  private final ListItemsUseCase listUseCase;
  private final CreateItemUseCase createUseCase;
  private final UpdateItemContentUseCase updateContentUseCase;
  private final MoveItemUseCase moveUseCase;
  private final ArchiveItemUseCase archiveUseCase;
  private final ForceDeleteItemUseCase forceDeleteUseCase;
  private final RestoreItemUseCase restoreUseCase;

  public KanbanItemController(
      ListItemsUseCase listUseCase,
      CreateItemUseCase createUseCase,
      UpdateItemContentUseCase updateContentUseCase,
      MoveItemUseCase moveUseCase,
      ArchiveItemUseCase archiveUseCase,
      ForceDeleteItemUseCase forceDeleteUseCase,
      RestoreItemUseCase restoreUseCase) {
    this.listUseCase = listUseCase;
    this.createUseCase = createUseCase;
    this.updateContentUseCase = updateContentUseCase;
    this.moveUseCase = moveUseCase;
    this.archiveUseCase = archiveUseCase;
    this.forceDeleteUseCase = forceDeleteUseCase;
    this.restoreUseCase = restoreUseCase;
  }

  @GetMapping
  public Map<KanbanColumn, List<KanbanItemResponse>> list(
      JwtAuthenticationToken auth, @RequestParam(defaultValue = "false") boolean includeArchived) {
    final Map<KanbanColumn, List<KanbanItem>> grouped =
        listUseCase.execute(auth.getToken().getSubject(), includeArchived);
    final Map<KanbanColumn, List<KanbanItemResponse>> out = new EnumMap<>(KanbanColumn.class);
    for (final KanbanColumn col : KanbanColumn.values()) {
      out.put(col, grouped.get(col).stream().map(KanbanItemResponse::from).toList());
    }
    return out;
  }

  @PostMapping
  public ResponseEntity<KanbanItemResponse> create(
      JwtAuthenticationToken auth, @Valid @RequestBody CreateKanbanItemRequest body) {
    final KanbanItem created =
        createUseCase.execute(
            auth.getToken().getSubject(),
            body.title(),
            body.bodyOrEmpty(),
            body.column(),
            body.typeOrDefault(),
            body.parentId(),
            body.shortcode());
    return ResponseEntity.status(HttpStatus.CREATED).body(KanbanItemResponse.from(created));
  }

  @PutMapping("/{id}")
  public KanbanItemResponse updateContent(
      JwtAuthenticationToken auth,
      @PathVariable @Min(1) long id,
      @Valid @RequestBody UpdateKanbanItemRequest body) {
    return KanbanItemResponse.from(
        updateContentUseCase.execute(
            auth.getToken().getSubject(),
            id,
            body.title(),
            body.body(),
            body.shortcode(),
            body.parentId()));
  }

  @PutMapping("/{id}/move")
  public KanbanItemResponse move(
      JwtAuthenticationToken auth,
      @PathVariable @Min(1) long id,
      @Valid @RequestBody MoveKanbanItemRequest body) {
    return KanbanItemResponse.from(
        moveUseCase.execute(auth.getToken().getSubject(), id, body.column(), body.position()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> archive(JwtAuthenticationToken auth, @PathVariable @Min(1) long id) {
    archiveUseCase.execute(auth.getToken().getSubject(), id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}/force")
  public ResponseEntity<Void> forceDelete(
      JwtAuthenticationToken auth, @PathVariable @Min(1) long id) {
    forceDeleteUseCase.execute(auth.getToken().getSubject(), id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/restore")
  public KanbanItemResponse restore(JwtAuthenticationToken auth, @PathVariable @Min(1) long id) {
    return KanbanItemResponse.from(restoreUseCase.execute(auth.getToken().getSubject(), id));
  }
}
