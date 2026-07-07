package org.mwolff.api.kanban.application;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tika.Tika;
import org.mwolff.api.kanban.domain.KanbanAttachment;
import org.mwolff.api.kanban.domain.KanbanAttachmentLimitExceededException;
import org.mwolff.api.kanban.domain.KanbanAttachmentPort;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validiert und speichert einen Datei-Anhang an einem eigenen Kanban-Eintrag (Item oder Epic,
 * #350). Der client-gemeldete Content-Type wird bewusst NICHT vertraut — der gespeicherte Typ
 * stammt aus der Magic-Byte-Detektion (Tika). Der Download erzwingt ohnehin {@code
 * Content-Disposition: attachment}, sodass kein Anhang inline gerendert wird.
 */
@Component
public class UploadAttachmentUseCase {

  /** Maximale Anzahl Anhänge pro Eintrag. */
  static final int MAX_ATTACHMENTS_PER_ITEM = 5;

  /** Maximale Dateigröße: 10 MB (passt zur Multipart-Konfiguration). */
  static final int MAX_SIZE_BYTES = 10 * 1024 * 1024;

  private final KanbanItemPort items;
  private final KanbanAttachmentPort attachments;

  // Tika.detect(byte[], String) liest die Magic-Bytes direkt aus dem Array und ist thread-safe.
  private final Tika tika = new Tika();

  public UploadAttachmentUseCase(KanbanItemPort items, KanbanAttachmentPort attachments) {
    this.items = items;
    this.attachments = attachments;
  }

  @Transactional
  public KanbanAttachment execute(String userSub, long itemId, byte[] data, String filename) {
    requireOwnedItem(userSub, itemId);
    if (data == null || data.length == 0) {
      throw new IllegalArgumentException("uploaded file is empty");
    }
    if (data.length > MAX_SIZE_BYTES) {
      throw new IllegalArgumentException(
          "file exceeds the 10 MB limit (" + data.length + " bytes)");
    }
    if (attachments.countByItem(itemId) >= MAX_ATTACHMENTS_PER_ITEM) {
      throw new KanbanAttachmentLimitExceededException(itemId, MAX_ATTACHMENTS_PER_ITEM);
    }
    final String contentType = tika.detect(data, filename);
    final String safeName = sanitizeFilename(filename);
    final String hash = DigestUtils.sha256Hex(data);
    return attachments.save(
        KanbanAttachment.newInstance(itemId, safeName, contentType, data, hash, userSub));
  }

  private void requireOwnedItem(String userSub, long itemId) {
    items
        .findById(itemId)
        .filter(i -> i.userSub().equals(userSub))
        .orElseThrow(() -> new KanbanItemNotFoundException(itemId));
  }

  /**
   * Entfernt Zeilenumbrüche, Anführungszeichen und Pfad-Trenner (verhindert Header-Injection in der
   * Content-Disposition und Pfad-Tricks) und kappt auf die Schema-Länge. Leerer/fehlender Name →
   * {@code "download"}.
   */
  static String sanitizeFilename(String raw) {
    if (raw == null) {
      return "download";
    }
    String cleaned = raw.replaceAll("[\\r\\n\"\\\\/]", "").trim();
    if (cleaned.isEmpty()) {
      return "download";
    }
    if (cleaned.length() > KanbanAttachment.MAX_FILENAME_LENGTH) {
      cleaned = cleaned.substring(0, KanbanAttachment.MAX_FILENAME_LENGTH);
    }
    return cleaned;
  }
}
