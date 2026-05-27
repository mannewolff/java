package org.mwolff.api.tools.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mwolff.api.tools.domain.CropOgParams;
import org.mwolff.api.tools.domain.InvalidUploadException;
import org.mwolff.api.tools.domain.PaletteParams;
import org.mwolff.api.tools.domain.PaletteResult;
import org.mwolff.api.tools.domain.PythonToolsPort;
import org.mwolff.api.tools.domain.ResizeParams;
import org.mwolff.api.tools.domain.SvgToPngParams;
import org.mwolff.api.tools.domain.ToolImageResult;
import org.mwolff.api.tools.domain.UploadValidatorPort;
import org.mwolff.api.tools.domain.UploadedImage;

class UseCasesTest {

  private final UploadValidatorPort validator = mock(UploadValidatorPort.class);
  private final PythonToolsPort tools = mock(PythonToolsPort.class);

  private final UploadedImage image = new UploadedImage(new byte[] {1}, "image/png", "x.png");
  private final ToolImageResult okImage = new ToolImageResult(new byte[] {9}, "image/jpeg");

  @Test
  void resizeShouldValidateAndDelegate() {
    final ResizeParams params = new ResizeParams(800, 600, "jpeg", 80);
    given(tools.resize(image, params)).willReturn(okImage);

    final ToolImageResult result = new ResizeImageUseCase(validator, tools).execute(image, params);

    assertThat(result).isSameAs(okImage);
    verify(validator).validateImage(image);
    verify(tools).resize(image, params);
  }

  @Test
  void resizeShouldShortCircuitOnInvalidUpload() {
    final ResizeParams params = new ResizeParams(800, 600, "jpeg", 80);
    willThrow(new InvalidUploadException("EMPTY_FILE", "no bytes"))
        .given(validator)
        .validateImage(image);

    assertThatThrownBy(() -> new ResizeImageUseCase(validator, tools).execute(image, params))
        .isInstanceOf(InvalidUploadException.class);
    verify(tools, never()).resize(any(), any());
  }

  @Test
  void cropOgShouldValidateAndDelegate() {
    final CropOgParams params = new CropOgParams(0.5, 0.5, 88, 1200, 630);
    given(tools.cropOg(image, params)).willReturn(okImage);

    final ToolImageResult result = new CropOgImageUseCase(validator, tools).execute(image, params);

    assertThat(result).isSameAs(okImage);
    verify(validator).validateImage(image);
  }

  @Test
  void cropOgShouldShortCircuitOnInvalidUpload() {
    final CropOgParams params = new CropOgParams(0.5, 0.5, 88, 1200, 630);
    willThrow(new InvalidUploadException("FILE_TOO_LARGE", "huge"))
        .given(validator)
        .validateImage(image);

    assertThatThrownBy(() -> new CropOgImageUseCase(validator, tools).execute(image, params))
        .isInstanceOf(InvalidUploadException.class);
    verify(tools, never()).cropOg(any(), any());
  }

  @Test
  void removeBackgroundShouldValidateAndDelegate() {
    given(tools.removeBackground(image)).willReturn(okImage);

    final ToolImageResult result = new RemoveBackgroundUseCase(validator, tools).execute(image);

    assertThat(result).isSameAs(okImage);
    verify(validator).validateImage(image);
  }

  @Test
  void removeBackgroundShouldShortCircuitOnInvalidUpload() {
    willThrow(new InvalidUploadException("UNSUPPORTED_FORMAT", "x"))
        .given(validator)
        .validateImage(image);

    assertThatThrownBy(() -> new RemoveBackgroundUseCase(validator, tools).execute(image))
        .isInstanceOf(InvalidUploadException.class);
    verify(tools, never()).removeBackground(any());
  }

  @Test
  void extractPaletteShouldValidateAndDelegate() {
    final PaletteParams params = new PaletteParams(6);
    final PaletteResult palette = new PaletteResult(List.of("#aaa", "#bbb"));
    given(tools.extractPalette(image, params)).willReturn(palette);

    final PaletteResult result = new ExtractPaletteUseCase(validator, tools).execute(image, params);

    assertThat(result).isSameAs(palette);
    verify(validator).validateImage(image);
  }

  @Test
  void extractPaletteShouldShortCircuitOnInvalidUpload() {
    final PaletteParams params = new PaletteParams(6);
    willThrow(new InvalidUploadException("READ_FAILED", "io"))
        .given(validator)
        .validateImage(image);

    assertThatThrownBy(() -> new ExtractPaletteUseCase(validator, tools).execute(image, params))
        .isInstanceOf(InvalidUploadException.class);
    verify(tools, never()).extractPalette(any(), any());
  }

  @Test
  void svgToPngShouldValidateAsSvgAndDelegate() {
    final UploadedImage svg = new UploadedImage(new byte[] {1}, "image/svg+xml", "logo.svg");
    final SvgToPngParams params = new SvgToPngParams(128, 128, "transparent");
    final ToolImageResult expected = new ToolImageResult(new byte[] {9}, "image/png");
    given(tools.convertSvgToPng(svg, params)).willReturn(expected);

    final ToolImageResult result = new SvgToPngUseCase(validator, tools).execute(svg, params);

    assertThat(result).isSameAs(expected);
    verify(validator).validateSvg(svg);
  }

  @Test
  void svgToPngShouldShortCircuitOnInvalidUpload() {
    final UploadedImage svg = new UploadedImage(new byte[] {1}, "image/svg+xml", "logo.svg");
    final SvgToPngParams params = new SvgToPngParams(null, null, "transparent");
    willThrow(new InvalidUploadException("UNSUPPORTED_FORMAT", "not svg"))
        .given(validator)
        .validateSvg(svg);

    assertThatThrownBy(() -> new SvgToPngUseCase(validator, tools).execute(svg, params))
        .isInstanceOf(InvalidUploadException.class);
    verify(tools, never()).convertSvgToPng(any(), any());
  }
}
