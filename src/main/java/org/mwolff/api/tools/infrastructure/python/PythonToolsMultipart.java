package org.mwolff.api.tools.infrastructure.python;

import org.mwolff.api.tools.domain.UploadedImage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Hilfsklasse zum Bauen von Multipart-Bodies für python-tools-Aufrufe.
 *
 * <p>Nimmt einen Domain-Record {@link UploadedImage} entgegen und baut daraus den korrekt benannten
 * file-Part inklusive Content-Type und Dateiname. Zusätzliche Form-Felder fügt der Aufrufer per
 * {@code body.add(...)} hinzu.
 */
final class PythonToolsMultipart {

  private PythonToolsMultipart() {
    // Utility class
  }

  static MultiValueMap<String, Object> withImage(UploadedImage image) {
    final HttpHeaders partHeaders = new HttpHeaders();
    final String contentType = image.contentType();
    partHeaders.setContentType(
        contentType == null || contentType.isBlank()
            ? MediaType.APPLICATION_OCTET_STREAM
            : MediaType.parseMediaType(contentType));

    final ByteArrayResource resource =
        new NamedByteArrayResource(image.bytes(), filenameOrFallback(image));
    final HttpEntity<ByteArrayResource> part = new HttpEntity<>(resource, partHeaders);

    final MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", part);
    return body;
  }

  private static String filenameOrFallback(UploadedImage image) {
    final String name = image.originalFilename();
    return name == null || name.isBlank() ? "upload" : name;
  }

  /** ByteArrayResource that returns a filename so RestClient renders Content-Disposition. */
  private static final class NamedByteArrayResource extends ByteArrayResource {
    private final String filename;

    NamedByteArrayResource(byte[] byteArray, String filename) {
      super(byteArray);
      this.filename = filename;
    }

    @Override
    public String getFilename() {
      return filename;
    }
  }
}
