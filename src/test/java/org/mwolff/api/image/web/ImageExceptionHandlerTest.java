package org.mwolff.api.image.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mwolff.api.image.domain.InvalidImageUploadException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ImageExceptionHandlerTest {

  private final ImageExceptionHandler handler = new ImageExceptionHandler();

  @Test
  void tooLargeMapsTo413() {
    final ResponseEntity<?> response =
        handler.handleInvalidUpload(new InvalidImageUploadException("TOO_LARGE", "big"));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
  }
}
