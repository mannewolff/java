package org.mwolff.api.kanban.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KanbanItemNotFoundExceptionTest {

  @Test
  void shouldCarryItemIdInMessage() {
    final KanbanItemNotFoundException ex = new KanbanItemNotFoundException(42L);
    assertThat(ex.getMessage()).contains("42");
  }
}
