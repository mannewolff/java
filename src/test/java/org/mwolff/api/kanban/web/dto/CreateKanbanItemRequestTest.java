package org.mwolff.api.kanban.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItemType;

class CreateKanbanItemRequestTest {

  @Test
  void bodyOrEmptyReturnsBodyWhenPresent() {
    final var req =
        new CreateKanbanItemRequest("Titel", "Inhalt", KanbanColumn.BACKLOG, null, null);

    assertThat(req.bodyOrEmpty()).isEqualTo("Inhalt");
  }

  @Test
  void bodyOrEmptyReturnsEmptyStringWhenNull() {
    final var req = new CreateKanbanItemRequest("Titel", null, KanbanColumn.BACKLOG, null, null);

    assertThat(req.bodyOrEmpty()).isEmpty();
  }

  @Test
  void typeOrDefaultReturnsItemWhenNull() {
    final var req = new CreateKanbanItemRequest("Titel", null, null, null, null);

    assertThat(req.typeOrDefault()).isEqualTo(KanbanItemType.ITEM);
  }

  @Test
  void typeOrDefaultReturnsExplicitType() {
    final var req = new CreateKanbanItemRequest("Titel", null, null, KanbanItemType.EPIC, null);

    assertThat(req.typeOrDefault()).isEqualTo(KanbanItemType.EPIC);
  }
}
