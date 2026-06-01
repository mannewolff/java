package org.mwolff.api.image.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface StoredImageJpaRepository extends JpaRepository<StoredImageEntity, Long> {}
