package org.mwolff.api.ingest.web;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.mwolff.api.ingest.infrastructure.ratelimit.IngestRateLimiter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtert authentifizierte Ingest-Requests nach Rate-Limit (60 req/min pro Token, konfigurierbar).
 * Wird hinter {@link IngestTokenAuthFilter} geschaltet, damit der Schluessel der Token-Inhaber ist;
 * ohne Auth fallback auf die IP.
 */
public class IngestRateLimitFilter extends OncePerRequestFilter {

  private final IngestRateLimiter rateLimiter;

  public IngestRateLimitFilter(IngestRateLimiter rateLimiter) {
    this.rateLimiter = rateLimiter;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    final String key = resolveKey(request);
    if (!rateLimiter.tryAcquire(key)) {
      response.setStatus(429);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\":\"rate limit exceeded\"}");
      return;
    }
    chain.doFilter(request, response);
  }

  private static String resolveKey(HttpServletRequest request) {
    final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof IngestTokenAuthentication ingestAuth && ingestAuth.getTokenId() != null) {
      return "token:" + ingestAuth.getTokenId();
    }
    return "ip:" + request.getRemoteAddr();
  }
}
