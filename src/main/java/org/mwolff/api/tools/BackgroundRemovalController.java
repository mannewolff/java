package org.mwolff.api.tools;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tools")
public class BackgroundRemovalController {

  private final BackgroundRemovalService service;
  private final UploadValidator uploadValidator;

  public BackgroundRemovalController(
      BackgroundRemovalService service, UploadValidator uploadValidator) {
    this.service = service;
    this.uploadValidator = uploadValidator;
  }

  @PostMapping(value = "/remove-background", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<byte[]> removeBackground(@RequestParam("file") MultipartFile file) {
    uploadValidator.validateImageUpload(file);
    final byte[] result = service.removeBackground(file);
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transparent.png\"")
        .body(result);
  }
}
