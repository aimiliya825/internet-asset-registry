package com.asset.registry.repository;

import com.asset.registry.entity.AssetChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetChangeLogRepository extends JpaRepository<AssetChangeLog, Long> {
    List<AssetChangeLog> findByAssetIdOrderByCreatedAtDesc(Long assetId);

    List<AssetChangeLog> findTop200ByOrderByCreatedAtDesc();

    List<AssetChangeLog> findByChangeTypeOrderByCreatedAtDesc(String changeType);
}
