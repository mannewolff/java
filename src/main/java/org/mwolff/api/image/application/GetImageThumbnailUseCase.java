package org.mwolff.api.image.application;

import org.mwolff.api.image.domain.ImageNotFoundException;
import org.mwolff.api.image.domain.ImageRepository;
import org.mwolff.api.image.domain.ImageThumbnailer;
import org.mwolff.api.image.domain.StoredImage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liefert ein verkleinertes PNG-Thumbnail eines gespeicherten Bildes (#200). Die angeforderte
 * Kantenlänge wird auf den erlaubten Bereich geklemmt; das Original wird nie hochskaliert.
 */
@Component
public class GetImageThumbnailUseCase {

  private final ImageRepository repository;
  private final ImageThumbnailer thumbnailer;

  public GetImageThumbnailUseCase(
      final ImageRepository repository, final ImageThumbnailer thumbnailer) {
    this.repository = repository;
    this.thumbnailer = thumbnailer;
  }

  @Transactional(readOnly = true)
  public byte[] execute(final String userSub, final long id, final Integer size) {
    final StoredImage image =
        repository
            .findByIdAndUserSub(id, userSub)
            .orElseThrow(() -> new ImageNotFoundException(id));
    final int maxEdge = clampEdge(size);
    return thumbnailer.toThumbnailPng(image.data(), maxEdge);
  }

  private static int clampEdge(final Integer size) {
    if (size == null) {
      return ImageThumbnailer.DEFAULT_MAX_EDGE;
    }
    return Math.min(Math.max(size, ImageThumbnailer.MIN_MAX_EDGE), ImageThumbnailer.MAX_MAX_EDGE);
  }
}
