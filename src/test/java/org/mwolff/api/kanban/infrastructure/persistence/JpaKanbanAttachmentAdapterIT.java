package org.mwolff.api.kanban.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mwolff.api.AbstractIntegrationTest;
import org.mwolff.api.kanban.domain.KanbanAttachment;
import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

/** Integrationstest des Anhang-Adapters gegen Testcontainers-MariaDB (#349). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaKanbanAttachmentAdapter.class, JpaKanbanAdapter.class})
class JpaKanbanAttachmentAdapterIT extends AbstractIntegrationTest {

  private static final String USER = "user-a";
  private static final byte[] BYTES = {10, 20, 30, 40};

  @Autowired private JpaKanbanAttachmentAdapter attachments;
  @Autowired private JpaKanbanAdapter items;
  @Autowired private TestEntityManager em;

  private long createItem(KanbanItemType type) {
    final int number = items.getMaxNumberForUser(USER).map(max -> max + 1).orElse(1);
    final int position =
        type == KanbanItemType.EPIC
            ? 0
            : items.findByUserAndColumn(USER, KanbanColumn.BACKLOG).size();
    return items
        .save(
            KanbanItem.newInstance(
                    USER, "Eintrag", "", KanbanColumn.BACKLOG, position, Instant.now(), type, null)
                .withNumber(number))
        .id();
  }

  @Test
  void saveAndReadRoundTripsBytes() {
    final long itemId = createItem(KanbanItemType.ITEM);
    final KanbanAttachment saved =
        attachments.save(
            KanbanAttachment.newInstance(
                itemId, "doc.pdf", "application/pdf", BYTES, "h", "sub-1"));

    assertThat(saved.id()).isNotNull();
    assertThat(saved.createdAt()).isNotNull();
    assertThat(attachments.findById(saved.id()))
        .hasValueSatisfying(
            a -> {
              assertThat(a.filename()).isEqualTo("doc.pdf");
              assertThat(a.contentType()).isEqualTo("application/pdf");
              assertThat(a.uploadedBySub()).isEqualTo("sub-1");
              assertThat(a.sizeBytes()).isEqualTo(BYTES.length);
              assertThat(a.data()).containsExactly(10, 20, 30, 40);
            });
  }

  @Test
  void findMetaByItemReturnsMetadataSortedOldestFirst() {
    final long itemId = createItem(KanbanItemType.ITEM);
    final KanbanAttachment first =
        attachments.save(
            KanbanAttachment.newInstance(itemId, "a.txt", "text/plain", BYTES, null, "sub-1"));
    final KanbanAttachment second =
        attachments.save(
            KanbanAttachment.newInstance(itemId, "b.txt", "text/plain", BYTES, null, "sub-1"));

    assertThat(attachments.findMetaByItem(itemId))
        .extracting(m -> m.id())
        .containsExactly(first.id(), second.id());
    assertThat(attachments.findMetaByItem(itemId))
        .allSatisfy(
            m -> {
              assertThat(m.sizeBytes()).isEqualTo(BYTES.length);
              assertThat(m.filename()).isNotBlank();
            });
  }

  @Test
  void countByItemCountsOnlyOwnItem() {
    final long itemA = createItem(KanbanItemType.ITEM);
    final long itemB = createItem(KanbanItemType.ITEM);
    attachments.save(KanbanAttachment.newInstance(itemA, "a", "text/plain", BYTES, null, "s"));
    attachments.save(KanbanAttachment.newInstance(itemA, "b", "text/plain", BYTES, null, "s"));
    attachments.save(KanbanAttachment.newInstance(itemB, "c", "text/plain", BYTES, null, "s"));

    assertThat(attachments.countByItem(itemA)).isEqualTo(2);
    assertThat(attachments.countByItem(itemB)).isEqualTo(1);
  }

  @Test
  void deleteByIdRemovesAttachment() {
    final long itemId = createItem(KanbanItemType.ITEM);
    final KanbanAttachment saved =
        attachments.save(
            KanbanAttachment.newInstance(itemId, "x", "text/plain", BYTES, null, "sub-1"));

    attachments.deleteById(saved.id());

    assertThat(attachments.findById(saved.id())).isEmpty();
  }

  @Test
  void deletingItemCascadesToAttachments() {
    final long itemId = createItem(KanbanItemType.ITEM);
    final KanbanAttachment saved =
        attachments.save(
            KanbanAttachment.newInstance(itemId, "x", "text/plain", BYTES, null, "sub-1"));

    items.deleteById(itemId);
    // DB-seitiges ON DELETE CASCADE ist Hibernate unbekannt: Context leeren, damit der Read den
    // DB-Stand sieht statt der gecachten Anhang-Entity.
    em.flush();
    em.clear();

    assertThat(attachments.findById(saved.id())).isEmpty();
    assertThat(attachments.countByItem(itemId)).isZero();
  }

  @Test
  void attachmentWorksOnEpicItemToo() {
    final long epicId = createItem(KanbanItemType.EPIC);
    final KanbanAttachment saved =
        attachments.save(
            KanbanAttachment.newInstance(epicId, "spec.md", "text/markdown", BYTES, null, "sub-1"));

    assertThat(attachments.findById(saved.id())).isPresent();
    assertThat(attachments.findMetaByItem(epicId))
        .extracting(m -> m.filename())
        .containsExactly("spec.md");
  }
}
