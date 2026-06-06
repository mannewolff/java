package org.mwolff.api.image.domain;

import java.util.Optional;

/** Domain-Port für die Persistenz gespeicherter Bilder (#181), owner-isoliert (#230). */
public interface ImageRepository {

  /**
   * Speichert das Bild und liefert es inkl. generierter {@code id}/{@code createdAt} zurück. Der
   * {@code userSub} des übergebenen Bildes legt den Eigentümer fest.
   */
  StoredImage save(StoredImage image);

  /**
   * Lädt ein eigenes Bild per id; leer, wenn keines existiert oder es einem anderen User gehört.
   */
  Optional<StoredImage> findByIdAndUserSub(long id, String userSub);

  /**
   * Liefert Bild-Metadaten (ohne Binärdaten) des Users absteigend nach id, beschränkt auf {@code
   * limit} ab {@code offset}. Für Galerie-Ansichten (#198).
   */
  java.util.List<ImageMetadata> findMetadataByUserSub(String userSub, int limit, int offset);

  /** Anzahl der eigenen Bilder — Basis für die Paginierung (#198). */
  long countByUserSub(String userSub);

  /**
   * Id eines existierenden eigenen Bildes mit diesem SHA-256-Hash, für die Duplikat-Erkennung
   * (#199). Per-User, damit ein Hash kein fremdes Bild verrät (#230).
   */
  Optional<Long> findIdByHashAndUserSub(String hash, String userSub);

  /** Löscht ein Bild endgültig (Hard-Delete, #202). No-op, wenn es nicht existiert. */
  void delete(long id);
}
