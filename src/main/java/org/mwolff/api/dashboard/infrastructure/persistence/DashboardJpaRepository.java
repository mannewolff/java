package org.mwolff.api.dashboard.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface DashboardJpaRepository extends JpaRepository<DashboardEntity, Long> {

  List<DashboardEntity> findAllByUserSubOrderByCreatedAtAsc(String userSub);

  Optional<DashboardEntity> findFirstByUserSubAndIsDefaultTrue(String userSub);

  @Modifying
  @Query("update DashboardEntity d set d.isDefault = false where d.userSub = :userSub")
  void clearDefaultForUser(@Param("userSub") String userSub);
}
