package org.mwolff.api.tools.application;

import org.mwolff.api.tools.domain.PythonToolsPort;
import org.mwolff.api.tools.domain.ResizeParams;
import org.mwolff.api.tools.domain.ToolImageResult;
import org.mwolff.api.tools.domain.UploadValidatorPort;
import org.mwolff.api.tools.domain.UploadedImage;
import org.springframework.stereotype.Component;

/**
 * Use-Case: gültiges Bild prüfen und an den python-tools-Port zur Verkleinerung weiterreichen.
 *
 * <p>Spring-Annotation steht hier, damit der Bean automatisch entdeckt wird — der Use-Case selbst
 * verwendet aber nur Domain-Typen, keine Spring-Web- oder Persistence-Klassen.
 */
@Component
public class ResizeImageUseCase {

  private final UploadValidatorPort validator;
  private final PythonToolsPort tools;

  public ResizeImageUseCase(UploadValidatorPort validator, PythonToolsPort tools) {
    this.validator = validator;
    this.tools = tools;
  }

  public ToolImageResult execute(UploadedImage image, ResizeParams params) {
    validator.validateImage(image);
    return tools.resize(image, params);
  }
}
