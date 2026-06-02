package org.mwolff.api.image.infrastructure.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.mwolff.api.image.domain.InvalidImageUploadException;

class ImageIoThumbnailerTest {

  private final ImageIoThumbnailer thumbnailer = new ImageIoThumbnailer();

  private static byte[] pngOf(final int w, final int h) throws IOException {
    final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(img, "png", out);
    return out.toByteArray();
  }

  private static BufferedImage decode(final byte[] png) throws IOException {
    return ImageIO.read(new ByteArrayInputStream(png));
  }

  @Test
  void scalesDownPreservingAspectRatio() throws IOException {
    // 400x200 → längere Kante auf 160 → 160x80.
    final byte[] thumb = thumbnailer.toThumbnailPng(pngOf(400, 200), 160);

    final BufferedImage out = decode(thumb);
    assertThat(out.getWidth()).isEqualTo(160);
    assertThat(out.getHeight()).isEqualTo(80);
  }

  @Test
  void scalesPortraitByLongerEdge() throws IOException {
    // 100x400 → längere Kante (Höhe) auf 160 → 40x160.
    final BufferedImage out = decode(thumbnailer.toThumbnailPng(pngOf(100, 400), 160));
    assertThat(out.getWidth()).isEqualTo(40);
    assertThat(out.getHeight()).isEqualTo(160);
  }

  @Test
  void doesNotUpscaleSmallImages() throws IOException {
    // 50x30 < 160 → bleibt 50x30.
    final BufferedImage out = decode(thumbnailer.toThumbnailPng(pngOf(50, 30), 160));
    assertThat(out.getWidth()).isEqualTo(50);
    assertThat(out.getHeight()).isEqualTo(30);
  }

  @Test
  void clampsTargetToAtLeastOnePixel() throws IOException {
    // 400x2 mit maxEdge 16 → Höhe rundet auf <1 → mindestens 1.
    final BufferedImage out = decode(thumbnailer.toThumbnailPng(pngOf(400, 2), 16));
    assertThat(out.getWidth()).isEqualTo(16);
    assertThat(out.getHeight()).isGreaterThanOrEqualTo(1);
  }

  @Test
  void rejectsUndecodableData() {
    assertThatThrownBy(() -> thumbnailer.toThumbnailPng(new byte[] {1, 2, 3, 4}, 160))
        .isInstanceOf(InvalidImageUploadException.class);
  }
}
