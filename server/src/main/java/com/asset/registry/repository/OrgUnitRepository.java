package com.asset.registry.repository;

import com.asset.registry.entity.OrgUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrgUnitRepository extends JpaRepository<OrgUnit, Long> {
    List<OrgUnit> findAllByEnabledTrueOrderBySortNoAsc();

    Optional<OrgUnit> findByUnitCode(String unitCode);
}
