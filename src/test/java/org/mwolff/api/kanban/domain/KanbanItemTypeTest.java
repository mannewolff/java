package org.mwolff.api.kanban.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KanbanItemTypeTest {

  @Test
  void containsExactlyItemAndEpic() {
    assertThat(KanbanItemType.values()).containsExactly(KanbanItemType.ITEM, KanbanItemType.EPIC);
    assertThat(KanbanItemType.valueOf("EPIC")).isEqualTo(KanbanItemType.EPIC);
  }
}
