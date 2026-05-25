package org.mwolff.api.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

class ResizeServiceTest {

  private MockWebServer server;
  private ResizeService service;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    final RestClient client = RestClient.builder().baseUrl(server.url("/").toString()).build();
    service = new ResizeService(client);
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  void shouldForwardAllFormFieldsAndReturnUpstreamBytesWithContentType() throws Exception {
    final byte[] processed = "fake-jpeg".getBytes(StandardCharsets.UTF_8);
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/jpeg")
            .setBody(new Buffer().write(processed)));
    final MockMultipartFile upload =
        new MockMultipartFile(
            "file", "photo.png", "image/png", "raw".getBytes(StandardCharsets.UTF_8));

    final ResizeResult result = service.resize(upload, 400, 300, "jpeg", 80);

    assertThat(result.bytes()).isEqualTo(processed);
    assertThat(result.contentType()).isEqualTo(MediaType.IMAGE_JPEG);
    final RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    assertThat(request.getPath()).isEqualTo("/resize");
    final String body = request.getBody().readString(StandardCharsets.UTF_8);
    assertThat(body).contains("name=\"file\"");
    assertThat(body).contains("name=\"width\"");
    assertThat(body).contains("400");
    assertThat(body).contains("name=\"height\"");
    assertThat(body).contains("300");
    assertThat(body).contains("name=\"output_format\"");
    assertThat(body).contains("jpeg");
    assertThat(body).contains("name=\"quality\"");
    assertThat(body).contains("80");
  }

  @Test
  void shouldFallBackToOctetStreamWhenUpstreamOmitsContentType() throws Exception {
    // okhttp MockWebServer also adds a default Content-Type — strip via overrides.
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .removeHeader("Content-Type")
            .setBody(new Buffer().write("data".getBytes(StandardCharsets.UTF_8))));
    final MockMultipartFile upload =
        new MockMultipartFile("file", "x.png", "image/png", "raw".getBytes(StandardCharsets.UTF_8));

    final ResizeResult result = service.resize(upload, 100, 100, "auto", 90);

    assertThat(result.contentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
  }

  @Test
  void shouldThrowPythonToolsExceptionOnUpstreamServerError() {
    server.enqueue(new MockResponse().setResponseCode(500).setBody("crash"));
    final MockMultipartFile upload =
        new MockMultipartFile("file", "x.png", "image/png", "raw".getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> service.resize(upload, 100, 100, "auto", 90))
        .isInstanceOf(PythonToolsException.class)
        .hasMessageContaining("python-tools call failed")
        // Upstream-body must not appear in the exception message; it is logged internally only.
        .hasMessageNotContaining("crash");
  }

  @Test
  void shouldThrowPythonToolsExceptionOnEmptyUpstreamBody() {
    server.enqueue(
        new MockResponse().setResponseCode(200).setHeader("Content-Type", "image/png").setBody(""));
    final MockMultipartFile upload =
        new MockMultipartFile("file", "x.png", "image/png", "raw".getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> service.resize(upload, 100, 100, "auto", 90))
        .isInstanceOf(PythonToolsException.class)
        .hasMessageContaining("empty body");
  }
}
