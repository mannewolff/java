package org.mwolff.api.kanban.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.validation.constraints.Min;

import org.mwolff.api.kanban.application.DeleteAttachmentUseCase;
import org.mwolff.api.kanban.application.GetAttachmentUseCase;
import org.mwolff.api.kanban.application.ListAttachmentsUseCase;
import org.mwolff.api.kanban.application.UploadAttachmentUseCase;
import org.mwolff.api.kanban.domain.KanbanAttachment;
import org.mwolff.api.kanban.web.dto.KanbanAttachmentResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST-Adapter für Datei-Anhänge an Kanban-Einträgen (Items UND Epics, #350). Geschützt durch
 * {@code SecurityConfig#requestMatchers("/api/kanban/**").hasRole("USER")}; der
 * Item-Eigentums-Check passiert in den Use-Cases über den JWT-{@code sub}. Der Download erzwingt
 * immer {@code Content-Disposition: attachment} — kein Anhang wird je inline gerendert.
 */
@RestController
@RequestMapping("/api/kanban/items/{itemId}/attachments")
@Validated
public class KanbanAttachmentController {

  private final ListAttachmentsUseCase listUseCase;
  private final UploadAttachmentUseCase uploadUseCase;
  private final GetAttachmentUseCase getUseCase;
  private final DeleteAttachmentUseCase deleteUseCase;

  public KanbanAttachmentController(
      ListAttachmentsUseCase listUseCase,
      UploadAttachmentUseCase uploadUseCase,
      GetAttachmentUseCase getUseCase,
      DeleteAttachmentUseCase deleteUseCase) {
    this.listUseCase = listUseCase;
    this.uploadUseCase = uploadUseCase;
    this.getUseCase = getUseCase;
    this.deleteUseCase = deleteUseCase;
  }

  @GetMapping
  public List<KanbanAttachmentResponse> list(
      JwtAuthenticationToken auth, @PathVariable @Min(1) long itemId) {
    return listUseCase.execute(auth.getToken().getSubject(), itemId).stream()
        .map(KanbanAttachmentResponse::from)
        .toList();
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<KanbanAttachmentResponse> upload(
      JwtAuthenticationToken auth,
      @PathVariable @Min(1) long itemId,
      @RequestParam("file") MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("uploaded file is empty");
    }
    final byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (final IOException ex) {
      throw new IllegalArgumentException("could not read uploaded file");
    }
    final KanbanAttachment saved =
        uploadUseCase.execute(
            auth.getToken().getSubject(), itemId, bytes, file.getOriginalFilename());
    return ResponseEntity.status(HttpStatus.CREATED).body(KanbanAttachmentResponse.from(saved));
  }

  @GetMapping("/{attachmentId}")
  public ResponseEntity<byte[]> download(
      JwtAuthenticationToken auth,
      @PathVariable @Min(1) long itemId,
      @PathVariable @Min(1) long attachmentId) {
    final KanbanAttachment attachment =
        getUseCase.execute(auth.getToken().getSubject(), itemId, attachmentId);
    // Immer als Download ausliefern (nie inline) — verhindert Stored-XSS über HTML/SVG-Anhänge.
    final ContentDisposition disposition =
        ContentDisposition.attachment()
            .filename(attachment.filename(), StandardCharsets.UTF_8)
            .build();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .contentType(MediaType.parseMediaType(attachment.contentType()))
        .cacheControl(CacheControl.empty().cachePrivate())
        .body(attachment.data());
  }

  @DeleteMapping("/{attachmentId}")
  public ResponseEntity<Void> delete(
      JwtAuthenticationToken auth,
      @PathVariable @Min(1) long itemId,
      @PathVariable @Min(1) long attachmentId) {
    deleteUseCase.execute(auth.getToken().getSubject(), itemId, attachmentId);
    return ResponseEntity.noContent().build();
  }
}
