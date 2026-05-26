package org.mwolff.api.tools.domain;

/**
 * Out-Port für die Upload-Validierung. Implementiert im Infrastructure-Layer (z.B. Tika-basiert).
 * Application-Use-Cases rufen ihn auf, bevor ein Bild an den python-tools-Port weitergereicht wird.
 */
public interface UploadValidatorPort {

  /**
   * Validiert ein hochgeladenes Bild auf Größe, Inhalt und tatsächlichen MIME-Typ.
   *
   * @throws InvalidUploadException bei fachlich invalidem Upload
   */
  void validateImage(UploadedImage image);
}
