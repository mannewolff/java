package org.mwolff.api.kanban.web;

import org.mwolff.api.kanban.application.ResolveKanbanTokenUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Stellt den {@link KanbanTokenAuthFilter} als Bean bereit, damit die zentrale {@code
 * SecurityConfig} (auth) ihn per {@code ObjectProvider} additiv in die Default-Chain einhaengen
 * kann (#365).
 *
 * <p>Bewusst eine eigene {@code @Configuration} (spiegelt {@code IngestSecurityConfig}): So bleibt
 * der Filter aus den {@code @WebMvcTest}-Controller-Slices heraus — die scannen {@code
 * Filter}-Beans mit, wuerden aber den {@code ResolveKanbanTokenUseCase} nicht wiren koennen.
 * Slice-Tests laden diese {@code @Configuration} nicht; der volle {@code @SpringBootTest}-Kontext
 * (und Produktion) schon.
 */
@Configuration
public class KanbanSecurityConfig {

  @Bean
  public KanbanTokenAuthFilter kanbanTokenAuthFilter(
      final ResolveKanbanTokenUseCase resolveUseCase) {
    return new KanbanTokenAuthFilter(resolveUseCase);
  }
}
