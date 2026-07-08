package org.mwolff.api.kanban.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mwolff.api.auth.infrastructure.SecurityConfig;
import org.mwolff.api.kanban.application.CreateKanbanTokenUseCase;
import org.mwolff.api.kanban.application.CreateKanbanTokenUseCase.CreatedKanbanToken;
import org.mwolff.api.kanban.application.ListKanbanTokensUseCase;
import org.mwolff.api.kanban.application.RevokeKanbanTokenUseCase;
import org.mwolff.api.kanban.domain.KanbanAccessToken;
import org.mwolff.api.kanban.domain.KanbanTokenNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(KanbanTokenController.class)
@Import({KanbanExceptionHandler.class, SecurityConfig.class})
class KanbanTokenControllerTest {

  private static final String SUB = "user-1";
  private static final String DISPLAY = "Manne";

  private static RequestPostProcessor userJwt() {
    return jwt()
        .jwt(j -> j.subject(SUB).claim("preferred_username", DISPLAY))
        .authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  /**
   * JWT ohne preferred_username — der Controller faellt fuer den displayName auf den sub zurueck.
   */
  private static RequestPostProcessor userJwtWithoutPreferredUsername() {
    return jwt().jwt(j -> j.subject(SUB)).authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ListKanbanTokensUseCase listUseCase;
  @MockitoBean private CreateKanbanTokenUseCase createUseCase;
  @MockitoBean private RevokeKanbanTokenUseCase revokeUseCase;

  // SecurityConfig zieht den JwtDecoder als Filter-Chain-Dependency.
  @MockitoBean private JwtDecoder jwtDecoder;

  private static KanbanAccessToken token(long id, boolean revoked) {
    return new KanbanAccessToken(id, SUB, DISPLAY, "Board", "h", Instant.EPOCH, null, revoked);
  }

  @Test
  void listReturnsSummaries() throws Exception {
    given(listUseCase.execute(SUB)).willReturn(List.of(token(1L, false), token(2L, true)));

    mockMvc
        .perform(get("/api/kanban-tokens").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].revoked").value(false))
        .andExpect(jsonPath("$[1].revoked").value(true));
  }

  @Test
  void createReturns201WithPlaintextAndUsesPreferredUsernameAsDisplayName() throws Exception {
    given(createUseCase.execute(SUB, DISPLAY, "Board"))
        .willReturn(new CreatedKanbanToken(token(99L, false), "tk_plaintext"));

    mockMvc
        .perform(
            post("/api/kanban-tokens")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Board\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(99))
        .andExpect(jsonPath("$.name").value("Board"))
        .andExpect(jsonPath("$.plaintext").value("tk_plaintext"));
  }

  @Test
  void createFallsBackToSubAsDisplayNameWhenPreferredUsernameMissing() throws Exception {
    given(createUseCase.execute(SUB, SUB, "Board"))
        .willReturn(new CreatedKanbanToken(token(100L, false), "tk_fallback"));

    mockMvc
        .perform(
            post("/api/kanban-tokens")
                .with(userJwtWithoutPreferredUsername())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Board\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.plaintext").value("tk_fallback"));
  }

  @Test
  void createReturns400WhenNameBlank() throws Exception {
    mockMvc
        .perform(
            post("/api/kanban-tokens")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void revokeReturns204() throws Exception {
    mockMvc
        .perform(delete("/api/kanban-tokens/1").with(userJwt()))
        .andExpect(status().isNoContent());
  }

  @Test
  void revokeReturns404WhenForeign() throws Exception {
    willThrow(new KanbanTokenNotFoundException(7L)).given(revokeUseCase).execute(SUB, 7L);

    mockMvc
        .perform(delete("/api/kanban-tokens/7").with(userJwt()))
        .andExpect(status().isNotFound());
  }
}
