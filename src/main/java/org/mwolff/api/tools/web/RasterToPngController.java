package org.mwolff.api.tools.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.mwolff.api.tools.application.RasterToPngUseCase;
import org.mwolff.api.tools.domain.RasterToPngParams;
import org.mwolff.api.tools.domain.ToolImageResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST-Adapter für die Raster → PNG-Konvertierung (JPEG/PNG → PNG). Validierung am Rand (Bean
 * Validation), echte Konvertierung im python-tools-Microservice via Pillow.
 */
@RestController
@RequestMapping("/api/tools")
@Validated
public class RasterToPngController {

  private static final int MIN_DIMENSION = 1;
  private static final int MAX_DIMENSION = 8192;

  private final RasterToPngUseCase useCase;

  public RasterToPngController(RasterToPngUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping(value = "/raster-to-png", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<byte[]> rasterToPng(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "width", required = false) @Min(MIN_DIMENSION) @Max(MAX_DIMENSION)
          Integer width,
      @RequestParam(value = "height", required = false) @Min(MIN_DIMENSION) @Max(MAX_DIMENSION)
          Integer height) {
    final ToolImageResult result =
        useCase.execute(UploadedImageMapper.toDomain(file), new RasterToPngParams(width, height));
    final MediaType contentType = MediaType.parseMediaType(result.contentType());
    final String filename = filenameFor(file, width, height);
    return ResponseEntity.ok()
        .contentType(contentType)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .body(result.bytes());
  }

  private static String filenameFor(MultipartFile file, Integer width, Integer height) {
    final String original = file.getOriginalFilename();
    final String base;
    if (!StringUtils.hasText(original)) {
      base = "image";
    } else {
      final int dot = original.lastIndexOf('.');
      base = dot > 0 ? original.substring(0, dot) : original;
    }
    final String size =
        width != null && height != null
            ? "-" + width + "x" + height
            : width != null ? "-w" + width : height != null ? "-h" + height : "";
    return base + size + ".png";
  }
}
