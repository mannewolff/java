package org.mwolff.api.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mwolff.api.auth.infrastructure.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@WebMvcTest(controllers = MeController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private CorsConfigurationSource corsConfigurationSource;

  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void shouldDenyAnonymousAccessToDashboards() throws Exception {
    mockMvc.perform(get("/api/dashboards/some-id")).andExpect(status().isUnauthorized());
  }

  @Test
  void shouldRejectDashboardsAccessForUserWithoutUserRole() throws Exception {
    mockMvc
        .perform(
            get("/api/dashboards/some-id")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PENDING"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldAllowDashboardsAccessForUserWithUserRole() throws Exception {
    // Endpoint existiert (noch) nicht — Akzeptanzkriterium fuer den Security-Filter ist,
    // dass Auth durchgeht. Der DispatcherServlet liefert in diesem Fall 404, NICHT 401/403.
    mockMvc
        .perform(
            get("/api/dashboards/some-id")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldDenyAnonymousAccessToTools() throws Exception {
    // Seit #65 ist /api/tools/** auf hasRole(USER) — anonym -> 401.
    mockMvc.perform(get("/api/tools/foo")).andExpect(status().isUnauthorized());
  }

  @Test
  void shouldRejectToolsAccessForUserWithoutUserRole() throws Exception {
    // PENDING-User (registriert, aber noch nicht promotet) hat keine USER-Rolle -> 403.
    mockMvc
        .perform(
            get("/api/tools/foo")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PENDING"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldAllowToolsAccessForUserWithUserRole() throws Exception {
    // Mit USER-Rolle: Auth geht durch, MockMvc liefert 404 (kein Mapping in diesem Slice-Test).
    mockMvc
        .perform(
            get("/api/tools/foo").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldAllowAnonymousAccessToOpenApiSpec() throws Exception {
    // #166: Die OpenAPI-Spec ist oeffentlich. Ohne Token geht Auth durch — im Slice-Test
    // ohne Springdoc-Mapping liefert der DispatcherServlet 404 (NICHT 401/403).
    mockMvc.perform(get("/api/openapi")).andExpect(status().isNotFound());
  }

  @Test
  void shouldAllowAnonymousAccessToSwaggerUiHtml() throws Exception {
    // #166: Swagger-UI ist oeffentlich (404 statt 401 = erlaubt im Slice).
    mockMvc.perform(get("/api/swagger-ui.html")).andExpect(status().isNotFound());
  }

  @Test
  void shouldAllowAnonymousAccessToSwaggerUiResources() throws Exception {
    // #166: Swagger-UI-Ressourcen unter /api/swagger-ui/** sind oeffentlich.
    mockMvc.perform(get("/api/swagger-ui/index.html")).andExpect(status().isNotFound());
  }

  @Test
  void shouldRejectExpiredOrInvalidJwtWithUnauthorized() throws Exception {
    // given — Mock-Decoder simuliert einen abgelaufenen / invaliden Token.
    // BadJwtException ist die Resource-Server-spezifische Variante, die in 401 uebersetzt wird
    // (statt 500, wie es bei generischen JwtException der Fall waere).
    when(jwtDecoder.decode(eq("expired-token")))
        .thenThrow(new BadJwtException("Jwt expired at 1970-01-01T00:00:00Z"));

    // when + then
    mockMvc
        .perform(
            get("/api/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer expired-token")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void corsConfigurationShouldAllowFrontendDevServerOriginForApiPath() {
    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/me");

    final CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(request);

    assertThat(config).isNotNull();
    assertThat(config.getAllowedOrigins())
        .containsExactlyInAnyOrder(
            "http://localhost:5173", "http://localhost:8080", "https://toolbox.mwolff.org");
    assertThat(config.getAllowedMethods())
        .containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "OPTIONS");
    assertThat(config.getAllowedHeaders())
        .containsExactlyInAnyOrder("Authorization", "Content-Type", "Accept", "X-Ingest-Token");
    assertThat(config.getAllowCredentials()).isTrue();
    assertThat(config.getMaxAge()).isEqualTo(3600L);
  }

  @Test
  void corsConfigurationShouldNotApplyOutsideApiPath() {
    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/index.html");

    final CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(request);

    assertThat(config).isNull();
  }
}
