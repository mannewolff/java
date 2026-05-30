package org.mwolff.api.kanban.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class KanbanItemTest {

  @Test
  void newInstanceInBacklogShouldNotSetMovedToDoneAt() {
    final KanbanItem item = KanbanItem.newInstance("sub-1", "T", "body", KanbanColumn.BACKLOG, 0);
    assertThat(item.id()).isNull();
    assertThat(item.movedToDoneAt()).isNull();
    assertThat(item.column()).isEqualTo(KanbanColumn.BACKLOG);
  }

  @Test
  void newInstanceInDoneShouldSetMovedToDoneAt() {
    final KanbanItem item = KanbanItem.newInstance("sub-1", "T", "", KanbanColumn.DONE, 0);
    assertThat(item.movedToDoneAt()).isNotNull();
  }

  @Test
  void shouldRejectNullUserSub() {
    assertThatThrownBy(
            () ->
                new KanbanItem(
                    null, null, "T", "", KanbanColumn.BACKLOG, 0, null, null, null, false))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectBlankUserSub() {
    assertThatThrownBy(
            () ->
                new KanbanItem(
                    null, " ", "T", "", KanbanColumn.BACKLOG, 0, null, null, null, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("userSub");
  }

  @Test
  void shouldRejectBlankTitle() {
    assertThatThrownBy(
            () ->
                new KanbanItem(null, "u", "", "", KanbanColumn.BACKLOG, 0, null, null, null, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("title");
  }

  @Test
  void shouldRejectTooLongTitle() {
    final String over = "x".repeat(201);
    assertThatThrownBy(
            () ->
                new KanbanItem(
                    null, "u", over, "", KanbanColumn.BACKLOG, 0, null, null, null, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("title");
  }

  @Test
  void shouldRejectTooLongBody() {
    final String over = "y".repeat(10_001);
    assertThatThrownBy(
            () ->
                new KanbanItem(
                    null, "u", "T", over, KanbanColumn.BACKLOG, 0, null, null, null, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("body");
  }

  @Test
  void shouldRejectNegativePosition() {
    assertThatThrownBy(
            () ->
                new KanbanItem(
                    null, "u", "T", "", KanbanColumn.BACKLOG, -1, null, null, null, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("position");
  }

  @Test
  void shouldRejectMovedToDoneAtOutsideDone() {
    final Instant now = Instant.parse("2026-01-01T00:00:00Z");
    assertThatThrownBy(
            () ->
                new KanbanItem(null, "u", "T", "", KanbanColumn.BACKLOG, 0, null, null, now, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("movedToDoneAt");
  }

  @Test
  void newInstanceShouldDefaultArchivedToFalse() {
    final KanbanItem item = KanbanItem.newInstance("u", "T", "", KanbanColumn.BACKLOG, 0);
    assertThat(item.archived()).isFalse();
  }

  @Test
  void withContentShouldPreserveArchivedFlag() {
    final KanbanItem original =
        new KanbanItem(1L, "u", "Old", "old body", KanbanColumn.BACKLOG, 0, null, null, null, true);
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
    final KanbanItem original = KanbanItem.newInstance("u", "T", "", KanbanColumn.IN_REVIEW, 2);
    final KanbanItem moved = original.withColumnAndPosition(KanbanColumn.DONE, 0, now);
    assertThat(moved.column()).isEqualTo(KanbanColumn.DONE);
    assertThat(moved.movedToDoneAt()).isEqualTo(now);
  }

  @Test
  void withColumnAndPositionResetsMovedToDoneAtOnExit() {
    final Instant earlier = Instant.parse("2026-01-01T00:00:00Z");
    final Instant now = Instant.parse("2026-01-02T00:00:00Z");
    final KanbanItem inDone =
        new KanbanItem(1L, "u", "T", "", KanbanColumn.DONE, 0, null, null, earlier, false);
    final KanbanItem moved = inDone.withColumnAndPosition(KanbanColumn.IN_REVIEW, 0, now);
    assertThat(moved.column()).isEqualTo(KanbanColumn.IN_REVIEW);
    assertThat(moved.movedToDoneAt()).isNull();
  }

  @Test
  void withColumnAndPositionWithinSameColumnPreservesMovedToDoneAt() {
    final Instant earlier = Instant.parse("2026-01-01T00:00:00Z");
    final Instant now = Instant.parse("2026-01-02T00:00:00Z");
    final KanbanItem inDone =
        new KanbanItem(1L, "u", "T", "", KanbanColumn.DONE, 0, null, null, earlier, false);
    final KanbanItem moved = inDone.withColumnAndPosition(KanbanColumn.DONE, 3, now);
    assertThat(moved.position()).isEqualTo(3);
    assertThat(moved.movedToDoneAt()).isEqualTo(earlier);
  }

  @Test
  void withPositionShouldPreserveArchivedFlag() {
    final KanbanItem original =
        new KanbanItem(1L, "u", "T", "", KanbanColumn.BACKLOG, 0, null, null, null, true);
    final KanbanItem moved = original.withPosition(5);
    assertThat(moved.position()).isEqualTo(5);
    assertThat(moved.archived()).isTrue();
    assertThat(moved.column()).isEqualTo(original.column());
  }
}
