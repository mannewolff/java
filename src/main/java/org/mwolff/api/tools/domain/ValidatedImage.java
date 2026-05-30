package org.mwolff.api.tools.domain;

import java.util.Objects;

/**
 * Ein Upload, der die Validierung passiert hat: Größe geprüft, MIME-Type per Magic-Bytes erkannt.
 *
 * <p>Der {@code contentType} ist hier vertrauenswürdig — er stammt aus der Byte-Signatur-Erkennung
 * (Tika), nicht vom Client. Das ist der Unterschied zu {@link UploadedImage}, dessen contentType
 * vom Client gemeldet und damit nicht verifiziert ist. Use-Cases reichen diesen Record (statt des
 * rohen {@link UploadedImage}) an den {@link PythonToolsPort} weiter, damit der erkannte MIME-Type
 * bis in den Multipart-Body propagiert (#135) und nicht der unkontrollierte Client-Wert.
 *
 * @param bytes Roh-Bytes der Datei — niemals {@code null}, niemals leer
 * @param contentType per Byte-Signatur erkannter MIME-Type — niemals {@code null}, niemals blank
 * @param originalFilename Original-Dateiname vom Client; {@code null} erlaubt
 */
public record ValidatedImage(byte[] bytes, String contentType, String originalFilename) {

  public ValidatedImage {
    Objects.requireNonNull(bytes, "bytes must not be null");
    if (bytes.length == 0) {
      throw new IllegalArgumentException("bytes must not be empty");
    }
    Objects.requireNonNull(contentType, "contentType must not be null");
    if (contentType.isBlank()) {
      throw new IllegalArgumentException("contentType must not be blank");
    }
    bytes = bytes.clone();
  }

  /** Defensive Kopie — verhindert dass Aufrufer das interne Array mutiert. */
  @Override
  public byte[] bytes() {
    return bytes.clone();
  }

  /** Größe in Bytes — kürzer als {@code bytes().length} und ohne Kopierkosten. */
  public long size() {
    return bytes.length;
  }
}
