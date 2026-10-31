package com.asset.registry.repository;

import com.asset.registry.entity.OwnerTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OwnerTransferRepository extends JpaRepository<OwnerTransfer, Long> {
    List<OwnerTransfer> findByStatusOrderByCreatedAtDesc(String status);

    List<OwnerTransfer> findAllByOrderByCreatedAtDesc();
}
