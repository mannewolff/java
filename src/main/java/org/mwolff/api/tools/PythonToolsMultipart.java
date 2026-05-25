package org.mwolff.api.tools;

import java.io.IOException;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

/**
 * Hilfsklasse zum Bauen von Multipart-Bodies fuer python-tools-Aufrufe.
 *
 * <p>Liefert ein {@link MultiValueMap} mit einem korrekt benannten file-Part (passender
 * Content-Type, filename ueber {@link ByteArrayResource#getFilename}). Zusaetzliche Form-Felder
 * werden vom Aufrufer per {@code body.add(...)} ergaenzt.
 */
final class PythonToolsMultipart {

  private PythonToolsMultipart() {
    // Utility class
  }

  static MultiValueMap<String, Object> withFile(MultipartFile file) {
    final byte[] payload = readBytes(file);
    final HttpHeaders partHeaders = new HttpHeaders();
    final String contentType = file.getContentType();
    partHeaders.setContentType(
        contentType == null
            ? MediaType.APPLICATION_OCTET_STREAM
            : MediaType.parseMediaType(contentType));

    final ByteArrayResource resource =
        new NamedByteArrayResource(payload, filenameOrFallback(file));
    final HttpEntity<ByteArrayResource> part = new HttpEntity<>(resource, partHeaders);

    final MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", part);
    return body;
  }

  private static byte[] readBytes(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException ex) {
      throw new PythonToolsException("Could not read upload bytes", ex);
    }
  }

  private static String filenameOrFallback(MultipartFile file) {
    final String name = file.getOriginalFilename();
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
