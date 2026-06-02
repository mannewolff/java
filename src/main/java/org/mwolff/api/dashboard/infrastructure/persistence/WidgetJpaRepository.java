package org.mwolff.api.dashboard.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WidgetJpaRepository extends JpaRepository<WidgetEntity, Long> {

  List<WidgetEntity> findAllByDashboardIdOrderByIdAsc(Long dashboardId);

  void deleteByDashboardId(Long dashboardId);

  /**
   * Zählt, wie viele IMAGE-Widgets ein bestimmtes Bild referenzieren (#202). {@code config} ist ein
   * JSON-Text mit dem Feld {@code imageId}; MariaDB extrahiert es per JSON_EXTRACT.
   */
  @Query(
      value =
          "SELECT COUNT(*) FROM widgets WHERE type = 'IMAGE' "
              + "AND CAST(JSON_VALUE(config, '$.imageId') AS UNSIGNED) = :imageId",
      nativeQuery = true)
  long countByImageId(@Param("imageId") long imageId);

  /**
   * Liefert (imageId, Anzahl) für alle in IMAGE-Widgets referenzierten Bilder (#202) — eine
   * Aggregat-Query als Basis für die Manager-Liste.
   */
  @Query(
      value =
          "SELECT CAST(JSON_VALUE(config, '$.imageId') AS UNSIGNED) AS imageId, "
              + "COUNT(*) AS usageCount FROM widgets WHERE type = 'IMAGE' "
              + "AND JSON_VALUE(config, '$.imageId') IS NOT NULL "
              + "GROUP BY CAST(JSON_VALUE(config, '$.imageId') AS UNSIGNED)",
      nativeQuery = true)
  List<ImageUsageRow> aggregateUsage();

  /** Projektionszeile für {@link #aggregateUsage()}. */
  interface ImageUsageRow {
    long getImageId();

    long getUsageCount();
  }
}
