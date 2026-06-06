package org.mwolff.api.image.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mwolff.api.image.application.DeleteImagesUseCase.DeleteResult;
import org.mwolff.api.image.domain.ImageInUseException;
import org.mwolff.api.image.domain.ImageNotFoundException;
import org.mwolff.api.image.domain.ImageRepository;
import org.mwolff.api.image.domain.ImageUsagePort;
import org.mwolff.api.image.domain.StoredImage;

class DeleteImagesUseCaseTest {

  private static final String SUB = "user-1";

  private final ImageRepository repository = mock(ImageRepository.class);
  private final ImageUsagePort usagePort = mock(ImageUsagePort.class);
  private final DeleteImagesUseCase useCase = new DeleteImagesUseCase(repository, usagePort);

  private static StoredImage stored(final long id) {
    return new StoredImage(id, SUB, "image/png", 3, new byte[] {1, 2, 3}, null, null);
  }

  // ----- deleteOne -----

  @Test
  void deleteOneRemovesUnusedImage() {
    when(repository.findByIdAndUserSub(5L, SUB)).thenReturn(Optional.of(stored(5L)));
    when(usagePort.countUsages(5L)).thenReturn(0L);

    useCase.deleteOne(SUB, 5L);

    verify(repository).delete(5L);
  }

  @Test
  void deleteOneThrowsWhenMissingOrForeign() {
    when(repository.findByIdAndUserSub(9L, SUB)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.deleteOne(SUB, 9L)).isInstanceOf(ImageNotFoundException.class);
    verify(repository, never()).delete(9L);
  }

  @Test
  void deleteOneThrowsWhenInUse() {
    when(repository.findByIdAndUserSub(5L, SUB)).thenReturn(Optional.of(stored(5L)));
    when(usagePort.countUsages(5L)).thenReturn(2L);

    assertThatThrownBy(() -> useCase.deleteOne(SUB, 5L)).isInstanceOf(ImageInUseException.class);
    verify(repository, never()).delete(5L);
  }

  // ----- deleteBatch -----

  @Test
  void deleteBatchSplitsDeletedAndFailed() {
    // 1 = löschbar, 2 = benutzt, 3 = fehlt/fremd
    when(repository.findByIdAndUserSub(1L, SUB)).thenReturn(Optional.of(stored(1L)));
    when(usagePort.countUsages(1L)).thenReturn(0L);
    when(repository.findByIdAndUserSub(2L, SUB)).thenReturn(Optional.of(stored(2L)));
    when(usagePort.countUsages(2L)).thenReturn(1L);
    when(repository.findByIdAndUserSub(3L, SUB)).thenReturn(Optional.empty());

    final DeleteResult result = useCase.deleteBatch(SUB, List.of(1L, 2L, 3L));

    assertThat(result.deleted()).containsExactly(1L);
    assertThat(result.failed())
        .extracting(DeleteImagesUseCase.Failure::id, DeleteImagesUseCase.Failure::reason)
        .containsExactlyInAnyOrder(
            org.assertj.core.groups.Tuple.tuple(2L, DeleteImagesUseCase.REASON_IN_USE),
            org.assertj.core.groups.Tuple.tuple(3L, DeleteImagesUseCase.REASON_NOT_FOUND));
    verify(repository).delete(1L);
    verify(repository, never()).delete(2L);
    verify(repository, never()).delete(3L);
  }
}
