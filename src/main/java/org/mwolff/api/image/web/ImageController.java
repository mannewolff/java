package org.mwolff.api.image.web;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import jakarta.validation.Valid;

import org.mwolff.api.image.application.CheckImageHashUseCase;
import org.mwolff.api.image.application.DeleteImagesUseCase;
import org.mwolff.api.image.application.GetImageThumbnailUseCase;
import org.mwolff.api.image.application.GetImageUseCase;
import org.mwolff.api.image.application.ListImagesUseCase;
import org.mwolff.api.image.application.ListManagedImagesUseCase;
import org.mwolff.api.image.application.UploadImageUseCase;
import org.mwolff.api.image.domain.InvalidImageUploadException;
import org.mwolff.api.image.domain.StoredImage;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Upload + Auslieferung gespeicherter Bilder (#182). Alle Endpoints sind durch {@code
 * SecurityConfig#requestMatchers("/api/images/**").hasRole("USER")} geschützt; der Owner-Check
 * passiert in den Use-Cases — der Controller leitet nur den {@code sub} aus dem JWT weiter (#230).
 */
@RestController
@RequestMapping("/api/images")
public class ImageController {

  private static String sub(final JwtAuthenticationToken auth) {
    return auth.getToken().getSubject();
  }

  private final UploadImageUseCase uploadUseCase;
  private final GetImageUseCase getUseCase;
  private final ListImagesUseCase listUseCase;
  private final CheckImageHashUseCase checkHashUseCase;
  private final GetImageThumbnailUseCase thumbnailUseCase;
  private final ListManagedImagesUseCase listManagedUseCase;
  private final DeleteImagesUseCase deleteUseCase;

  public ImageController(
      final UploadImageUseCase uploadUseCase,
      final GetImageUseCase getUseCase,
      final ListImagesUseCase listUseCase,
      final CheckImageHashUseCase checkHashUseCase,
      final GetImageThumbnailUseCase thumbnailUseCase,
      final ListManagedImagesUseCase listManagedUseCase,
      final DeleteImagesUseCase deleteUseCase) {
    this.uploadUseCase = uploadUseCase;
    this.getUseCase = getUseCase;
    this.listUseCase = listUseCase;
    this.checkHashUseCase = checkHashUseCase;
    this.thumbnailUseCase = thumbnailUseCase;
    this.listManagedUseCase = listManagedUseCase;
    this.deleteUseCase = deleteUseCase;
  }

  @GetMapping
  public ImageListResponse list(
      final JwtAuthenticationToken auth,
      @RequestParam(required = false) final Integer limit,
      @RequestParam(required = false) final Integer offset) {
    return ImageListResponse.from(listUseCase.execute(sub(auth), limit, offset));
  }

  @GetMapping("/manage")
  public ManagedImageListResponse listManaged(
      final JwtAuthenticationToken auth,
      @RequestParam(required = false) final Integer limit,
      @RequestParam(required = false) final Integer offset) {
    return ManagedImageListResponse.from(listManagedUseCase.execute(sub(auth), limit, offset));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
      final JwtAuthenticationToken auth, @PathVariable final long id) {
    deleteUseCase.deleteOne(sub(auth), id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping(path = "/batch-delete", consumes = MediaType.APPLICATION_JSON_VALUE)
  public BatchDeleteResponse batchDelete(
      final JwtAuthenticationToken auth, @Valid @RequestBody final BatchDeleteRequest request) {
    return BatchDeleteResponse.from(deleteUseCase.deleteBatch(sub(auth), request.ids()));
  }

  @PostMapping(path = "/check-hash", consumes = MediaType.APPLICATION_JSON_VALUE)
  public CheckHashResponse checkHash(
      final JwtAuthenticationToken auth, @Valid @RequestBody final CheckHashRequest request) {
    final Optional<Long> existing = checkHashUseCase.execute(sub(auth), request.hash());
    return new CheckHashResponse(existing.isPresent(), existing.orElse(null));
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ImageUploadResponse> upload(
      final JwtAuthenticationToken auth, @RequestParam("file") final MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new InvalidImageUploadException("EMPTY_FILE", "Uploaded file is empty.");
    }
    final byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (final IOException ex) {
      throw new InvalidImageUploadException("READ_FAILED", "Could not read uploaded file.");
    }
    // file.getOriginalFilename() dient Tika nur als Hint; der MIME-Typ wird in der UseCase aus den
    // Bytes detektiert (#231), der client-gemeldete Content-Type wird ignoriert.
    final StoredImage saved = uploadUseCase.execute(sub(auth), bytes, file.getOriginalFilename());
    final ImageUploadResponse response = ImageUploadResponse.from(saved);
    return ResponseEntity.created(URI.create(response.url())).body(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<byte[]> get(
      final JwtAuthenticationToken auth, @PathVariable final long id) {
    final StoredImage image = getUseCase.execute(sub(auth), id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(image.contentType()))
        .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(365)).cachePrivate())
        .body(image.data());
  }

  @GetMapping("/{id}/thumbnail")
  public ResponseEntity<byte[]> thumbnail(
      final JwtAuthenticationToken auth,
      @PathVariable final long id,
      @RequestParam(required = false) final Integer size) {
    final byte[] png = thumbnailUseCase.execute(sub(auth), id, size);
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(365)).cachePrivate())
        .body(png);
  }
}
