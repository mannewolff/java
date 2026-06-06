package org.mwolff.api.image.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mwolff.api.image.domain.ImageNotFoundException;
import org.mwolff.api.image.domain.ImageRepository;
import org.mwolff.api.image.domain.StoredImage;

@ExtendWith(MockitoExtension.class)
class GetImageUseCaseTest {

  private static final String SUB = "user-1";

  @Mock private ImageRepository repository;

  @Test
  void returnsStoredImageWhenPresent() {
    final GetImageUseCase useCase = new GetImageUseCase(repository);
    final StoredImage image = StoredImage.of(SUB, "image/png", new byte[] {1});
    when(repository.findByIdAndUserSub(5L, SUB)).thenReturn(Optional.of(image));

    assertThat(useCase.execute(SUB, 5L)).isSameAs(image);
  }

  @Test
  void throwsWhenMissingOrForeign() {
    final GetImageUseCase useCase = new GetImageUseCase(repository);
    when(repository.findByIdAndUserSub(9L, SUB)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(SUB, 9L))
        .isInstanceOf(ImageNotFoundException.class)
        .hasMessageContaining("9");
  }
}
