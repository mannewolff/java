package org.mwolff.api.kanban.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class KanbanAttachmentMetaTest {

  @Test
  void exposesAllMetadataFields() {
    final Instant now = Instant.parse("2026-07-07T10:00:00Z");
    final KanbanAttachmentMeta meta =
        new KanbanAttachmentMeta(3L, 7L, "doc.pdf", "application/pdf", 42, "sub-1", now);

    assertThat(meta.id()).isEqualTo(3L);
    assertThat(meta.itemId()).isEqualTo(7L);
    assertThat(meta.filename()).isEqualTo("doc.pdf");
    assertThat(meta.contentType()).isEqualTo("application/pdf");
    assertThat(meta.sizeBytes()).isEqualTo(42);
    assertThat(meta.uploadedBySub()).isEqualTo("sub-1");
    assertThat(meta.createdAt()).isEqualTo(now);
  }
}
