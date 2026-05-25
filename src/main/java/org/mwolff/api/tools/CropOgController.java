package org.mwolff.api.tools;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

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
public class CropOgController {

  private static final int MIN_DIMENSION = 200;
  private static final int MAX_DIMENSION = 4096;
  private static final String DEFAULT_WIDTH = "1200";
  private static final String DEFAULT_HEIGHT = "630";

  private final CropOgService service;
  private final UploadValidator uploadValidator;

  public CropOgController(CropOgService service, UploadValidator uploadValidator) {
    this.service = service;
    this.uploadValidator = uploadValidator;
  }

  @PostMapping(value = "/crop-og", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<byte[]> crop(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "y_offset", defaultValue = "0.5") @DecimalMin("0.0") @DecimalMax("1.0")
          double yOffset,
      @RequestParam(value = "x_offset", defaultValue = "0.5") @DecimalMin("0.0") @DecimalMax("1.0")
          double xOffset,
      @RequestParam(value = "quality", defaultValue = "88") @Min(50) @Max(95) int quality,
      @RequestParam(value = "width", defaultValue = DEFAULT_WIDTH)
          @Min(MIN_DIMENSION)
          @Max(MAX_DIMENSION)
          int width,
      @RequestParam(value = "height", defaultValue = DEFAULT_HEIGHT)
          @Min(MIN_DIMENSION)
          @Max(MAX_DIMENSION)
          int height) {
    uploadValidator.validateImageUpload(file);
    final byte[] result = service.crop(file, yOffset, xOffset, quality, width, height);
    final String filename = "featured-" + width + "x" + height + ".jpg";
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_JPEG)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .body(result);
  }
}
