package org.mwolff.api.image.application;

import org.mwolff.api.image.domain.ImageNotFoundException;
import org.mwolff.api.image.domain.ImageRepository;
import org.mwolff.api.image.domain.StoredImage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Lädt ein gespeichertes Bild oder wirft {@link ImageNotFoundException} (#182). */
@Component
public class GetImageUseCase {

  private final ImageRepository repository;

  public GetImageUseCase(final ImageRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public StoredImage execute(final String userSub, final long id) {
    return repository
        .findByIdAndUserSub(id, userSub)
        .orElseThrow(() -> new ImageNotFoundException(id));
  }
}
