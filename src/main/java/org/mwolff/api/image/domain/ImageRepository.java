package org.mwolff.api.image.domain;

import java.util.Optional;

/** Domain-Port für die Persistenz gespeicherter Bilder (#181). */
public interface ImageRepository {

  /** Speichert das Bild und liefert es inkl. generierter {@code id}/{@code createdAt} zurück. */
  StoredImage save(StoredImage image);

  /** Lädt ein Bild per id; leer, wenn keines existiert. */
  Optional<StoredImage> findById(long id);

  /**
   * Liefert Bild-Metadaten (ohne Binärdaten) absteigend nach id, beschränkt auf {@code limit} ab
   * {@code offset}. Für Galerie-Ansichten (#198).
   */
  java.util.List<ImageMetadata> findMetadata(int limit, int offset);

  /** Gesamtzahl gespeicherter Bilder — Basis für die Paginierung (#198). */
  long count();

  /** Id eines existierenden Bildes mit diesem SHA-256-Hash, für die Duplikat-Erkennung (#199). */
  Optional<Long> findIdByHash(String hash);
}
