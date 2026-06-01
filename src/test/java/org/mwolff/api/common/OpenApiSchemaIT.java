package org.mwolff.api.common;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mwolff.api.AbstractIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifiziert, dass das OpenAPI-Schema unter {@code /api/openapi} verfuegbar ist, mindestens den
 * Ingest-Endpoint listet und das {@code ingestTokenAuth}-Schema enthaelt. Laeuft als IT weil
 * springdoc den vollen ApplicationContext braucht (springdoc registriert sich erst beim Boot-Start,
 * das ist im {@code @WebMvcTest} nicht aktiv).
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiSchemaIT extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void schemaIsAvailableForLoggedInUser() throws Exception {
    mockMvc
        .perform(
            get("/api/openapi")
                .with(
                    jwt()
                        .jwt(j -> j.subject("user-1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.info.title").value("Toolbox API"))
        .andExpect(jsonPath("$.paths['/api/ingest']").exists())
        .andExpect(jsonPath("$.components.securitySchemes.ingestTokenAuth").exists());
  }

  @Test
  void schemaIsPublicWithoutAuth() throws Exception {
    // OpenAPI-Schema + Swagger-UI sind seit #166 bewusst oeffentlich (permitAll), damit
    // Entwickler ohne Token darauf zugreifen koennen — daher 200 auch ohne Auth.
    mockMvc.perform(get("/api/openapi")).andExpect(status().isOk());
  }
}
