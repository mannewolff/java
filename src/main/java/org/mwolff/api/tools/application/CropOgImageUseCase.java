package org.mwolff.api.tools.application;

import org.mwolff.api.tools.domain.CropOgParams;
import org.mwolff.api.tools.domain.PythonToolsPort;
import org.mwolff.api.tools.domain.ToolImageResult;
import org.mwolff.api.tools.domain.UploadValidatorPort;
import org.mwolff.api.tools.domain.UploadedImage;
import org.mwolff.api.tools.domain.ValidatedImage;
import org.springframework.stereotype.Component;

@Component
public class CropOgImageUseCase {

  private final UploadValidatorPort validator;
  private final PythonToolsPort tools;

  public CropOgImageUseCase(UploadValidatorPort validator, PythonToolsPort tools) {
    this.validator = validator;
    this.tools = tools;
  }

  public ToolImageResult execute(UploadedImage image, CropOgParams params) {
    final ValidatedImage validated = validator.validateImage(image);
    return tools.cropOg(validated, params);
  }
}
