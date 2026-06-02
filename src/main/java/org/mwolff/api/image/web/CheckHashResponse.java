package org.mwolff.api.image.web;

/** Ergebnis der Duplikat-Erkennung: ob ein Bild existiert und ggf. dessen id (#199). */
public record CheckHashResponse(boolean exists, Long id) {}
