package org.mwolff.api.kanban.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mwolff.api.kanban.application.CleanupExpiredDoneItemsUseCase;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class DoneItemCleanupJobTest {

  // Log-Capture: das beobachtbare Verhalten des Jobs ist (neben dem Use-Case-Aufruf) genau
  // die Logzeile — nur darüber sind die Mutanten auf `deleted > 0` killbar (#207).
  private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
  private final Logger jobLogger = (Logger) LoggerFactory.getLogger(DoneItemCleanupJob.class);

  @BeforeEach
  void attachLogAppender() {
    logAppender.start();
    jobLogger.addAppender(logAppender);
  }

  @AfterEach
  void detachLogAppender() {
    jobLogger.detachAppender(logAppender);
  }

  @Test
  void runShouldDelegateToUseCase() {
    final CleanupExpiredDoneItemsUseCase useCase = mock(CleanupExpiredDoneItemsUseCase.class);
    given(useCase.execute()).willReturn(3);

    new DoneItemCleanupJob(useCase).run();

    verify(useCase).execute();
  }

  @Test
  void runShouldLogDeletionCountWhenItemsWereDeleted() {
    final CleanupExpiredDoneItemsUseCase useCase = mock(CleanupExpiredDoneItemsUseCase.class);
    given(useCase.execute()).willReturn(3);

    new DoneItemCleanupJob(useCase).run();

    assertThat(logAppender.list).hasSize(1);
    assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.INFO);
    assertThat(logAppender.list.get(0).getFormattedMessage()).contains("3");
  }

  @Test
  void runShouldSwallowZeroDeletionsWithoutLogging() {
    final CleanupExpiredDoneItemsUseCase useCase = mock(CleanupExpiredDoneItemsUseCase.class);
    given(useCase.execute()).willReturn(0);

    // Kein Throw — Job soll auch bei 0 Deletes leise zurückkehren, ohne Log-Rauschen.
    new DoneItemCleanupJob(useCase).run();

    verify(useCase).execute();
    assertThat(logAppender.list).isEmpty();
  }
}
