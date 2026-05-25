package org.mwolff.api.tools;

import java.time.Duration;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the upstream python-tools microservice. Timeouts are validated so a misconfig
 * surfaces at application start instead of producing hung servlet threads at runtime.
 */
@ConfigurationProperties(prefix = "python-tools")
@Validated
public record PythonToolsProperties(
    @NotEmpty String url, @NotNull Duration connectTimeout, @NotNull Duration readTimeout) {

  public PythonToolsProperties {
    if (connectTimeout == null) {
      connectTimeout = Duration.ofSeconds(5);
    }
    if (readTimeout == null) {
      readTimeout = Duration.ofSeconds(30);
    }
  }
}
