package org.mwolff.api.kanban.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class KanbanAttachmentTest {

  private static final byte[] DATA = {1, 2, 3};

  @Test
  void newInstanceLeavesIdAndCreatedAtNull() {
    final KanbanAttachment a =
        KanbanAttachment.newInstance(7L, "doc.pdf", "application/pdf", DATA, "hash", "sub-1");
    assertThat(a.id()).isNull();
    assertThat(a.createdAt()).isNull();
    assertThat(a.itemId()).isEqualTo(7L);
    assertThat(a.sizeBytes()).isEqualTo(3);
    assertThat(a.uploadedBySub()).isEqualTo("sub-1");
  }

  @Test
  void dataIsDefensivelyCopiedOnConstruction() {
    final byte[] source = {1, 2, 3};
    final KanbanAttachment a =
        KanbanAttachment.newInstance(1L, "f", "text/plain", source, null, "sub");
    source[0] = 9; // Mutation der Quelle darf nicht durchschlagen
    assertThat(a.data()).containsExactly(1, 2, 3);
  }

  @Test
  void dataAccessorReturnsCopy() {
    final KanbanAttachment a =
        KanbanAttachment.newInstance(1L, "f", "text/plain", DATA, null, "sub");
    a.data()[0] = 9; // Mutation der Kopie darf den internen Zustand nicht ändern
    assertThat(a.data()).containsExactly(1, 2, 3);
  }

  @Test
  void rejectsNullFilename() {
    assertThatThrownBy(
            () -> KanbanAttachment.newInstance(1L, null, "text/plain", DATA, null, "sub"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsBlankFilename() {
    assertThatThrownBy(
            () -> KanbanAttachment.newInstance(1L, "  ", "text/plain", DATA, null, "sub"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsTooLongFilename() {
    final String longName = "x".repeat(KanbanAttachment.MAX_FILENAME_LENGTH + 1);
    assertThatThrownBy(
            () -> KanbanAttachment.newInstance(1L, longName, "text/plain", DATA, null, "sub"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsBlankContentType() {
    assertThatThrownBy(() -> KanbanAttachment.newInstance(1L, "f", " ", DATA, null, "sub"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsBlankUploadedBySub() {
    assertThatThrownBy(() -> KanbanAttachment.newInstance(1L, "f", "text/plain", DATA, null, " "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsEmptyData() {
    assertThatThrownBy(
            () -> KanbanAttachment.newInstance(1L, "f", "text/plain", new byte[0], null, "sub"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullData() {
    assertThatThrownBy(() -> KanbanAttachment.newInstance(1L, "f", "text/plain", null, null, "sub"))
        .isInstanceOf(NullPointerException.class);
  }
}
