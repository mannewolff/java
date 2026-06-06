package org.mwolff.api.image.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mwolff.api.image.domain.ImageNotFoundException;
import org.mwolff.api.image.domain.ImageRepository;
import org.mwolff.api.image.domain.ImageThumbnailer;
import org.mwolff.api.image.domain.StoredImage;

class GetImageThumbnailUseCaseTest {

  private static final String SUB = "user-1";

  private final ImageRepository repository = mock(ImageRepository.class);
  private final ImageThumbnailer thumbnailer = mock(ImageThumbnailer.class);
  private final GetImageThumbnailUseCase useCase =
      new GetImageThumbnailUseCase(repository, thumbnailer);

  private static StoredImage stored() {
    return new StoredImage(1L, SUB, "image/png", 3, new byte[] {1, 2, 3}, null, null);
  }

  @Test
  void usesDefaultEdgeWhenSizeNull() {
    when(repository.findByIdAndUserSub(1L, SUB)).thenReturn(Optional.of(stored()));
    when(thumbnailer.toThumbnailPng(
            eq(new byte[] {1, 2, 3}), eq(ImageThumbnailer.DEFAULT_MAX_EDGE)))
        .thenReturn(new byte[] {9});

    assertThat(useCase.execute(SUB, 1L, null)).containsExactly(9);
    verify(thumbnailer).toThumbnailPng(new byte[] {1, 2, 3}, ImageThumbnailer.DEFAULT_MAX_EDGE);
  }

  @Test
  void clampsSizeToMax() {
    when(repository.findByIdAndUserSub(1L, SUB)).thenReturn(Optional.of(stored()));
    when(thumbnailer.toThumbnailPng(eq(new byte[] {1, 2, 3}), eq(ImageThumbnailer.MAX_MAX_EDGE)))
        .thenReturn(new byte[] {1});

    useCase.execute(SUB, 1L, ImageThumbnailer.MAX_MAX_EDGE + 1000);

    verify(thumbnailer).toThumbnailPng(new byte[] {1, 2, 3}, ImageThumbnailer.MAX_MAX_EDGE);
  }

  @Test
  void clampsSizeToMin() {
    when(repository.findByIdAndUserSub(1L, SUB)).thenReturn(Optional.of(stored()));
    when(thumbnailer.toThumbnailPng(eq(new byte[] {1, 2, 3}), eq(ImageThumbnailer.MIN_MAX_EDGE)))
        .thenReturn(new byte[] {1});

    useCase.execute(SUB, 1L, 1);

    verify(thumbnailer).toThumbnailPng(new byte[] {1, 2, 3}, ImageThumbnailer.MIN_MAX_EDGE);
  }

  @Test
  void passesValidSizeThrough() {
    when(repository.findByIdAndUserSub(1L, SUB)).thenReturn(Optional.of(stored()));
    when(thumbnailer.toThumbnailPng(eq(new byte[] {1, 2, 3}), eq(120))).thenReturn(new byte[] {1});

    useCase.execute(SUB, 1L, 120);

    verify(thumbnailer).toThumbnailPng(new byte[] {1, 2, 3}, 120);
  }

  @Test
  void throwsWhenImageMissingOrForeign() {
    when(repository.findByIdAndUserSub(9L, SUB)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(SUB, 9L, 100))
        .isInstanceOf(ImageNotFoundException.class);
  }
}
