package org.mwolff.api.kanban.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KanbanCommentForbiddenExceptionTest {

  @Test
  void shouldCarryCommentIdInMessage() {
    final KanbanCommentForbiddenException ex = new KanbanCommentForbiddenException(7L);
    assertThat(ex.getMessage()).contains("7");
  }
}
