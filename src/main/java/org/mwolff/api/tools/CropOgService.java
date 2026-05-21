package org.mwolff.api.tools;

import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CropOgService {

    private static final String CROP_PATH = "/crop";

    private final RestClient client;

    public CropOgService(RestClient pythonToolsRestClient) {
        this.client = pythonToolsRestClient;
    }

    public byte[] crop(MultipartFile file, double yOffset, int quality) {
        final MultiValueMap<String, Object> body = PythonToolsMultipart.withFile(file);
        body.add("y_offset", Double.toString(yOffset));
        body.add("quality", Integer.toString(quality));

        try {
            final byte[] response = client.post()
                    .uri(CROP_PATH)
                    .body(body)
                    .retrieve()
                    .body(byte[].class);
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
