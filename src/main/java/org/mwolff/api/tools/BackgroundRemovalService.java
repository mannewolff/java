package org.mwolff.api.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BackgroundRemovalService {

  private static final Logger LOG = LoggerFactory.getLogger(BackgroundRemovalService.class);
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
      LOG.warn(
          "python-tools remove-bg call returned {}: {}",
          ex.getStatusCode(),
          ex.getResponseBodyAsString());
      throw new PythonToolsException("python-tools call failed", ex);
    } catch (RestClientException ex) {
      LOG.warn("python-tools remove-bg call failed", ex);
      throw new PythonToolsException("python-tools call failed", ex);
    }
  }
}
