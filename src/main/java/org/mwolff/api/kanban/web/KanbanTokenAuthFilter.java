package org.mwolff.api.kanban.web;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.mwolff.api.kanban.application.ResolveKanbanTokenUseCase;
import org.mwolff.api.kanban.domain.InvalidKanbanTokenException;
import org.mwolff.api.kanban.domain.KanbanAccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Liest den {@code X-Kanban-Token}-Header (PAT) und legt — bei gueltigem Token — ein synthetisches
 * {@link JwtAuthenticationToken} mit der Authority {@code ROLE_KANBAN} in den {@code
 * SecurityContext}. Bei ungueltigem/fehlendem Token bleibt der Context leer; die nachfolgende
 * {@code authorizeHttpRequests}-Regel lehnt dann ab.
 *
 * <p>Bewusst ein {@code JwtAuthenticationToken} (statt einer eigenen Authentication-Klasse wie im
 * Ingest-Modul): So funktionieren die bestehenden Kanban-Controller unveraendert weiter — sie lesen
 * den Eigentuemer ueber {@code auth.getToken().getSubject()} und den Autor-Namen ueber den {@code
 * preferred_username}-Claim (#365). Der synthetische {@link Jwt} traegt {@code sub = userSub} und
 * {@code preferred_username = displayName} des Tokens.
 *
 * <p>Der PAT wird bewusst NICHT im {@code Authorization: Bearer}-Header erwartet: dort wuerde der
 * Resource-Server-BearerFilter {@code tk_…} als JWT zu dekodieren versuchen und mit 401 abbrechen.
 * Deshalb der eigene Header (spiegelt {@code X-Ingest-Token}).
 *
 * <p>Bewusst <strong>kein</strong> {@code @Component}: sonst wuerde jeder {@code @WebMvcTest} den
 * Filter (als {@code Filter}-Bean) instanziieren und dabei den {@code ResolveKanbanTokenUseCase}
 * verlangen. Der Filter wird stattdessen in {@link KanbanSecurityConfig} als Bean bereitgestellt
 * (spiegelt {@code IngestSecurityConfig}) und in der Default-Chain per {@code ObjectProvider}
 * eingehaengt.
 */
public class KanbanTokenAuthFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(KanbanTokenAuthFilter.class);

  /** HTTP-Header, ueber den der Plaintext-Token gesendet wird. */
  public static final String HEADER = "X-Kanban-Token";

  private final ResolveKanbanTokenUseCase resolveUseCase;

  public KanbanTokenAuthFilter(ResolveKanbanTokenUseCase resolveUseCase) {
    this.resolveUseCase = resolveUseCase;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    final String header = request.getHeader(HEADER);
    if (header != null && !header.isBlank()) {
      try {
        final KanbanAccessToken token = resolveUseCase.execute(header);
        final Jwt jwt =
            Jwt.withTokenValue("kanban-pat")
                .header("alg", "none")
                .subject(token.userSub())
                .claim("preferred_username", token.displayName())
                .build();
        final JwtAuthenticationToken auth =
            new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_KANBAN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
      } catch (InvalidKanbanTokenException ex) {
        // Bewusst kein Token-Wert ins Log — nur die IP zur Forensik.
        LOG.warn(
            "Rejected kanban token auth from {}: {}", request.getRemoteAddr(), ex.getMessage());
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request, response);
  }
}
