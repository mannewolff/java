package org.mwolff.api.tools;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "python-tools")
public record PythonToolsProperties(String url) {}
