package org.mwolff.api.appversion.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mwolff.api.appversion.application.GetAppVersionUseCase;
import org.mwolff.api.appversion.application.IncrementMajorVersionUseCase;
import org.mwolff.api.appversion.application.IncrementMinorVersionUseCase;
import org.mwolff.api.appversion.domain.AppVersion;
import org.mwolff.api.auth.infrastructure.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = AppVersionController.class,
    properties = "app.version.increment-secret=test-secret")
@Import(SecurityConfig.class)
class AppVersionControllerTest {

  private static final String TOKEN_HEADER = "X-Version-Token";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GetAppVersionUseCase getUseCase;
  @MockitoBean private IncrementMinorVersionUseCase incrementMinorUseCase;
  @MockitoBean private IncrementMajorVersionUseCase incrementMajorUseCase;
  @MockitoBean private AppVersionRateLimiter rateLimiter;
  @MockitoBean private JwtDecoder jwtDecoder;

  @BeforeEach
  void allowRateLimitByDefault() {
    // Standardmäßig Requests durchlassen; der Drosselungs-Test überschreibt das gezielt.
    given(rateLimiter.tryAcquire(any())).willReturn(true);
  }

  private static org.springframework.test.web.servlet.request.RequestPostProcessor userJwt() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  @Test
  void getReturnsCurrentVersionForAuthenticatedUser() throws Exception {
    given(getUseCase.execute()).willReturn(AppVersion.of(0, 1));

    mockMvc
        .perform(get("/api/app/version").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.major").value(0))
        .andExpect(jsonPath("$.minor").value(1));
  }

  @Test
  void getIsRejectedWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/api/app/version")).andExpect(status().isUnauthorized());
  }

  @Test
  void incrementMinorWithValidTokenReturnsRaisedVersion() throws Exception {
    given(incrementMinorUseCase.execute()).willReturn(AppVersion.of(0, 2));

    mockMvc
        .perform(post("/api/app/version/increment-minor").header(TOKEN_HEADER, "test-secret"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.major").value(0))
        .andExpect(jsonPath("$.minor").value(2));
  }

  @Test
  void incrementMinorWithoutTokenIsUnauthorized() throws Exception {
    mockMvc.perform(post("/api/app/version/increment-minor")).andExpect(status().isUnauthorized());
  }

  @Test
  void incrementMinorWithWrongTokenIsUnauthorized() throws Exception {
    mockMvc
        .perform(post("/api/app/version/increment-minor").header(TOKEN_HEADER, "wrong"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void incrementMajorWithValidTokenReturnsRaisedVersion() throws Exception {
    given(incrementMajorUseCase.execute()).willReturn(AppVersion.of(1, 0));

    mockMvc
        .perform(post("/api/app/version/increment-major").header(TOKEN_HEADER, "test-secret"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.major").value(1))
        .andExpect(jsonPath("$.minor").value(0));
  }

  @Test
  void incrementMajorWithWrongTokenIsUnauthorized() throws Exception {
    mockMvc
        .perform(post("/api/app/version/increment-major").header(TOKEN_HEADER, "wrong"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void incrementIsThrottledWhenRateLimitExceeded() throws Exception {
    // Rate-Limit greift VOR der Secret-Prüfung (#311): selbst mit gültigem Token -> 429.
    given(rateLimiter.tryAcquire(any())).willReturn(false);

    mockMvc
        .perform(post("/api/app/version/increment-minor").header(TOKEN_HEADER, "test-secret"))
        .andExpect(status().isTooManyRequests());
  }
}
