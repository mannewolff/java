package org.mwolff.api.kanban.infrastructure.scheduler;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mwolff.api.kanban.application.CleanupExpiredDoneItemsUseCase;

class DoneItemCleanupJobTest {

  @Test
  void runShouldDelegateToUseCase() {
    final CleanupExpiredDoneItemsUseCase useCase = mock(CleanupExpiredDoneItemsUseCase.class);
    given(useCase.execute()).willReturn(3);

    new DoneItemCleanupJob(useCase).run();

    verify(useCase).execute();
  }

  @Test
  void runShouldSwallowZeroDeletionsWithoutLogging() {
    final CleanupExpiredDoneItemsUseCase useCase = mock(CleanupExpiredDoneItemsUseCase.class);
    given(useCase.execute()).willReturn(0);

    // Kein Throw — Job soll auch bei 0 Deletes leise zurückkehren.
    new DoneItemCleanupJob(useCase).run();

    verify(useCase).execute();
  }
}
