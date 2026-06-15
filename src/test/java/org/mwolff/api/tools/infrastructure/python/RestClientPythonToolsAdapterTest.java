package org.mwolff.api.tools.infrastructure.python;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mwolff.api.tools.domain.CropOgParams;
import org.mwolff.api.tools.domain.PaletteParams;
import org.mwolff.api.tools.domain.PaletteResult;
import org.mwolff.api.tools.domain.PythonToolsException;
import org.mwolff.api.tools.domain.RasterToPngParams;
import org.mwolff.api.tools.domain.ResizeParams;
import org.mwolff.api.tools.domain.SvgToPngParams;
import org.mwolff.api.tools.domain.ToolImageResult;
import org.mwolff.api.tools.domain.ValidatedImage;
import org.springframework.web.client.RestClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

class RestClientPythonToolsAdapterTest {

  private MockWebServer server;
  private RestClientPythonToolsAdapter adapter;

  private final ValidatedImage image =
      new ValidatedImage("raw".getBytes(StandardCharsets.UTF_8), "image/png", "photo.png");

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    final RestClient client = RestClient.builder().baseUrl(server.url("/").toString()).build();
    adapter = new RestClientPythonToolsAdapter(client);
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  // ----- internal auth header (#265) ---------------------------------------

  private RestClientPythonToolsAdapter adapterWithKey(String internalKey) {
    final PythonToolsProperties props =
        new PythonToolsProperties(
            server.url("/").toString(), Duration.ofSeconds(5), Duration.ofSeconds(5), internalKey);
    final RestClient client =
        new PythonToolsConfig().pythonToolsRestClient(RestClient.builder(), props);
    return new RestClientPythonToolsAdapter(client);
  }

  @Test
  void shouldSendInternalKeyHeaderWhenConfigured() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/png")
            .setBody(new Buffer().write("png".getBytes(StandardCharsets.UTF_8))));

    adapterWithKey("secret-key").removeBackground(image);

    final RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    assertThat(request.getHeader("X-Internal-Key")).isEqualTo("secret-key");
  }

  @Test
  void shouldNotSendInternalKeyHeaderWhenBlank() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/png")
            .setBody(new Buffer().write("png".getBytes(StandardCharsets.UTF_8))));

    adapterWithKey("").removeBackground(image);

    final RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    assertThat(request.getHeader("X-Internal-Key")).isNull();
  }

  // ----- resize ------------------------------------------------------------

  @Test
  void resizeShouldForwardAllFormFieldsAndReturnUpstreamBytesWithContentType() throws Exception {
    final byte[] processed = "fake-jpeg".getBytes(StandardCharsets.UTF_8);
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/jpeg")
            .setBody(new Buffer().write(processed)));

    final ToolImageResult result = adapter.resize(image, new ResizeParams(400, 300, "jpeg", 80));

    assertThat(result.bytes()).isEqualTo(processed);
    assertThat(result.contentType()).isEqualTo("image/jpeg");
    final RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    assertThat(request.getPath()).isEqualTo("/resize");
    final String body = request.getBody().readString(StandardCharsets.UTF_8);
    assertThat(body).contains("name=\"file\"");
    assertThat(body).contains("name=\"width\"").contains("400");
    assertThat(body).contains("name=\"height\"").contains("300");
    assertThat(body).contains("name=\"output_format\"").contains("jpeg");
    assertThat(body).contains("name=\"quality\"").contains("80");
  }

  @Test
  void resizeShouldFallBackToOctetStreamWhenUpstreamOmitsContentType() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .removeHeader("Content-Type")
            .setBody(new Buffer().write("data".getBytes(StandardCharsets.UTF_8))));

    final ToolImageResult result = adapter.resize(image, new ResizeParams(100, 100, "auto", 90));

    assertThat(result.contentType()).isEqualTo("application/octet-stream");
  }

  @Test
  void resizeShouldThrowOnUpstreamServerError() {
    server.enqueue(new MockResponse().setResponseCode(500).setBody("crash"));

    assertThatThrownBy(() -> adapter.resize(image, new ResizeParams(100, 100, "auto", 90)))
        .isInstanceOf(PythonToolsException.class)
        .hasMessageContaining("python-tools call failed")
        .hasMessageNotContaining("crash");
  }

  @Test
  void resizeShouldThrowOnEmptyUpstreamBody() {
    // RestClient + JDK HttpClient interpretiert empty bodies bei 200 als null —
    // deckt den payload == null Branch in postForImage.
    server.enqueue(
        new MockResponse().setResponseCode(200).setHeader("Content-Type", "image/png").setBody(""));

    assertThatThrownBy(() -> adapter.resize(image, new ResizeParams(100, 100, "auto", 90)))
        .isInstanceOf(PythonToolsException.class)
        .hasMessageContaining("empty body");
  }

  @Test
  void resizeShouldThrowOnTransportException() throws IOException {
    // Trifft den anderen catch-Block in postForImage (RestClientException statt
    // RestClientResponseException) — Server ist down, also keine HTTP-Antwort.
    server.shutdown();

    assertThatThrownBy(() -> adapter.resize(image, new ResizeParams(100, 100, "auto", 90)))
        .isInstanceOf(PythonToolsException.class);
  }

  // ----- cropOg ------------------------------------------------------------

  @Test
  void cropOgShouldForwardFormFieldsAndReturnBytes() throws Exception {
    final byte[] processed = "jpeg-bytes".getBytes(StandardCharsets.UTF_8);
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/jpeg")
            .setBody(new Buffer().write(processed)));

    final ToolImageResult result =
        adapter.cropOg(image, new CropOgParams(0.3, 0.7, 88, 1200, 630, "jpeg"));

    assertThat(result.bytes()).isEqualTo(processed);
    final RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    assertThat(request.getPath()).isEqualTo("/crop");
    final String body = request.getBody().readString(StandardCharsets.UTF_8);
    assertThat(body).contains("y_offset").contains("0.3");
    assertThat(body).contains("x_offset").contains("0.7");
    assertThat(body).contains("quality").contains("88");
    assertThat(body).contains("width").contains("1200");
    assertThat(body).contains("height").contains("630");
    assertThat(body).contains("name=\"format\"").contains("jpeg");
  }

  @Test
  void cropOgShouldForwardPngFormat() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/png")
            .setBody(new Buffer().write(new byte[] {1})));

    adapter.cropOg(image, new CropOgParams(0.5, 0.5, 88, 1200, 630, "png"));

    final RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    final String body = request.getBody().readString(StandardCharsets.UTF_8);
    assertThat(body).contains("name=\"format\"").contains("png");
  }

  @Test
  void cropOgShouldThrowOnUpstreamFailure() {
    server.enqueue(new MockResponse().setResponseCode(500).setBody("crash"));

    assertThatThrownBy(
            () -> adapter.cropOg(image, new CropOgParams(0.5, 0.5, 88, 1200, 630, "jpeg")))
        .isInstanceOf(PythonToolsException.class);
  }

  // ----- removeBackground --------------------------------------------------

  @Test
  void removeBackgroundShouldReturnUpstreamBytes() throws Exception {
    final byte[] processed = "png-bytes".getBytes(StandardCharsets.UTF_8);
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/png")
            .setBody(new Buffer().write(processed)));

    final ToolImageResult result = adapter.removeBackground(image);

    assertThat(result.bytes()).isEqualTo(processed);
    final RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    assertThat(request.getPath()).isEqualTo("/remove-bg");
  }

  @Test
  void removeBackgroundShouldThrowOnUpstreamFailure() {
    server.enqueue(new MockResponse().setResponseCode(503));

    assertThatThrownBy(() -> adapter.removeBackground(image))
        .isInstanceOf(PythonToolsException.class);
  }

  // ----- extractPalette ----------------------------------------------------

  @Test
  void extractPaletteShouldParseJsonResponse() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"colors\":[\"#abc123\",\"#def456\"]}"));

    final PaletteResult result = adapter.extractPalette(image, new PaletteParams(6));

    assertThat(result.colors()).containsExactly("#abc123", "#def456");
    final RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    assertThat(request.getPath()).isEqualTo("/palette");
    final String body = request.getBody().readString(StandardCharsets.UTF_8);
    assertThat(body).contains("name=\"count\"").contains("6");
  }

  @Test
  void extractPaletteShouldThrowOnEmptyColors() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"colors\":[]}"));

    assertThatThrownBy(() -> adapter.extractPalette(image, new PaletteParams(6)))
        .isInstanceOf(PythonToolsException.class)
        .hasMessageContaining("empty palette");
  }

  @Test
  void extractPaletteShouldThrowOnNullPayload() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("null"));

    assertThatThrownBy(() -> adapter.extractPalette(image, new PaletteParams(6)))
        .isInstanceOf(PythonToolsException.class);
  }

  @Test
  void extractPaletteShouldThrowWhenColorsFieldIsNull() {
    // PalettePayload mit colors=null — deckt den mittleren Zweig der OR-Pruefung.
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{}"));

    assertThatThrownBy(() -> adapter.extractPalette(image, new PaletteParams(6)))
        .isInstanceOf(PythonToolsException.class)
        .hasMessageContaining("empty palette");
  }

  @Test
  void resizeShouldThrowWhenUpstreamSends204NoContent() {
    // Status 204 -> RestClient liefert null als Body. Deckt den null-Zweig der OR-Pruefung
    // in postForImage (payload == null), während die length-Variante bereits via empty-body
    // Test gedeckt ist.
    server.enqueue(new MockResponse().setResponseCode(204));

    assertThatThrownBy(() -> adapter.resize(image, new ResizeParams(100, 100, "auto", 90)))
        .isInstanceOf(PythonToolsException.class)
        .hasMessageContaining("empty body");
  }

  @Test
  void extractPaletteShouldThrowOnUpstreamFailure() {
    server.enqueue(new MockResponse().setResponseCode(500));

    assertThatThrownBy(() -> adapter.extractPalette(image, new PaletteParams(6)))
        .isInstanceOf(PythonToolsException.class);
  }

  @Test
  void extractPaletteShouldThrowOnTransportException() throws IOException {
    server.shutdown();

    assertThatThrownBy(() -> adapter.extractPalette(image, new PaletteParams(6)))
        .isInstanceOf(PythonToolsException.class);
  }

  // ----- Multipart edge cases (covers PythonToolsMultipart branches) -------

  @Test
  void shouldSendValidatedContentTypeAsFilePartHeader() throws Exception {
    // #135: der erkannte (vertrauenswürdige) MIME-Type aus ValidatedImage muss als
    // Content-Type des file-Parts in den Multipart-Body wandern — kein octet-stream-Fallback,
    // kein Client-Wert.
    final ValidatedImage webpImage =
        new ValidatedImage("raw".getBytes(StandardCharsets.UTF_8), "image/webp", "photo.webp");
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/png")
            .setBody(new Buffer().write(new byte[] {7})));

    adapter.resize(webpImage, new ResizeParams(10, 10, "auto", 90));

    final RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    final String body = request.getBody().readString(StandardCharsets.UTF_8);
    assertThat(body).contains("name=\"file\"");
    assertThat(body).contains("Content-Type: image/webp");
    assertThat(body).doesNotContain("application/octet-stream");
  }

  @Test
  void shouldFallBackToUploadFilenameWhenOriginalFilenameIsNull() throws Exception {
    final ValidatedImage noFilename =
        new ValidatedImage("raw".getBytes(StandardCharsets.UTF_8), "image/png", null);
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/png")
            .setBody(new Buffer().write(new byte[] {7})));

    adapter.resize(noFilename, new ResizeParams(10, 10, "auto", 90));

    final RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    final String body = request.getBody().readString(StandardCharsets.UTF_8);
    // Default-Fallback ist "upload" — siehe PythonToolsMultipart.filenameOrFallback
    assertThat(body).contains("filename=\"upload\"");
  }

  @Test
  void shouldFallBackToUploadFilenameWhenOriginalFilenameIsBlank() throws Exception {
    final ValidatedImage blankFilename =
        new ValidatedImage("raw".getBytes(StandardCharsets.UTF_8), "image/png", "   ");
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/png")
            .setBody(new Buffer().write(new byte[] {7})));

    adapter.resize(blankFilename, new ResizeParams(10, 10, "auto", 90));

    final RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    final String body = request.getBody().readString(StandardCharsets.UTF_8);
    assertThat(body).contains("filename=\"upload\"");
  }

  // ----- svg-to-png --------------------------------------------------------

  private final ValidatedImage svgImage =
      new ValidatedImage("<svg/>".getBytes(StandardCharsets.UTF_8), "image/svg+xml", "logo.svg");

  @Test
  void svgToPngShouldForwardAllFormFieldsWhenDimensionsGiven() throws Exception {
    final byte[] processed = "fake-png".getBytes(StandardCharsets.UTF_8);
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/png")
            .setBody(new Buffer().write(processed)));

    final ToolImageResult result =
        adapter.convertSvgToPng(svgImage, new SvgToPngParams(512, 256, "#ffffff"));

    assertThat(result.bytes()).isEqualTo(processed);
    assertThat(result.contentType()).isEqualTo("image/png");
    final RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    assertThat(request.getPath()).isEqualTo("/svg-to-png");
    final String body = request.getBody().readString(StandardCharsets.UTF_8);
    assertThat(body).contains("name=\"file\"");
    assertThat(body).contains("name=\"width\"").contains("512");
    assertThat(body).contains("name=\"height\"").contains("256");
    assertThat(body).contains("name=\"background\"").contains("#ffffff");
  }

  @Test
  void svgToPngShouldOmitWidthAndHeightWhenNull() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/png")
            .setBody(new Buffer().write(new byte[] {1})));

    adapter.convertSvgToPng(svgImage, new SvgToPngParams(null, null, "transparent"));

    final RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    final String body = request.getBody().readString(StandardCharsets.UTF_8);
    assertThat(body).doesNotContain("name=\"width\"");
    assertThat(body).doesNotContain("name=\"height\"");
    assertThat(body).contains("name=\"background\"").contains("transparent");
  }

  // ----- raster-to-png -------------------------------------------------------

  @Test
  void rasterToPngShouldForwardWidthAndHeightWhenBothGiven() throws Exception {
    final byte[] processed = "fake-png".getBytes(StandardCharsets.UTF_8);
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/png")
            .setBody(new Buffer().write(processed)));

    final ToolImageResult result =
        adapter.convertRasterToPng(image, new RasterToPngParams(800, 600));

    assertThat(result.bytes()).isEqualTo(processed);
    assertThat(result.contentType()).isEqualTo("image/png");
    final RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    assertThat(request.getPath()).isEqualTo("/raster-to-png");
    final String body = request.getBody().readString(StandardCharsets.UTF_8);
    assertThat(body).contains("name=\"width\"").contains("800");
    assertThat(body).contains("name=\"height\"").contains("600");
  }

  @Test
  void rasterToPngShouldOmitWidthAndHeightWhenNull() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/png")
            .setBody(new Buffer().write(new byte[] {1})));

    adapter.convertRasterToPng(image, new RasterToPngParams(null, null));

    final RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    final String body = request.getBody().readString(StandardCharsets.UTF_8);
    assertThat(body).doesNotContain("name=\"width\"");
    assertThat(body).doesNotContain("name=\"height\"");
  }

  @Test
  void rasterToPngShouldSendWidthOnlyWhenHeightIsNull() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/png")
            .setBody(new Buffer().write(new byte[] {1})));

    adapter.convertRasterToPng(image, new RasterToPngParams(400, null));

    final RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    final String body = request.getBody().readString(StandardCharsets.UTF_8);
    assertThat(body).contains("name=\"width\"").contains("400");
    assertThat(body).doesNotContain("name=\"height\"");
  }

  @Test
  void rasterToPngShouldSendHeightOnlyWhenWidthIsNull() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/png")
            .setBody(new Buffer().write(new byte[] {1})));

    adapter.convertRasterToPng(image, new RasterToPngParams(null, 300));

    final RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    final String body = request.getBody().readString(StandardCharsets.UTF_8);
    assertThat(body).doesNotContain("name=\"width\"");
    assertThat(body).contains("name=\"height\"").contains("300");
  }

  @Test
  void rasterToPngShouldThrowOnUpstreamServerError() {
    server.enqueue(new MockResponse().setResponseCode(500).setBody("pillow-crash"));

    assertThatThrownBy(() -> adapter.convertRasterToPng(image, new RasterToPngParams(null, null)))
        .isInstanceOf(PythonToolsException.class)
        .hasMessageContaining("python-tools call failed")
        .hasMessageNotContaining("pillow-crash");
  }

  @Test
  void svgToPngShouldThrowOnUpstreamServerError() {
    server.enqueue(new MockResponse().setResponseCode(500).setBody("cairo-crash"));

    assertThatThrownBy(
            () -> adapter.convertSvgToPng(svgImage, new SvgToPngParams(null, null, "transparent")))
        .isInstanceOf(PythonToolsException.class)
        .hasMessageContaining("python-tools call failed")
        .hasMessageNotContaining("cairo-crash");
  }
}
