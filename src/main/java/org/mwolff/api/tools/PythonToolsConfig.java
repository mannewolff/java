package org.mwolff.api.tools;

import java.net.http.HttpClient;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PythonToolsProperties.class)
public class PythonToolsConfig {

  @Bean
  public RestClient pythonToolsRestClient(
      RestClient.Builder builder, PythonToolsProperties properties) {
    // Force HTTP/1.1 — uvicorn rejects the JDK HttpClient default HTTP/2
    // upgrade attempt ("Unsupported upgrade request"), which can leave the
    // first POST with a malformed body.
    final HttpClient http1Client =
        HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    return builder
        .baseUrl(properties.url())
        .requestFactory(new JdkClientHttpRequestFactory(http1Client))
        .build();
  }
}
