package org.mwolff.api.auth.infrastructure;

import java.util.List;

import org.mwolff.api.kanban.web.KanbanTokenAuthFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security-Wiring der Auth-Komponente. Liegt in {@code infrastructure}, weil hier die JWT-/CORS-
 * Adapter konfiguriert werden und mit {@link JwtAuthoritiesConverter} ein reiner Mapping-Adapter
 * (Keycloak-Claims → Spring-Authorities) verdrahtet wird — keine REST-Transportlogik. Der einzige
 * Web-Adapter der Komponente, {@code MeController}, lebt getrennt in {@code auth.web}.
 */
@Configuration
public class SecurityConfig {

  /**
   * Default-Filter-Chain fuer alle Pfade ausser {@code /api/ingest/**} — JWT-basierte Auth fuer das
   * Web-UI. Die Ingest-Filter-Chain liegt in {@link org.mwolff.api.ingest.web.IngestSecurityConfig}
   * und greift via {@code @Order(1)} vorher.
   */
  @Bean
  @Order(2)
  SecurityFilterChain filterChain(
      final HttpSecurity http, final ObjectProvider<KanbanTokenAuthFilter> kanbanTokenAuthFilter)
      throws Exception {
    final JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new JwtAuthoritiesConverter());

    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/me")
                    .authenticated()
                    .requestMatchers("/api/dashboards/**")
                    .hasRole("USER")
                    // Tool-Endpoints: gleicher Auth-Gate wie Dashboards. Aktiviert in #65 nach
                    // Phase 0 (#36-#38) und dem hexagonalen Refactor (#68).
                    .requestMatchers("/api/tools/**")
                    .hasRole("USER")
                    // Kanban-Endpoints — Web-JWT (USER) ODER Board-PAT (KANBAN, #365). Der
                    // KanbanTokenAuthFilter setzt bei gueltigem X-Kanban-Token ROLE_KANBAN;
                    // /api/kanban-tokens/** bleibt bewusst USER-only (kein PAT-Self-Management).
                    .requestMatchers("/api/kanban/**")
                    .hasAnyRole("USER", "KANBAN")
                    // TimeSeries-Endpoints — gleicher Auth-Gate wie Dashboards (#90).
                    .requestMatchers("/api/timeseries/**")
                    .hasRole("USER")
                    // Ingest-Token-Verwaltung: JWT-User legt Tokens fuer Maschinen an (#92).
                    .requestMatchers("/api/ingest-tokens/**")
                    .hasRole("USER")
                    // Kanban-Access-Token-Verwaltung (#364): JWT-User legt Board-PATs an.
                    // Bewusst NICHT per PAT verwaltbar (Least Privilege) — die PAT-Auth (#365)
                    // gilt nur fuer /api/kanban/**, nicht fuer /api/kanban-tokens/**.
                    .requestMatchers("/api/kanban-tokens/**")
                    .hasRole("USER")
                    // Image-Store (#182): Upload UND Auslieferung nur fuer authentifizierte USER.
                    // Die Auslieferung ist bewusst auth-pflichtig — das Frontend laedt Bilder ueber
                    // den authentifizierten API-Client als Blob (kein direktes <img src>).
                    .requestMatchers("/api/images/**")
                    .hasRole("USER")
                    // OpenAPI-Doku (#166): Schema + Swagger-UI oeffentlich, damit Entwickler
                    // ohne Token-Fummelei darauf zugreifen koennen. Es wird nur die
                    // Dokumentation (Endpoint-Liste + Schemas) oeffentlich — die Endpoints
                    // selbst bleiben auth-geschuetzt, die Daten bleiben sicher.
                    // Springdoc-Pfade laut application.yml: /api/openapi (Spec) und
                    // /api/swagger-ui.html (UI).
                    .requestMatchers(
                        "/api/openapi/**", "/api/swagger-ui/**", "/api/swagger-ui.html")
                    .permitAll()
                    // App-Version (#229): GET nur fuer eingeloggte USER (wird im Header
                    // angezeigt). Die mutierenden POSTs sind bewusst permitAll auf
                    // Security-Ebene — sie werden im Controller per Shared-Secret-Header
                    // geschuetzt, weil das Deploy-Skript (#225) kein JWT mitbringt.
                    .requestMatchers(HttpMethod.GET, "/api/app/version")
                    .hasRole("USER")
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/app/version/increment-minor",
                        "/api/app/version/increment-major")
                    .permitAll()
                    // Actuator-Health bleibt fuer Container-Healthchecks oeffentlich.
                    .requestMatchers("/actuator/health/**")
                    .permitAll()
                    // #229: Default-Deny fuer alle uebrigen /api/**-Routen — neue Endpoints
                    // sind secure-by-default und muessen explizit oben freigeschaltet werden.
                    .requestMatchers("/api/**")
                    .denyAll()
                    // SPA-Forwarding (statisches + Root, kein /api) bleibt oeffentlich —
                    // Auth-Gate ist im Frontend.
                    .anyRequest()
                    .permitAll())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
    // Board-PAT (#365): additiv vor dem AnonymousAuthenticationFilter. Der Filter lebt im
    // Kanban-Modul; via ObjectProvider bleibt diese Chain in Controller-Slice-Tests ohne den
    // Bean lauffaehig (dort wird kein PAT verdrahtet), waehrend die Produktion ihn einhaengt.
    kanbanTokenAuthFilter.ifAvailable(
        filter -> http.addFilterBefore(filter, AnonymousAuthenticationFilter.class));
    return http.build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    final CorsConfiguration configuration = new CorsConfiguration();
    // Erlaubte Origins: Dev (Vite/Spring lokal) + Production. Same-origin-Requests
    // im Browser bei PUT/POST mit JSON-Body senden trotzdem den `Origin`-Header und
    // gehen durch diesen Filter — fehlt Production hier, lehnt Spring mit
    // "Invalid CORS request" und HTTP 403 ab (auch wenn das Frontend
    // dieselbe Domain bedient).
    configuration.setAllowedOrigins(
        List.of("http://localhost:5173", "http://localhost:8080", "https://toolbox.mwolff.org"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        List.of("Authorization", "Content-Type", "Accept", "X-Ingest-Token", "X-Kanban-Token"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }
}
