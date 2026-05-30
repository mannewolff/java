package org.mwolff.api.tools.domain;

/**
 * Out-Port für die Upload-Validierung. Implementiert im Infrastructure-Layer (z.B. Tika-basiert).
 * Application-Use-Cases rufen ihn auf, bevor ein Bild an den python-tools-Port weitergereicht wird.
 */
public interface UploadValidatorPort {

  /**
   * Validiert ein hochgeladenes Bild auf Größe, Inhalt und tatsächlichen MIME-Typ.
   *
   * @return das validierte Bild mit dem per Byte-Signatur erkannten, vertrauenswürdigen MIME-Typ
   * @throws InvalidUploadException bei fachlich invalidem Upload
   */
  ValidatedImage validateImage(UploadedImage image);

  /**
   * Validiert eine hochgeladene SVG-Datei auf Größe und tatsächlichen MIME-Typ ({@code
   * image/svg+xml}). Eigener Pfad, weil SVG nicht zu den rastergrafischen Formaten gehört und
   * deshalb von {@link #validateImage(UploadedImage)} ausgeschlossen wird.
   *
   * @return das validierte SVG mit dem erkannten, vertrauenswürdigen MIME-Typ
   * @throws InvalidUploadException bei fachlich invalidem Upload
   */
  ValidatedImage validateSvg(UploadedImage image);
}
