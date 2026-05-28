package org.mwolff.api.ingest.web;

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
import org.mwolff.api.auth.SecurityConfig;
import org.mwolff.api.ingest.application.CreateIngestTokenUseCase;
import org.mwolff.api.ingest.application.CreateIngestTokenUseCase.CreatedIngestToken;
import org.mwolff.api.ingest.application.ListIngestTokensUseCase;
import org.mwolff.api.ingest.application.ResolveIngestTokenUseCase;
import org.mwolff.api.ingest.application.RevokeIngestTokenUseCase;
import org.mwolff.api.ingest.domain.IngestToken;
import org.mwolff.api.ingest.domain.IngestTokenNotFoundException;
import org.mwolff.api.ingest.infrastructure.ratelimit.IngestRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IngestTokenController.class)
@Import({IngestExceptionHandler.class, SecurityConfig.class})
class IngestTokenControllerTest {

  private static final String SUB = "user-1";

  private static org.springframework.test.web.servlet.request.RequestPostProcessor userJwt() {
    return jwt().jwt(j -> j.subject(SUB)).authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ListIngestTokensUseCase listUseCase;
  @MockitoBean private CreateIngestTokenUseCase createUseCase;
  @MockitoBean private RevokeIngestTokenUseCase revokeUseCase;

  // SecurityConfig zieht diese Beans als Filter-Chain-Dependencies.
  @MockitoBean private JwtDecoder jwtDecoder;
  @MockitoBean private ResolveIngestTokenUseCase resolveIngestTokenUseCase;
  @MockitoBean private IngestRateLimiter ingestRateLimiter;

  private static IngestToken token(long id, boolean revoked) {
    return new IngestToken(id, SUB, "Pi", "h", Instant.EPOCH, null, revoked);
  }

  @Test
  void listReturnsSummaries() throws Exception {
    given(listUseCase.execute(SUB)).willReturn(List.of(token(1L, false), token(2L, true)));

    mockMvc
        .perform(get("/api/ingest-tokens").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].revoked").value(false))
        .andExpect(jsonPath("$[1].revoked").value(true));
  }

  @Test
  void createReturns201WithPlaintext() throws Exception {
    given(createUseCase.execute(SUB, "Pi"))
        .willReturn(new CreatedIngestToken(token(99L, false), "tk_plaintext"));

    mockMvc
        .perform(
            post("/api/ingest-tokens")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Pi\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(99))
        .andExpect(jsonPath("$.plaintext").value("tk_plaintext"));
  }

  @Test
  void createReturns400WhenNameBlank() throws Exception {
    mockMvc
        .perform(
            post("/api/ingest-tokens")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void revokeReturns204() throws Exception {
    mockMvc
        .perform(delete("/api/ingest-tokens/1").with(userJwt()))
        .andExpect(status().isNoContent());
  }

  @Test
  void revokeReturns404WhenForeign() throws Exception {
    willThrow(new IngestTokenNotFoundException(7L)).given(revokeUseCase).execute(SUB, 7L);

    mockMvc
        .perform(delete("/api/ingest-tokens/7").with(userJwt()))
        .andExpect(status().isNotFound());
  }
}
