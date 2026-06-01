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

  @Mock private ImageRepository repository;

  @Test
  void returnsStoredImageWhenPresent() {
    final GetImageUseCase useCase = new GetImageUseCase(repository);
    final StoredImage image = StoredImage.of("image/png", new byte[] {1});
    when(repository.findById(5L)).thenReturn(Optional.of(image));

    assertThat(useCase.execute(5L)).isSameAs(image);
  }

  @Test
  void throwsWhenMissing() {
    final GetImageUseCase useCase = new GetImageUseCase(repository);
    when(repository.findById(9L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(9L))
        .isInstanceOf(ImageNotFoundException.class)
        .hasMessageContaining("9");
  }
}
