package org.mwolff.api.image.domain;

import java.util.List;

/** Eine Seite Bild-Metadaten plus Gesamtanzahl für die Paginierung (#198). */
public record ImagePage(List<ImageMetadata> images, long total) {

  public ImagePage {
    images = List.copyOf(images);
  }
}
