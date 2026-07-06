package org.mwolff.api.kanban.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.mwolff.api.AbstractIntegrationTest;
import org.mwolff.api.kanban.application.ArchiveItemUseCase;
import org.mwolff.api.kanban.application.CreateItemUseCase;
import org.mwolff.api.kanban.application.MoveItemUseCase;
import org.mwolff.api.kanban.application.RestoreItemUseCase;
import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Verifiziert die Positions-Invariante der Kanban-Spalten (#309) gegen echtes MariaDB inkl.
 * Unique-Constraint {@code uk_kanban_active_position}: Nach jeder Folge von Create/Archive/Restore/
 * Move sind die aktiven Positionen jeder Spalte lückenlos (0..n-1) und eindeutig. Zusätzlich greift
 * der Constraint als Sicherheitsnetz gegen doppelte aktive Positionen.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  JpaKanbanAdapter.class,
  CreateItemUseCase.class,
  ArchiveItemUseCase.class,
  RestoreItemUseCase.class,
  MoveItemUseCase.class,
  KanbanPositionIntegrityIT.FixedClockConfig.class
})
class KanbanPositionIntegrityIT extends AbstractIntegrationTest {

  private static final String USER = "pos-user";

  @Autowired private CreateItemUseCase create;
  @Autowired private ArchiveItemUseCase archive;
  @Autowired private RestoreItemUseCase restore;
  @Autowired private MoveItemUseCase move;
  @Autowired private JpaKanbanAdapter adapter;
  @Autowired private KanbanItemJpaRepository repo;

  /** Aktive Positionen der Spalte müssen exakt 0..n-1 in Reihenfolge sein. */
  private void assertCompact(KanbanColumn column) {
    final List<Integer> positions =
        adapter.findByUserAndColumn(USER, column).stream().map(KanbanItem::position).toList();
    assertThat(positions).isEqualTo(IntStream.range(0, positions.size()).boxed().toList());
  }

  private long createIn(KanbanColumn column, String title) {
    return create.execute(USER, title, "", column).id();
  }

  @Test
  void archiveThenCreateKeepsColumnCompact() {
    final long a = createIn(KanbanColumn.BACKLOG, "a"); // 0
    final long b = createIn(KanbanColumn.BACKLOG, "b"); // 1
    createIn(KanbanColumn.BACKLOG, "c"); // 2

    archive.execute(USER, b); // Lücke bei 1 schließt sich: a@0, c@1

    assertCompact(KanbanColumn.BACKLOG);
    // Neu anlegen vergibt die nächste freie Position ohne Kollision mit dem alten c(2).
    createIn(KanbanColumn.BACKLOG, "d"); // 2
    assertCompact(KanbanColumn.BACKLOG);
    assertThat(adapter.findById(a)).hasValueSatisfying(i -> assertThat(i.position()).isEqualTo(0));
  }

  @Test
  void restoreAppendsToEndOfActiveColumn() {
    final long a = createIn(KanbanColumn.BACKLOG, "a"); // 0
    createIn(KanbanColumn.BACKLOG, "b"); // 1
    createIn(KanbanColumn.BACKLOG, "c"); // 2

    archive.execute(USER, a); // b@0, c@1
    createIn(KanbanColumn.BACKLOG, "d"); // 2

    restore.execute(USER, a); // a ans Ende → 3

    assertCompact(KanbanColumn.BACKLOG);
    assertThat(adapter.findById(a)).hasValueSatisfying(i -> assertThat(i.position()).isEqualTo(3));
  }

  @Test
  void moveWithinColumnKeepsCompactAndPlacesItem() {
    final long a = createIn(KanbanColumn.BACKLOG, "a"); // 0
    createIn(KanbanColumn.BACKLOG, "b"); // 1
    createIn(KanbanColumn.BACKLOG, "c"); // 2
    createIn(KanbanColumn.BACKLOG, "d"); // 3

    move.execute(USER, a, KanbanColumn.BACKLOG, 2);

    assertCompact(KanbanColumn.BACKLOG);
    assertThat(adapter.findById(a)).hasValueSatisfying(i -> assertThat(i.position()).isEqualTo(2));
  }

  @Test
  void moveAcrossColumnsKeepsBothColumnsCompact() {
    createIn(KanbanColumn.BACKLOG, "a"); // 0
    final long b = createIn(KanbanColumn.BACKLOG, "b"); // 1
    createIn(KanbanColumn.BACKLOG, "c"); // 2
    createIn(KanbanColumn.IN_PROGRESS, "x"); // 0
    createIn(KanbanColumn.IN_PROGRESS, "y"); // 1

    move.execute(USER, b, KanbanColumn.IN_PROGRESS, 0);

    assertCompact(KanbanColumn.BACKLOG);
    assertCompact(KanbanColumn.IN_PROGRESS);
    assertThat(adapter.findById(b))
        .hasValueSatisfying(
            i -> {
              assertThat(i.column()).isEqualTo(KanbanColumn.IN_PROGRESS);
              assertThat(i.position()).isEqualTo(0);
            });
  }

  @Test
  void uniqueConstraintRejectsDuplicateActivePosition() {
    createIn(KanbanColumn.BACKLOG, "a"); // aktive Position 0

    final KanbanItemEntity duplicate =
        new KanbanItemEntity(
            USER, "dup", "", KanbanItemType.ITEM, null, null, KanbanColumn.BACKLOG, 0, null);
    duplicate.setNumber(999);

    assertThatThrownBy(() -> repo.saveAndFlush(duplicate))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @TestConfiguration
  static class FixedClockConfig {
    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-07-04T00:00:00Z"), ZoneId.of("UTC"));
    }
  }
}
