package org.mwolff.api.tools.application;

import org.mwolff.api.tools.domain.PythonToolsPort;
import org.mwolff.api.tools.domain.ToolImageResult;
import org.mwolff.api.tools.domain.UploadValidatorPort;
import org.mwolff.api.tools.domain.UploadedImage;
import org.springframework.stereotype.Component;

@Component
public class RemoveBackgroundUseCase {

  private final UploadValidatorPort validator;
  private final PythonToolsPort tools;

  public RemoveBackgroundUseCase(UploadValidatorPort validator, PythonToolsPort tools) {
    this.validator = validator;
    this.tools = tools;
  }

  public ToolImageResult execute(UploadedImage image) {
    validator.validateImage(image);
    return tools.removeBackground(image);
  }
}
