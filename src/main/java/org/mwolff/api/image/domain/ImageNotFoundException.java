package org.mwolff.api.image.domain;

/** Geworfen, wenn kein Bild mit der angefragten id existiert (#182). */
public class ImageNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ImageNotFoundException(final long id) {
    super("Image not found: " + id);
  }
}
