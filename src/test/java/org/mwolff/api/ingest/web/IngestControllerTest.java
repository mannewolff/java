package org.mwolff.api.ingest.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mwolff.api.auth.infrastructure.SecurityConfig;
import org.mwolff.api.ingest.application.CreateIngestTokenUseCase;
import org.mwolff.api.ingest.application.ListIngestTokensUseCase;
import org.mwolff.api.ingest.application.ResolveIngestTokenUseCase;
import org.mwolff.api.ingest.application.RevokeIngestTokenUseCase;
import org.mwolff.api.ingest.domain.IngestToken;
import org.mwolff.api.ingest.domain.InvalidIngestTokenException;
import org.mwolff.api.ingest.infrastructure.ratelimit.IngestRateLimiter;
import org.mwolff.api.timeseries.application.AddEntryUseCase;
import org.mwolff.api.timeseries.domain.TimeSeriesEntry;
import org.mwolff.api.timeseries.domain.TimeSeriesNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IngestController.class)
@Import({IngestExceptionHandler.class, SecurityConfig.class, IngestSecurityConfig.class})
class IngestControllerTest {

  private static final String SUB = "user-1";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AddEntryUseCase addEntryUseCase;
  @MockitoBean private ResolveIngestTokenUseCase resolveIngestTokenUseCase;
  @MockitoBean private IngestRateLimiter ingestRateLimiter;

  // SecurityConfig zieht diese auch, obwohl wir hier nur den Ingest-Pfad testen.
  @MockitoBean private JwtDecoder jwtDecoder;
  @MockitoBean private ListIngestTokensUseCase listIngestTokensUseCase;
  @MockitoBean private CreateIngestTokenUseCase createIngestTokenUseCase;
  @MockitoBean private RevokeIngestTokenUseCase revokeIngestTokenUseCase;

  private void allowAuth(String plaintext) {
    given(resolveIngestTokenUseCase.execute(plaintext))
        .willReturn(new IngestToken(7L, SUB, "Pi", "h", Instant.EPOCH, Instant.EPOCH, false));
    given(ingestRateLimiter.tryAcquire(anyString())).willReturn(true);
  }

  @Test
  void rejectsWhenHeaderMissing() throws Exception {
    given(ingestRateLimiter.tryAcquire(anyString())).willReturn(true);

    mockMvc
        .perform(
            post("/api/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timeSeriesId\":1,\"timestamp\":\"2026-05-27T12:00:00Z\",\"value\":1}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void rejectsWhenTokenInvalid() throws Exception {
    given(ingestRateLimiter.tryAcquire(anyString())).willReturn(true);
    willThrow(new InvalidIngestTokenException("no"))
        .given(resolveIngestTokenUseCase)
        .execute("tk_bad");

    mockMvc
        .perform(
            post("/api/ingest")
                .header("X-Ingest-Token", "tk_bad")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timeSeriesId\":1,\"timestamp\":\"2026-05-27T12:00:00Z\",\"value\":1}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void ingestSucceedsWithValidToken() throws Exception {
    allowAuth("tk_ok");
    given(
            addEntryUseCase.execute(
                eq(SUB),
                eq(1L),
                eq(Instant.parse("2026-05-27T12:00:00Z")),
                eq(new BigDecimal("78.5"))))
        .willReturn(
            new TimeSeriesEntry(
                99L, 1L, Instant.parse("2026-05-27T12:00:00Z"), new BigDecimal("78.5")));

    mockMvc
        .perform(
            post("/api/ingest")
                .header("X-Ingest-Token", "tk_ok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"timeSeriesId\":1,\"timestamp\":\"2026-05-27T12:00:00Z\",\"value\":78.5}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(99))
        .andExpect(jsonPath("$.value").value(78.5));
  }

  @Test
  void returns404WhenForeignSeries() throws Exception {
    allowAuth("tk_ok");
    willThrow(new TimeSeriesNotFoundException(99L))
        .given(addEntryUseCase)
        .execute(eq(SUB), eq(99L), any(), any());

    mockMvc
        .perform(
            post("/api/ingest")
                .header("X-Ingest-Token", "tk_ok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"timeSeriesId\":99,\"timestamp\":\"2026-05-27T12:00:00Z\",\"value\":1}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void returns400OnValidationError() throws Exception {
    allowAuth("tk_ok");

    mockMvc
        .perform(
            post("/api/ingest")
                .header("X-Ingest-Token", "tk_ok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"timeSeriesId\":-1,\"timestamp\":\"2026-05-27T12:00:00Z\",\"value\":1}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void returns429WhenRateLimitExceeded() throws Exception {
    given(resolveIngestTokenUseCase.execute("tk_ok"))
        .willReturn(new IngestToken(7L, SUB, "Pi", "h", Instant.EPOCH, Instant.EPOCH, false));
    given(ingestRateLimiter.tryAcquire(anyString())).willReturn(false);

    mockMvc
        .perform(
            post("/api/ingest")
                .header("X-Ingest-Token", "tk_ok")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timeSeriesId\":1,\"timestamp\":\"2026-05-27T12:00:00Z\",\"value\":1}"))
        .andExpect(status().isTooManyRequests());
  }
}
