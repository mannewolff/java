package org.mwolff.api.common;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * OpenAPI 3-Schema-Konfiguration. Definiert die beiden Auth-Schemata:
 *
 * <ul>
 *   <li>{@code bearerAuth} (JWT) fuer alle Endpoints ausser {@code /api/ingest}
 *   <li>{@code ingestTokenAuth} (Header {@code X-Ingest-Token}) ausschliesslich fuer {@code
 *       /api/ingest}
 * </ul>
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI toolboxOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Toolbox API")
                .version("v1")
                .description(
                    "REST-API der Toolbox. JWT fuer Web-UI-Endpoints, X-Ingest-Token fuer "
                        + "den oeffentlichen Ingest."))
        .servers(
            List.of(
                new Server().url("https://toolbox.mwolff.org").description("Production"),
                new Server().url("http://localhost:8080").description("Dev")))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .addSecuritySchemes(
                    "ingestTokenAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-Ingest-Token")
                        .description("Langlebiger Ingest-Token im Format tk_<64-hex>")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }
}
