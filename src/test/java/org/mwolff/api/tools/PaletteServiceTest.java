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

class PaletteServiceTest {

  private MockWebServer server;
  private PaletteService service;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    final RestClient client = RestClient.builder().baseUrl(server.url("/").toString()).build();
    service = new PaletteService(client);
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  void shouldDeserializeJsonPaletteResponse() throws Exception {
    // Given
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"colors\":[\"#aabbcc\",\"#001122\",\"#abcdef\"]}"));
    final MockMultipartFile upload =
        new MockMultipartFile("file", "x.png", "image/png", "raw".getBytes(StandardCharsets.UTF_8));

    // When
    final PaletteResponse response = service.extractPalette(upload, 3);

    // Then
    assertThat(response.colors()).containsExactly("#aabbcc", "#001122", "#abcdef");
    final RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    assertThat(request.getPath()).isEqualTo("/palette");
    final String body = request.getBody().readString(StandardCharsets.UTF_8);
    assertThat(body).contains("name=\"file\"");
    assertThat(body).contains("name=\"count\"");
    assertThat(body).contains("3");
  }

  @Test
  void shouldThrowPythonToolsExceptionOnUpstreamError() {
    // Given
    server.enqueue(new MockResponse().setResponseCode(500).setBody("kmeans crashed"));
    final MockMultipartFile upload =
        new MockMultipartFile("file", "x.png", "image/png", "raw".getBytes(StandardCharsets.UTF_8));

    // When / Then
    assertThatThrownBy(() -> service.extractPalette(upload, 6))
        .isInstanceOf(PythonToolsException.class)
        .hasMessageContaining("python-tools call failed")
        .hasMessageContaining("kmeans crashed");
  }

  @Test
  void shouldThrowWhenUpstreamPaletteIsEmpty() {
    // Given
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"colors\":[]}"));
    final MockMultipartFile upload =
        new MockMultipartFile("file", "x.png", "image/png", "raw".getBytes(StandardCharsets.UTF_8));

    // When / Then
    assertThatThrownBy(() -> service.extractPalette(upload, 6))
        .isInstanceOf(PythonToolsException.class)
        .hasMessageContaining("empty palette");
  }

  @Test
  void shouldThrowWhenUpstreamReturnsNullColors() {
    // Given
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{}"));
    final MockMultipartFile upload =
        new MockMultipartFile("file", "x.png", "image/png", "raw".getBytes(StandardCharsets.UTF_8));

    // When / Then
    assertThatThrownBy(() -> service.extractPalette(upload, 6))
        .isInstanceOf(PythonToolsException.class)
        .hasMessageContaining("empty palette");
  }
}
