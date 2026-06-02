package org.mwolff.api.image.web;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/** Anfrage zum Batch-Löschen: nicht-leere Liste von Bild-Ids (#202). */
public record BatchDeleteRequest(@NotEmpty List<Long> ids) {}
