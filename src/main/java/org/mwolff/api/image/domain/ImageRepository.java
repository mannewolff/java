package org.mwolff.api.image.domain;

import java.util.Optional;

/** Domain-Port für die Persistenz gespeicherter Bilder (#181). */
public interface ImageRepository {

  /** Speichert das Bild und liefert es inkl. generierter {@code id}/{@code createdAt} zurück. */
  StoredImage save(StoredImage image);

  /** Lädt ein Bild per id; leer, wenn keines existiert. */
  Optional<StoredImage> findById(long id);
}
