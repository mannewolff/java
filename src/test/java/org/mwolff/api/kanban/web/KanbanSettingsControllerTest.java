package org.mwolff.api.kanban.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mwolff.api.auth.infrastructure.SecurityConfig;
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
    given(getUseCase.execute(SUB))
        .willReturn(new KanbanSettings(SUB, 7, java.util.Set.of("BACKLOG", "archived")));

    mockMvc
        .perform(get("/api/kanban/settings").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.doneRetentionDays").value(7))
        .andExpect(jsonPath("$.activeFilters").isArray())
        .andExpect(jsonPath("$.activeFilters[0]").value("BACKLOG"))
        .andExpect(jsonPath("$.activeFilters[1]").value("archived"));
  }

  @Test
  void putUpdatesValueAndFilters() throws Exception {
    given(updateUseCase.execute(SUB, 10, java.util.List.of("READY", "DONE")))
        .willReturn(new KanbanSettings(SUB, 10, java.util.Set.of("READY", "DONE")));

    mockMvc
        .perform(
            put("/api/kanban/settings")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"doneRetentionDays\":10,\"activeFilters\":[\"READY\",\"DONE\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.doneRetentionDays").value(10))
        .andExpect(
            jsonPath("$.activeFilters", org.hamcrest.Matchers.containsInAnyOrder("READY", "DONE")));
  }

  @Test
  void putWithoutFiltersPassesNull() throws Exception {
    given(updateUseCase.execute(SUB, 10, null)).willReturn(new KanbanSettings(SUB, 10));

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
  void putRejectsOutOfRangeWithFiltersPresent() throws Exception {
    // Sichert @Min gegen einen PIT-Mutanten ab: auch mit gesetzten activeFilters
    // muss ein ungueltiges doneRetentionDays (0) zu 400 fuehren.
    mockMvc
        .perform(
            put("/api/kanban/settings")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"doneRetentionDays\":0,\"activeFilters\":[\"READY\"]}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void putRejectsAboveMaxWithFiltersPresent() throws Exception {
    // Analog fuer @Max: 31 ueberschreitet die Obergrenze.
    mockMvc
        .perform(
            put("/api/kanban/settings")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"doneRetentionDays\":31,\"activeFilters\":[\"READY\"]}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getWithoutJwtIs401() throws Exception {
    mockMvc.perform(get("/api/kanban/settings")).andExpect(status().isUnauthorized());
  }
}
