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
public class CropOgService {

  private static final Logger LOG = LoggerFactory.getLogger(CropOgService.class);
  private static final String CROP_PATH = "/crop";

  private final RestClient client;

  public CropOgService(RestClient pythonToolsRestClient) {
    this.client = pythonToolsRestClient;
  }

  public byte[] crop(
      MultipartFile file, double yOffset, double xOffset, int quality, int width, int height) {
    final MultiValueMap<String, Object> body = PythonToolsMultipart.withFile(file);
    body.add("y_offset", Double.toString(yOffset));
    body.add("x_offset", Double.toString(xOffset));
    body.add("quality", Integer.toString(quality));
    body.add("width", Integer.toString(width));
    body.add("height", Integer.toString(height));

    try {
      final byte[] response = client.post().uri(CROP_PATH).body(body).retrieve().body(byte[].class);
      if (response == null || response.length == 0) {
        throw new PythonToolsException("python-tools returned empty body");
      }
      return response;
    } catch (RestClientResponseException ex) {
      LOG.warn(
          "python-tools crop call returned {}: {}",
          ex.getStatusCode(),
          ex.getResponseBodyAsString());
      throw new PythonToolsException("python-tools call failed", ex);
    } catch (RestClientException ex) {
      LOG.warn("python-tools crop call failed", ex);
      throw new PythonToolsException("python-tools call failed", ex);
    }
  }
}
