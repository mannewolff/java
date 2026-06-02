package org.mwolff.api.image.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mwolff.api.image.domain.ImageMetadata;
import org.mwolff.api.image.domain.ImagePage;
import org.mwolff.api.image.domain.ImageRepository;

class ListImagesUseCaseTest {

  private final ImageRepository repository = mock(ImageRepository.class);
  private final ListImagesUseCase useCase = new ListImagesUseCase(repository);

  private static ImageMetadata meta(final long id) {
    return new ImageMetadata(id, "image/png", 10, Instant.parse("2026-06-01T00:00:00Z"), null);
  }

  @Test
  void usesDefaultsWhenLimitAndOffsetAreNull() {
    when(repository.findMetadata(ListImagesUseCase.DEFAULT_LIMIT, 0)).thenReturn(List.of(meta(1)));
    when(repository.count()).thenReturn(1L);

    final ImagePage page = useCase.execute(null, null);

    assertThat(page.total()).isEqualTo(1L);
    assertThat(page.images()).hasSize(1);
    verify(repository).findMetadata(ListImagesUseCase.DEFAULT_LIMIT, 0);
  }

  @Test
  void clampsLimitToMax() {
    when(repository.findMetadata(ListImagesUseCase.MAX_LIMIT, 0)).thenReturn(List.of());
    when(repository.count()).thenReturn(0L);

    useCase.execute(ListImagesUseCase.MAX_LIMIT + 50, 0);

    verify(repository).findMetadata(ListImagesUseCase.MAX_LIMIT, 0);
  }

  @Test
  void clampsLimitToAtLeastOne() {
    when(repository.findMetadata(1, 0)).thenReturn(List.of());
    when(repository.count()).thenReturn(0L);

    useCase.execute(0, 0);

    verify(repository).findMetadata(1, 0);
  }

  @Test
  void clampsNegativeOffsetToZero() {
    when(repository.findMetadata(10, 0)).thenReturn(List.of());
    when(repository.count()).thenReturn(0L);

    useCase.execute(10, -5);

    verify(repository).findMetadata(10, 0);
  }

  @Test
  void passesValidLimitAndOffsetThrough() {
    when(repository.findMetadata(25, 50)).thenReturn(List.of(meta(3)));
    when(repository.count()).thenReturn(99L);

    final ImagePage page = useCase.execute(25, 50);

    assertThat(page.total()).isEqualTo(99L);
    verify(repository).findMetadata(25, 50);
  }
}
