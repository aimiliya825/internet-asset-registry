package com.asset.registry.repository;

import com.asset.registry.entity.AssetAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetAttachmentRepository extends JpaRepository<AssetAttachment, Long> {
    List<AssetAttachment> findByAssetIdOrderByUploadedAtDesc(Long assetId);

    long countByAssetId(Long assetId);
}
