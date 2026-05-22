package org.mwolff.api.tools;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResizeService {

    private static final String RESIZE_PATH = "/resize";

    private final RestClient client;

    public ResizeService(RestClient pythonToolsRestClient) {
        this.client = pythonToolsRestClient;
    }

    public ResizeResult resize(
            MultipartFile file,
            int width,
            int height,
            String outputFormat,
            int quality) {
        final MultiValueMap<String, Object> body = PythonToolsMultipart.withFile(file);
        body.add("width", Integer.toString(width));
        body.add("height", Integer.toString(height));
        body.add("output_format", outputFormat);
        body.add("quality", Integer.toString(quality));

        try {
            final ResponseEntity<byte[]> response = client.post()
                    .uri(RESIZE_PATH)
                    .body(body)
                    .retrieve()
                    .toEntity(byte[].class);
            final byte[] payload = response.getBody();
            if (payload == null || payload.length == 0) {
                throw new PythonToolsException("python-tools returned empty body");
            }
            final MediaType contentType = response.getHeaders().getContentType();
            return new ResizeResult(payload, contentType == null ? MediaType.APPLICATION_OCTET_STREAM : contentType);
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
