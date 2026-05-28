package org.mwolff.api.auth;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
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
                    // Kanban-Endpoints — gleicher Auth-Gate wie Dashboards (#99).
                    .requestMatchers("/api/kanban/**")
                    .hasRole("USER")
                    // TimeSeries-Endpoints — gleicher Auth-Gate wie Dashboards (#90).
                    .requestMatchers("/api/timeseries/**")
                    .hasRole("USER")
                    // Actuator-Health bleibt fuer Container-Healthchecks oeffentlich.
                    .requestMatchers("/actuator/health/**")
                    .permitAll()
                    // SPA-Forwarding (statisches + Root) bleibt oeffentlich — Auth-Gate ist im
                    // Frontend.
                    .anyRequest()
                    .permitAll())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
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
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }
}
