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

class BackgroundRemovalServiceTest {

  private MockWebServer server;
  private BackgroundRemovalService service;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    RestClient client = RestClient.builder().baseUrl(server.url("/").toString()).build();
    service = new BackgroundRemovalService(client);
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  void shouldForwardImageAndReturnResponseBytes() throws Exception {
    // Given
    byte[] processed = "fake-png-output".getBytes(StandardCharsets.UTF_8);
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/png")
            .setBody(new Buffer().write(processed)));
    MockMultipartFile upload =
        new MockMultipartFile(
            "file", "input.png", "image/png", "raw-bytes".getBytes(StandardCharsets.UTF_8));

    // When
    byte[] result = service.removeBackground(upload);

    // Then
    assertThat(result).isEqualTo(processed);
    RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    assertThat(request.getPath()).isEqualTo("/remove-bg");
    assertThat(request.getMethod()).isEqualTo("POST");
    String contentType = request.getHeader("Content-Type");
    assertThat(contentType).startsWith("multipart/form-data");
    String body = request.getBody().readString(StandardCharsets.UTF_8);
    assertThat(body).contains("raw-bytes");
    assertThat(body).contains("name=\"file\"");
    assertThat(body).contains("filename=\"input.png\"");
    assertThat(body).contains("Content-Type: image/png");
  }

  @Test
  void shouldFallBackToDefaultFilenameWhenMissing() throws Exception {
    // Given
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/png")
            .setBody(new Buffer().write("png".getBytes(StandardCharsets.UTF_8))));
    MockMultipartFile upload =
        new MockMultipartFile("file", null, null, "data".getBytes(StandardCharsets.UTF_8));

    // When
    service.removeBackground(upload);

    // Then
    RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    String body = request.getBody().readString(StandardCharsets.UTF_8);
    assertThat(body).contains("filename=\"upload\"");
  }

  @Test
  void shouldThrowPythonToolsExceptionOnUpstreamServerError() {
    // Given
    server.enqueue(new MockResponse().setResponseCode(500).setBody("oops"));
    MockMultipartFile upload =
        new MockMultipartFile(
            "file", "x.png", "image/png", "data".getBytes(StandardCharsets.UTF_8));

    // When / Then
    assertThatThrownBy(() -> service.removeBackground(upload))
        .isInstanceOf(PythonToolsException.class)
        .hasMessageContaining("python-tools call failed");
  }

  @Test
  void shouldThrowPythonToolsExceptionOnUpstreamClientError() {
    // Given
    server.enqueue(new MockResponse().setResponseCode(415).setBody("nope"));
    MockMultipartFile upload =
        new MockMultipartFile(
            "file", "x.png", "image/png", "data".getBytes(StandardCharsets.UTF_8));

    // When / Then
    assertThatThrownBy(() -> service.removeBackground(upload))
        .isInstanceOf(PythonToolsException.class);
  }

  @Test
  void shouldThrowPythonToolsExceptionOnEmptyUpstreamBody() {
    // Given
    server.enqueue(
        new MockResponse().setResponseCode(200).setHeader("Content-Type", "image/png").setBody(""));
    MockMultipartFile upload =
        new MockMultipartFile(
            "file", "x.png", "image/png", "data".getBytes(StandardCharsets.UTF_8));

    // When / Then
    assertThatThrownBy(() -> service.removeBackground(upload))
        .isInstanceOf(PythonToolsException.class)
        .hasMessageContaining("empty body");
  }

  @Test
  void shouldThrowPythonToolsExceptionWhenUploadCannotBeRead() throws IOException {
    // Given — server is irrelevant, we never reach it
    MockMultipartFile upload =
        new MockMultipartFile("file", "x.png", "image/png", new byte[0]) {
          @Override
          public byte[] getBytes() throws IOException {
            throw new IOException("disk error");
          }
        };

    // When / Then
    assertThatThrownBy(() -> service.removeBackground(upload))
        .isInstanceOf(PythonToolsException.class)
        .hasMessageContaining("read upload bytes");
  }

  @Test
  void shouldFallBackToDefaultFilenameWhenBlank() throws Exception {
    // Given
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/png")
            .setBody(new Buffer().write("png".getBytes(StandardCharsets.UTF_8))));
    MockMultipartFile upload =
        new MockMultipartFile("file", "   ", "image/png", "data".getBytes(StandardCharsets.UTF_8));

    // When
    service.removeBackground(upload);

    // Then
    RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    String body = request.getBody().readString(StandardCharsets.UTF_8);
    assertThat(body).contains("filename=\"upload\"");
  }
}
