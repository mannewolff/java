package org.mwolff.api.kanban.web;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.mwolff.api.kanban.application.AddCommentUseCase;
import org.mwolff.api.kanban.application.DeleteCommentUseCase;
import org.mwolff.api.kanban.application.ListCommentsUseCase;
import org.mwolff.api.kanban.application.UpdateCommentUseCase;
import org.mwolff.api.kanban.web.dto.KanbanCommentRequest;
import org.mwolff.api.kanban.web.dto.KanbanCommentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Adapter für Kanban-Kommentare. Geschützt durch {@code
 * SecurityConfig#requestMatchers("/api/kanban/**").hasRole("USER")}. Item-Eigentum prüfen die
 * Use-Cases via JWT-{@code sub}; der Autor eines Kommentars ist der {@code preferred_username}.
 */
@RestController
@RequestMapping("/api/kanban/items/{itemId}/comments")
@Validated
public class KanbanCommentController {

  private final ListCommentsUseCase listUseCase;
  private final AddCommentUseCase addUseCase;
  private final UpdateCommentUseCase updateUseCase;
  private final DeleteCommentUseCase deleteUseCase;

  public KanbanCommentController(
      ListCommentsUseCase listUseCase,
      AddCommentUseCase addUseCase,
      UpdateCommentUseCase updateUseCase,
      DeleteCommentUseCase deleteUseCase) {
    this.listUseCase = listUseCase;
    this.addUseCase = addUseCase;
    this.updateUseCase = updateUseCase;
    this.deleteUseCase = deleteUseCase;
  }

  @GetMapping
  public List<KanbanCommentResponse> list(JwtAuthenticationToken auth, @PathVariable @Min(1) long itemId) {
    return listUseCase.execute(auth.getToken().getSubject(), itemId).stream()
        .map(KanbanCommentResponse::from)
        .toList();
  }

  @PostMapping
  public ResponseEntity<KanbanCommentResponse> create(
      JwtAuthenticationToken auth,
      @PathVariable @Min(1) long itemId,
      @Valid @RequestBody KanbanCommentRequest body) {
    final KanbanCommentResponse created =
        KanbanCommentResponse.from(
            addUseCase.execute(auth.getToken().getSubject(), author(auth), itemId, body.body()));
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PutMapping("/{commentId}")
  public KanbanCommentResponse update(
      JwtAuthenticationToken auth,
      @PathVariable @Min(1) long itemId,
      @PathVariable @Min(1) long commentId,
      @Valid @RequestBody KanbanCommentRequest body) {
    return KanbanCommentResponse.from(
        updateUseCase.execute(
            auth.getToken().getSubject(), author(auth), itemId, commentId, body.body()));
  }

  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> delete(
      JwtAuthenticationToken auth, @PathVariable @Min(1) long itemId, @PathVariable @Min(1) long commentId) {
    deleteUseCase.execute(auth.getToken().getSubject(), author(auth), itemId, commentId);
    return ResponseEntity.noContent().build();
  }

  private static String author(JwtAuthenticationToken auth) {
    return auth.getToken().getClaimAsString("preferred_username");
  }
}
