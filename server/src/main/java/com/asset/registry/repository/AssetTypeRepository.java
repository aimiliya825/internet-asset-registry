package com.asset.registry.repository;

import com.asset.registry.entity.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetTypeRepository extends JpaRepository<AssetType, Long> {
    List<AssetType> findAllByEnabledTrueOrderBySortNoAsc();

    Optional<AssetType> findByTypeCode(String typeCode);
}
