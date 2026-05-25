package org.mwolff.api.tools;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tools")
@Validated
public class ResizeController {

  private static final int MIN_DIMENSION = 1;
  private static final int MAX_DIMENSION = 8192;

  private final ResizeService service;

  public ResizeController(ResizeService service) {
    this.service = service;
  }

  @PostMapping(value = "/resize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<byte[]> resize(
      @RequestParam("file") MultipartFile file,
      @RequestParam("width") @Min(MIN_DIMENSION) @Max(MAX_DIMENSION) int width,
      @RequestParam("height") @Min(MIN_DIMENSION) @Max(MAX_DIMENSION) int height,
      @RequestParam(value = "output_format", defaultValue = "auto")
          @Pattern(regexp = "auto|png|jpeg|webp")
          String outputFormat,
      @RequestParam(value = "quality", defaultValue = "90") @Min(50) @Max(95) int quality) {
    final ResizeResult result = service.resize(file, width, height, outputFormat, quality);
    final String filename = "resized-" + width + "x" + height + extensionFor(result.contentType());
    return ResponseEntity.ok()
        .contentType(result.contentType())
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .body(result.bytes());
  }

  private static String extensionFor(MediaType type) {
    if (MediaType.IMAGE_PNG.equalsTypeAndSubtype(type)) {
      return ".png";
    }
    if (MediaType.IMAGE_JPEG.equalsTypeAndSubtype(type)) {
      return ".jpg";
    }
    if ("image".equals(type.getType()) && "webp".equals(type.getSubtype())) {
      return ".webp";
    }
    return ".bin";
  }
}
