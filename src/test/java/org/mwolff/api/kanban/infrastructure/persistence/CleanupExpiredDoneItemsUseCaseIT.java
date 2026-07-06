package org.mwolff.api.kanban.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.mwolff.api.AbstractIntegrationTest;
import org.mwolff.api.kanban.application.CleanupExpiredDoneItemsUseCase;
import org.mwolff.api.kanban.application.ExpiredDoneItemsPerUserCleanup;
import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reproduziert den Scheduler-Prod-Pfad von {@link CleanupExpiredDoneItemsUseCase#execute()}: Aufruf
 * <b>ohne</b> umgebende Transaktion (via {@code @Transactional(propagation = NOT_SUPPORTED)}). Vor
 * dem Fix umging der Selbstaufruf {@code this.archiveForUser(...)} den Spring-Proxy, sodass die
 * {@code @Modifying}-Query ohne Transaktion lief und {@code TransactionRequiredException} warf
 * (#305). Jetzt öffnet {@link ExpiredDoneItemsPerUserCleanup} pro User eine eigene Transaktion.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  JpaKanbanAdapter.class,
  ExpiredDoneItemsPerUserCleanup.class,
  CleanupExpiredDoneItemsUseCase.class,
  CleanupExpiredDoneItemsUseCaseIT.FixedClockConfig.class
})
class CleanupExpiredDoneItemsUseCaseIT extends AbstractIntegrationTest {

  private static final String USER = "cleanup-user";
  private static final Instant NOW = Instant.parse("2026-07-04T00:00:00Z");

  @Autowired private CleanupExpiredDoneItemsUseCase cleanup;
  @Autowired private JpaKanbanAdapter adapter;

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void executeArchivesExpiredDoneItemsWithoutSurroundingTransaction() {
    final long expiredId = persistDone(NOW.minus(java.time.Duration.ofDays(365)));
    final long freshId = persistDone(NOW);

    final int archived = cleanup.execute();

    assertThat(archived).isEqualTo(1);
    // #327: abgelaufene DONE-Items werden archiviert (Soft-Delete), nicht gelöscht.
    assertThat(adapter.findById(expiredId))
        .hasValueSatisfying(
            i -> {
              assertThat(i.archived()).isTrue();
              assertThat(i.column()).isEqualTo(KanbanColumn.DONE);
            });
    assertThat(adapter.findById(freshId))
        .hasValueSatisfying(i -> assertThat(i.archived()).isFalse());
  }

  /**
   * Legt ein DONE-Item mit explizitem {@code movedToDoneAt} an (analog {@code JpaKanbanAdapterIT}).
   */
  private long persistDone(Instant movedToDoneAt) {
    final int number = adapter.getMaxNumberForUser(USER).map(max -> max + 1).orElse(1);
    final KanbanItem created =
        adapter.save(
            KanbanItem.newInstance(USER, "T", "", KanbanColumn.DONE, number, Instant.now())
                .withNumber(number));
    final KanbanItem withDoneAt =
        new KanbanItem(
            created.id(),
            USER,
            "T",
            "",
            KanbanColumn.DONE,
            created.position(),
            created.createdAt(),
            created.updatedAt(),
            movedToDoneAt,
            false,
            created.number());
    return adapter.save(withDoneAt).id();
  }

  @TestConfiguration
  static class FixedClockConfig {
    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneId.of("UTC"));
    }
  }
}
