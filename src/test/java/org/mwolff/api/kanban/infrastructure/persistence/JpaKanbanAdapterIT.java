package org.mwolff.api.kanban.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mwolff.api.AbstractIntegrationTest;
import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/** Integrationstest des Kanban-Adapters gegen Testcontainers-MariaDB. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaKanbanAdapter.class)
class JpaKanbanAdapterIT extends AbstractIntegrationTest {

  @Autowired private JpaKanbanAdapter adapter;

  private static final String USER_A = "user-a";
  private static final String USER_B = "user-b";

  /**
   * Legt ein Item an und vergibt — wie der CreateItemUseCase (#187) — die nächste pro-User
   * eindeutige Nummer, damit der Unique-Index uk_kanban_item_number_per_user nicht verletzt wird.
   */
  private KanbanItem persist(
      String user, String title, String body, KanbanColumn column, int position) {
    final int number = adapter.getMaxNumberForUser(user).map(max -> max + 1).orElse(1);
    return adapter.save(
        KanbanItem.newInstance(user, title, body, column, position, Instant.now())
            .withNumber(number));
  }

  // ----- Items --------------------------------------------------------------

  @Test
  void saveAndReadItemFromBacklog() {
    final KanbanItem saved = persist(USER_A, "Title", "body", KanbanColumn.BACKLOG, 0);

    assertThat(saved.id()).isNotNull();
    assertThat(saved.createdAt()).isNotNull();
    assertThat(adapter.findById(saved.id()))
        .hasValueSatisfying(i -> assertThat(i.title()).isEqualTo("Title"));
  }

  @Test
  void saveAndReadItemFromReady() {
    final KanbanItem saved = persist(USER_A, "Ready-Item", "body", KanbanColumn.READY, 0);

    assertThat(adapter.findById(saved.id()))
        .hasValueSatisfying(i -> assertThat(i.column()).isEqualTo(KanbanColumn.READY));
    assertThat(adapter.findByUserAndColumn(USER_A, KanbanColumn.READY))
        .extracting(KanbanItem::id)
        .containsExactly(saved.id());
  }

  @Test
  void findByUserAndColumnReturnsSorted() {
    final KanbanItem a = persist(USER_A, "A", "", KanbanColumn.BACKLOG, 0);
    final KanbanItem b = persist(USER_A, "B", "", KanbanColumn.BACKLOG, 1);

    assertThat(adapter.findByUserAndColumn(USER_A, KanbanColumn.BACKLOG))
        .extracting(KanbanItem::id)
        .containsExactly(a.id(), b.id());
  }

  @Test
  void findAllByUserFiltersByOwner() {
    persist(USER_A, "Mine", "", KanbanColumn.BACKLOG, 0);
    persist(USER_B, "Other", "", KanbanColumn.BACKLOG, 0);

    assertThat(adapter.findAllByUser(USER_A)).extracting(KanbanItem::title).containsExactly("Mine");
  }

  @Test
  void updatePositionPersists() {
    final KanbanItem a = persist(USER_A, "A", "", KanbanColumn.BACKLOG, 0);
    adapter.updatePosition(a.id(), 5);
    assertThat(adapter.findById(a.id()))
        .hasValueSatisfying(i -> assertThat(i.position()).isEqualTo(5));
  }

  @Test
  void saveExistingUpdatesTitleColumnPositionAndMovedToDoneAt() {
    final KanbanItem a = persist(USER_A, "Old", "old body", KanbanColumn.BACKLOG, 0);

    final Instant doneAt = Instant.parse("2026-05-27T12:00:00Z");
    final KanbanItem moved =
        new KanbanItem(
            a.id(),
            USER_A,
            "New",
            "new body",
            KanbanColumn.DONE,
            0,
            a.createdAt(),
            a.updatedAt(),
            doneAt,
            false,
            a.number());
    final KanbanItem persisted = adapter.save(moved);

    assertThat(persisted.title()).isEqualTo("New");
    assertThat(persisted.body()).isEqualTo("new body");
    assertThat(persisted.column()).isEqualTo(KanbanColumn.DONE);
    assertThat(persisted.movedToDoneAt()).isEqualTo(doneAt);
  }

  @Test
  void deleteByIdRemoves() {
    final KanbanItem a = persist(USER_A, "A", "", KanbanColumn.BACKLOG, 0);
    adapter.deleteById(a.id());
    assertThat(adapter.findById(a.id())).isEmpty();
  }

  @Test
  void deleteDoneOlderThanRemovesOnlyExpiredDoneItems() {
    final Instant old = Instant.parse("2026-01-01T00:00:00Z");
    final Instant fresh = Instant.parse("2026-05-27T00:00:00Z");
    // Erst alle anlegen, dann via save() in DONE setzen mit explizitem movedToDoneAt.
    final KanbanItem oldDone = persist(USER_A, "Old", "", KanbanColumn.DONE, 0);
    final KanbanItem freshDone = persist(USER_A, "Fresh", "", KanbanColumn.DONE, 1);
    final KanbanItem backlog = persist(USER_A, "Backlog", "", KanbanColumn.BACKLOG, 0);
    // movedToDoneAt für oldDone ueberschreiben
    adapter.save(
        new KanbanItem(
            oldDone.id(),
            USER_A,
            "Old",
            "",
            KanbanColumn.DONE,
            0,
            oldDone.createdAt(),
            oldDone.updatedAt(),
            old,
            false,
            oldDone.number()));
    adapter.save(
        new KanbanItem(
            freshDone.id(),
            USER_A,
            "Fresh",
            "",
            KanbanColumn.DONE,
            1,
            freshDone.createdAt(),
            freshDone.updatedAt(),
            fresh,
            false,
            freshDone.number()));

    final int deleted = adapter.deleteDoneOlderThan(USER_A, Instant.parse("2026-03-01T00:00:00Z"));

    assertThat(deleted).isEqualTo(1);
    assertThat(adapter.findById(oldDone.id())).isEmpty();
    assertThat(adapter.findById(freshDone.id())).isPresent();
    assertThat(adapter.findById(backlog.id())).isPresent();
  }

  @Test
  void distinctUsersWithDoneItemsListsOwners() {
    persist(USER_A, "A", "", KanbanColumn.DONE, 0);
    persist(USER_B, "B", "", KanbanColumn.DONE, 0);
    persist("user-c", "C", "", KanbanColumn.BACKLOG, 0);

    assertThat(adapter.distinctUsersWithDoneItems()).containsExactlyInAnyOrder(USER_A, USER_B);
  }

  @Test
  void archiveByIdSetsArchivedTrue() {
    final KanbanItem item = persist(USER_A, "T", "", KanbanColumn.BACKLOG, 0);
    assertThat(item.archived()).isFalse();

    adapter.archiveById(item.id());

    assertThat(adapter.findById(item.id()))
        .hasValueSatisfying(i -> assertThat(i.archived()).isTrue());
  }

  @Test
  void restoreByIdSetsArchivedFalse() {
    final KanbanItem item = persist(USER_A, "T", "", KanbanColumn.BACKLOG, 0);
    adapter.archiveById(item.id());
    assertThat(adapter.findById(item.id()))
        .hasValueSatisfying(i -> assertThat(i.archived()).isTrue());

    adapter.restoreById(item.id());

    assertThat(adapter.findById(item.id()))
        .hasValueSatisfying(i -> assertThat(i.archived()).isFalse());
  }

  @Test
  void findAllByUserExcludesArchivedByDefault() {
    final KanbanItem active = persist(USER_A, "Active", "", KanbanColumn.BACKLOG, 0);
    final KanbanItem archived = persist(USER_A, "Archived", "", KanbanColumn.BACKLOG, 1);
    adapter.archiveById(archived.id());

    assertThat(adapter.findAllByUser(USER_A))
        .extracting(KanbanItem::id)
        .containsExactly(active.id());
  }

  @Test
  void findAllByUserIncludingArchivedReturnsAll() {
    final KanbanItem active = persist(USER_A, "Active", "", KanbanColumn.BACKLOG, 0);
    final KanbanItem archived = persist(USER_A, "Archived", "", KanbanColumn.BACKLOG, 1);
    adapter.archiveById(archived.id());

    assertThat(adapter.findAllByUserIncludingArchived(USER_A))
        .extracting(KanbanItem::id)
        .containsExactlyInAnyOrder(active.id(), archived.id());
  }

  @Test
  void deleteDoneOlderThanSkipsArchivedItems() {
    final KanbanItem archivedDone = persist(USER_A, "OldArchived", "", KanbanColumn.DONE, 0);
    adapter.save(
        new KanbanItem(
            archivedDone.id(),
            USER_A,
            "OldArchived",
            "",
            KanbanColumn.DONE,
            0,
            archivedDone.createdAt(),
            archivedDone.updatedAt(),
            Instant.parse("2026-01-01T00:00:00Z"),
            false,
            archivedDone.number()));
    adapter.archiveById(archivedDone.id());

    final int deleted = adapter.deleteDoneOlderThan(USER_A, Instant.parse("2026-06-01T00:00:00Z"));

    assertThat(deleted).isEqualTo(0);
    assertThat(adapter.findById(archivedDone.id())).isPresent();
  }

  // ----- Settings -----------------------------------------------------------

  @Test
  void settingsFindByUserEmptyByDefault() {
    assertThat(adapter.findByUser("nobody")).isEmpty();
  }

  @Test
  void settingsSaveAndLoad() {
    final KanbanSettings saved = adapter.save(new KanbanSettings(USER_A, 14));
    assertThat(saved.doneRetentionDays()).isEqualTo(14);
    assertThat(adapter.findByUser(USER_A))
        .hasValueSatisfying(s -> assertThat(s.doneRetentionDays()).isEqualTo(14));
  }

  @Test
  void settingsUpsertOverwritesValue() {
    adapter.save(new KanbanSettings(USER_A, 10));
    adapter.save(new KanbanSettings(USER_A, 20));
    assertThat(adapter.findByUser(USER_A))
        .hasValueSatisfying(s -> assertThat(s.doneRetentionDays()).isEqualTo(20));
  }

  @Test
  void createdAndUpdatedAtAreSet() {
    final KanbanItem item = persist(USER_A, "T", "", KanbanColumn.BACKLOG, 0);
    assertThat(item.createdAt()).isNotNull();
    assertThat(item.updatedAt()).isNotNull();
  }
}
