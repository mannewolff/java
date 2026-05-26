package org.mwolff.api.tools.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ParamsTest {

  @Test
  void resizeParamsShouldExposeFieldsWhenValid() {
    final ResizeParams params = new ResizeParams(800, 600, "jpeg", 80);
    assertThat(params.width()).isEqualTo(800);
    assertThat(params.height()).isEqualTo(600);
    assertThat(params.outputFormat()).isEqualTo("jpeg");
    assertThat(params.quality()).isEqualTo(80);
  }

  @Test
  void resizeParamsShouldRejectZeroWidth() {
    assertThatThrownBy(() -> new ResizeParams(0, 600, "jpeg", 80))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("width");
  }

  @Test
  void resizeParamsShouldRejectZeroHeight() {
    assertThatThrownBy(() -> new ResizeParams(800, 0, "jpeg", 80))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("height");
  }

  @Test
  void resizeParamsShouldRejectNullOutputFormat() {
    assertThatThrownBy(() -> new ResizeParams(800, 600, null, 80))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void resizeParamsShouldRejectBlankOutputFormat() {
    assertThatThrownBy(() -> new ResizeParams(800, 600, "  ", 80))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("blank");
  }

  @Test
  void resizeParamsShouldRejectQualityOutOfRange() {
    assertThatThrownBy(() -> new ResizeParams(800, 600, "jpeg", 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ResizeParams(800, 600, "jpeg", 101))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void cropOgParamsShouldRejectInvalidOffsets() {
    assertThatThrownBy(() -> new CropOgParams(-0.1, 0.5, 80, 1200, 630))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("yOffset");
    assertThatThrownBy(() -> new CropOgParams(0.5, 1.5, 80, 1200, 630))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("xOffset");
  }

  @Test
  void cropOgParamsShouldRejectInvalidQualityOrDimensions() {
    assertThatThrownBy(() -> new CropOgParams(0.5, 0.5, 0, 1200, 630))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("quality");
    assertThatThrownBy(() -> new CropOgParams(0.5, 0.5, 80, 0, 630))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("width");
    assertThatThrownBy(() -> new CropOgParams(0.5, 0.5, 80, 1200, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("height");
  }

  @Test
  void cropOgParamsShouldExposeFieldsWhenValid() {
    final CropOgParams params = new CropOgParams(0.3, 0.7, 88, 1200, 630);
    assertThat(params.yOffset()).isEqualTo(0.3);
    assertThat(params.xOffset()).isEqualTo(0.7);
    assertThat(params.quality()).isEqualTo(88);
    assertThat(params.width()).isEqualTo(1200);
    assertThat(params.height()).isEqualTo(630);
  }

  @Test
  void paletteParamsShouldRejectZeroCount() {
    assertThatThrownBy(() -> new PaletteParams(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("count");
  }

  @Test
  void paletteParamsShouldExposeCount() {
    assertThat(new PaletteParams(6).count()).isEqualTo(6);
  }
}
