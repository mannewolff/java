package org.mwolff.api.tools;

import org.springframework.http.MediaType;

public record ResizeResult(byte[] bytes, MediaType contentType) {}
