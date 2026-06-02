package org.mwolff.api.image.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Anfrage an die Duplikat-Erkennung: erwartet einen 64-stelligen SHA-256-Hex-Hash (#199). */
public record CheckHashRequest(
    @NotBlank @Pattern(regexp = "[0-9a-fA-F]{64}", message = "must be a 64-char hex SHA-256")
        String hash) {}
