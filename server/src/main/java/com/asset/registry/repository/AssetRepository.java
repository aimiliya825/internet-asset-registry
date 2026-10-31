package com.asset.registry.repository;

import com.asset.registry.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {

    Optional<Asset> findByIdentNorm(String identNorm);

    boolean existsByIdentNorm(String identNorm);

    boolean existsByIdentNormAndIdNot(String identNorm, Long id);

    long countByRunStatus(String runStatus);

    boolean existsByUnitIdAndAssetNameAndTypeCode(Long unitId, String assetName, String typeCode);

    long countByVerifyStatus(String verifyStatus);

    List<Asset> findByVerifyStatusInOrderByVerifyDueDateAsc(List<String> statuses);

    @Query("select count(a) from Asset a where exists (select 1 from AssetAttachment t where t.assetId = a.id)")
    long countWithAttachment();

    @Query("select a.typeCode, count(a) from Asset a " +
           "where (:unitId is null or a.unitId = :unitId) " +
           "and (:from is null or a.onlineDate >= :from) " +
           "and (:to is null or a.onlineDate <= :to) " +
           "group by a.typeCode")
    List<Object[]> groupByType(@Param("unitId") Long unitId,
                               @Param("from") LocalDate from,
                               @Param("to") LocalDate to);

    @Query("select a.unitId, count(a) from Asset a " +
           "where (:unitId is null or a.unitId = :unitId) " +
           "and (:from is null or a.onlineDate >= :from) " +
           "and (:to is null or a.onlineDate <= :to) " +
           "group by a.unitId")
    List<Object[]> groupByUnit(@Param("unitId") Long unitId,
                               @Param("from") LocalDate from,
                               @Param("to") LocalDate to);

    @Query("select a.runStatus, count(a) from Asset a " +
           "where (:unitId is null or a.unitId = :unitId) " +
           "and (:from is null or a.onlineDate >= :from) " +
           "and (:to is null or a.onlineDate <= :to) " +
           "group by a.runStatus")
    List<Object[]> groupByStatus(@Param("unitId") Long unitId,
                                 @Param("from") LocalDate from,
                                 @Param("to") LocalDate to);
}
