package org.mwolff.api.kanban.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class KanbanCommentTest {

  @Test
  void newInstanceCreatesUnsavedComment() {
    final KanbanComment c = KanbanComment.newInstance(7L, "sub-1", "alice", "Hallo");

    assertThat(c.id()).isNull();
    assertThat(c.itemId()).isEqualTo(7L);
    assertThat(c.authorSub()).isEqualTo("sub-1");
    assertThat(c.author()).isEqualTo("alice");
    assertThat(c.body()).isEqualTo("Hallo");
    assertThat(c.createdAt()).isNull();
    assertThat(c.updatedAt()).isNull();
  }

  @Test
  void withBodyReplacesOnlyBody() {
    final Instant t = Instant.parse("2026-05-29T10:00:00Z");
    final KanbanComment c = new KanbanComment(3L, 7L, "sub-1", "alice", "alt", t, t);

    final KanbanComment updated = c.withBody("neu");

    assertThat(updated.id()).isEqualTo(3L);
    assertThat(updated.itemId()).isEqualTo(7L);
    assertThat(updated.authorSub()).isEqualTo("sub-1");
    assertThat(updated.author()).isEqualTo("alice");
    assertThat(updated.body()).isEqualTo("neu");
    assertThat(updated.createdAt()).isEqualTo(t);
    assertThat(updated.updatedAt()).isEqualTo(t);
  }

  @Test
  void isOwnedByMatchesAuthorSubOnly() {
    final KanbanComment c = new KanbanComment(3L, 7L, "sub-1", "alice", "b", null, null);

    assertThat(c.isOwnedBy("sub-1")).isTrue();
    assertThat(c.isOwnedBy("sub-2")).isFalse();
  }

  @Test
  void rejectsNullAuthorSub() {
    assertThatThrownBy(() -> new KanbanComment(null, 1L, null, "alice", "body", null, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsBlankAuthorSub() {
    assertThatThrownBy(() -> new KanbanComment(null, 1L, "  ", "alice", "body", null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("authorSub");
  }

  @Test
  void rejectsNullAuthor() {
    assertThatThrownBy(() -> new KanbanComment(null, 1L, "sub-1", null, "body", null, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsBlankAuthor() {
    assertThatThrownBy(() -> new KanbanComment(null, 1L, "sub-1", "  ", "body", null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("author");
  }

  @Test
  void rejectsNullBody() {
    assertThatThrownBy(() -> new KanbanComment(null, 1L, "sub-1", "alice", null, null, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsBlankBody() {
    assertThatThrownBy(() -> new KanbanComment(null, 1L, "sub-1", "alice", "   ", null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("body");
  }

  @Test
  void rejectsBodyExceedingMaxLength() {
    final String tooLong = "x".repeat(KanbanComment.MAX_BODY_LENGTH + 1);
    assertThatThrownBy(() -> new KanbanComment(null, 1L, "sub-1", "alice", tooLong, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("10000");
  }

  @Test
  void acceptsBodyAtMaxLength() {
    final String atLimit = "x".repeat(KanbanComment.MAX_BODY_LENGTH);
    final KanbanComment c = new KanbanComment(null, 1L, "sub-1", "alice", atLimit, null, null);
    assertThat(c.body()).hasSize(KanbanComment.MAX_BODY_LENGTH);
  }
}
