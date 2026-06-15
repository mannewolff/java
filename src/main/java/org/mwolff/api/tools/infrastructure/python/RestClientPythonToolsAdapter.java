package org.mwolff.api.tools.infrastructure.python;

import java.util.List;

import org.mwolff.api.tools.domain.CropOgParams;
import org.mwolff.api.tools.domain.PaletteParams;
import org.mwolff.api.tools.domain.PaletteResult;
import org.mwolff.api.tools.domain.PythonToolsException;
import org.mwolff.api.tools.domain.PythonToolsPort;
import org.mwolff.api.tools.domain.RasterToPngParams;
import org.mwolff.api.tools.domain.ResizeParams;
import org.mwolff.api.tools.domain.SvgToPngParams;
import org.mwolff.api.tools.domain.ToolImageResult;
import org.mwolff.api.tools.domain.ValidatedImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * RestClient-basierter Adapter, der den {@link PythonToolsPort} gegen den python-tools-
 * Microservice implementiert. Übersetzt Domain-Records in Multipart-Requests, gibt fachliche
 * Domain-Records zurück.
 */
@Component
public class RestClientPythonToolsAdapter implements PythonToolsPort {

  private static final Logger LOG = LoggerFactory.getLogger(RestClientPythonToolsAdapter.class);
  private static final String RESIZE_PATH = "/resize";
  private static final String CROP_PATH = "/crop";
  private static final String REMOVE_BG_PATH = "/remove-bg";
  private static final String PALETTE_PATH = "/palette";
  private static final String SVG_TO_PNG_PATH = "/svg-to-png";
  private static final String RASTER_TO_PNG_PATH = "/raster-to-png";

  private final RestClient client;

  public RestClientPythonToolsAdapter(RestClient pythonToolsRestClient) {
    this.client = pythonToolsRestClient;
  }

  @Override
  public ToolImageResult resize(ValidatedImage image, ResizeParams params) {
    final MultiValueMap<String, Object> body = PythonToolsMultipart.withImage(image);
    body.add("width", Integer.toString(params.width()));
    body.add("height", Integer.toString(params.height()));
    body.add("output_format", params.outputFormat());
    body.add("quality", Integer.toString(params.quality()));
    return postForImage(RESIZE_PATH, body, "resize");
  }

  @Override
  public ToolImageResult cropOg(ValidatedImage image, CropOgParams params) {
    final MultiValueMap<String, Object> body = PythonToolsMultipart.withImage(image);
    body.add("y_offset", Double.toString(params.yOffset()));
    body.add("x_offset", Double.toString(params.xOffset()));
    body.add("quality", Integer.toString(params.quality()));
    body.add("width", Integer.toString(params.width()));
    body.add("height", Integer.toString(params.height()));
    body.add("format", params.format());
    return postForImage(CROP_PATH, body, "crop");
  }

  @Override
  public ToolImageResult removeBackground(ValidatedImage image) {
    final MultiValueMap<String, Object> body = PythonToolsMultipart.withImage(image);
    return postForImage(REMOVE_BG_PATH, body, "remove-bg");
  }

  @Override
  public ToolImageResult convertSvgToPng(ValidatedImage image, SvgToPngParams params) {
    final MultiValueMap<String, Object> body = PythonToolsMultipart.withImage(image);
    // width/height optional — nur weiterleiten wenn gesetzt, sonst nimmt cairosvg
    // die SVG-eigene Geometrie.
    if (params.width() != null) {
      body.add("width", Integer.toString(params.width()));
    }
    if (params.height() != null) {
      body.add("height", Integer.toString(params.height()));
    }
    body.add("background", params.background());
    return postForImage(SVG_TO_PNG_PATH, body, "svg-to-png");
  }

  @Override
  public ToolImageResult convertRasterToPng(ValidatedImage image, RasterToPngParams params) {
    final MultiValueMap<String, Object> body = PythonToolsMultipart.withImage(image);
    // width/height optional — nur weiterleiten wenn gesetzt, sonst bleibt Originalgröße.
    if (params.width() != null) {
      body.add("width", Integer.toString(params.width()));
    }
    if (params.height() != null) {
      body.add("height", Integer.toString(params.height()));
    }
    return postForImage(RASTER_TO_PNG_PATH, body, "raster-to-png");
  }

  @Override
  public PaletteResult extractPalette(ValidatedImage image, PaletteParams params) {
    final MultiValueMap<String, Object> body = PythonToolsMultipart.withImage(image);
    body.add("count", Integer.toString(params.count()));

    try {
      final PalettePayload payload =
          client.post().uri(PALETTE_PATH).body(body).retrieve().body(PalettePayload.class);
      if (payload == null || payload.colors() == null || payload.colors().isEmpty()) {
        throw new PythonToolsException("python-tools returned empty palette");
      }
      return new PaletteResult(payload.colors());
    } catch (RestClientResponseException ex) {
      LOG.warn(
          "python-tools palette call returned {}: {}",
          ex.getStatusCode(),
          ex.getResponseBodyAsString());
      throw new PythonToolsException("python-tools call failed", ex);
    } catch (RestClientException ex) {
      LOG.warn("python-tools palette call failed", ex);
      throw new PythonToolsException("python-tools call failed", ex);
    }
  }

  private ToolImageResult postForImage(
      String path, MultiValueMap<String, Object> body, String opName) {
    try {
      final ResponseEntity<byte[]> response =
          client.post().uri(path).body(body).retrieve().toEntity(byte[].class);
      final byte[] payload = response.getBody();
      // RestClient + JDK HttpClient liefert bei leeren Bodies oder Status 204
      // konsistent null (nicht byte[0]) — nur null-Check noetig. Ein theoretisches
      // byte[0] wird vom ToolImageResult-Konstruktor mit IllegalArgumentException
      // abgefangen.
      if (payload == null) {
        throw new PythonToolsException("python-tools returned empty body");
      }
      final MediaType contentType = response.getHeaders().getContentType();
      final String contentTypeValue =
          contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType.toString();
      return new ToolImageResult(payload, contentTypeValue);
    } catch (RestClientResponseException ex) {
      LOG.warn(
          "python-tools {} call returned {}: {}",
          opName,
          ex.getStatusCode(),
          ex.getResponseBodyAsString());
      throw new PythonToolsException("python-tools call failed", ex);
    } catch (RestClientException ex) {
      LOG.warn("python-tools {} call failed", opName, ex);
      throw new PythonToolsException("python-tools call failed", ex);
    }
  }

  /** Wire-Format des python-tools /palette-Endpoints, intern im Infrastructure-Layer. */
  record PalettePayload(List<String> colors) {}
}
