package org.mwolff.api.kanban.web;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.mwolff.api.kanban.application.CreateItemUseCase;
import org.mwolff.api.kanban.application.DeleteItemUseCase;
import org.mwolff.api.kanban.application.ListItemsUseCase;
import org.mwolff.api.kanban.application.MoveItemUseCase;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Adapter für Kanban-Items. Alle Endpoints sind durch {@code
 * SecurityConfig#requestMatchers("/api/kanban/**").hasRole("USER")} geschützt. Owner-Check erfolgt
 * in den Use-Cases via JWT-{@code sub}.
 */
@RestController
@RequestMapping("/api/kanban/items")
public class KanbanItemController {

  private final ListItemsUseCase listUseCase;
  private final CreateItemUseCase createUseCase;
  private final UpdateItemContentUseCase updateContentUseCase;
  private final MoveItemUseCase moveUseCase;
  private final DeleteItemUseCase deleteUseCase;

  public KanbanItemController(
      ListItemsUseCase listUseCase,
      CreateItemUseCase createUseCase,
      UpdateItemContentUseCase updateContentUseCase,
      MoveItemUseCase moveUseCase,
      DeleteItemUseCase deleteUseCase) {
    this.listUseCase = listUseCase;
    this.createUseCase = createUseCase;
    this.updateContentUseCase = updateContentUseCase;
    this.moveUseCase = moveUseCase;
    this.deleteUseCase = deleteUseCase;
  }

  @GetMapping
  public Map<KanbanColumn, List<KanbanItemResponse>> list(JwtAuthenticationToken auth) {
    final Map<KanbanColumn, List<KanbanItem>> grouped =
        listUseCase.execute(auth.getToken().getSubject());
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
            auth.getToken().getSubject(), body.title(), body.bodyOrEmpty(), body.column());
    return ResponseEntity.status(HttpStatus.CREATED).body(KanbanItemResponse.from(created));
  }

  @PutMapping("/{id}")
  public KanbanItemResponse updateContent(
      JwtAuthenticationToken auth,
      @PathVariable long id,
      @Valid @RequestBody UpdateKanbanItemRequest body) {
    return KanbanItemResponse.from(
        updateContentUseCase.execute(
            auth.getToken().getSubject(), id, body.title(), body.body()));
  }

  @PutMapping("/{id}/move")
  public KanbanItemResponse move(
      JwtAuthenticationToken auth,
      @PathVariable long id,
      @Valid @RequestBody MoveKanbanItemRequest body) {
    return KanbanItemResponse.from(
        moveUseCase.execute(auth.getToken().getSubject(), id, body.column(), body.position()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(JwtAuthenticationToken auth, @PathVariable long id) {
    deleteUseCase.execute(auth.getToken().getSubject(), id);
    return ResponseEntity.noContent().build();
  }
}
