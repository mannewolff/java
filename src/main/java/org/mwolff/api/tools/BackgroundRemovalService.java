package org.mwolff.api.tools;

import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BackgroundRemovalService {

  private static final String REMOVE_BG_PATH = "/remove-bg";

  private final RestClient client;

  public BackgroundRemovalService(RestClient pythonToolsRestClient) {
    this.client = pythonToolsRestClient;
  }

  public byte[] removeBackground(MultipartFile file) {
    final MultiValueMap<String, Object> body = PythonToolsMultipart.withFile(file);

    try {
      // Do NOT pre-set Content-Type — Spring's FormHttpMessageConverter
      // detects multipart from the MultiValueMap and writes the full
      // Content-Type header including the generated boundary.
      final byte[] response =
          client.post().uri(REMOVE_BG_PATH).body(body).retrieve().body(byte[].class);
      if (response == null || response.length == 0) {
        throw new PythonToolsException("python-tools returned empty body");
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
