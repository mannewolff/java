package org.mwolff.api.kanban.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mwolff.api.auth.SecurityConfig;
import org.mwolff.api.kanban.application.GetSettingsUseCase;
import org.mwolff.api.kanban.application.UpdateSettingsUseCase;
import org.mwolff.api.kanban.domain.KanbanSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(KanbanSettingsController.class)
@Import({KanbanExceptionHandler.class, SecurityConfig.class})
class KanbanSettingsControllerTest {

  private static final String SUB = "user-1";

  private static org.springframework.test.web.servlet.request.RequestPostProcessor userJwt() {
    return jwt().jwt(j -> j.subject(SUB)).authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GetSettingsUseCase getUseCase;
  @MockitoBean private UpdateSettingsUseCase updateUseCase;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void getReturnsCurrentValue() throws Exception {
    given(getUseCase.execute(SUB)).willReturn(new KanbanSettings(SUB, 7));

    mockMvc
        .perform(get("/api/kanban/settings").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.doneRetentionDays").value(7));
  }

  @Test
  void putUpdatesValue() throws Exception {
    given(updateUseCase.execute(SUB, 10)).willReturn(new KanbanSettings(SUB, 10));

    mockMvc
        .perform(
            put("/api/kanban/settings")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"doneRetentionDays\":10}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.doneRetentionDays").value(10));
  }

  @Test
  void putRejectsOutOfRange() throws Exception {
    mockMvc
        .perform(
            put("/api/kanban/settings")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"doneRetentionDays\":99}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getWithoutJwtIs401() throws Exception {
    mockMvc.perform(get("/api/kanban/settings")).andExpect(status().isUnauthorized());
  }
}
