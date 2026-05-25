package org.mwolff.api.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

class CropOgServiceTest {

  private MockWebServer server;
  private CropOgService service;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    final RestClient client = RestClient.builder().baseUrl(server.url("/").toString()).build();
    service = new CropOgService(client);
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  void shouldForwardImageWithFormFieldsAndReturnResponseBytes() throws Exception {
    // Given
    final byte[] processed = "fake-jpeg".getBytes(StandardCharsets.UTF_8);
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/jpeg")
            .setBody(new Buffer().write(processed)));
    final MockMultipartFile upload =
        new MockMultipartFile(
            "file", "photo.jpg", "image/jpeg", "raw-bytes".getBytes(StandardCharsets.UTF_8));

    // When
    final byte[] result = service.crop(upload, 0.3, 0.5, 88, 1200, 630);

    // Then
    assertThat(result).isEqualTo(processed);
    final RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    assertThat(request.getPath()).isEqualTo("/crop");
    assertThat(request.getMethod()).isEqualTo("POST");
    final String body = request.getBody().readString(StandardCharsets.UTF_8);
    assertThat(body).contains("name=\"file\"");
    assertThat(body).contains("filename=\"photo.jpg\"");
    assertThat(body).contains("Content-Type: image/jpeg");
    assertThat(body).contains("name=\"y_offset\"");
    assertThat(body).contains("0.3");
    assertThat(body).contains("name=\"quality\"");
    assertThat(body).contains("88");
    assertThat(body).contains("name=\"width\"");
    assertThat(body).contains("1200");
    assertThat(body).contains("name=\"height\"");
    assertThat(body).contains("630");
    assertThat(body).contains("name=\"x_offset\"");
  }

  @Test
  void shouldForwardCustomDimensions() throws Exception {
    // Given
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/jpeg")
            .setBody(new Buffer().write("jpeg".getBytes(StandardCharsets.UTF_8))));
    final MockMultipartFile upload =
        new MockMultipartFile(
            "file", "photo.jpg", "image/jpeg", "raw".getBytes(StandardCharsets.UTF_8));

    // When
    service.crop(upload, 0.5, 0.5, 88, 1080, 1080);

    // Then
    final RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    final String body = request.getBody().readString(StandardCharsets.UTF_8);
    assertThat(body).contains("1080");
  }

  @Test
  void shouldThrowPythonToolsExceptionOnUpstreamServerError() {
    // Given
    server.enqueue(new MockResponse().setResponseCode(500).setBody("crash"));
    final MockMultipartFile upload =
        new MockMultipartFile(
            "file", "x.jpg", "image/jpeg", "data".getBytes(StandardCharsets.UTF_8));

    // When / Then
    assertThatThrownBy(() -> service.crop(upload, 0.5, 0.5, 88, 1200, 630))
        .isInstanceOf(PythonToolsException.class)
        .hasMessageContaining("python-tools call failed")
        // Upstream-body must not appear in the exception message; it is logged internally only.
        .hasMessageNotContaining("crash");
  }

  @Test
  void shouldThrowPythonToolsExceptionOnEmptyUpstreamBody() {
    // Given
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/jpeg")
            .setBody(""));
    final MockMultipartFile upload =
        new MockMultipartFile(
            "file", "x.jpg", "image/jpeg", "data".getBytes(StandardCharsets.UTF_8));

    // When / Then
    assertThatThrownBy(() -> service.crop(upload, 0.5, 0.5, 88, 1200, 630))
        .isInstanceOf(PythonToolsException.class)
        .hasMessageContaining("empty body");
  }
}
