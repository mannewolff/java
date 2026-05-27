package org.mwolff.api.tools.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import org.mwolff.api.tools.application.SvgToPngUseCase;
import org.mwolff.api.tools.domain.SvgToPngParams;
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
 * REST-Adapter für die SVG → PNG-Konvertierung. Validierung am Rand (Bean Validation), echte
 * Konvertierung im python-tools-Microservice.
 */
@RestController
@RequestMapping("/api/tools")
@Validated
public class SvgToPngController {

  private static final int MIN_DIMENSION = 1;
  private static final int MAX_DIMENSION = 8192;
  private static final String BACKGROUND_PATTERN = "^(transparent|#[0-9a-fA-F]{6})$";

  private final SvgToPngUseCase useCase;

  public SvgToPngController(SvgToPngUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping(value = "/svg-to-png", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<byte[]> svgToPng(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "width", required = false) @Min(MIN_DIMENSION) @Max(MAX_DIMENSION)
          Integer width,
      @RequestParam(value = "height", required = false) @Min(MIN_DIMENSION) @Max(MAX_DIMENSION)
          Integer height,
      @RequestParam(value = "background", defaultValue = SvgToPngParams.TRANSPARENT)
          @Pattern(regexp = BACKGROUND_PATTERN)
          String background) {
    final ToolImageResult result =
        useCase.execute(
            UploadedImageMapper.toDomain(file), new SvgToPngParams(width, height, background));
    final MediaType contentType = MediaType.parseMediaType(result.contentType());
    final String filename = filenameFor(file, width, height);
    return ResponseEntity.ok()
        .contentType(contentType)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .body(result.bytes());
  }

  private static String filenameFor(MultipartFile file, Integer width, Integer height) {
    // StringUtils.hasText fasst null/empty/blank in einem einzigen Branch zusammen — JaCoCo
    // wuerde sonst auf der `|| original.isBlank()`-Kette einen unbedeckten Branch melden,
    // weil Spring's MockMultipartFile-Parsing in MockMvc-Tests den null-Pfad nicht zuverlaessig
    // durchreicht.
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
