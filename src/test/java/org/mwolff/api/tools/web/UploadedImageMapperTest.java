package org.mwolff.api.tools.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.mwolff.api.tools.domain.InvalidUploadException;
import org.mwolff.api.tools.domain.UploadedImage;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class UploadedImageMapperTest {

  @Test
  void shouldMapMultipartFileToDomainRecord() {
    final byte[] payload = new byte[] {1, 2, 3};
    final MockMultipartFile file = new MockMultipartFile("file", "x.png", "image/png", payload);

    final UploadedImage image = UploadedImageMapper.toDomain(file);

    assertThat(image.bytes()).isEqualTo(payload);
    assertThat(image.contentType()).isEqualTo("image/png");
    assertThat(image.originalFilename()).isEqualTo("x.png");
  }

  @Test
  void shouldRejectNullFile() {
    assertThatThrownBy(() -> UploadedImageMapper.toDomain(null))
        .isInstanceOf(InvalidUploadException.class)
        .matches(ex -> ((InvalidUploadException) ex).code().equals("EMPTY_FILE"));
  }

  @Test
  void shouldRejectEmptyFile() {
    final MockMultipartFile file = new MockMultipartFile("file", "x.png", "image/png", new byte[0]);
    assertThatThrownBy(() -> UploadedImageMapper.toDomain(file))
        .isInstanceOf(InvalidUploadException.class)
        .matches(ex -> ((InvalidUploadException) ex).code().equals("EMPTY_FILE"));
  }

  @Test
  void shouldRejectWhenGetBytesFails() {
    final MultipartFile file =
        new MockMultipartFile("file", "x.png", "image/png", new byte[] {1}) {
          @Override
          public byte[] getBytes() throws IOException {
            throw new IOException("disk failure");
          }
        };

    assertThatThrownBy(() -> UploadedImageMapper.toDomain(file))
        .isInstanceOf(InvalidUploadException.class)
        .matches(ex -> ((InvalidUploadException) ex).code().equals("READ_FAILED"));
  }
}
