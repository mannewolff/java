package org.mwolff.api.timeseries.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface TimeSeriesJpaRepository extends JpaRepository<TimeSeriesEntity, Long> {

  List<TimeSeriesEntity> findAllByUserSubOrderByCreatedAtAsc(String userSub);
}
