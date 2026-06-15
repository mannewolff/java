package org.mwolff.api.tools.domain;

/**
 * Out-Port zum python-tools-Microservice. Wird im Infrastructure-Layer via {@code RestClient}
 * implementiert. Die Application-Schicht spricht ausschließlich gegen dieses Interface, damit
 * Transportdetails (HTTP, Multipart, Timeouts) nicht in die fachliche Logik leaken.
 *
 * <p>Der Port nimmt ausschließlich {@link ValidatedImage} entgegen — also Bilder, deren MIME-Type
 * bereits per Byte-Signatur erkannt wurde. Dadurch trägt der Adapter den vertrauenswürdigen Typ in
 * den Multipart-Body und niemals den unkontrollierten Client-Wert (#135).
 */
public interface PythonToolsPort {

  ToolImageResult resize(ValidatedImage image, ResizeParams params);

  ToolImageResult cropOg(ValidatedImage image, CropOgParams params);

  ToolImageResult removeBackground(ValidatedImage image);

  PaletteResult extractPalette(ValidatedImage image, PaletteParams params);

  ToolImageResult convertSvgToPng(ValidatedImage image, SvgToPngParams params);

  ToolImageResult convertRasterToPng(ValidatedImage image, RasterToPngParams params);

  /**
   * Konvertiert Markdown-Text zu PDF (#27). Liefert die PDF-Bytes als {@link ToolImageResult}
   * (bytes + content-type {@code application/pdf}); der Result-Typ ist das generische
   * Binär-Ergebnis der Tool-Schicht.
   */
  ToolImageResult convertMarkdownToPdf(String markdown);
}
