package org.mwolff.api.image.web;

import java.util.List;

import org.mwolff.api.image.domain.ManagedImagePage;

/** Paginierte Antwort des Image-Managers: Bilder inkl. Verwendungszähler + Gesamtanzahl (#202). */
public record ManagedImageListResponse(List<ManagedImageResponse> images, long total) {

  public static ManagedImageListResponse from(final ManagedImagePage page) {
    return new ManagedImageListResponse(
        page.images().stream().map(ManagedImageResponse::from).toList(), page.total());
  }
}
