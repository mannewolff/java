package org.mwolff.api.image.web;

import org.mwolff.api.image.domain.StoredImage;

/** Antwort auf einen erfolgreichen Upload: id + relative URL zum Serve-Endpoint (#182). */
public record ImageUploadResponse(long id, String url) {

  public static ImageUploadResponse from(final StoredImage image) {
    return new ImageUploadResponse(image.id(), "/api/images/" + image.id());
  }
}
