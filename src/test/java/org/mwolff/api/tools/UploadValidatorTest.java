package org.mwolff.api.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class UploadValidatorTest {

  private static final byte[] PNG_HEADER =
      new byte[] {
        (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 0, 0, 0, 0, 'I', 'H', 'D', 'R'
      };

  private static final byte[] JPEG_HEADER =
      new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};

  private final UploadValidator validator = new UploadValidator();

  @Test
  void shouldAcceptRealPngEvenWhenContentTypeIsLying() {
    // Given a PNG header but Content-Type set to text/plain (typical client-trust bug)
    final MultipartFile upload =
        new MockMultipartFile("file", "image.png", "text/plain", PNG_HEADER);

    // When + Then: validation must pass because the magic bytes win over the content type
    validator.validateImageUpload(upload);
  }

  @Test
  void shouldAcceptRealJpeg() {
    // Given a real JPEG byte signature
    final MultipartFile upload =
        new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG_HEADER);

    // When + Then
    validator.validateImageUpload(upload);
  }

  @Test
  void shouldRejectEmptyFile() {
    // Given an empty MultipartFile
    final MultipartFile upload = new MockMultipartFile("file", "x.png", "image/png", new byte[0]);

    // When + Then
    assertThatThrownBy(() -> validator.validateImageUpload(upload))
        .isInstanceOf(InvalidUploadException.class)
        .matches(ex -> ((InvalidUploadException) ex).code().equals("EMPTY_FILE"));
  }

  @Test
  void shouldRejectNullFile() {
    assertThatThrownBy(() -> validator.validateImageUpload(null))
        .isInstanceOf(InvalidUploadException.class)
        .matches(ex -> ((InvalidUploadException) ex).code().equals("EMPTY_FILE"));
  }

  @Test
  void shouldRejectFileExceedingMaximumSize() {
    // Given a file that claims to be 11 MB
    final MultipartFile upload =
        new MockMultipartFile("file", "huge.png", "image/png", PNG_HEADER) {
          @Override
          public long getSize() {
            return UploadValidator.MAX_BYTES + 1;
          }
        };

    // When + Then
    assertThatThrownBy(() -> validator.validateImageUpload(upload))
        .isInstanceOf(InvalidUploadException.class)
        .matches(ex -> ((InvalidUploadException) ex).code().equals("FILE_TOO_LARGE"));
  }

  @Test
  void shouldRejectPlainTextEvenWhenLabelledAsPng() {
    // Given plain ASCII bytes claiming to be image/png
    final MultipartFile upload =
        new MockMultipartFile("file", "fake.png", "image/png", "Hello, world".getBytes());

    // When + Then
    assertThatThrownBy(() -> validator.validateImageUpload(upload))
        .isInstanceOf(InvalidUploadException.class)
        .matches(ex -> ((InvalidUploadException) ex).code().equals("UNSUPPORTED_FORMAT"));
  }

  @Test
  void shouldRejectFileWhenStreamCannotBeRead() {
    // Given a multipart file whose InputStream throws
    final MultipartFile upload =
        new MockMultipartFile("file", "image.png", "image/png", PNG_HEADER) {
          @Override
          public InputStream getInputStream() throws IOException {
            throw new IOException("disk failure");
          }
        };

    // When + Then
    assertThatThrownBy(() -> validator.validateImageUpload(upload))
        .isInstanceOf(InvalidUploadException.class)
        .matches(ex -> ((InvalidUploadException) ex).code().equals("READ_FAILED"));
  }

  @Test
  void shouldExposeCodeOnException() {
    final InvalidUploadException ex = new InvalidUploadException("FOO", "bar");
    assertThat(ex.code()).isEqualTo("FOO");
    assertThat(ex.getMessage()).isEqualTo("bar");
  }
}
