package org.mwolff.api.kanban.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mwolff.api.AbstractIntegrationTest;
import org.mwolff.api.kanban.application.CreateKanbanTokenUseCase;
import org.mwolff.api.kanban.application.CreateKanbanTokenUseCase.CreatedKanbanToken;
import org.mwolff.api.kanban.application.RevokeKanbanTokenUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-End-Verifikation der PAT-Authentifizierung (#365) gegen den vollen ApplicationContext und
 * eine echte MariaDB (Testcontainers): X-Kanban-Token wirkt auf /api/kanban/**, der Web-JWT-Pfad
 * bleibt intakt, und der PAT ist auf Kanban begrenzt (Dashboards -> 403).
 *
 * <p>Das {@link TestPropertySource}-Marker-Property gibt dieser Klasse bewusst eine eigene
 * Context-Cache-Signatur. Sonst teilte sie sich den gecachten Kontext mit einem anderen
 * {@code @SpringBootTest @AutoConfigureMockMvc}-IT (z. B. {@code OpenApiSchemaIT}); bei einem
 * Cache-Treffer ueber Klassengrenzen zeigte die {@code @ServiceConnection}-DataSource des
 * wiederverwendeten Kontexts auf den bereits gestoppten Testcontainer der anderen Klasse
 * (per-Klasse-Container-Lifecycle) -> "Connection refused". Mit eigener Signatur wird der Kontext
 * frisch gegen den Container DIESER Klasse gebaut.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "toolbox.test.context=kanban-token-auth-it")
class KanbanTokenAuthIT extends AbstractIntegrationTest {

  private static final String USER = "kanban-pat-user";

  @Autowired private MockMvc mockMvc;
  @Autowired private CreateKanbanTokenUseCase createUseCase;
  @Autowired private RevokeKanbanTokenUseCase revokeUseCase;

  private String createPlaintextToken() {
    return createUseCase.execute(USER, "Manne", "Board").plaintext();
  }

  @Test
  void validPatIsAcceptedOnKanbanItems() throws Exception {
    final String plaintext = createPlaintextToken();

    mockMvc
        .perform(get("/api/kanban/items").header(KanbanTokenAuthFilter.HEADER, plaintext))
        .andExpect(status().isOk());
  }

  @Test
  void revokedPatIsRejected() throws Exception {
    final CreatedKanbanToken created = createUseCase.execute(USER, "Manne", "Board");
    revokeUseCase.execute(USER, created.token().id());

    mockMvc
        .perform(get("/api/kanban/items").header(KanbanTokenAuthFilter.HEADER, created.plaintext()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void unknownPatIsRejected() throws Exception {
    mockMvc
        .perform(get("/api/kanban/items").header(KanbanTokenAuthFilter.HEADER, "tk_does_not_exist"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void missingCredentialsAreRejected() throws Exception {
    mockMvc.perform(get("/api/kanban/items")).andExpect(status().isUnauthorized());
  }

  @Test
  void webJwtStillWorksOnKanbanItems() throws Exception {
    mockMvc
        .perform(
            get("/api/kanban/items")
                .with(
                    jwt()
                        .jwt(j -> j.subject(USER))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(status().isOk());
  }

  @Test
  void patIsScopedToKanbanAndForbiddenOnDashboards() throws Exception {
    final String plaintext = createPlaintextToken();

    mockMvc
        .perform(get("/api/dashboards/some-id").header(KanbanTokenAuthFilter.HEADER, plaintext))
        .andExpect(status().isForbidden());
  }
}
