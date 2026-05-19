package org.mwolff.api.book.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 255) String author,
        @Size(max = 32) String isbn
) {
}
