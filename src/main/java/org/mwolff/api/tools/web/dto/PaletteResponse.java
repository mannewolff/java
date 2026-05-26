package org.mwolff.api.tools.web.dto;

import java.util.List;

import org.mwolff.api.tools.domain.PaletteResult;

/** HTTP-Antwort des Palette-Endpoints. */
public record PaletteResponse(List<String> colors) {

  public static PaletteResponse from(PaletteResult result) {
    return new PaletteResponse(result.colors());
  }
}
