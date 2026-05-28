package org.mwolff.api.timeseries.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.mwolff.api.timeseries.domain.TimeSeries;
import org.mwolff.api.timeseries.domain.TimeSeriesEntry;
import org.mwolff.api.timeseries.domain.TimeSeriesEntryPort;
import org.mwolff.api.timeseries.domain.TimeSeriesPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * JPA-Implementierung von {@link TimeSeriesPort} und {@link TimeSeriesEntryPort}. Beide Ports
 * liegen in einem Adapter, weil sie ueber dieselben JPA-Repos und Mappings laufen — identisch zur
 * Dashboard-Konvention.
 */
@Component
class JpaTimeSeriesAdapter implements TimeSeriesPort, TimeSeriesEntryPort {

  private final TimeSeriesJpaRepository timeSeriesRepo;
  private final TimeSeriesEntryJpaRepository entryRepo;

  JpaTimeSeriesAdapter(
      TimeSeriesJpaRepository timeSeriesRepo, TimeSeriesEntryJpaRepository entryRepo) {
    this.timeSeriesRepo = timeSeriesRepo;
    this.entryRepo = entryRepo;
  }

  // ----- TimeSeriesPort ----------------------------------------------------

  @Override
  public List<TimeSeries> findAllByUser(String userSub) {
    return timeSeriesRepo.findAllByUserSubOrderByCreatedAtAsc(userSub).stream()
        .map(JpaTimeSeriesAdapter::toDomain)
        .toList();
  }

  @Override
  public Optional<TimeSeries> findById(long id) {
    return timeSeriesRepo.findById(id).map(JpaTimeSeriesAdapter::toDomain);
  }

  @Override
  public TimeSeries save(TimeSeries timeSeries) {
    final TimeSeriesEntity entity;
    if (timeSeries.id() == null) {
      entity =
          new TimeSeriesEntity(
              null,
              timeSeries.userSub(),
              timeSeries.name(),
              timeSeries.description(),
              timeSeries.unit(),
              timeSeries.dataType());
    } else {
      entity =
          timeSeriesRepo
              .findById(timeSeries.id())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "TimeSeries " + timeSeries.id() + " disappeared during save"));
      entity.setName(timeSeries.name());
      entity.setDescription(timeSeries.description());
      entity.setUnit(timeSeries.unit());
      entity.setDataType(timeSeries.dataType());
    }
    return toDomain(timeSeriesRepo.save(entity));
  }

  @Override
  public void deleteById(long id) {
    timeSeriesRepo.deleteById(id);
  }

  @Override
  public long countEntries(long timeSeriesId) {
    return entryRepo.countByTimeSeriesId(timeSeriesId);
  }

  // ----- TimeSeriesEntryPort ----------------------------------------------

  @Override
  public List<TimeSeriesEntry> findByTimeSeries(
      long timeSeriesId, Optional<Instant> from, Optional<Instant> to, int limit) {
    return entryRepo
        .findInRange(timeSeriesId, from.orElse(null), to.orElse(null), PageRequest.of(0, limit))
        .stream()
        .map(JpaTimeSeriesAdapter::toDomain)
        .toList();
  }

  @Override
  public TimeSeriesEntry save(TimeSeriesEntry entry) {
    final TimeSeriesEntryEntity entity =
        new TimeSeriesEntryEntity(entry.timeSeriesId(), entry.timestamp(), entry.value());
    return toDomain(entryRepo.save(entity));
  }

  @Override
  public List<TimeSeriesEntry> saveAll(List<TimeSeriesEntry> newEntries) {
    final List<TimeSeriesEntryEntity> entities =
        newEntries.stream()
            .map(e -> new TimeSeriesEntryEntity(e.timeSeriesId(), e.timestamp(), e.value()))
            .toList();
    return entryRepo.saveAll(entities).stream().map(JpaTimeSeriesAdapter::toDomain).toList();
  }

  // ----- Mapping -----------------------------------------------------------

  private static TimeSeries toDomain(TimeSeriesEntity entity) {
    return new TimeSeries(
        entity.getId(),
        entity.getUserSub(),
        entity.getName(),
        entity.getDescription(),
        entity.getUnit(),
        entity.getDataType(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  private static TimeSeriesEntry toDomain(TimeSeriesEntryEntity entity) {
    return new TimeSeriesEntry(
        entity.getId(), entity.getTimeSeriesId(), entity.getTimestamp(), entity.getValue());
  }
}
