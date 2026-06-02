package org.mwolff.api.image.web;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import jakarta.validation.Valid;

import org.mwolff.api.image.application.CheckImageHashUseCase;
import org.mwolff.api.image.application.GetImageUseCase;
import org.mwolff.api.image.application.ListImagesUseCase;
import org.mwolff.api.image.application.UploadImageUseCase;
import org.mwolff.api.image.domain.InvalidImageUploadException;
import org.mwolff.api.image.domain.StoredImage;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Upload + Auslieferung gespeicherter Bilder (#182). */
@RestController
@RequestMapping("/api/images")
public class ImageController {

  private final UploadImageUseCase uploadUseCase;
  private final GetImageUseCase getUseCase;
  private final ListImagesUseCase listUseCase;
  private final CheckImageHashUseCase checkHashUseCase;

  public ImageController(
      final UploadImageUseCase uploadUseCase,
      final GetImageUseCase getUseCase,
      final ListImagesUseCase listUseCase,
      final CheckImageHashUseCase checkHashUseCase) {
    this.uploadUseCase = uploadUseCase;
    this.getUseCase = getUseCase;
    this.listUseCase = listUseCase;
    this.checkHashUseCase = checkHashUseCase;
  }

  @GetMapping
  public ImageListResponse list(
      @RequestParam(required = false) final Integer limit,
      @RequestParam(required = false) final Integer offset) {
    return ImageListResponse.from(listUseCase.execute(limit, offset));
  }

  @PostMapping(path = "/check-hash", consumes = MediaType.APPLICATION_JSON_VALUE)
  public CheckHashResponse checkHash(@Valid @RequestBody final CheckHashRequest request) {
    final Optional<Long> existing = checkHashUseCase.execute(request.hash());
    return new CheckHashResponse(existing.isPresent(), existing.orElse(null));
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ImageUploadResponse> upload(
      @RequestParam("file") final MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new InvalidImageUploadException("EMPTY_FILE", "Uploaded file is empty.");
    }
    final byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (final IOException ex) {
      throw new InvalidImageUploadException("READ_FAILED", "Could not read uploaded file.");
    }
    final StoredImage saved = uploadUseCase.execute(file.getContentType(), bytes);
    final ImageUploadResponse response = ImageUploadResponse.from(saved);
    return ResponseEntity.created(URI.create(response.url())).body(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<byte[]> get(@PathVariable final long id) {
    final StoredImage image = getUseCase.execute(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(image.contentType()))
        .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(365)).cachePrivate())
        .body(image.data());
  }
}
