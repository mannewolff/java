package org.mwolff.api.image.domain;

import java.util.List;

/** Eine Seite verwalteter Bilder (inkl. Verwendungszähler) plus Gesamtanzahl (#202). */
public record ManagedImagePage(List<ManagedImage> images, long total) {

  public ManagedImagePage {
    images = List.copyOf(images);
  }
}
