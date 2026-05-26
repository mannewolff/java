package org.mwolff.api.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MeController.class)
@Import(SecurityConfig.class)
class MeControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldReturnUnauthorizedWhenNoJwtPresent() throws Exception {
    mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void shouldExposeMappedClaimsForAuthenticatedUser() throws Exception {
    mockMvc
        .perform(
            get("/api/me")
                .with(
                    jwt()
                        .jwt(
                            builder ->
                                builder
                                    .subject("sub-42")
                                    .claim("preferred_username", "alice")
                                    .claim("email", "alice@example.com")
                                    .claim("given_name", "Alice")
                                    .claim("family_name", "Example")
                                    .claim("realm_access", Map.of("roles", List.of("USER"))))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.subject").value("sub-42"))
        .andExpect(jsonPath("$.username").value("alice"))
        .andExpect(jsonPath("$.email").value("alice@example.com"))
        .andExpect(jsonPath("$.givenName").value("Alice"))
        .andExpect(jsonPath("$.familyName").value("Example"))
        .andExpect(jsonPath("$.roles[0]").value("USER"));
  }

  @Test
  void shouldReturnNullValuesForMissingOptionalClaims() throws Exception {
    mockMvc
        .perform(get("/api/me").with(jwt().jwt(builder -> builder.subject("sub-only"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.subject").value("sub-only"))
        .andExpect(jsonPath("$.username").doesNotExist())
        .andExpect(jsonPath("$.email").doesNotExist())
        .andExpect(jsonPath("$.givenName").doesNotExist())
        .andExpect(jsonPath("$.familyName").doesNotExist())
        .andExpect(jsonPath("$.roles").isEmpty());
  }

  @Test
  void shouldReturnEmptyRolesWhenRealmAccessClaimMissing() throws Exception {
    mockMvc
        .perform(
            get("/api/me")
                .with(
                    jwt()
                        .jwt(
                            builder ->
                                builder
                                    .subject("sub-no-realm-access")
                                    .claim("preferred_username", "bob"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles").isEmpty());
  }

  @Test
  void shouldReturnEmptyRolesWhenRealmAccessHasNoRolesList() throws Exception {
    mockMvc
        .perform(
            get("/api/me")
                .with(
                    jwt()
                        .jwt(
                            builder ->
                                builder
                                    .subject("sub-no-roles")
                                    .claim("realm_access", Map.of("other", "value")))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles").isEmpty());
  }

  @Test
  void shouldDropNonStringEntriesWhenMappingRoles() throws Exception {
    mockMvc
        .perform(
            get("/api/me")
                .with(
                    jwt()
                        .jwt(
                            builder ->
                                builder
                                    .subject("sub-mixed-roles")
                                    .claim(
                                        "realm_access",
                                        Map.of("roles", List.of("USER", 42, "ADMIN"))))))
        .andExpect(status().isOk())
        // Reihenfolge ist deterministisch — Stream behaelt sie aus der Input-Liste bei.
        .andExpect(jsonPath("$.roles[0]").value("USER"))
        .andExpect(jsonPath("$.roles[1]").value("ADMIN"))
        .andExpect(jsonPath("$.roles[2]").doesNotExist());
  }
}
