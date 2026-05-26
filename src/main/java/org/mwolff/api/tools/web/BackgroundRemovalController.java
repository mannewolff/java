package org.mwolff.api.tools.web;

import org.mwolff.api.tools.application.RemoveBackgroundUseCase;
import org.mwolff.api.tools.domain.ToolImageResult;
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

  private final RemoveBackgroundUseCase useCase;

  public BackgroundRemovalController(RemoveBackgroundUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping(value = "/remove-background", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<byte[]> removeBackground(@RequestParam("file") MultipartFile file) {
    final ToolImageResult result = useCase.execute(UploadedImageMapper.toDomain(file));
    // /remove-bg liefert PNG (transparenter Hintergrund) — Web-Vertrag explizit setzen.
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transparent.png\"")
        .body(result.bytes());
  }
}
