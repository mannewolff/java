package org.mwolff.api.tools;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PythonToolsProperties.class)
public class PythonToolsConfig {

    @Bean
    public RestClient pythonToolsRestClient(RestClient.Builder builder, PythonToolsProperties properties) {
        return builder.baseUrl(properties.url()).build();
    }
}
