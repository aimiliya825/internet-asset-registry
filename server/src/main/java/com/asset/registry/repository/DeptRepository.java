package com.asset.registry.repository;

import com.asset.registry.entity.Dept;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeptRepository extends JpaRepository<Dept, Long> {
    List<Dept> findByUnitIdAndEnabledTrueOrderBySortNoAsc(Long unitId);

    List<Dept> findAllByOrderByUnitIdAscSortNoAsc();
}
