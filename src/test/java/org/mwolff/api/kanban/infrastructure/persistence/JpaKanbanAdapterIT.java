package org.mwolff.api.kanban.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mwolff.api.AbstractIntegrationTest;
import org.mwolff.api.kanban.application.ArchiveItemUseCase;
import org.mwolff.api.kanban.application.ForceDeleteItemUseCase;
import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemType;
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

  /**
   * Regressionstest #341: Ein archiviertes Item liegt außerhalb des aktiven Positions-Namespace
   * (active_position = NULL), seine {@code position_in_column} überlappt mit aktiven Items. Der
   * {@link ForceDeleteItemUseCase} darf beim Löschen eines archivierten Items daher NICHT
   * reindizieren — sonst schiebt er aktive Items übereinander und verletzt gegen die echte MariaDB
   * den Unique-Constraint uk_kanban_active_position (früher: 409 „endgültig löschen geht nicht").
   */
  @Test
  void forceDeletingArchivedItemDoesNotViolateActivePositionConstraint() {
    persist(USER_A, "A", "", KanbanColumn.BACKLOG, 0);
    final KanbanItem b = persist(USER_A, "B", "", KanbanColumn.BACKLOG, 1);
    persist(USER_A, "C", "", KanbanColumn.BACKLOG, 2);
    persist(USER_A, "D", "", KanbanColumn.BACKLOG, 3);

    // Archivieren rückt C/D auf; das archivierte B behält seine Position (überlappt jetzt mit C).
    new ArchiveItemUseCase(adapter).execute(USER_A, b.id());

    assertThatCode(() -> new ForceDeleteItemUseCase(adapter).execute(USER_A, b.id()))
        .doesNotThrowAnyException();

    final List<Integer> activePositions =
        adapter.findByUserAndColumn(USER_A, KanbanColumn.BACKLOG).stream()
            .filter(i -> !i.archived())
            .map(KanbanItem::position)
            .toList();
    assertThat(activePositions).containsExactlyInAnyOrder(0, 1, 2);
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
  void archiveDoneOlderThanArchivesOnlyExpiredDoneItems() {
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

    final int archived =
        adapter.archiveDoneOlderThan(USER_A, Instant.parse("2026-03-01T00:00:00Z"));

    assertThat(archived).isEqualTo(1);
    // #327: abgelaufenes DONE-Item ist archiviert (nicht gelöscht) und bleibt in DONE.
    assertThat(adapter.findById(oldDone.id()))
        .hasValueSatisfying(
            i -> {
              assertThat(i.archived()).isTrue();
              assertThat(i.column()).isEqualTo(KanbanColumn.DONE);
            });
    assertThat(adapter.findById(freshDone.id()))
        .hasValueSatisfying(i -> assertThat(i.archived()).isFalse());
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
  void archiveDoneOlderThanSkipsAlreadyArchivedItems() {
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

    final int archived =
        adapter.archiveDoneOlderThan(USER_A, Instant.parse("2026-06-01T00:00:00Z"));

    // Bereits archivierte Items werden nicht erneut angefasst (0 Änderungen).
    assertThat(archived).isEqualTo(0);
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
  void settingsPersistAndReloadActiveFilters() {
    adapter.save(new KanbanSettings(USER_A, 5, java.util.Set.of("BACKLOG", "archived")));
    assertThat(adapter.findByUser(USER_A))
        .hasValueSatisfying(
            s -> assertThat(s.activeFilters()).containsExactlyInAnyOrder("BACKLOG", "archived"));
  }

  @Test
  void settingsPersistEmptyFilterSet() {
    adapter.save(new KanbanSettings(USER_A, 5, java.util.Set.of()));
    assertThat(adapter.findByUser(USER_A))
        .hasValueSatisfying(s -> assertThat(s.activeFilters()).isEmpty());
  }

  @Test
  void settingsSavedViaConvenienceCtorReloadWithDefaultFilters() {
    adapter.save(new KanbanSettings(USER_A, 5));
    assertThat(adapter.findByUser(USER_A))
        .hasValueSatisfying(
            s -> assertThat(s.activeFilters()).isEqualTo(KanbanSettings.DEFAULT_FILTERS));
  }

  @Test
  void createdAndUpdatedAtAreSet() {
    final KanbanItem item = persist(USER_A, "T", "", KanbanColumn.BACKLOG, 0);
    assertThat(item.createdAt()).isNotNull();
    assertThat(item.updatedAt()).isNotNull();
  }

  // ----- Epics (#321) --------------------------------------------------------

  /** Legt ein Epic an (position 0 fix — Epics halten keine aktive Position). */
  private KanbanItem persistEpic(String user, String title) {
    final int number = adapter.getMaxNumberForUser(user).map(max -> max + 1).orElse(1);
    return adapter.save(
        KanbanItem.newInstance(
                user, title, "", KanbanColumn.BACKLOG, 0, Instant.now(), KanbanItemType.EPIC, null)
            .withNumber(number));
  }

  @Test
  void epicRoundTripsTypeAndIsExcludedFromBoardQueries() {
    final KanbanItem epic = persistEpic(USER_A, "Mein Epic");
    persist(USER_A, "Normales Item", "", KanbanColumn.BACKLOG, 0);

    // Direktzugriff liefert das Epic mit Typ...
    assertThat(adapter.findById(epic.id()))
        .hasValueSatisfying(e -> assertThat(e.type()).isEqualTo(KanbanItemType.EPIC));
    // ...aber Board-, Spalten- und Listen-Queries sehen nur ITEMs.
    assertThat(adapter.findAllByUser(USER_A))
        .extracting(KanbanItem::title)
        .containsExactly("Normales Item");
    assertThat(adapter.findByUserAndColumn(USER_A, KanbanColumn.BACKLOG))
        .extracting(KanbanItem::title)
        .containsExactly("Normales Item");
    assertThat(adapter.findAllByUserIncludingArchived(USER_A))
        .extracting(KanbanItem::title)
        .containsExactly("Normales Item");
  }

  @Test
  void epicDoesNotCollideWithItemPositionZero() {
    // Beide auf (BACKLOG, position 0): der Unique-Index uk_kanban_active_position darf nicht
    // anschlagen, weil Epics per V22 aus dem aktiven Positions-Namespace fallen.
    persistEpic(USER_A, "Epic auf Position 0");
    final KanbanItem item = persist(USER_A, "Item auf Position 0", "", KanbanColumn.BACKLOG, 0);

    assertThat(adapter.findById(item.id())).isPresent();
  }

  @Test
  void findEpicsByUserReturnsOnlyOwnEpicsOrderedByNumber() {
    // Reihenfolge: Nummer aufsteigend. Zweites Epic zuerst anlegen, damit die Sortierung nicht
    // zufällig mit der Insert-Reihenfolge übereinstimmt.
    final KanbanItem second = persistEpic(USER_A, "Zweites Epic");
    final KanbanItem first = persistEpic(USER_A, "Erstes Epic");
    persist(USER_A, "Kein Epic", "", KanbanColumn.BACKLOG, 0);
    persistEpic(USER_B, "Fremdes Epic");

    assertThat(adapter.findEpicsByUser(USER_A))
        .extracting(KanbanItem::id)
        .containsExactly(second.id(), first.id()); // second hat die kleinere Nummer
    assertThat(adapter.findEpicsByUser(USER_A))
        .allSatisfy(e -> assertThat(e.type()).isEqualTo(KanbanItemType.EPIC));
  }

  @Test
  void shortcodeRoundTripsOnInsertAndUpdate() {
    final int number = adapter.getMaxNumberForUser(USER_A).map(max -> max + 1).orElse(1);
    final KanbanItem epic =
        adapter.save(
            KanbanItem.newInstance(
                    USER_A,
                    "Epic",
                    "",
                    KanbanColumn.BACKLOG,
                    0,
                    Instant.now(),
                    KanbanItemType.EPIC,
                    null,
                    "ITB")
                .withNumber(number));

    assertThat(epic.shortcode()).isEqualTo("ITB");
    assertThat(adapter.findById(epic.id()))
        .hasValueSatisfying(e -> assertThat(e.shortcode()).isEqualTo("ITB"));

    // Update-Pfad ändert das Kürzel (withContent(3-arg)).
    final KanbanItem renamed = adapter.save(epic.withContent("Epic", "", "NEU"));
    assertThat(renamed.shortcode()).isEqualTo("NEU");
    assertThat(adapter.findById(epic.id()))
        .hasValueSatisfying(e -> assertThat(e.shortcode()).isEqualTo("NEU"));
  }

  @Test
  void parentIdRoundTripsOnInsertAndUpdate() {
    final KanbanItem epic = persistEpic(USER_A, "Parent-Epic");
    final int number = adapter.getMaxNumberForUser(USER_A).map(max -> max + 1).orElse(1);
    final KanbanItem story =
        adapter.save(
            KanbanItem.newInstance(
                    USER_A,
                    "Story",
                    "",
                    KanbanColumn.BACKLOG,
                    0,
                    Instant.now(),
                    KanbanItemType.ITEM,
                    epic.id())
                .withNumber(number));

    assertThat(story.parentId()).isEqualTo(epic.id());
    assertThat(adapter.findById(story.id()))
        .hasValueSatisfying(s -> assertThat(s.parentId()).isEqualTo(epic.id()));

    // Update-Pfad erhält die Zuordnung (withContent trägt parentId weiter).
    final KanbanItem renamed = adapter.save(story.withContent("Story umbenannt", "b"));
    assertThat(renamed.parentId()).isEqualTo(epic.id());
  }

  @Test
  void countChildrenCountsAllReferencingItemsIncludingArchived() {
    final KanbanItem epic = persistEpic(USER_A, "Epic mit Kindern");
    final KanbanItem other = persistEpic(USER_A, "Anderes Epic");

    assertThat(adapter.countChildren(epic.id())).isZero();

    final KanbanItem child = persistChild(USER_A, "Kind", epic.id());
    final KanbanItem archivedChild = persistChild(USER_A, "Archiviertes Kind", epic.id());
    persistChild(USER_A, "Kind von anderem Epic", other.id());

    adapter.archiveById(archivedChild.id());

    // Archivierte Kinder halten weiterhin eine Referenz und zählen mit (#330).
    assertThat(adapter.countChildren(epic.id())).isEqualTo(2);
    assertThat(adapter.countChildren(other.id())).isEqualTo(1);

    adapter.deleteById(child.id());
    adapter.deleteById(archivedChild.id());
    assertThat(adapter.countChildren(epic.id())).isZero();
  }

  private KanbanItem persistChild(String user, String title, long parentId) {
    final int number = adapter.getMaxNumberForUser(user).map(max -> max + 1).orElse(1);
    return adapter.save(
        KanbanItem.newInstance(
                user,
                title,
                "",
                KanbanColumn.BACKLOG,
                number,
                Instant.now(),
                KanbanItemType.ITEM,
                parentId)
            .withNumber(number));
  }
}
