package org.mwolff.api.kanban.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Ein Datei-Anhang an genau einem {@link KanbanItem} (Item ODER Epic, #349). Die Binärdaten werden
 * defensiv kopiert, damit das Record unveränderlich bleibt. {@code id} und {@code createdAt} sind
 * erst nach dem Persistieren gesetzt.
 *
 * @param id ID nach Speicherung (vor Erstinsert {@code null})
 * @param itemId ID des Kanban-Eintrags, an dem der Anhang hängt
 * @param filename Original-Dateiname (sanitisiert, max 255 Zeichen)
 * @param contentType per Tika aus den Magic-Bytes ermittelter MIME-Typ (nicht Client-Header)
 * @param sizeBytes Größe in Bytes
 * @param data Binärdaten (defensive Kopie)
 * @param hash SHA-256-Hex der Binärdaten (Integritätsfeld), darf {@code null} sein
 * @param uploadedBySub Keycloak-{@code sub} des Uploaders (Herkunft)
 * @param createdAt Erstanlage
 */
public record KanbanAttachment(
    Long id,
    long itemId,
    String filename,
    String contentType,
    long sizeBytes,
    byte[] data,
    String hash,
    String uploadedBySub,
    Instant createdAt) {

  /** Maximale Länge des Dateinamens — entspricht dem Schema. */
  public static final int MAX_FILENAME_LENGTH = 255;

  public KanbanAttachment {
    Objects.requireNonNull(filename, "filename must not be null");
    if (filename.isBlank()) {
      throw new IllegalArgumentException("filename must not be blank");
    }
    if (filename.length() > MAX_FILENAME_LENGTH) {
      throw new IllegalArgumentException(
          "filename must be at most " + MAX_FILENAME_LENGTH + " chars");
    }
    Objects.requireNonNull(contentType, "contentType must not be null");
    if (contentType.isBlank()) {
      throw new IllegalArgumentException("contentType must not be blank");
    }
    Objects.requireNonNull(uploadedBySub, "uploadedBySub must not be null");
    if (uploadedBySub.isBlank()) {
      throw new IllegalArgumentException("uploadedBySub must not be blank");
    }
    Objects.requireNonNull(data, "data must not be null");
    if (data.length == 0) {
      throw new IllegalArgumentException("data must not be empty");
    }
    data = data.clone();
  }

  /** Fabrik für einen noch nicht persistierten Anhang (ohne id/createdAt). */
  public static KanbanAttachment newInstance(
      long itemId,
      String filename,
      String contentType,
      byte[] data,
      String hash,
      String uploadedBySub) {
    return new KanbanAttachment(
        null, itemId, filename, contentType, data.length, data, hash, uploadedBySub, null);
  }

  @Override
  public byte[] data() {
    return data.clone();
  }
}
