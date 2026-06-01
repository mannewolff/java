package org.mwolff.api.appversion.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mwolff.api.appversion.application.GetAppVersionUseCase;
import org.mwolff.api.appversion.application.IncrementMajorVersionUseCase;
import org.mwolff.api.appversion.application.IncrementMinorVersionUseCase;
import org.mwolff.api.appversion.domain.AppVersion;
import org.mwolff.api.auth.infrastructure.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AppVersionController.class)
@Import(SecurityConfig.class)
class AppVersionControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GetAppVersionUseCase getUseCase;
  @MockitoBean private IncrementMinorVersionUseCase incrementMinorUseCase;
  @MockitoBean private IncrementMajorVersionUseCase incrementMajorUseCase;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void getReturnsCurrentVersionAsJson() throws Exception {
    given(getUseCase.execute()).willReturn(AppVersion.of(0, 1));

    mockMvc
        .perform(get("/api/app/version"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.major").value(0))
        .andExpect(jsonPath("$.minor").value(1));
  }

  @Test
  void incrementMinorReturnsRaisedVersion() throws Exception {
    given(incrementMinorUseCase.execute()).willReturn(AppVersion.of(0, 2));

    mockMvc
        .perform(post("/api/app/version/increment-minor"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.major").value(0))
        .andExpect(jsonPath("$.minor").value(2));
  }

  @Test
  void incrementMajorReturnsRaisedVersion() throws Exception {
    given(incrementMajorUseCase.execute()).willReturn(AppVersion.of(1, 0));

    mockMvc
        .perform(post("/api/app/version/increment-major"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.major").value(1))
        .andExpect(jsonPath("$.minor").value(0));
  }
}
