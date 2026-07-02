package org.mwolff.api.kanban.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.mwolff.api.kanban.domain.KanbanSettings;
import org.mwolff.api.kanban.domain.KanbanSettingsPort;

class KanbanUseCasesTest {

  private static final String SUB_OWNER = "user-1";
  private static final String SUB_OTHER = "user-2";
  private static final Instant FIXED_NOW = Instant.parse("2026-05-27T12:00:00Z");

  private final KanbanItemPort items = mock(KanbanItemPort.class);
  private final KanbanSettingsPort settings = mock(KanbanSettingsPort.class);
  private final Clock clock = Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));

  private static KanbanItem item(
      long id, String sub, KanbanColumn column, int position, Instant movedToDoneAt) {
    return new KanbanItem(
        id,
        sub,
        "T-" + id,
        "body-" + id,
        column,
        position,
        Instant.EPOCH,
        Instant.EPOCH,
        movedToDoneAt,
        false,
        0);
  }

  private static KanbanItem item(long id, String sub, KanbanColumn column, int position) {
    return item(id, sub, column, position, column == KanbanColumn.DONE ? Instant.EPOCH : null);
  }

  private static KanbanItem archivedItem(long id, String sub, KanbanColumn column, int position) {
    return new KanbanItem(
        id,
        sub,
        "T-" + id,
        "body-" + id,
        column,
        position,
        Instant.EPOCH,
        Instant.EPOCH,
        column == KanbanColumn.DONE ? Instant.EPOCH : null,
        true,
        0);
  }

  // ----- list ---------------------------------------------------------------

  @Test
  void listShouldGroupItemsByColumn() {
    given(items.findAllByUser(SUB_OWNER))
        .willReturn(
            List.of(
                item(1, SUB_OWNER, KanbanColumn.BACKLOG, 0),
                item(2, SUB_OWNER, KanbanColumn.IN_PROGRESS, 0),
                item(3, SUB_OWNER, KanbanColumn.BACKLOG, 1)));

    final Map<KanbanColumn, List<KanbanItem>> result =
        new ListItemsUseCase(items).execute(SUB_OWNER);

    assertThat(result).containsOnlyKeys(KanbanColumn.values());
    assertThat(result.get(KanbanColumn.BACKLOG)).hasSize(2);
    assertThat(result.get(KanbanColumn.READY)).isEmpty();
    assertThat(result.get(KanbanColumn.IN_PROGRESS)).hasSize(1);
    assertThat(result.get(KanbanColumn.IN_REVIEW)).isEmpty();
    assertThat(result.get(KanbanColumn.DONE)).isEmpty();
  }

  @Test
  void createShouldAllowReadyAsExplicitColumn() {
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.READY)).willReturn(List.of());
    given(items.save(any())).willAnswer(inv -> inv.getArgument(0));

    final KanbanItem created =
        new CreateItemUseCase(items, clock).execute(SUB_OWNER, "Neu", "", KanbanColumn.READY);

    assertThat(created.column()).isEqualTo(KanbanColumn.READY);
  }

  @Test
  void moveFromBacklogToReadyReindexesSource() {
    final KanbanItem a = item(1, SUB_OWNER, KanbanColumn.BACKLOG, 0);
    final KanbanItem b = item(2, SUB_OWNER, KanbanColumn.BACKLOG, 1);
    given(items.findById(1L)).willReturn(Optional.of(a));
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.BACKLOG)).willReturn(List.of(b));
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.READY)).willReturn(List.of());
    given(items.save(any())).willAnswer(inv -> inv.getArgument(0));

    final KanbanItem moved =
        new MoveItemUseCase(items, clock).execute(SUB_OWNER, 1L, KanbanColumn.READY, 0);

    assertThat(moved.column()).isEqualTo(KanbanColumn.READY);
    verify(items).updatePosition(2L, 0);
  }

  // ----- create -------------------------------------------------------------

  @Test
  void createShouldAppendToTargetColumnAtEnd() {
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.BACKLOG))
        .willReturn(List.of(item(1, SUB_OWNER, KanbanColumn.BACKLOG, 0)));
    given(items.save(any())).willAnswer(inv -> inv.getArgument(0));

    final KanbanItem created =
        new CreateItemUseCase(items, clock).execute(SUB_OWNER, "Neu", "body", null);

    assertThat(created.column()).isEqualTo(KanbanColumn.BACKLOG);
    assertThat(created.position()).isEqualTo(1);
  }

  @Test
  void createShouldUseExplicitColumnWhenProvided() {
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.DONE)).willReturn(List.of());
    given(items.save(any())).willAnswer(inv -> inv.getArgument(0));

    final KanbanItem created =
        new CreateItemUseCase(items, clock).execute(SUB_OWNER, "Neu", "", KanbanColumn.DONE);

    assertThat(created.column()).isEqualTo(KanbanColumn.DONE);
    assertThat(created.movedToDoneAt()).isNotNull();
  }

  @Test
  void createShouldAssignNumberOneForFirstItem() {
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.BACKLOG)).willReturn(List.of());
    given(items.getMaxNumberForUser(SUB_OWNER)).willReturn(java.util.Optional.empty());
    given(items.save(any())).willAnswer(inv -> inv.getArgument(0));

    final KanbanItem created =
        new CreateItemUseCase(items, clock).execute(SUB_OWNER, "Neu", "", null);

    assertThat(created.number()).isEqualTo(1);
  }

  @Test
  void createShouldAssignNextNumberAfterExistingMax() {
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.BACKLOG)).willReturn(List.of());
    given(items.getMaxNumberForUser(SUB_OWNER)).willReturn(java.util.Optional.of(5));
    given(items.save(any())).willAnswer(inv -> inv.getArgument(0));

    final KanbanItem created =
        new CreateItemUseCase(items, clock).execute(SUB_OWNER, "Neu", "", null);

    assertThat(created.number()).isEqualTo(6);
  }

  // ----- update content -----------------------------------------------------

  @Test
  void updateContentShouldPersistNewTitleAndBody() {
    given(items.findById(1L)).willReturn(Optional.of(item(1, SUB_OWNER, KanbanColumn.BACKLOG, 0)));
    given(items.save(any())).willAnswer(inv -> inv.getArgument(0));

    final KanbanItem updated =
        new UpdateItemContentUseCase(items).execute(SUB_OWNER, 1L, "Neuer Titel", "Neuer Body");

    assertThat(updated.title()).isEqualTo("Neuer Titel");
    assertThat(updated.body()).isEqualTo("Neuer Body");
  }

  @Test
  void updateContentShouldThrowForForeignItem() {
    given(items.findById(1L)).willReturn(Optional.of(item(1, SUB_OWNER, KanbanColumn.BACKLOG, 0)));

    assertThatThrownBy(() -> new UpdateItemContentUseCase(items).execute(SUB_OTHER, 1L, "x", "y"))
        .isInstanceOf(KanbanItemNotFoundException.class);
    verify(items, never()).save(any());
  }

  @Test
  void updateContentShouldThrowWhenMissing() {
    given(items.findById(99L)).willReturn(Optional.empty());
    assertThatThrownBy(() -> new UpdateItemContentUseCase(items).execute(SUB_OWNER, 99L, "x", "y"))
        .isInstanceOf(KanbanItemNotFoundException.class);
  }

  // ----- move ---------------------------------------------------------------

  @Test
  void moveCrossColumnShouldReindexBothColumnsAndSetMovedToDoneAt() {
    // BACKLOG: a(0), b(1), c(2)  →  Move b to IN_PROGRESS pos 0.
    final KanbanItem a = item(1, SUB_OWNER, KanbanColumn.BACKLOG, 0);
    final KanbanItem b = item(2, SUB_OWNER, KanbanColumn.BACKLOG, 1);
    final KanbanItem c = item(3, SUB_OWNER, KanbanColumn.BACKLOG, 2);
    given(items.findById(2L)).willReturn(Optional.of(b));
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.BACKLOG))
        .willReturn(List.of(a, c)); // Quelle nach implizitem Remove
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.IN_PROGRESS)).willReturn(List.of());
    given(items.save(any())).willAnswer(inv -> inv.getArgument(0));

    final KanbanItem moved =
        new MoveItemUseCase(items, clock).execute(SUB_OWNER, 2L, KanbanColumn.IN_PROGRESS, 0);

    assertThat(moved.column()).isEqualTo(KanbanColumn.IN_PROGRESS);
    assertThat(moved.position()).isEqualTo(0);
    // Reindex der Quelle (Items mit position > 1 verschieben sich um 1 runter).
    // In unserem Mock-Modell ist die Quelle nach "implizitem Remove" als
    // findByUserAndColumn-Antwort
    // gegeben — wir prüfen, dass c (position=2) re-indiziert wird.
    verify(items).updatePosition(3L, 1);
  }

  @Test
  void moveEnteringDoneSetsMovedToDoneAtFromClock() {
    final KanbanItem source = item(1, SUB_OWNER, KanbanColumn.IN_REVIEW, 0, null);
    given(items.findById(1L)).willReturn(Optional.of(source));
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.IN_REVIEW)).willReturn(List.of());
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.DONE)).willReturn(List.of());
    given(items.save(any())).willAnswer(inv -> inv.getArgument(0));

    final KanbanItem moved =
        new MoveItemUseCase(items, clock).execute(SUB_OWNER, 1L, KanbanColumn.DONE, 0);

    assertThat(moved.movedToDoneAt()).isEqualTo(FIXED_NOW);
  }

  @Test
  void moveLeavingDoneResetsMovedToDoneAt() {
    final KanbanItem source = item(1, SUB_OWNER, KanbanColumn.DONE, 0, Instant.EPOCH);
    given(items.findById(1L)).willReturn(Optional.of(source));
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.DONE)).willReturn(List.of());
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.BACKLOG)).willReturn(List.of());
    given(items.save(any())).willAnswer(inv -> inv.getArgument(0));

    final KanbanItem moved =
        new MoveItemUseCase(items, clock).execute(SUB_OWNER, 1L, KanbanColumn.BACKLOG, 0);

    assertThat(moved.movedToDoneAt()).isNull();
  }

  @Test
  void moveSameColumnDownShiftsBetweenItemsUp() {
    // BACKLOG: a(0), b(1), c(2), d(3)  →  Move a to position 2.
    final KanbanItem a = item(1, SUB_OWNER, KanbanColumn.BACKLOG, 0);
    final KanbanItem b = item(2, SUB_OWNER, KanbanColumn.BACKLOG, 1);
    final KanbanItem c = item(3, SUB_OWNER, KanbanColumn.BACKLOG, 2);
    final KanbanItem d = item(4, SUB_OWNER, KanbanColumn.BACKLOG, 3);
    given(items.findById(1L)).willReturn(Optional.of(a));
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.BACKLOG))
        .willReturn(List.of(a, b, c, d));
    given(items.save(any())).willAnswer(inv -> inv.getArgument(0));

    final KanbanItem moved =
        new MoveItemUseCase(items, clock).execute(SUB_OWNER, 1L, KanbanColumn.BACKLOG, 2);

    assertThat(moved.position()).isEqualTo(2);
    verify(items).updatePosition(2L, 0); // b: 1 -> 0
    verify(items).updatePosition(3L, 1); // c: 2 -> 1
    verify(items, never()).updatePosition(4L, 2); // d bleibt
    // Das verschobene Item selbst (a, position 0) darf NIE reindexiert werden — killt den
    // Grenzwert-Mutanten `position > fromPosition` -> `>=` (#203).
    verify(items, never())
        .updatePosition(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.anyInt());
  }

  @Test
  void moveSameColumnUpShiftsBetweenItemsDown() {
    // BACKLOG: a(0), b(1), c(2), d(3)  →  Move d to position 1.
    final KanbanItem a = item(1, SUB_OWNER, KanbanColumn.BACKLOG, 0);
    final KanbanItem b = item(2, SUB_OWNER, KanbanColumn.BACKLOG, 1);
    final KanbanItem c = item(3, SUB_OWNER, KanbanColumn.BACKLOG, 2);
    final KanbanItem d = item(4, SUB_OWNER, KanbanColumn.BACKLOG, 3);
    given(items.findById(4L)).willReturn(Optional.of(d));
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.BACKLOG))
        .willReturn(List.of(a, b, c, d));
    given(items.save(any())).willAnswer(inv -> inv.getArgument(0));

    new MoveItemUseCase(items, clock).execute(SUB_OWNER, 4L, KanbanColumn.BACKLOG, 1);

    verify(items).updatePosition(2L, 2); // b: 1 -> 2
    verify(items).updatePosition(3L, 3); // c: 2 -> 3
    // Das verschobene Item selbst (d, position 3) darf NIE reindexiert werden — killt den
    // Grenzwert-Mutanten `position < fromPosition` -> `<=` (#203).
    verify(items, never())
        .updatePosition(org.mockito.ArgumentMatchers.eq(4L), org.mockito.ArgumentMatchers.anyInt());
  }

  @Test
  void moveSameColumnClampedToCurrentPositionIsEarlyReturn() {
    // Same-Column, targetPosition außerhalb gültigen Bereichs, clamp landet zufällig auf
    // fromPosition. Hier prüft der Use-Case den early-return im reindex (Zeile clamped==from).
    final KanbanItem a = item(1, SUB_OWNER, KanbanColumn.BACKLOG, 0);
    final KanbanItem b = item(2, SUB_OWNER, KanbanColumn.BACKLOG, 1);
    final KanbanItem c = item(3, SUB_OWNER, KanbanColumn.BACKLOG, 2);
    given(items.findById(3L)).willReturn(Optional.of(c));
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.BACKLOG)).willReturn(List.of(a, b, c));
    given(items.save(any())).willAnswer(inv -> inv.getArgument(0));

    final KanbanItem moved =
        new MoveItemUseCase(items, clock).execute(SUB_OWNER, 3L, KanbanColumn.BACKLOG, 99);

    // Clamp auf max = size-1 = 2 (kein +1) und Early-Return liefert genau diese Position
    // zurück (kein 0) — killt Math- und PrimitiveReturns-Mutanten (#203).
    assertThat(moved.position()).isEqualTo(2);
    // Keine Reindex-Aufrufe, weil clamped (= 2) == fromPosition (= 2).
    verify(items, never())
        .updatePosition(
            org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt());
  }

  @Test
  void moveCrossColumnInsertInMiddleShiftsOnlyTailOfTarget() {
    // BACKLOG: a(0)  →  IN_PROGRESS: x(0), y(1), z(2). Move a → IN_PROGRESS pos 1.
    // x (pos < 1) bleibt, y und z rutschen je um 1 hoch.
    final KanbanItem a = item(1, SUB_OWNER, KanbanColumn.BACKLOG, 0);
    final KanbanItem x = item(10, SUB_OWNER, KanbanColumn.IN_PROGRESS, 0);
    final KanbanItem y = item(11, SUB_OWNER, KanbanColumn.IN_PROGRESS, 1);
    final KanbanItem z = item(12, SUB_OWNER, KanbanColumn.IN_PROGRESS, 2);
    given(items.findById(1L)).willReturn(Optional.of(a));
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.BACKLOG)).willReturn(List.of());
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.IN_PROGRESS))
        .willReturn(List.of(x, y, z));
    given(items.save(any())).willAnswer(inv -> inv.getArgument(0));

    final KanbanItem moved =
        new MoveItemUseCase(items, clock).execute(SUB_OWNER, 1L, KanbanColumn.IN_PROGRESS, 1);

    // Insert-Position (clamped = 1) wird unverändert zurückgegeben — killt PrimitiveReturns->0
    // (#203).
    assertThat(moved.position()).isEqualTo(1);
    verify(items, never()).updatePosition(10L, 0); // x bleibt
    verify(items).updatePosition(11L, 2); // y: 1 -> 2
    verify(items).updatePosition(12L, 3); // z: 2 -> 3
  }

  @Test
  void moveCrossColumnIntoNonEmptyTargetShiftsTarget() {
    // BACKLOG: a(0)  →  IN_PROGRESS: x(0), y(1). Move a → IN_PROGRESS pos 0.
    // Erwartung: x und y rutschen um 1 hoch im IN_PROGRESS.
    final KanbanItem a = item(1, SUB_OWNER, KanbanColumn.BACKLOG, 0);
    final KanbanItem x = item(10, SUB_OWNER, KanbanColumn.IN_PROGRESS, 0);
    final KanbanItem y = item(11, SUB_OWNER, KanbanColumn.IN_PROGRESS, 1);
    given(items.findById(1L)).willReturn(Optional.of(a));
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.BACKLOG)).willReturn(List.of());
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.IN_PROGRESS)).willReturn(List.of(x, y));
    given(items.save(any())).willAnswer(inv -> inv.getArgument(0));

    new MoveItemUseCase(items, clock).execute(SUB_OWNER, 1L, KanbanColumn.IN_PROGRESS, 0);

    verify(items).updatePosition(10L, 1);
    verify(items).updatePosition(11L, 2);
  }

  @Test
  void moveNoopWhenSamePositionAndColumn() {
    final KanbanItem a = item(1, SUB_OWNER, KanbanColumn.BACKLOG, 0);
    given(items.findById(1L)).willReturn(Optional.of(a));

    final KanbanItem result =
        new MoveItemUseCase(items, clock).execute(SUB_OWNER, 1L, KanbanColumn.BACKLOG, 0);

    // Idempotenter No-op gibt das bestehende Item zurück (nicht null) (#203).
    assertThat(result).isSameAs(a);
    verify(items, never())
        .updatePosition(
            org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt());
    verify(items, never()).save(any());
  }

  @Test
  void moveThrowsForForeignItem() {
    given(items.findById(1L)).willReturn(Optional.of(item(1, SUB_OWNER, KanbanColumn.BACKLOG, 0)));
    assertThatThrownBy(
            () ->
                new MoveItemUseCase(items, clock)
                    .execute(SUB_OTHER, 1L, KanbanColumn.IN_PROGRESS, 0))
        .isInstanceOf(KanbanItemNotFoundException.class);
  }

  @Test
  void moveCrossColumnDoesNotReindexItemAtRemovedPosition() {
    // Quelle enthält ein Item GENAU auf der entfernten Position (1). Es darf nicht verschoben
    // werden — killt den Grenzwert-Mutanten `position > removedPosition` -> `>=` (#203).
    final KanbanItem b = item(2, SUB_OWNER, KanbanColumn.BACKLOG, 1);
    final KanbanItem atGap = item(5, SUB_OWNER, KanbanColumn.BACKLOG, 1);
    final KanbanItem below = item(6, SUB_OWNER, KanbanColumn.BACKLOG, 2);
    given(items.findById(2L)).willReturn(Optional.of(b));
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.BACKLOG))
        .willReturn(List.of(atGap, below));
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.IN_PROGRESS)).willReturn(List.of());
    given(items.save(any())).willAnswer(inv -> inv.getArgument(0));

    new MoveItemUseCase(items, clock).execute(SUB_OWNER, 2L, KanbanColumn.IN_PROGRESS, 0);

    verify(items).updatePosition(6L, 1); // below (2) rutscht hoch
    verify(items, never())
        .updatePosition(org.mockito.ArgumentMatchers.eq(5L), org.mockito.ArgumentMatchers.anyInt());
  }

  // ----- archive ------------------------------------------------------------

  @Test
  void archiveShouldSetArchivedFlag() {
    given(items.findById(2L)).willReturn(Optional.of(item(2, SUB_OWNER, KanbanColumn.BACKLOG, 1)));

    new ArchiveItemUseCase(items).execute(SUB_OWNER, 2L);

    verify(items).archiveById(2L);
    verify(items, never()).deleteById(org.mockito.ArgumentMatchers.anyLong());
  }

  @Test
  void archiveThrowsForForeignItem() {
    given(items.findById(1L)).willReturn(Optional.of(item(1, SUB_OWNER, KanbanColumn.BACKLOG, 0)));
    assertThatThrownBy(() -> new ArchiveItemUseCase(items).execute(SUB_OTHER, 1L))
        .isInstanceOf(KanbanItemNotFoundException.class);
    verify(items, never()).archiveById(org.mockito.ArgumentMatchers.anyLong());
  }

  @Test
  void archiveThrowsWhenItemMissing() {
    given(items.findById(99L)).willReturn(Optional.empty());
    assertThatThrownBy(() -> new ArchiveItemUseCase(items).execute(SUB_OWNER, 99L))
        .isInstanceOf(KanbanItemNotFoundException.class);
  }

  // ----- restore ------------------------------------------------------------

  @Test
  void restoreShouldClearArchivedFlag() {
    given(items.findById(2L))
        .willReturn(Optional.of(archivedItem(2, SUB_OWNER, KanbanColumn.BACKLOG, 1)));

    final KanbanItem result = new RestoreItemUseCase(items).execute(SUB_OWNER, 2L);

    verify(items).restoreById(2L);
    // Wiederhergestelltes Item wird zurückgegeben (nicht null) — killt NullReturnVals (#203).
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(2L);
  }

  @Test
  void restoreThrowsForForeignItem() {
    given(items.findById(1L))
        .willReturn(Optional.of(archivedItem(1, SUB_OWNER, KanbanColumn.BACKLOG, 0)));
    assertThatThrownBy(() -> new RestoreItemUseCase(items).execute(SUB_OTHER, 1L))
        .isInstanceOf(KanbanItemNotFoundException.class);
    verify(items, never()).restoreById(org.mockito.ArgumentMatchers.anyLong());
  }

  @Test
  void restoreThrowsWhenItemMissing() {
    given(items.findById(99L)).willReturn(Optional.empty());
    assertThatThrownBy(() -> new RestoreItemUseCase(items).execute(SUB_OWNER, 99L))
        .isInstanceOf(KanbanItemNotFoundException.class);
  }

  // ----- force delete -------------------------------------------------------

  @Test
  void forceDeleteShouldPhysicallyDeleteAndCloseGap() {
    final KanbanItem a = item(1, SUB_OWNER, KanbanColumn.BACKLOG, 0);
    final KanbanItem c = item(3, SUB_OWNER, KanbanColumn.BACKLOG, 2);
    given(items.findById(2L)).willReturn(Optional.of(item(2, SUB_OWNER, KanbanColumn.BACKLOG, 1)));
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.BACKLOG)).willReturn(List.of(a, c));

    new ForceDeleteItemUseCase(items).execute(SUB_OWNER, 2L);

    verify(items).deleteById(2L);
    verify(items).updatePosition(3L, 1);
  }

  @Test
  void forceDeleteDoesNotShiftItemAtGapPosition() {
    // Ein Item GENAU auf der Lücken-Position (gap = 1) darf nicht verschoben werden — killt den
    // Grenzwert-Mutanten `position > gap` -> `>=` (#203).
    final KanbanItem atGap = item(5, SUB_OWNER, KanbanColumn.BACKLOG, 1);
    final KanbanItem below = item(6, SUB_OWNER, KanbanColumn.BACKLOG, 2);
    given(items.findById(2L)).willReturn(Optional.of(item(2, SUB_OWNER, KanbanColumn.BACKLOG, 1)));
    given(items.findByUserAndColumn(SUB_OWNER, KanbanColumn.BACKLOG))
        .willReturn(List.of(atGap, below));

    new ForceDeleteItemUseCase(items).execute(SUB_OWNER, 2L);

    verify(items).updatePosition(6L, 1); // below (2) rutscht hoch
    verify(items, never())
        .updatePosition(org.mockito.ArgumentMatchers.eq(5L), org.mockito.ArgumentMatchers.anyInt());
  }

  @Test
  void forceDeleteThrowsForForeignItem() {
    given(items.findById(1L)).willReturn(Optional.of(item(1, SUB_OWNER, KanbanColumn.BACKLOG, 0)));
    assertThatThrownBy(() -> new ForceDeleteItemUseCase(items).execute(SUB_OTHER, 1L))
        .isInstanceOf(KanbanItemNotFoundException.class);
    verify(items, never()).deleteById(org.mockito.ArgumentMatchers.anyLong());
  }

  // ----- list archived ------------------------------------------------------

  @Test
  void listArchivedShouldReturnOnlyArchivedItems() {
    given(items.findAllByUserIncludingArchived(SUB_OWNER))
        .willReturn(
            List.of(
                item(1, SUB_OWNER, KanbanColumn.BACKLOG, 0),
                archivedItem(2, SUB_OWNER, KanbanColumn.IN_PROGRESS, 1),
                archivedItem(3, SUB_OWNER, KanbanColumn.DONE, 0)));

    final List<KanbanItem> result = new ListArchivedItemsUseCase(items).execute(SUB_OWNER);

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(KanbanItem::archived);
  }

  @Test
  void listArchivedShouldReturnEmptyWhenNoneArchived() {
    given(items.findAllByUserIncludingArchived(SUB_OWNER))
        .willReturn(List.of(item(1, SUB_OWNER, KanbanColumn.BACKLOG, 0)));

    final List<KanbanItem> result = new ListArchivedItemsUseCase(items).execute(SUB_OWNER);

    assertThat(result).isEmpty();
  }

  // ----- settings -----------------------------------------------------------

  @Test
  void getSettingsReturnsDefaultsWhenMissing() {
    given(settings.findByUser(SUB_OWNER)).willReturn(Optional.empty());
    final KanbanSettings result = new GetSettingsUseCase(settings).execute(SUB_OWNER);
    assertThat(result.doneRetentionDays()).isEqualTo(KanbanSettings.DEFAULT_RETENTION_DAYS);
  }

  @Test
  void getSettingsReturnsPersistedValueWhenPresent() {
    given(settings.findByUser(SUB_OWNER))
        .willReturn(Optional.of(new KanbanSettings(SUB_OWNER, 14)));
    assertThat(new GetSettingsUseCase(settings).execute(SUB_OWNER).doneRetentionDays())
        .isEqualTo(14);
  }

  @Test
  void updateSettingsSavesNewValue() {
    given(settings.save(any())).willAnswer(inv -> inv.getArgument(0));
    final KanbanSettings result = new UpdateSettingsUseCase(settings).execute(SUB_OWNER, 10);
    assertThat(result.doneRetentionDays()).isEqualTo(10);
  }

  // ----- cleanup ------------------------------------------------------------

  @Test
  void cleanupDeletesForEachUserUsingTheirRetention() {
    given(items.distinctUsersWithDoneItems()).willReturn(List.of(SUB_OWNER, SUB_OTHER));
    given(settings.findByUser(SUB_OWNER))
        .willReturn(Optional.of(new KanbanSettings(SUB_OWNER, 10)));
    given(settings.findByUser(SUB_OTHER)).willReturn(Optional.empty()); // Default 5
    given(items.deleteDoneOlderThan(org.mockito.ArgumentMatchers.eq(SUB_OWNER), any()))
        .willReturn(2);
    given(items.deleteDoneOlderThan(org.mockito.ArgumentMatchers.eq(SUB_OTHER), any()))
        .willReturn(3);

    final int deleted = new CleanupExpiredDoneItemsUseCase(items, settings, clock).execute();

    assertThat(deleted).isEqualTo(5);
    verify(items, times(1)).deleteDoneOlderThan(org.mockito.ArgumentMatchers.eq(SUB_OWNER), any());
    verify(items, times(1)).deleteDoneOlderThan(org.mockito.ArgumentMatchers.eq(SUB_OTHER), any());
  }
}
