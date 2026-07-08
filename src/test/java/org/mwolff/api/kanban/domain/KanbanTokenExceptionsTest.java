package org.mwolff.api.kanban.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KanbanTokenExceptionsTest {

  @Test
  void invalidTokenExceptionShouldKeepMessage() {
    final InvalidKanbanTokenException ex = new InvalidKanbanTokenException("nope");
    assertThat(ex.getMessage()).isEqualTo("nope");
  }

  @Test
  void notFoundExceptionShouldIncludeId() {
    final KanbanTokenNotFoundException ex = new KanbanTokenNotFoundException(42L);
    assertThat(ex.getMessage()).contains("42");
  }
}
