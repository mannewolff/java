package org.mwolff.api.timeseries.infrastructure.persistence;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TimeSeriesEntryJpaRepository extends JpaRepository<TimeSeriesEntryEntity, Long> {

  long countByTimeSeriesId(Long timeSeriesId);

  /**
   * {@code true}, wenn mindestens ein Eintrag einen Nachkommaanteil hat ({@code value !=
   * FLOOR(value)} — vorzeichenunabhängig korrekt). Grundlage der Kompatibilitätsprüfung beim {@code
   * dataType}-Wechsel auf INTEGER.
   */
  @Query(
      "select case when count(e) > 0 then true else false end "
          + "from TimeSeriesEntryEntity e "
          + "where e.timeSeriesId = :timeSeriesId "
          + "and e.value <> function('floor', e.value)")
  boolean existsFractionalValue(@Param("timeSeriesId") Long timeSeriesId);

  /**
   * Listet Eintraege im optionalen Zeitfenster — {@code from} und {@code to} sind {@code null}-bar.
   * Reihenfolge: timestamp DESC (neuester zuerst). Das {@link Pageable} liefert das Limit.
   */
  @Query(
      "select e from TimeSeriesEntryEntity e "
          + "where e.timeSeriesId = :timeSeriesId "
          + "and (:from is null or e.timestamp >= :from) "
          + "and (:to is null or e.timestamp <= :to) "
          + "order by e.timestamp desc")
  List<TimeSeriesEntryEntity> findInRange(
      @Param("timeSeriesId") Long timeSeriesId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      Pageable pageable);
}
