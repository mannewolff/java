package org.mwolff.api.tools.application;

import org.mwolff.api.tools.domain.PythonToolsPort;
import org.mwolff.api.tools.domain.RasterToPngParams;
import org.mwolff.api.tools.domain.ToolImageResult;
import org.mwolff.api.tools.domain.UploadValidatorPort;
import org.mwolff.api.tools.domain.UploadedImage;
import org.mwolff.api.tools.domain.ValidatedImage;
import org.springframework.stereotype.Component;

/**
 * Use-Case: Raster-Upload (JPEG/PNG) validieren und an den python-tools-Port zur PNG-Konvertierung
 * weiterreichen. Verwendet {@link UploadValidatorPort#validateImage}, der PNG, JPEG und WEBP
 * akzeptiert.
 */
@Component
public class RasterToPngUseCase {

  private final UploadValidatorPort validator;
  private final PythonToolsPort tools;

  public RasterToPngUseCase(UploadValidatorPort validator, PythonToolsPort tools) {
    this.validator = validator;
    this.tools = tools;
  }

  public ToolImageResult execute(UploadedImage image, RasterToPngParams params) {
    final ValidatedImage validated = validator.validateImage(image);
    return tools.convertRasterToPng(validated, params);
  }
}
