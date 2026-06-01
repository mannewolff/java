package org.mwolff.api.appversion.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring-Data-Repository fuer {@link AppVersionEntity}. Die einzige Zeile hat {@code id = 1}. */
interface AppVersionJpaRepository extends JpaRepository<AppVersionEntity, Long> {}
