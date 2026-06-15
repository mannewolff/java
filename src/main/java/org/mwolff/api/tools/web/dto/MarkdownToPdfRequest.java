package org.mwolff.api.tools.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request-Body für {@code POST /api/tools/md-to-pdf} (#27).
 *
 * @param markdown Markdown-Quelltext; nicht leer, max. {@link #MAX_LENGTH} Zeichen (deckungsgleich
 *     mit dem 1-MiB-Limit von python-tools).
 */
public record MarkdownToPdfRequest(
    @NotBlank @Size(max = MarkdownToPdfRequest.MAX_LENGTH) String markdown) {

  public static final int MAX_LENGTH = 1_000_000;
}
