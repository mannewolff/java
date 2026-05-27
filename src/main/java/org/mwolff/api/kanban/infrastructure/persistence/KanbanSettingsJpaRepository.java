package org.mwolff.api.kanban.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface KanbanSettingsJpaRepository extends JpaRepository<KanbanSettingsEntity, String> {}
