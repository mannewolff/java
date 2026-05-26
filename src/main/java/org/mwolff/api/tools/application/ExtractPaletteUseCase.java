package org.mwolff.api.tools.application;

import org.mwolff.api.tools.domain.PaletteParams;
import org.mwolff.api.tools.domain.PaletteResult;
import org.mwolff.api.tools.domain.PythonToolsPort;
import org.mwolff.api.tools.domain.UploadValidatorPort;
import org.mwolff.api.tools.domain.UploadedImage;
import org.springframework.stereotype.Component;

@Component
public class ExtractPaletteUseCase {

  private final UploadValidatorPort validator;
  private final PythonToolsPort tools;

  public ExtractPaletteUseCase(UploadValidatorPort validator, PythonToolsPort tools) {
    this.validator = validator;
    this.tools = tools;
  }

  public PaletteResult execute(UploadedImage image, PaletteParams params) {
    validator.validateImage(image);
    return tools.extractPalette(image, params);
  }
}
