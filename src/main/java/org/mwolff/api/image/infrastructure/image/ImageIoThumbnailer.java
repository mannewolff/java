package org.mwolff.api.image.infrastructure.image;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

import org.mwolff.api.image.domain.ImageThumbnailer;
import org.mwolff.api.image.domain.InvalidImageUploadException;
import org.springframework.stereotype.Component;

/**
 * JVM-seitige Thumbnail-Skalierung via {@link ImageIO} + {@link Graphics2D} (#200). Bewusst keine
 * Auslagerung an python-tools: die ArchUnit-Cross-Modul-Regel verbietet image→tools. Ausgabe ist
 * stets PNG (verlustfrei, Alpha-fähig, ImageIO-nativ).
 */
@Component
class ImageIoThumbnailer implements ImageThumbnailer {

  @Override
  public byte[] toThumbnailPng(final byte[] source, final int maxEdge) {
    final BufferedImage original = decode(source);
    final int w = original.getWidth();
    final int h = original.getHeight();
    // Kein Upscaling: Faktor maximal 1.0.
    final double scale = Math.min(1.0, (double) maxEdge / Math.max(w, h));
    final int targetW = Math.max(1, (int) Math.round(w * scale));
    final int targetH = Math.max(1, (int) Math.round(h * scale));

    final BufferedImage scaled = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = scaled.createGraphics();
    try {
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.drawImage(original, 0, 0, targetW, targetH, null);
    } finally {
      g.dispose();
    }
    return encodePng(scaled);
  }

  private static BufferedImage decode(final byte[] source) {
    try {
      final BufferedImage image = ImageIO.read(new ByteArrayInputStream(source));
      if (image == null) {
        throw new InvalidImageUploadException("UNDECODABLE", "Image data could not be decoded.");
      }
      return image;
    } catch (final IOException ex) {
      throw new InvalidImageUploadException("UNDECODABLE", "Image data could not be decoded.");
    }
  }

  private static byte[] encodePng(final BufferedImage image) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      ImageIO.write(image, "png", out);
    } catch (final IOException ex) {
      // In-Memory-Stream wirft praktisch nie — defensiv.
      throw new UncheckedIOException(ex);
    }
    return out.toByteArray();
  }
}
