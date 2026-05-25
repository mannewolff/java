package org.mwolff.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApiApplicationIT extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void shouldLoadCoreApplicationBeans() {
        // given: a Spring Boot application context wired against a Testcontainers MariaDB

        // when: the integration test boots the full context
        final String[] beans = context.getBeanDefinitionNames();

        // then: the application bean is present and at least one tool-specific bean exists
        assertThat(beans).contains("apiApplication");
        assertThat(context.containsBean("globalExceptionHandler")).isTrue();
    }
}
