package org.mwolff.api.appversion.infrastructure.persistence;

import org.mwolff.api.appversion.domain.AppVersion;
import org.mwolff.api.appversion.domain.AppVersionPort;
import org.springframework.stereotype.Component;

/**
 * JPA-Adapter fuer {@link AppVersionPort}. Liest und schreibt die einzige Versionszeile ({@code id
 * = 1}).
 */
@Component
class JpaAppVersionAdapter implements AppVersionPort {

  private static final long VERSION_ROW_ID = 1L;

  private final AppVersionJpaRepository repository;

  JpaAppVersionAdapter(final AppVersionJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public AppVersion getCurrent() {
    final AppVersionEntity entity = load();
    return AppVersion.of(entity.getMajor(), entity.getMinor());
  }

  @Override
  public void setVersion(final AppVersion version) {
    final AppVersionEntity entity = load();
    entity.setMajor(version.major());
    entity.setMinor(version.minor());
    repository.save(entity);
  }

  private AppVersionEntity load() {
    return repository
        .findById(VERSION_ROW_ID)
        .orElseThrow(() -> new IllegalStateException("app_version row id=1 missing"));
  }
}
