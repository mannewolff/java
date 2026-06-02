package org.mwolff.api.image.application;

import java.util.Optional;

import org.mwolff.api.image.domain.ImageRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Prüft, ob bereits ein Bild mit gegebenem SHA-256-Hash existiert (#199, Duplikat-Erkennung). */
@Component
public class CheckImageHashUseCase {

  private final ImageRepository repository;

  public CheckImageHashUseCase(final ImageRepository repository) {
    this.repository = repository;
  }

  /** Liefert die id eines existierenden Bildes mit diesem Hash, falls vorhanden. */
  @Transactional(readOnly = true)
  public Optional<Long> execute(final String hash) {
    if (hash == null || hash.isBlank()) {
      return Optional.empty();
    }
    return repository.findIdByHash(hash);
  }
}
