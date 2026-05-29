package org.mwolff.api.kanban.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KanbanCommentNotFoundExceptionTest {

  @Test
  void shouldCarryCommentIdInMessage() {
    final KanbanCommentNotFoundException ex = new KanbanCommentNotFoundException(42L);
    assertThat(ex.getMessage()).contains("42");
  }
}
