package org.mwolff.api.dashboard.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mwolff.api.auth.SecurityConfig;
import org.mwolff.api.dashboard.application.CreateDashboardUseCase;
import org.mwolff.api.dashboard.application.DeleteDashboardUseCase;
import org.mwolff.api.dashboard.application.GetDashboardUseCase;
import org.mwolff.api.dashboard.application.GetDashboardUseCase.DashboardWithWidgets;
import org.mwolff.api.dashboard.application.ListDashboardsUseCase;
import org.mwolff.api.dashboard.application.MarkAsDefaultUseCase;
import org.mwolff.api.dashboard.application.UpdateLayoutUseCase;
import org.mwolff.api.dashboard.domain.Dashboard;
import org.mwolff.api.dashboard.domain.DashboardNotFoundException;
import org.mwolff.api.dashboard.domain.Widget;
import org.mwolff.api.dashboard.domain.WidgetPosition;
import org.mwolff.api.dashboard.domain.WidgetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@Import({DashboardExceptionHandler.class, SecurityConfig.class})
class DashboardControllerTest {

  /** Mit USER-Rolle authentifizierter Aufruf, sub = SUB. */
  private static org.springframework.test.web.servlet.request.RequestPostProcessor userJwt() {
    return jwt().jwt(j -> j.subject(SUB)).authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ListDashboardsUseCase listUseCase;
  @MockitoBean private CreateDashboardUseCase createUseCase;
  @MockitoBean private GetDashboardUseCase getUseCase;
  @MockitoBean private UpdateLayoutUseCase updateLayoutUseCase;
  @MockitoBean private MarkAsDefaultUseCase markDefaultUseCase;
  @MockitoBean private DeleteDashboardUseCase deleteUseCase;

  // SecurityConfig will autowire einen JwtDecoder — fuer Slice-Tests reicht ein Mock.
  @MockitoBean private JwtDecoder jwtDecoder;

  private static final String SUB = "user-1";

  private static Dashboard dashboard(long id, boolean isDefault) {
    return new Dashboard(id, SUB, "Main", isDefault, Instant.EPOCH, Instant.EPOCH);
  }

  // ----- GET /api/dashboards -----------------------------------------------

  @Test
  void listShouldReturnSummaries() throws Exception {
    given(listUseCase.execute(SUB)).willReturn(List.of(dashboard(1L, true), dashboard(2L, false)));

    mockMvc
        .perform(get("/api/dashboards").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].isDefault").value(true))
        .andExpect(jsonPath("$[1].id").value(2))
        .andExpect(jsonPath("$[1].isDefault").value(false));
  }

  @Test
  void listShouldReturnEmptyArrayWhenNoDashboards() throws Exception {
    given(listUseCase.execute(SUB)).willReturn(List.of());

    mockMvc
        .perform(get("/api/dashboards").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  // ----- POST /api/dashboards ----------------------------------------------

  @Test
  void createShouldReturn201WithCreatedDashboard() throws Exception {
    given(createUseCase.execute(SUB, "Main")).willReturn(dashboard(1L, true));

    mockMvc
        .perform(
            post("/api/dashboards")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Main\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Main"))
        .andExpect(jsonPath("$.isDefault").value(true));
  }

  @Test
  void createShouldReturn400WhenNameBlank() throws Exception {
    mockMvc
        .perform(
            post("/api/dashboards")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createShouldReturn400WhenNameTooLong() throws Exception {
    final String tooLong = "x".repeat(101);
    mockMvc
        .perform(
            post("/api/dashboards")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + tooLong + "\"}"))
        .andExpect(status().isBadRequest());
  }

  // ----- GET /api/dashboards/{id} ------------------------------------------

  @Test
  void getShouldReturnDashboardWithWidgets() throws Exception {
    final Widget w =
        new Widget(
            10L,
            1L,
            WidgetType.TEXTBOX,
            new WidgetPosition(0, 0, 2, 2),
            "{\"text\":\"hi\"}",
            Instant.EPOCH,
            Instant.EPOCH);
    given(getUseCase.execute(SUB, 1L))
        .willReturn(new DashboardWithWidgets(dashboard(1L, true), List.of(w)));

    mockMvc
        .perform(get("/api/dashboards/1").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.widgets[0].id").value(10))
        .andExpect(jsonPath("$.widgets[0].type").value("TEXTBOX"))
        .andExpect(jsonPath("$.widgets[0].posX").value(0));
  }

  @Test
  void getShouldReturn404ForForeignOrMissingDashboard() throws Exception {
    willThrow(new DashboardNotFoundException(99L)).given(getUseCase).execute(eq(SUB), eq(99L));

    mockMvc.perform(get("/api/dashboards/99").with(userJwt())).andExpect(status().isNotFound());
  }

  // ----- PUT /api/dashboards/{id} (Layout) ---------------------------------

  @Test
  void updateLayoutShouldReplaceWidgetsAndReturnDetail() throws Exception {
    final Widget saved =
        new Widget(
            42L,
            1L,
            WidgetType.KPI,
            new WidgetPosition(1, 2, 3, 4),
            "{\"value\":1}",
            Instant.EPOCH,
            Instant.EPOCH);
    given(updateLayoutUseCase.execute(eq(SUB), eq(1L), any())).willReturn(List.of(saved));
    given(getUseCase.execute(SUB, 1L))
        .willReturn(new DashboardWithWidgets(dashboard(1L, true), List.of(saved)));

    mockMvc
        .perform(
            put("/api/dashboards/1")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"widgets\":[{\"type\":\"KPI\",\"posX\":1,\"posY\":2,\"width\":3,\"height\":4,\"config\":\"{\\\"value\\\":1}\"}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.widgets[0].id").value(42))
        .andExpect(jsonPath("$.widgets[0].type").value("KPI"));
  }

  @Test
  void updateLayoutShouldReturn400WhenWidgetWidthZero() throws Exception {
    mockMvc
        .perform(
            put("/api/dashboards/1")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"widgets\":[{\"type\":\"KPI\",\"posX\":0,\"posY\":0,\"width\":0,\"height\":1,\"config\":\"{}\"}]}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateLayoutShouldReturn404WhenDashboardNotOwned() throws Exception {
    willThrow(new DashboardNotFoundException(7L))
        .given(updateLayoutUseCase)
        .execute(eq(SUB), eq(7L), any());

    mockMvc
        .perform(
            put("/api/dashboards/7")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"widgets\":[]}"))
        .andExpect(status().isNotFound());
  }

  // ----- PUT /api/dashboards/{id}/default ----------------------------------

  @Test
  void markAsDefaultShouldReturnUpdatedSummary() throws Exception {
    given(markDefaultUseCase.execute(SUB, 3L)).willReturn(dashboard(3L, true));

    mockMvc
        .perform(put("/api/dashboards/3/default").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(3))
        .andExpect(jsonPath("$.isDefault").value(true));
  }

  @Test
  void markAsDefaultShouldReturn404WhenForeign() throws Exception {
    willThrow(new DashboardNotFoundException(7L)).given(markDefaultUseCase).execute(SUB, 7L);

    mockMvc
        .perform(put("/api/dashboards/7/default").with(userJwt()))
        .andExpect(status().isNotFound());
  }

  // ----- DELETE /api/dashboards/{id} ---------------------------------------

  @Test
  void deleteShouldReturn204() throws Exception {
    mockMvc.perform(delete("/api/dashboards/1").with(userJwt())).andExpect(status().isNoContent());
  }

  @Test
  void deleteShouldReturn404WhenForeign() throws Exception {
    willThrow(new DashboardNotFoundException(7L)).given(deleteUseCase).execute(SUB, 7L);

    mockMvc.perform(delete("/api/dashboards/7").with(userJwt())).andExpect(status().isNotFound());
  }
}
