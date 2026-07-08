package org.mwolff.api.kanban.application;

import java.util.List;

import org.mwolff.api.kanban.domain.KanbanAccessToken;
import org.mwolff.api.kanban.domain.KanbanAccessTokenPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Liefert alle Kanban-Access-Tokens eines Users (inkl. widerrufener). */
@Component
public class ListKanbanTokensUseCase {

  private final KanbanAccessTokenPort tokens;

  public ListKanbanTokensUseCase(KanbanAccessTokenPort tokens) {
    this.tokens = tokens;
  }

  @Transactional(readOnly = true)
  public List<KanbanAccessToken> execute(String userSub) {
    return tokens.findAllByUser(userSub);
  }
}
