package org.mwolff.api.tools;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class BackgroundRemovalService {

    private static final String REMOVE_BG_PATH = "/remove-bg";

    private final RestClient client;

    public BackgroundRemovalService(RestClient pythonToolsRestClient) {
        this.client = pythonToolsRestClient;
    }

    public byte[] removeBackground(MultipartFile file) {
        final byte[] payload = readBytes(file);
        final MultiValueMap<String, HttpEntity<?>> body = buildMultipart(file, payload);

        try {
            final byte[] response = client.post()
                    .uri(REMOVE_BG_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
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

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new PythonToolsException("Could not read upload bytes", ex);
        }
    }

    private static MultiValueMap<String, HttpEntity<?>> buildMultipart(MultipartFile file, byte[] payload) {
        final HttpHeaders partHeaders = new HttpHeaders();
        final String contentType = file.getContentType();
        partHeaders.setContentType(
                contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType));
        partHeaders.setContentDispositionFormData("file", filenameOrFallback(file));

        final ByteArrayResource resource = new NamedByteArrayResource(payload, filenameOrFallback(file));
        final HttpEntity<ByteArrayResource> part = new HttpEntity<>(resource, partHeaders);

        final MultiValueMap<String, HttpEntity<?>> body = new LinkedMultiValueMap<>();
        body.add("file", part);
        return body;
    }

    private static String filenameOrFallback(MultipartFile file) {
        final String name = file.getOriginalFilename();
        return name == null || name.isBlank() ? "upload" : name;
    }

    /** ByteArrayResource that returns a filename so RestClient renders Content-Disposition. */
    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
