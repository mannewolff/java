package org.mwolff.api.timeseries.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mwolff.api.AbstractIntegrationTest;
import org.mwolff.api.timeseries.domain.TimeSeries;
import org.mwolff.api.timeseries.domain.TimeSeriesDataType;
import org.mwolff.api.timeseries.domain.TimeSeriesEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Integrationstest des JPA-Adapters gegen MariaDB via Testcontainers. Prueft Persistierung,
 * Owner-Filterung, Cascade-Delete auf Entries und Time-Range-Queries.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaTimeSeriesAdapter.class)
class JpaTimeSeriesAdapterIT extends AbstractIntegrationTest {

  @Autowired private JpaTimeSeriesAdapter adapter;
  @Autowired private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager em;

  private static final String USER_A = "user-a";
  private static final String USER_B = "user-b";

  @Test
  void shouldPersistAndReadTimeSeries() {
    final TimeSeries saved =
        adapter.save(
            TimeSeries.newInstance(USER_A, "Weight", "Body", "kg", TimeSeriesDataType.DECIMAL));

    assertThat(saved.id()).isNotNull();
    assertThat(saved.createdAt()).isNotNull();
    assertThat(saved.updatedAt()).isNotNull();

    final List<TimeSeries> all = adapter.findAllByUser(USER_A);
    assertThat(all).hasSize(1);
    assertThat(all.get(0).name()).isEqualTo("Weight");
  }

  @Test
  void shouldFilterByUser() {
    adapter.save(TimeSeries.newInstance(USER_A, "A", null, "kg", TimeSeriesDataType.DECIMAL));
    adapter.save(TimeSeries.newInstance(USER_B, "B", null, "kg", TimeSeriesDataType.DECIMAL));

    assertThat(adapter.findAllByUser(USER_A)).extracting(TimeSeries::name).containsExactly("A");
    assertThat(adapter.findAllByUser(USER_B)).extracting(TimeSeries::name).containsExactly("B");
  }

  @Test
  void shouldFindByIdRegardlessOfOwner() {
    final TimeSeries a =
        adapter.save(
            TimeSeries.newInstance(USER_A, "owned", null, "kg", TimeSeriesDataType.DECIMAL));

    assertThat(adapter.findById(a.id())).isPresent();
  }

  @Test
  void shouldDeleteByIdAndCascadeEntries() {
    final TimeSeries ts =
        adapter.save(
            TimeSeries.newInstance(USER_A, "to delete", null, "kg", TimeSeriesDataType.DECIMAL));
    adapter.save(
        TimeSeriesEntry.newInstance(
            ts.id(), Instant.parse("2026-05-27T10:00:00Z"), new BigDecimal("1.0")));
    adapter.save(
        TimeSeriesEntry.newInstance(
            ts.id(), Instant.parse("2026-05-27T10:00:00Z").plusSeconds(60), new BigDecimal("2.0")));

    adapter.deleteById(ts.id());
    // Delete flushen + Context leeren, damit der DB-seitige ON DELETE CASCADE auf die Entries
    // sichtbar wird (sonst zählt die Query den noch nicht geflushten/gecachten Stand).
    em.flush();
    em.clear();

    assertThat(adapter.findById(ts.id())).isEmpty();
    assertThat(adapter.countEntries(ts.id())).isZero();
  }

  @Test
  void shouldUpdateMetadataOnExistingSeries() {
    final TimeSeries original =
        adapter.save(
            TimeSeries.newInstance(USER_A, "Old", "old", "kg", TimeSeriesDataType.DECIMAL));

    final TimeSeries updated =
        adapter.save(original.withMetadata("New", "new", "g", TimeSeriesDataType.INTEGER));

    assertThat(updated.id()).isEqualTo(original.id());
    assertThat(adapter.findById(original.id()))
        .hasValueSatisfying(
            ts -> {
              assertThat(ts.name()).isEqualTo("New");
              assertThat(ts.description()).isEqualTo("new");
              assertThat(ts.unit()).isEqualTo("g");
              assertThat(ts.dataType()).isEqualTo(TimeSeriesDataType.INTEGER);
            });
  }

  @Test
  void shouldCountEntries() {
    final TimeSeries ts =
        adapter.save(
            TimeSeries.newInstance(USER_A, "counted", null, "kg", TimeSeriesDataType.DECIMAL));
    adapter.save(
        TimeSeriesEntry.newInstance(
            ts.id(), Instant.parse("2026-05-27T10:00:00Z"), BigDecimal.ONE));
    adapter.save(
        TimeSeriesEntry.newInstance(
            ts.id(), Instant.parse("2026-05-27T10:00:00Z").plusSeconds(60), BigDecimal.TEN));

    assertThat(adapter.countEntries(ts.id())).isEqualTo(2L);
  }

  @Test
  void shouldFindEntriesDescendingWithLimit() {
    final TimeSeries ts =
        adapter.save(
            TimeSeries.newInstance(USER_A, "history", null, "kg", TimeSeriesDataType.DECIMAL));
    final Instant base = Instant.parse("2026-05-27T10:00:00Z");
    adapter.save(TimeSeriesEntry.newInstance(ts.id(), base, new BigDecimal("1")));
    adapter.save(TimeSeriesEntry.newInstance(ts.id(), base.plusSeconds(60), new BigDecimal("2")));
    adapter.save(TimeSeriesEntry.newInstance(ts.id(), base.plusSeconds(120), new BigDecimal("3")));

    final List<TimeSeriesEntry> result =
        adapter.findByTimeSeries(ts.id(), Optional.empty(), Optional.empty(), 2);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).value()).isEqualByComparingTo("3");
    assertThat(result.get(1).value()).isEqualByComparingTo("2");
  }

  @Test
  void shouldFilterEntriesByFromAndTo() {
    final TimeSeries ts =
        adapter.save(
            TimeSeries.newInstance(USER_A, "filtered", null, "kg", TimeSeriesDataType.DECIMAL));
    final Instant base = Instant.parse("2026-05-27T10:00:00Z");
    adapter.save(TimeSeriesEntry.newInstance(ts.id(), base, new BigDecimal("1")));
    adapter.save(TimeSeriesEntry.newInstance(ts.id(), base.plusSeconds(60), new BigDecimal("2")));
    adapter.save(TimeSeriesEntry.newInstance(ts.id(), base.plusSeconds(120), new BigDecimal("3")));

    final List<TimeSeriesEntry> result =
        adapter.findByTimeSeries(
            ts.id(), Optional.of(base.plusSeconds(30)), Optional.of(base.plusSeconds(90)), 100);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).value()).isEqualByComparingTo("2");
  }

  @Test
  void hasFractionalValuesDetectsDecimalEntries() {
    final TimeSeries ts =
        adapter.save(
            TimeSeries.newInstance(USER_A, "mixed", null, "kg", TimeSeriesDataType.DECIMAL));
    final Instant base = Instant.parse("2026-05-27T10:00:00Z");
    adapter.save(TimeSeriesEntry.newInstance(ts.id(), base, new BigDecimal("2.000000")));

    assertThat(adapter.hasFractionalValues(ts.id())).isFalse();

    adapter.save(TimeSeriesEntry.newInstance(ts.id(), base.plusSeconds(60), new BigDecimal("2.5")));

    assertThat(adapter.hasFractionalValues(ts.id())).isTrue();
  }

  @Test
  void hasFractionalValuesHandlesNegativeIntegersAsWhole() {
    final TimeSeries ts =
        adapter.save(TimeSeries.newInstance(USER_A, "neg", null, "kg", TimeSeriesDataType.DECIMAL));
    final Instant base = Instant.parse("2026-05-27T10:00:00Z");
    adapter.save(TimeSeriesEntry.newInstance(ts.id(), base, new BigDecimal("-3.000000")));

    // -3 hat keinen Nachkommaanteil, obwohl FLOOR(-3) = -3 (Vorzeichen-Robustheit der Query).
    assertThat(adapter.hasFractionalValues(ts.id())).isFalse();
  }
}
