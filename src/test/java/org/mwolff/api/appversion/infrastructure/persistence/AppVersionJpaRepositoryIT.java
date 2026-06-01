package org.mwolff.api.appversion.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mwolff.api.AbstractIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Integrationstest der {@code app_version}-Persistenz gegen MariaDB via Testcontainers. Prueft die
 * von Flyway (V14) gesetzte Startversion 0.1 sowie ein Update.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AppVersionJpaRepositoryIT extends AbstractIntegrationTest {

  @Autowired private AppVersionJpaRepository repository;

  @Test
  void shouldLoadFlywaySeededStartVersion() {
    // Given Flyway V14 die Startzeile id=1, major=0, minor=1 angelegt hat

    // When
    final var entity = repository.findById(1L);

    // Then
    assertThat(entity).isPresent();
    assertThat(entity.get().getMajor()).isZero();
    assertThat(entity.get().getMinor()).isEqualTo(1);
    assertThat(entity.get().getCreatedAt()).isNotNull();
    assertThat(entity.get().getUpdatedAt()).isNotNull();
  }

  @Test
  void shouldPersistUpdatedMinor() {
    // Given
    final AppVersionEntity entity = repository.findById(1L).orElseThrow();
    entity.setMinor(7);

    // When
    repository.saveAndFlush(entity);

    // Then
    assertThat(repository.findById(1L).orElseThrow().getMinor()).isEqualTo(7);
  }
}
