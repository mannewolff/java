package org.mwolff.api.image.web;

import java.util.List;

import org.mwolff.api.image.application.DeleteImagesUseCase.DeleteResult;

/** Ergebnis des Batch-Löschens: gelöschte Ids + fehlgeschlagene mit Begründung (#202). */
public record BatchDeleteResponse(List<Long> deleted, List<Failure> failed) {

  public record Failure(long id, String reason) {}

  public static BatchDeleteResponse from(final DeleteResult result) {
    return new BatchDeleteResponse(
        result.deleted(),
        result.failed().stream().map(f -> new Failure(f.id(), f.reason())).toList());
  }
}
