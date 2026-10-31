package com.asset.registry.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 出参集合：字段命名与 UI 原型一一对应，便于前端直接绑定。
 */
public final class Views {

    private Views() {
    }

    public record FieldMeta(String key, String label, String type, boolean required, boolean unique,
                           List<String> options, String placeholder) {
    }

    public record TypeMeta(String code, String name, String color, String evidenceHint, String icon,
                          List<FieldMeta> fields) {
    }

    public record DeptMeta(Long id, Long unitId, String name) {
    }

    public record UnitMeta(Long id, String code, String name, String shortName, List<DeptMeta> depts) {
    }

    public record UserMeta(Long id, String realName, String phone, String roleCode, Long unitId, String unitName) {
    }

    public record StatusMeta(String code, String label) {
    }

    public record Meta(List<TypeMeta> types, List<UnitMeta> units, List<StatusMeta> statuses, List<UserMeta> users) {
    }

    public record AssetItem(
            Long id, String assetNo, String assetName, String typeCode, String typeName, String typeColor,
            String assetIdent, String beianNo, String ipSegment, Long unitId, String unitName,
            Long deptId, String deptName, Long ownerId, String ownerName, String ownerPhone,
            String deployLocation, LocalDate onlineDate, String runStatus, String runStatusLabel,
            String verifyStatus, LocalDate verifyDueDate, String remark, LocalDateTime updatedAt) {
    }

    public record AttachmentItem(Long id, String materialType, String fileName, String fileExt, Long fileSize,
                                String uploadedBy, LocalDateTime uploadedAt) {
    }

    public record LogItem(Long id, Long assetId, String assetName, String changeType, String fieldKey,
                          String fieldLabel, String oldValue, String newValue, String reason,
                          String operatorName, LocalDateTime createdAt) {
    }

    public record AssetDetail(AssetItem asset, Map<String, Object> extra,
                              List<AttachmentItem> attachments, List<LogItem> logs,
                              String lastVerifyPeriod) {
    }

    public record SeriesItem(String key, String label, Long value, String color) {
    }

    public record Stats(String dim, Long total, List<SeriesItem> series, Long unitCount,
                        Long typeCount, Double completeRate) {
    }

    public record TodoItem(String level, String title, String desc, String action, String target) {
    }

    public record Dashboard(Long total, Long active, Long due, Long dead, Double activeRate,
                            List<SeriesItem> byType, List<SeriesItem> byUnit, List<SeriesItem> byStatus,
                            List<LogItem> recentLogs, List<TodoItem> todos) {
    }

    public record TransferItem(Long id, Long assetId, String assetName, String fromName, String toName,
                              String applyReason, String status, LocalDateTime createdAt) {
    }
}
