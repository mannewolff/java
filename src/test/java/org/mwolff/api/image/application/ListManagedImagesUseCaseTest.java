package org.mwolff.api.image.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mwolff.api.image.domain.ImageMetadata;
import org.mwolff.api.image.domain.ImageRepository;
import org.mwolff.api.image.domain.ImageUsagePort;
import org.mwolff.api.image.domain.ManagedImagePage;

class ListManagedImagesUseCaseTest {

  private final ImageRepository repository = mock(ImageRepository.class);
  private final ImageUsagePort usagePort = mock(ImageUsagePort.class);
  private final ListManagedImagesUseCase useCase =
      new ListManagedImagesUseCase(repository, usagePort);

  private static ImageMetadata meta(final long id) {
    return new ImageMetadata(id, "image/png", 10, Instant.parse("2026-06-01T00:00:00Z"), null);
  }

  @Test
  void enrichesMetadataWithUsageCountsAndDefaultsToZero() {
    when(repository.findMetadata(ListImagesUseCase.DEFAULT_LIMIT, 0))
        .thenReturn(List.of(meta(1), meta(2)));
    when(repository.count()).thenReturn(2L);
    when(usagePort.usageCounts()).thenReturn(Map.of(1L, 3L)); // 2 ist ungenutzt

    final ManagedImagePage page = useCase.execute(null, null);

    assertThat(page.total()).isEqualTo(2L);
    assertThat(page.images()).hasSize(2);
    assertThat(page.images().get(0).image().id()).isEqualTo(1L);
    assertThat(page.images().get(0).usageCount()).isEqualTo(3L);
    assertThat(page.images().get(1).usageCount()).isZero();
  }

  @Test
  void clampsLimitAndOffset() {
    when(repository.findMetadata(ListImagesUseCase.MAX_LIMIT, 0)).thenReturn(List.of());
    when(repository.count()).thenReturn(0L);
    when(usagePort.usageCounts()).thenReturn(Map.of());

    useCase.execute(ListImagesUseCase.MAX_LIMIT + 99, -3);

    verify(repository).findMetadata(ListImagesUseCase.MAX_LIMIT, 0);
  }

  @Test
  void clampsLimitToAtLeastOne() {
    when(repository.findMetadata(1, 5)).thenReturn(List.of());
    when(repository.count()).thenReturn(0L);
    when(usagePort.usageCounts()).thenReturn(Map.of());

    useCase.execute(0, 5);

    verify(repository).findMetadata(1, 5);
  }
}
