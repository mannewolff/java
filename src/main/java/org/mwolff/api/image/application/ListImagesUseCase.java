package org.mwolff.api.image.application;

import org.mwolff.api.image.domain.ImagePage;
import org.mwolff.api.image.domain.ImageRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Listet Bild-Metadaten (ohne Binärdaten) paginiert für die Galerie-Ansichten (#198). */
@Component
public class ListImagesUseCase {

  /** Standard-Seitengröße, wenn der Aufrufer keine angibt. */
  public static final int DEFAULT_LIMIT = 100;

  /** Hartes Maximum pro Seite — schützt vor zu großen Antworten. */
  public static final int MAX_LIMIT = 500;

  private final ImageRepository repository;

  public ListImagesUseCase(final ImageRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public ImagePage execute(final String userSub, final Integer limit, final Integer offset) {
    final int effectiveLimit =
        limit == null ? DEFAULT_LIMIT : Math.min(Math.max(1, limit), MAX_LIMIT);
    final int effectiveOffset = offset == null ? 0 : Math.max(0, offset);
    return new ImagePage(
        repository.findMetadataByUserSub(userSub, effectiveLimit, effectiveOffset),
        repository.countByUserSub(userSub));
  }
}
