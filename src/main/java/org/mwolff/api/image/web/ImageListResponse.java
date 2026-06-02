package org.mwolff.api.image.web;

import java.util.List;

import org.mwolff.api.image.domain.ImagePage;

/** Paginierte Galerie-Antwort: Metadaten-Liste + Gesamtanzahl (#198). */
public record ImageListResponse(List<ImageMetadataResponse> images, long total) {

  public static ImageListResponse from(final ImagePage page) {
    return new ImageListResponse(
        page.images().stream().map(ImageMetadataResponse::from).toList(), page.total());
  }
}
