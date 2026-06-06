package org.mwolff.api.image.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mwolff.api.image.domain.ImageRepository;

class CheckImageHashUseCaseTest {

  private static final String SUB = "user-1";

  private final ImageRepository repository = mock(ImageRepository.class);
  private final CheckImageHashUseCase useCase = new CheckImageHashUseCase(repository);

  @Test
  void returnsIdWhenHashExists() {
    when(repository.findIdByHashAndUserSub("abc", SUB)).thenReturn(Optional.of(7L));

    assertThat(useCase.execute(SUB, "abc")).contains(7L);
  }

  @Test
  void returnsEmptyWhenHashUnknown() {
    when(repository.findIdByHashAndUserSub("abc", SUB)).thenReturn(Optional.empty());

    assertThat(useCase.execute(SUB, "abc")).isEmpty();
  }

  @Test
  void returnsEmptyForNullHashWithoutHittingRepository() {
    assertThat(useCase.execute(SUB, null)).isEmpty();
    verifyNoInteractions(repository);
  }

  @Test
  void returnsEmptyForBlankHashWithoutHittingRepository() {
    assertThat(useCase.execute(SUB, "   ")).isEmpty();
    verifyNoInteractions(repository);
  }
}
