package org.mwolff.api.kanban.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class KanbanSettingsTest {

  @Test
  void defaultForShouldUseDefaultRetention() {
    final KanbanSettings s = KanbanSettings.defaultFor("sub-1");
    assertThat(s.userSub()).isEqualTo("sub-1");
    assertThat(s.doneRetentionDays()).isEqualTo(KanbanSettings.DEFAULT_RETENTION_DAYS);
  }

  @Test
  void defaultForShouldEnableAllColumnsAndHideArchive() {
    final KanbanSettings s = KanbanSettings.defaultFor("sub-1");
    assertThat(s.activeFilters())
        .containsExactlyInAnyOrder("BACKLOG", "READY", "IN_PROGRESS", "IN_REVIEW", "DONE")
        .doesNotContain(KanbanSettings.ARCHIVED_FILTER);
  }

  @Test
  void convenienceConstructorUsesDefaultFilters() {
    assertThat(new KanbanSettings("u", 5).activeFilters())
        .isEqualTo(KanbanSettings.DEFAULT_FILTERS);
  }

  @Test
  void shouldRejectNullFilters() {
    assertThatThrownBy(() -> new KanbanSettings("u", 5, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void activeFiltersAreImmutable() {
    final Set<String> filters = new java.util.HashSet<>(Set.of("BACKLOG"));
    final KanbanSettings s = new KanbanSettings("u", 5, filters);
    filters.add("DONE"); // Mutation der Quelle darf nicht durchschlagen
    assertThat(s.activeFilters()).containsExactly("BACKLOG");
  }

  @Test
  void sanitizeDropsUnknownKeys() {
    assertThat(KanbanSettings.sanitizeFilters(List.of("BACKLOG", "bogus", "archived")))
        .containsExactlyInAnyOrder("BACKLOG", "archived");
  }

  @Test
  void sanitizeNullFallsBackToDefault() {
    assertThat(KanbanSettings.sanitizeFilters(null)).isEqualTo(KanbanSettings.DEFAULT_FILTERS);
  }

  @Test
  void sanitizeEmptyStaysEmpty() {
    assertThat(KanbanSettings.sanitizeFilters(List.of())).isEmpty();
  }

  @Test
  void serializeUsesCanonicalOrder() {
    assertThat(KanbanSettings.serializeFilters(Set.of("DONE", "archived", "BACKLOG")))
        .isEqualTo("BACKLOG,DONE,archived");
  }

  @Test
  void serializeEmptyGivesEmptyString() {
    assertThat(KanbanSettings.serializeFilters(Set.of())).isEmpty();
  }

  @Test
  void parseNullFallsBackToDefault() {
    assertThat(KanbanSettings.parseFilters(null)).isEqualTo(KanbanSettings.DEFAULT_FILTERS);
  }

  @Test
  void parseBlankGivesEmptySet() {
    assertThat(KanbanSettings.parseFilters("")).isEmpty();
  }

  @Test
  void parseRoundTripsSerialize() {
    final Set<String> filters = Set.of("READY", "archived");
    assertThat(KanbanSettings.parseFilters(KanbanSettings.serializeFilters(filters)))
        .isEqualTo(filters);
  }

  @Test
  void parseDropsUnknownKeys() {
    assertThat(KanbanSettings.parseFilters("BACKLOG,bogus")).containsExactly("BACKLOG");
  }

  @Test
  void parseOnlyUnknownKeysGivesEmptySet() {
    // Grenzt gegen parseNullFallsBackToDefault ab: null -> Default, ausschliesslich
    // unbekannte Keys -> bewusst leere Menge (kein Default-Fallback).
    assertThat(KanbanSettings.parseFilters("bogus,xxx")).isEmpty();
  }

  @Test
  void orderedFiltersFollowCanonicalOrder() {
    assertThat(KanbanSettings.orderedFilters(Set.of("archived", "BACKLOG", "IN_REVIEW")))
        .containsExactly("BACKLOG", "IN_REVIEW", "archived");
  }

  @Test
  void shouldRejectNullUserSub() {
    assertThatThrownBy(() -> new KanbanSettings(null, 5)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectBlankUserSub() {
    assertThatThrownBy(() -> new KanbanSettings("  ", 5))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectRetentionBelowMin() {
    assertThatThrownBy(() -> new KanbanSettings("u", 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectRetentionAboveMax() {
    assertThatThrownBy(() -> new KanbanSettings("u", 31))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldAcceptMinBoundary() {
    assertThat(new KanbanSettings("u", 1).doneRetentionDays()).isEqualTo(1);
  }

  @Test
  void shouldAcceptMaxBoundary() {
    assertThat(new KanbanSettings("u", 30).doneRetentionDays()).isEqualTo(30);
  }
}
