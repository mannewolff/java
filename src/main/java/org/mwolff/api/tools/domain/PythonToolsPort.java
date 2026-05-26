package org.mwolff.api.tools.domain;

/**
 * Out-Port zum python-tools-Microservice. Wird im Infrastructure-Layer via {@code RestClient}
 * implementiert. Die Application-Schicht spricht ausschließlich gegen dieses Interface, damit
 * Transportdetails (HTTP, Multipart, Timeouts) nicht in die fachliche Logik leaken.
 */
public interface PythonToolsPort {

  ToolImageResult resize(UploadedImage image, ResizeParams params);

  ToolImageResult cropOg(UploadedImage image, CropOgParams params);

  ToolImageResult removeBackground(UploadedImage image);

  PaletteResult extractPalette(UploadedImage image, PaletteParams params);
}
