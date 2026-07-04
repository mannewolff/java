package org.mwolff.api.kanban.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mwolff.api.AbstractIntegrationTest;
import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanComment;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/** Integrationstest des Kommentar-Adapters gegen Testcontainers-MariaDB. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaKanbanCommentAdapter.class, JpaKanbanAdapter.class})
class JpaKanbanCommentAdapterIT extends AbstractIntegrationTest {

  private static final String USER = "user-a";

  @Autowired private JpaKanbanCommentAdapter comments;
  @Autowired private JpaKanbanAdapter items;
  @Autowired private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager em;

  private long createItem() {
    // Eindeutige pro-User-Nummer vergeben (#187), sonst verletzen mehrere Items den Unique-Index.
    final int number = items.getMaxNumberForUser(USER).map(max -> max + 1).orElse(1);
    // Eindeutige, lückenlose Position pro Spalte (#309), sonst verletzt das zweite Item den
    // Unique-Constraint uk_kanban_active_position.
    final int position = items.findByUserAndColumn(USER, KanbanColumn.BACKLOG).size();
    return items
        .save(
            KanbanItem.newInstance(USER, "Item", "", KanbanColumn.BACKLOG, position, Instant.now())
                .withNumber(number))
        .id();
  }

  @Test
  void saveAndReadComment() {
    final long itemId = createItem();

    final KanbanComment saved = comments.save(KanbanComment.newInstance(itemId, "alice", "Hallo"));

    assertThat(saved.id()).isNotNull();
    assertThat(saved.createdAt()).isNotNull();
    assertThat(saved.updatedAt()).isNotNull();
    assertThat(comments.findById(saved.id()))
        .hasValueSatisfying(c -> assertThat(c.body()).isEqualTo("Hallo"));
  }

  @Test
  void findByItemNewestFirstOrdersByIdDescTiebreak() {
    final long itemId = createItem();
    final KanbanComment first = comments.save(KanbanComment.newInstance(itemId, "alice", "erst"));
    final KanbanComment second = comments.save(KanbanComment.newInstance(itemId, "alice", "zweit"));

    assertThat(comments.findByItemNewestFirst(itemId))
        .extracting(KanbanComment::id)
        .containsExactly(second.id(), first.id());
  }

  @Test
  void findByItemNewestFirstFiltersByItem() {
    final long itemA = createItem();
    final long itemB = createItem();
    comments.save(KanbanComment.newInstance(itemA, "alice", "A"));
    comments.save(KanbanComment.newInstance(itemB, "alice", "B"));

    assertThat(comments.findByItemNewestFirst(itemA))
        .extracting(KanbanComment::body)
        .containsExactly("A");
  }

  @Test
  void saveExistingUpdatesBody() {
    final long itemId = createItem();
    final KanbanComment saved = comments.save(KanbanComment.newInstance(itemId, "alice", "alt"));

    final KanbanComment persisted = comments.save(saved.withBody("neu"));

    assertThat(persisted.id()).isEqualTo(saved.id());
    assertThat(persisted.body()).isEqualTo("neu");
    assertThat(comments.findById(saved.id()))
        .hasValueSatisfying(c -> assertThat(c.body()).isEqualTo("neu"));
  }

  @Test
  void deleteByIdRemovesComment() {
    final long itemId = createItem();
    final KanbanComment saved = comments.save(KanbanComment.newInstance(itemId, "alice", "x"));

    comments.deleteById(saved.id());

    assertThat(comments.findById(saved.id())).isEmpty();
  }

  @Test
  void deletingItemCascadesToComments() {
    final long itemId = createItem();
    final KanbanComment saved = comments.save(KanbanComment.newInstance(itemId, "alice", "x"));

    items.deleteById(itemId);
    // DB-seitiges ON DELETE CASCADE ist Hibernate unbekannt: Context leeren, damit der Read
    // den DB-Stand sieht statt der gecachten Kommentar-Entity.
    em.flush();
    em.clear();

    assertThat(comments.findById(saved.id())).isEmpty();
  }
}
