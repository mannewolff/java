package org.mwolff.api.image.application;

import java.util.ArrayList;
import java.util.List;

import org.mwolff.api.image.domain.ImageInUseException;
import org.mwolff.api.image.domain.ImageNotFoundException;
import org.mwolff.api.image.domain.ImageRepository;
import org.mwolff.api.image.domain.ImageUsagePort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Löscht ungenutzte Bilder endgültig (Hard-Delete, #202). Referenzierte Bilder bleiben erhalten:
 * Einzel-Löschung wirft {@link ImageInUseException}, Batch-Löschung sammelt sie als Fehler.
 */
@Component
public class DeleteImagesUseCase {

  /** Grund-Codes für fehlgeschlagene Batch-Löschungen. */
  public static final String REASON_IN_USE = "IN_USE";

  public static final String REASON_NOT_FOUND = "NOT_FOUND";

  /** Ein nicht gelöschtes Bild mit Begründung. */
  public record Failure(long id, String reason) {}

  /** Ergebnis einer Batch-Löschung. */
  public record DeleteResult(List<Long> deleted, List<Failure> failed) {}

  private final ImageRepository repository;
  private final ImageUsagePort usagePort;

  public DeleteImagesUseCase(final ImageRepository repository, final ImageUsagePort usagePort) {
    this.repository = repository;
    this.usagePort = usagePort;
  }

  /**
   * Löscht ein einzelnes Bild.
   *
   * @throws ImageNotFoundException wenn das Bild nicht existiert
   * @throws ImageInUseException wenn es noch von Widgets referenziert wird
   */
  @Transactional
  public void deleteOne(final String userSub, final long id) {
    if (repository.findByIdAndUserSub(id, userSub).isEmpty()) {
      throw new ImageNotFoundException(id);
    }
    final long usage = usagePort.countUsages(id);
    if (usage > 0) {
      throw new ImageInUseException(id, usage);
    }
    repository.delete(id);
  }

  /**
   * Löscht mehrere eigene Bilder; ungelöschte (benutzt/fehlend) werden als Fehler zurückgegeben.
   */
  @Transactional
  public DeleteResult deleteBatch(final String userSub, final List<Long> ids) {
    final List<Long> deleted = new ArrayList<>();
    final List<Failure> failed = new ArrayList<>();
    for (final long id : ids) {
      if (repository.findByIdAndUserSub(id, userSub).isEmpty()) {
        failed.add(new Failure(id, REASON_NOT_FOUND));
        continue;
      }
      if (usagePort.countUsages(id) > 0) {
        failed.add(new Failure(id, REASON_IN_USE));
        continue;
      }
      repository.delete(id);
      deleted.add(id);
    }
    return new DeleteResult(deleted, failed);
  }
}
