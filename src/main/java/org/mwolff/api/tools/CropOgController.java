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

    private final CropOgService service;

    public CropOgController(CropOgService service) {
        this.service = service;
    }

    @PostMapping(value = "/crop-og", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> crop(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "y_offset", defaultValue = "0.5")
            @DecimalMin("0.0") @DecimalMax("1.0") double yOffset,
            @RequestParam(value = "quality", defaultValue = "88")
            @Min(50) @Max(95) int quality) {
        final byte[] result = service.crop(file, yOffset, quality);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"featured.jpg\"")
                .body(result);
    }
}
