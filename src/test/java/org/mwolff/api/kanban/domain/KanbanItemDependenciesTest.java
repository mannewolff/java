package org.mwolff.api.kanban.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class KanbanItemDependenciesTest {

  private static KanbanItem item() {
    return KanbanItem.newInstance("sub", "T", "", KanbanColumn.BACKLOG, 0, Instant.EPOCH);
  }

  @Test
  void defaultsToEmptyList() {
    assertThat(item().dependencies()).isEmpty();
  }

  @Test
  void normalizesDeduplicatesSortsAndDropsNonPositive() {
    // Enthält Duplikat, unsortiert, 0, negativ und null → nur positive, dedupliziert, sortiert.
    final KanbanItem i = item().withDependencies(Arrays.asList(3, 1, 3, 0, -2, null, 2));
    assertThat(i.dependencies()).containsExactly(1, 2, 3);
  }

  @Test
  void nullDependenciesBecomeEmpty() {
    assertThat(item().withDependencies(null).dependencies()).isEmpty();
  }

  @Test
  void toCsvJoinsWithComma() {
    assertThat(KanbanItem.dependenciesToCsv(List.of(12, 34))).isEqualTo("12,34");
  }

  @Test
  void toCsvEmptyGivesEmptyString() {
    assertThat(KanbanItem.dependenciesToCsv(List.of())).isEmpty();
  }

  @Test
  void fromCsvNullOrBlankGivesEmpty() {
    assertThat(KanbanItem.dependenciesFromCsv(null)).isEmpty();
    assertThat(KanbanItem.dependenciesFromCsv("")).isEmpty();
    assertThat(KanbanItem.dependenciesFromCsv("  ")).isEmpty();
  }

  @Test
  void fromCsvParsesNormalizesAndSorts() {
    assertThat(KanbanItem.dependenciesFromCsv("34, 12, 12")).containsExactly(12, 34);
  }

  @Test
  void fromCsvDropsNonPositiveAndEmptyTokens() {
    assertThat(KanbanItem.dependenciesFromCsv("5,,-1,0,3")).containsExactly(3, 5);
  }

  @Test
  void csvRoundTrips() {
    final List<Integer> deps = List.of(2, 5, 9);
    assertThat(KanbanItem.dependenciesFromCsv(KanbanItem.dependenciesToCsv(deps))).isEqualTo(deps);
  }

  @Test
  void withContentPreservesDependencies() {
    final KanbanItem i = item().withDependencies(List.of(7)).withContent("Neu", "Body");
    assertThat(i.dependencies()).containsExactly(7);
    assertThat(i.title()).isEqualTo("Neu");
  }

  @Test
  void withNumberPreservesDependencies() {
    final KanbanItem i = item().withDependencies(List.of(4)).withNumber(9);
    assertThat(i.dependencies()).containsExactly(4);
    assertThat(i.number()).isEqualTo(9);
  }
}
