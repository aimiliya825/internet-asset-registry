package com.asset.registry.repository;

import com.asset.registry.entity.AssetVerifyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetVerifyRecordRepository extends JpaRepository<AssetVerifyRecord, Long> {
    List<AssetVerifyRecord> findByAssetIdOrderByPeriodDesc(Long assetId);

    Optional<AssetVerifyRecord> findTopByAssetIdOrderByPeriodDesc(Long assetId);

    Optional<AssetVerifyRecord> findByAssetIdAndPeriod(Long assetId, String period);

    List<AssetVerifyRecord> findByResultIsNullOrderBySentAtAsc();
}
