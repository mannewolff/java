package org.mwolff.api.appversion.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mwolff.api.AbstractIntegrationTest;
import org.mwolff.api.appversion.domain.AppVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/** Integrationstest des {@link JpaAppVersionAdapter} gegen MariaDB via Testcontainers. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAppVersionAdapter.class)
class JpaAppVersionAdapterIT extends AbstractIntegrationTest {

  @Autowired private JpaAppVersionAdapter adapter;

  @Test
  void getCurrentReturnsFlywayStartVersion() {
    assertThat(adapter.getCurrent()).isEqualTo(AppVersion.of(0, 1));
  }

  @Test
  void setVersionPersistsAndIsReadBack() {
    // When
    adapter.setVersion(AppVersion.of(1, 3));

    // Then
    assertThat(adapter.getCurrent()).isEqualTo(AppVersion.of(1, 3));
  }
}
