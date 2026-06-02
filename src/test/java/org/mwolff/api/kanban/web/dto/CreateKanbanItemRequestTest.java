package org.mwolff.api.kanban.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mwolff.api.kanban.domain.KanbanColumn;

class CreateKanbanItemRequestTest {

  @Test
  void bodyOrEmptyReturnsBodyWhenPresent() {
    final var req = new CreateKanbanItemRequest("Titel", "Inhalt", KanbanColumn.BACKLOG);

    assertThat(req.bodyOrEmpty()).isEqualTo("Inhalt");
  }

  @Test
  void bodyOrEmptyReturnsEmptyStringWhenNull() {
    final var req = new CreateKanbanItemRequest("Titel", null, KanbanColumn.BACKLOG);

    assertThat(req.bodyOrEmpty()).isEmpty();
  }
}
