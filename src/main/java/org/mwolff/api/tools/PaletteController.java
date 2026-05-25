package org.mwolff.api.tools;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tools")
@Validated
public class PaletteController {

  private final PaletteService service;
  private final UploadValidator uploadValidator;

  public PaletteController(PaletteService service, UploadValidator uploadValidator) {
    this.service = service;
    this.uploadValidator = uploadValidator;
  }

  @PostMapping(
      value = "/palette",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public PaletteResponse palette(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "count", defaultValue = "6") @Min(2) @Max(10) int count) {
    uploadValidator.validateImageUpload(file);
    return service.extractPalette(file, count);
  }
}
