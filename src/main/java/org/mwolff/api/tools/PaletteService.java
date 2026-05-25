package org.mwolff.api.tools;

import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PaletteService {

  private static final String PALETTE_PATH = "/palette";

  private final RestClient client;

  public PaletteService(RestClient pythonToolsRestClient) {
    this.client = pythonToolsRestClient;
  }

  public PaletteResponse extractPalette(MultipartFile file, int count) {
    final MultiValueMap<String, Object> body = PythonToolsMultipart.withFile(file);
    body.add("count", Integer.toString(count));

    try {
      final PaletteResponse response =
          client.post().uri(PALETTE_PATH).body(body).retrieve().body(PaletteResponse.class);
      if (response == null || response.colors() == null || response.colors().isEmpty()) {
        throw new PythonToolsException("python-tools returned empty palette");
      }
      return response;
    } catch (RestClientResponseException ex) {
      final String upstream = ex.getResponseBodyAsString();
      final String detail = upstream.isBlank() ? ex.getStatusText() : upstream;
      throw new PythonToolsException(
          "python-tools call failed (" + ex.getStatusCode() + "): " + detail, ex);
    } catch (RestClientException ex) {
      throw new PythonToolsException("python-tools call failed", ex);
    }
  }
}
