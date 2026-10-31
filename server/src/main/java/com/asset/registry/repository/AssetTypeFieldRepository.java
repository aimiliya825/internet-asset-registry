package com.asset.registry.repository;

import com.asset.registry.entity.AssetTypeField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetTypeFieldRepository extends JpaRepository<AssetTypeField, Long> {
    List<AssetTypeField> findByTypeCodeOrderBySortNoAsc(String typeCode);

    List<AssetTypeField> findAllByOrderByTypeCodeAscSortNoAsc();
}
