package org.mwolff.api.kanban.web;

import java.util.List;

import jakarta.validation.Valid;

import org.mwolff.api.kanban.application.CreateKanbanTokenUseCase;
import org.mwolff.api.kanban.application.ListKanbanTokensUseCase;
import org.mwolff.api.kanban.application.RevokeKanbanTokenUseCase;
import org.mwolff.api.kanban.web.dto.CreateKanbanTokenRequest;
import org.mwolff.api.kanban.web.dto.CreatedKanbanTokenResponse;
import org.mwolff.api.kanban.web.dto.KanbanTokenSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verwaltung der Kanban-Access-Tokens. JWT-geschuetzt (Rolle {@code USER}) — der User legt Tokens
 * an, der Plaintext wird ausschliesslich in der POST-Antwort geliefert. Dieser Endpoint ist bewusst
 * NICHT per PAT (X-Kanban-Token) verwaltbar (Least Privilege, siehe #365).
 */
@RestController
@RequestMapping("/api/kanban-tokens")
public class KanbanTokenController {

  private final ListKanbanTokensUseCase listUseCase;
  private final CreateKanbanTokenUseCase createUseCase;
  private final RevokeKanbanTokenUseCase revokeUseCase;

  public KanbanTokenController(
      ListKanbanTokensUseCase listUseCase,
      CreateKanbanTokenUseCase createUseCase,
      RevokeKanbanTokenUseCase revokeUseCase) {
    this.listUseCase = listUseCase;
    this.createUseCase = createUseCase;
    this.revokeUseCase = revokeUseCase;
  }

  @GetMapping
  public List<KanbanTokenSummaryResponse> list(JwtAuthenticationToken auth) {
    return listUseCase.execute(auth.getToken().getSubject()).stream()
        .map(KanbanTokenSummaryResponse::from)
        .toList();
  }

  @PostMapping
  public ResponseEntity<CreatedKanbanTokenResponse> create(
      JwtAuthenticationToken auth, @Valid @RequestBody CreateKanbanTokenRequest body) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            CreatedKanbanTokenResponse.from(
                createUseCase.execute(
                    auth.getToken().getSubject(), displayName(auth), body.name())));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> revoke(JwtAuthenticationToken auth, @PathVariable long id) {
    revokeUseCase.execute(auth.getToken().getSubject(), id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Anzeigename des Token-Eigentuemers: {@code preferred_username}, oder — falls dieser Claim fehlt
   * — der stabile {@code sub}. Der Kanban-Auth-Filter (#365) setzt diesen Wert spaeter als {@code
   * preferred_username}-Claim, damit token-erzeugte Kommentare einen Autor-Namen tragen.
   */
  private static String displayName(JwtAuthenticationToken auth) {
    final String preferredUsername = auth.getToken().getClaimAsString("preferred_username");
    if (preferredUsername != null && !preferredUsername.isBlank()) {
      return preferredUsername;
    }
    return auth.getToken().getSubject();
  }
}
