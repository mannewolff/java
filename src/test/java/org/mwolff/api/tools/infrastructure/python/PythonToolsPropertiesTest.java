package org.mwolff.api.tools.infrastructure.python;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class PythonToolsPropertiesTest {

  @Test
  void shouldKeepProvidedTimeoutsAsIs() {
    final Duration connect = Duration.ofSeconds(3);
    final Duration read = Duration.ofSeconds(45);

    final PythonToolsProperties props =
        new PythonToolsProperties("http://x", connect, read, "secret");

    assertThat(props.connectTimeout()).isEqualTo(connect);
    assertThat(props.readTimeout()).isEqualTo(read);
    assertThat(props.url()).isEqualTo("http://x");
    assertThat(props.internalKey()).isEqualTo("secret");
  }

  @Test
  void shouldFallBackToDefaultsWhenTimeoutsAreNull() {
    final PythonToolsProperties props = new PythonToolsProperties("http://x", null, null, null);

    assertThat(props.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
    assertThat(props.readTimeout()).isEqualTo(Duration.ofSeconds(30));
    // internalKey faellt auf Leerstring zurueck (#265).
    assertThat(props.internalKey()).isEmpty();
  }
}
