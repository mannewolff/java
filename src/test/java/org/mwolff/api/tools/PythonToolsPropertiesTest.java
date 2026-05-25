package org.mwolff.api.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class PythonToolsPropertiesTest {

  @Test
  void shouldKeepProvidedTimeoutsAsIs() {
    // Given explicit timeouts from configuration
    final Duration connect = Duration.ofSeconds(3);
    final Duration read = Duration.ofSeconds(45);

    // When
    final PythonToolsProperties props = new PythonToolsProperties("http://x", connect, read);

    // Then
    assertThat(props.connectTimeout()).isEqualTo(connect);
    assertThat(props.readTimeout()).isEqualTo(read);
    assertThat(props.url()).isEqualTo("http://x");
  }

  @Test
  void shouldFallBackToDefaultsWhenTimeoutsAreNull() {
    // Given a properties record built without timeouts (configuration omitted)
    final PythonToolsProperties props = new PythonToolsProperties("http://x", null, null);

    // Then defaults from the compact constructor kick in
    assertThat(props.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
    assertThat(props.readTimeout()).isEqualTo(Duration.ofSeconds(30));
  }
}
