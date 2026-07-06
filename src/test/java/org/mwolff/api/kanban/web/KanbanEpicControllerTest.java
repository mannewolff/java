package org.mwolff.api.kanban.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mwolff.api.auth.infrastructure.SecurityConfig;
import org.mwolff.api.kanban.application.GetEpicsUseCase;
import org.mwolff.api.kanban.application.GetEpicsUseCase.EpicWithProgress;
import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(KanbanEpicController.class)
@Import({KanbanExceptionHandler.class, SecurityConfig.class})
class KanbanEpicControllerTest {

  private static final String SUB = "user-1";

  private static RequestPostProcessor userJwt() {
    return jwt().jwt(j -> j.subject(SUB)).authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GetEpicsUseCase getEpicsUseCase;
  @MockitoBean private JwtDecoder jwtDecoder;

  private static KanbanItem epic(long id, int number, String title) {
    return new KanbanItem(
        id,
        SUB,
        title,
        "desc",
        KanbanColumn.BACKLOG,
        0,
        Instant.EPOCH,
        Instant.EPOCH,
        null,
        false,
        number,
        KanbanItemType.EPIC,
        null);
  }

  @Test
  void listReturnsEpicsWithProgress() throws Exception {
    given(getEpicsUseCase.execute(SUB))
        .willReturn(List.of(new EpicWithProgress(epic(5L, 3, "Workshop"), 1, 4)));

    mockMvc
        .perform(get("/api/kanban/epics").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].number").value(3))
        .andExpect(jsonPath("$[0].title").value("Workshop"))
        .andExpect(jsonPath("$[0].type").value("EPIC"))
        .andExpect(jsonPath("$[0].progress.done").value(1))
        .andExpect(jsonPath("$[0].progress.total").value(4));
  }

  @Test
  void listWithoutJwtReturns401() throws Exception {
    mockMvc.perform(get("/api/kanban/epics")).andExpect(status().isUnauthorized());
  }
}
