package org.mwolff.api.ingest.web;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.mwolff.api.ingest.application.ResolveIngestTokenUseCase;
import org.mwolff.api.ingest.domain.IngestToken;
import org.mwolff.api.ingest.domain.InvalidIngestTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Liest den {@code X-Ingest-Token}-Header und legt — bei gueltigem Token — eine {@link
 * IngestTokenAuthentication} in den {@code SecurityContext}. Bei ungueltigem/fehlendem Token bleibt
 * der Context leer; die nachfolgende {@code authorizeHttpRequests}-Regel lehnt dann mit 401 ab.
 */
public class IngestTokenAuthFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(IngestTokenAuthFilter.class);

  /** HTTP-Header, ueber den der Plaintext-Token gesendet wird. */
  public static final String HEADER = "X-Ingest-Token";

  private final ResolveIngestTokenUseCase resolveUseCase;

  public IngestTokenAuthFilter(ResolveIngestTokenUseCase resolveUseCase) {
    this.resolveUseCase = resolveUseCase;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    final String header = request.getHeader(HEADER);
    if (header != null && !header.isBlank()) {
      try {
        final IngestToken token = resolveUseCase.execute(header);
        final IngestTokenAuthentication auth =
            new IngestTokenAuthentication(token.userSub(), token.id());
        SecurityContextHolder.getContext().setAuthentication(auth);
      } catch (InvalidIngestTokenException ex) {
        // Bewusst kein Token-Wert ins Log — nur die IP zur Forensik. Fehlerbild fuer Operations.
        LOG.warn("Rejected ingest auth from {}: {}", request.getRemoteAddr(), ex.getMessage());
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request, response);
  }
}
