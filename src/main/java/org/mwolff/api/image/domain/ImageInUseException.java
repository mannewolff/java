package org.mwolff.api.image.domain;

/** Wird geworfen, wenn ein noch von Widgets referenziertes Bild gelöscht werden soll (#202). */
public class ImageInUseException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ImageInUseException(final long imageId, final long usageCount) {
    super("Image " + imageId + " is still used by " + usageCount + " widget(s).");
  }
}
