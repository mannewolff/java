package org.mwolff.api.image.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mwolff.api.image.domain.ImageRepository;

class CheckImageHashUseCaseTest {

  private final ImageRepository repository = mock(ImageRepository.class);
  private final CheckImageHashUseCase useCase = new CheckImageHashUseCase(repository);

  @Test
  void returnsIdWhenHashExists() {
    when(repository.findIdByHash("abc")).thenReturn(Optional.of(7L));

    assertThat(useCase.execute("abc")).contains(7L);
  }

  @Test
  void returnsEmptyWhenHashUnknown() {
    when(repository.findIdByHash("abc")).thenReturn(Optional.empty());

    assertThat(useCase.execute("abc")).isEmpty();
  }

  @Test
  void returnsEmptyForNullHashWithoutHittingRepository() {
    assertThat(useCase.execute(null)).isEmpty();
    verifyNoInteractions(repository);
  }

  @Test
  void returnsEmptyForBlankHashWithoutHittingRepository() {
    assertThat(useCase.execute("   ")).isEmpty();
    verifyNoInteractions(repository);
  }
}
