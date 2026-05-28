package org.mwolff.api.ingest.web;

import java.util.List;

import jakarta.validation.Valid;

import org.mwolff.api.ingest.application.CreateIngestTokenUseCase;
import org.mwolff.api.ingest.application.ListIngestTokensUseCase;
import org.mwolff.api.ingest.application.RevokeIngestTokenUseCase;
import org.mwolff.api.ingest.web.dto.CreateIngestTokenRequest;
import org.mwolff.api.ingest.web.dto.CreatedIngestTokenResponse;
import org.mwolff.api.ingest.web.dto.IngestTokenSummaryResponse;
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
 * Verwaltung der Ingest-Tokens. JWT-geschuetzt (Rolle {@code USER}) — der User legt Tokens an, der
 * Plaintext wird ausschliesslich in der POST-Antwort geliefert.
 */
@RestController
@RequestMapping("/api/ingest-tokens")
public class IngestTokenController {

  private final ListIngestTokensUseCase listUseCase;
  private final CreateIngestTokenUseCase createUseCase;
  private final RevokeIngestTokenUseCase revokeUseCase;

  public IngestTokenController(
      ListIngestTokensUseCase listUseCase,
      CreateIngestTokenUseCase createUseCase,
      RevokeIngestTokenUseCase revokeUseCase) {
    this.listUseCase = listUseCase;
    this.createUseCase = createUseCase;
    this.revokeUseCase = revokeUseCase;
  }

  @GetMapping
  public List<IngestTokenSummaryResponse> list(JwtAuthenticationToken auth) {
    return listUseCase.execute(auth.getToken().getSubject()).stream()
        .map(IngestTokenSummaryResponse::from)
        .toList();
  }

  @PostMapping
  public ResponseEntity<CreatedIngestTokenResponse> create(
      JwtAuthenticationToken auth, @Valid @RequestBody CreateIngestTokenRequest body) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            CreatedIngestTokenResponse.from(
                createUseCase.execute(auth.getToken().getSubject(), body.name())));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> revoke(JwtAuthenticationToken auth, @PathVariable long id) {
    revokeUseCase.execute(auth.getToken().getSubject(), id);
    return ResponseEntity.noContent().build();
  }
}
