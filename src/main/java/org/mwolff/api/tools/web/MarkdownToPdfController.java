package org.mwolff.api.tools.web;

import jakarta.validation.Valid;

import org.mwolff.api.tools.application.MarkdownToPdfUseCase;
import org.mwolff.api.tools.domain.ToolImageResult;
import org.mwolff.api.tools.web.dto.MarkdownToPdfRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Adapter für die Markdown → PDF-Konvertierung (#27). Validierung am Rand (Bean Validation),
 * echte Konvertierung im python-tools-Microservice (weasyprint).
 */
@RestController
@RequestMapping("/api/tools")
public class MarkdownToPdfController {

  private final MarkdownToPdfUseCase useCase;

  public MarkdownToPdfController(MarkdownToPdfUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping(value = "/md-to-pdf", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<byte[]> mdToPdf(@Valid @RequestBody MarkdownToPdfRequest request) {
    final ToolImageResult result = useCase.execute(request.markdown());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(result.contentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document.pdf\"")
        .body(result.bytes());
  }
}
