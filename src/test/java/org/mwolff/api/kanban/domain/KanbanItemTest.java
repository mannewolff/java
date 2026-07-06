package org.mwolff.api.kanban.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class KanbanItemTest {

  @Test
  void newInstanceInBacklogShouldNotSetMovedToDoneAt() {
    final KanbanItem item =
        KanbanItem.newInstance("sub-1", "T", "body", KanbanColumn.BACKLOG, 0, Instant.EPOCH);
    assertThat(item.id()).isNull();
    assertThat(item.movedToDoneAt()).isNull();
    assertThat(item.column()).isEqualTo(KanbanColumn.BACKLOG);
  }

  @Test
  void newInstanceInDoneShouldSetMovedToDoneAt() {
    final KanbanItem item =
        KanbanItem.newInstance("sub-1", "T", "", KanbanColumn.DONE, 0, Instant.EPOCH);
    assertThat(item.movedToDoneAt()).isNotNull();
  }

  @Test
  void shouldRejectNullUserSub() {
    assertThatThrownBy(
            () ->
                new KanbanItem(
                    null, null, "T", "", KanbanColumn.BACKLOG, 0, null, null, null, false, 0))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectBlankUserSub() {
    assertThatThrownBy(
            () ->
                new KanbanItem(
                    null, " ", "T", "", KanbanColumn.BACKLOG, 0, null, null, null, false, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("userSub");
  }

  @Test
  void shouldRejectBlankTitle() {
    assertThatThrownBy(
            () ->
                new KanbanItem(
                    null, "u", "", "", KanbanColumn.BACKLOG, 0, null, null, null, false, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("title");
  }

  @Test
  void shouldRejectTooLongTitle() {
    final String over = "x".repeat(201);
    assertThatThrownBy(
            () ->
                new KanbanItem(
                    null, "u", over, "", KanbanColumn.BACKLOG, 0, null, null, null, false, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("title");
  }

  @Test
  void shouldRejectTooLongBody() {
    final String over = "y".repeat(10_001);
    assertThatThrownBy(
            () ->
                new KanbanItem(
                    null, "u", "T", over, KanbanColumn.BACKLOG, 0, null, null, null, false, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("body");
  }

  @Test
  void shouldRejectNegativePosition() {
    assertThatThrownBy(
            () ->
                new KanbanItem(
                    null, "u", "T", "", KanbanColumn.BACKLOG, -1, null, null, null, false, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("position");
  }

  @Test
  void shouldRejectMovedToDoneAtOutsideDone() {
    final Instant now = Instant.parse("2026-01-01T00:00:00Z");
    assertThatThrownBy(
            () ->
                new KanbanItem(
                    null, "u", "T", "", KanbanColumn.BACKLOG, 0, null, null, now, false, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("movedToDoneAt");
  }

  @Test
  void newInstanceShouldDefaultArchivedToFalse() {
    final KanbanItem item =
        KanbanItem.newInstance("u", "T", "", KanbanColumn.BACKLOG, 0, Instant.EPOCH);
    assertThat(item.archived()).isFalse();
  }

  @Test
  void withContentShouldPreserveArchivedFlag() {
    final KanbanItem original =
        new KanbanItem(
            1L, "u", "Old", "old body", KanbanColumn.BACKLOG, 0, null, null, null, true, 0);
    final KanbanItem updated = original.withContent("New", "new body");
    assertThat(updated.title()).isEqualTo("New");
    assertThat(updated.body()).isEqualTo("new body");
    assertThat(updated.archived()).isTrue();
    assertThat(updated.column()).isEqualTo(original.column());
    assertThat(updated.position()).isEqualTo(original.position());
  }

  @Test
  void withColumnAndPositionSetsMovedToDoneAtOnEntry() {
    final Instant now = Instant.parse("2026-01-01T12:00:00Z");
    final KanbanItem original =
        KanbanItem.newInstance("u", "T", "", KanbanColumn.IN_REVIEW, 2, Instant.EPOCH);
    final KanbanItem moved = original.withColumnAndPosition(KanbanColumn.DONE, 0, now);
    assertThat(moved.column()).isEqualTo(KanbanColumn.DONE);
    assertThat(moved.movedToDoneAt()).isEqualTo(now);
  }

  @Test
  void withColumnAndPositionResetsMovedToDoneAtOnExit() {
    final Instant earlier = Instant.parse("2026-01-01T00:00:00Z");
    final Instant now = Instant.parse("2026-01-02T00:00:00Z");
    final KanbanItem inDone =
        new KanbanItem(1L, "u", "T", "", KanbanColumn.DONE, 0, null, null, earlier, false, 0);
    final KanbanItem moved = inDone.withColumnAndPosition(KanbanColumn.IN_REVIEW, 0, now);
    assertThat(moved.column()).isEqualTo(KanbanColumn.IN_REVIEW);
    assertThat(moved.movedToDoneAt()).isNull();
  }

  @Test
  void withColumnAndPositionWithinSameColumnPreservesMovedToDoneAt() {
    final Instant earlier = Instant.parse("2026-01-01T00:00:00Z");
    final Instant now = Instant.parse("2026-01-02T00:00:00Z");
    final KanbanItem inDone =
        new KanbanItem(1L, "u", "T", "", KanbanColumn.DONE, 0, null, null, earlier, false, 0);
    final KanbanItem moved = inDone.withColumnAndPosition(KanbanColumn.DONE, 3, now);
    assertThat(moved.position()).isEqualTo(3);
    assertThat(moved.movedToDoneAt()).isEqualTo(earlier);
  }

  @Test
  void newInstanceDefaultsNumberToZero() {
    final KanbanItem item =
        KanbanItem.newInstance("u", "T", "", KanbanColumn.BACKLOG, 0, Instant.EPOCH);
    assertThat(item.number()).isZero();
  }

  @Test
  void withNumberSetsNumberAndPreservesRest() {
    final KanbanItem item =
        KanbanItem.newInstance("u", "T", "b", KanbanColumn.BACKLOG, 2, Instant.EPOCH).withNumber(7);
    assertThat(item.number()).isEqualTo(7);
    assertThat(item.title()).isEqualTo("T");
    assertThat(item.position()).isEqualTo(2);
  }

  @Test
  void shouldRejectNegativeNumber() {
    assertThatThrownBy(
            () ->
                new KanbanItem(
                    null, "u", "T", "", KanbanColumn.BACKLOG, 0, null, null, null, false, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("number");
  }

  @Test
  void withPositionShouldPreserveArchivedFlag() {
    final KanbanItem original =
        new KanbanItem(1L, "u", "T", "", KanbanColumn.BACKLOG, 0, null, null, null, true, 0);
    final KanbanItem moved = original.withPosition(5);
    assertThat(moved.position()).isEqualTo(5);
    assertThat(moved.archived()).isTrue();
    assertThat(moved.column()).isEqualTo(original.column());
  }

  // ----- Epics (#321) --------------------------------------------------------

  @Test
  void compatConstructorDefaultsToItemWithoutParent() {
    final KanbanItem item =
        new KanbanItem(1L, "u", "T", "", KanbanColumn.BACKLOG, 0, null, null, null, false, 1);
    assertThat(item.type()).isEqualTo(KanbanItemType.ITEM);
    assertThat(item.parentId()).isNull();
  }

  @Test
  void newInstanceDefaultsToItemWithoutParent() {
    final KanbanItem item =
        KanbanItem.newInstance("u", "T", "", KanbanColumn.BACKLOG, 0, Instant.EPOCH);
    assertThat(item.type()).isEqualTo(KanbanItemType.ITEM);
    assertThat(item.parentId()).isNull();
  }

  @Test
  void newInstanceCreatesEpicWithoutParent() {
    final KanbanItem epic =
        KanbanItem.newInstance(
            "u", "Epic", "", KanbanColumn.BACKLOG, 0, Instant.EPOCH, KanbanItemType.EPIC, null);
    assertThat(epic.type()).isEqualTo(KanbanItemType.EPIC);
    assertThat(epic.parentId()).isNull();
  }

  @Test
  void newInstanceCreatesItemAssignedToEpic() {
    final KanbanItem story =
        KanbanItem.newInstance(
            "u", "Story", "", KanbanColumn.BACKLOG, 0, Instant.EPOCH, KanbanItemType.ITEM, 42L);
    assertThat(story.type()).isEqualTo(KanbanItemType.ITEM);
    assertThat(story.parentId()).isEqualTo(42L);
  }

  @Test
  void shouldRejectEpicWithParent() {
    assertThatThrownBy(
            () ->
                KanbanItem.newInstance(
                    "u",
                    "Epic",
                    "",
                    KanbanColumn.BACKLOG,
                    0,
                    Instant.EPOCH,
                    KanbanItemType.EPIC,
                    42L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("parent");
  }

  @Test
  void shouldRejectNullType() {
    assertThatThrownBy(
            () ->
                new KanbanItem(
                    null,
                    "u",
                    "T",
                    "",
                    KanbanColumn.BACKLOG,
                    0,
                    null,
                    null,
                    null,
                    false,
                    0,
                    null,
                    null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("type");
  }

  @Test
  void copyMethodsPreserveTypeAndParent() {
    final KanbanItem story =
        KanbanItem.newInstance(
            "u", "Story", "", KanbanColumn.BACKLOG, 0, Instant.EPOCH, KanbanItemType.ITEM, 7L);

    assertThat(story.withNumber(3).parentId()).isEqualTo(7L);
    assertThat(story.withContent("N", "b").parentId()).isEqualTo(7L);
    assertThat(story.withPosition(2).parentId()).isEqualTo(7L);
    final KanbanItem moved =
        story.withColumnAndPosition(KanbanColumn.IN_PROGRESS, 0, Instant.EPOCH);
    assertThat(moved.parentId()).isEqualTo(7L);
    assertThat(moved.type()).isEqualTo(KanbanItemType.ITEM);
  }
}
