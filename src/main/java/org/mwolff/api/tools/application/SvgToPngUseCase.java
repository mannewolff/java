package org.mwolff.api.tools.application;

import org.mwolff.api.tools.domain.PythonToolsPort;
import org.mwolff.api.tools.domain.SvgToPngParams;
import org.mwolff.api.tools.domain.ToolImageResult;
import org.mwolff.api.tools.domain.UploadValidatorPort;
import org.mwolff.api.tools.domain.UploadedImage;
import org.springframework.stereotype.Component;

/**
 * Use-Case: SVG-Upload validieren und an den python-tools-Port zur PNG-Konvertierung weiterreichen.
 */
@Component
public class SvgToPngUseCase {

  private final UploadValidatorPort validator;
  private final PythonToolsPort tools;

  public SvgToPngUseCase(UploadValidatorPort validator, PythonToolsPort tools) {
    this.validator = validator;
    this.tools = tools;
  }

  public ToolImageResult execute(UploadedImage image, SvgToPngParams params) {
    validator.validateSvg(image);
    return tools.convertSvgToPng(image, params);
  }
}
