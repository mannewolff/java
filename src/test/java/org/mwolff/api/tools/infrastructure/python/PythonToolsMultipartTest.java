package org.mwolff.api.tools.infrastructure.python;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mwolff.api.tools.domain.ValidatedImage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;

class PythonToolsMultipartTest {

  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G'};

  private static HttpEntity<?> filePart(MultiValueMap<String, Object> body) {
    return (HttpEntity<?>) body.getFirst("file");
  }

  private static ByteArrayResource fileResource(MultiValueMap<String, Object> body) {
    return (ByteArrayResource) filePart(body).getBody();
  }

  @Test
  void withImageBuildsSingleFilePartWithDetectedContentType() {
    // given/when
    final MultiValueMap<String, Object> body =
        PythonToolsMultipart.withImage(new ValidatedImage(PNG, "image/png", "logo.png"));

    // then — genau ein file-Part, Content-Type aus der Byte-Signatur-Erkennung (#135)
    assertThat(body.keySet()).containsExactly("file");
    assertThat(filePart(body).getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
  }

  @Test
  void withImageCarriesBytesAndOriginalFilename() {
    final MultiValueMap<String, Object> body =
        PythonToolsMultipart.withImage(new ValidatedImage(PNG, "image/png", "logo.png"));

    final ByteArrayResource resource = fileResource(body);
    assertThat(resource.getFilename()).isEqualTo("logo.png");
    assertThat(resource.getByteArray()).isEqualTo(PNG);
  }

  @Test
  void withImageFallsBackToUploadWhenFilenameNull() {
    final MultiValueMap<String, Object> body =
        PythonToolsMultipart.withImage(new ValidatedImage(PNG, "image/png", null));

    assertThat(fileResource(body).getFilename()).isEqualTo("upload");
  }

  @Test
  void withImageFallsBackToUploadWhenFilenameBlank() {
    final MultiValueMap<String, Object> body =
        PythonToolsMultipart.withImage(new ValidatedImage(PNG, "image/png", "   "));

    assertThat(fileResource(body).getFilename()).isEqualTo("upload");
  }
}
