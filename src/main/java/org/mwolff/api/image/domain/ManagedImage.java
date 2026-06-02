package org.mwolff.api.image.domain;

/**
 * Bild-Metadaten plus Verwendungszähler für den Image-Manager (#202). {@code usageCount == 0}
 * bedeutet ungenutzt und damit löschbar.
 */
public record ManagedImage(ImageMetadata image, long usageCount) {}
