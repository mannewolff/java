package org.mwolff.api.appversion.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mwolff.api.appversion.application.GetAppVersionUseCase;
import org.mwolff.api.appversion.application.IncrementMajorVersionUseCase;
import org.mwolff.api.appversion.application.IncrementMinorVersionUseCase;
import org.mwolff.api.auth.infrastructure.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * #229: Ist kein increment-Secret konfiguriert (leer), muss der mutierende Endpunkt grundsaetzlich
 * mit 401 ablehnen (deny-by-default) — auch wenn ein beliebiger Header mitgeschickt wird.
 */
@WebMvcTest(controllers = AppVersionController.class, properties = "app.version.increment-secret=")
@Import(SecurityConfig.class)
class AppVersionControllerNoSecretTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GetAppVersionUseCase getUseCase;
  @MockitoBean private IncrementMinorVersionUseCase incrementMinorUseCase;
  @MockitoBean private IncrementMajorVersionUseCase incrementMajorUseCase;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void incrementIsUnauthorizedWhenNoSecretConfigured() throws Exception {
    mockMvc
        .perform(post("/api/app/version/increment-minor").header("X-Version-Token", "anything"))
        .andExpect(status().isUnauthorized());
  }
}
