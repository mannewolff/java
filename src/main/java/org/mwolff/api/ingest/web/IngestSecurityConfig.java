package org.mwolff.api.ingest.web;

import org.mwolff.api.ingest.application.ResolveIngestTokenUseCase;
import org.mwolff.api.ingest.infrastructure.ratelimit.IngestRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * Separate Filter-Chain fuer den oeffentlichen Ingest-Endpoint. Bewusst aus der zentralen {@code
 * SecurityConfig} herausgeloest, damit Slice-Tests anderer Controller (Dashboard, Kanban,
 * TimeSeries) keine Ingest-Beans wiren muessen.
 *
 * <p>{@link IngestTokenAuthFilter} liest {@code X-Ingest-Token} und legt eine {@link
 * IngestTokenAuthentication} in den Context. Danach folgt {@link IngestRateLimitFilter} mit 60
 * req/min (konfigurierbar). Ohne Auth oder bei Limit-Ueberschreitung wird abgelehnt.
 */
@Configuration
public class IngestSecurityConfig {

  @Bean
  @Order(1)
  public SecurityFilterChain ingestFilterChain(
      final HttpSecurity http,
      final ResolveIngestTokenUseCase resolveUseCase,
      final IngestRateLimiter rateLimiter)
      throws Exception {
    http.securityMatcher("/api/ingest/**")
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(AbstractHttpConfigurer::disable)
        .addFilterBefore(
            new IngestTokenAuthFilter(resolveUseCase), AnonymousAuthenticationFilter.class)
        .addFilterAfter(new IngestRateLimitFilter(rateLimiter), IngestTokenAuthFilter.class)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/ingest/**").hasRole("INGEST").anyRequest().denyAll());
    return http.build();
  }
}
