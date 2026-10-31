package com.asset.registry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 请求体集合。所有写操作都必须携带变更原因（状态/批量），以满足审计追溯要求。
 */
public final class Payloads {

    private Payloads() {
    }

    public record AssetCreate(
            @NotBlank(message = "资产名称不能为空") @Size(max = 80, message = "资产名称不超过 80 字") String assetName,
            @NotBlank(message = "资产类型不能为空") String typeCode,
            @NotBlank(message = "资产标识不能为空") @Size(max = 120, message = "资产标识不超过 120 字") String assetIdent,
            @Size(max = 40) String beianNo,
            @Size(max = 24) String ipSegment,
            Map<String, Object> extra,
            @NotNull(message = "归属单位不能为空") Long unitId,
            Long deptId,
            @NotBlank(message = "责任人不能为空") String ownerName,
            @NotBlank(message = "联系方式不能为空") String ownerPhone,
            @NotBlank(message = "部署位置不能为空") String deployLocation,
            @NotNull(message = "上线时间不能为空") @PastOrPresent(message = "上线时间不能晚于今天") LocalDate onlineDate,
            @Size(max = 500, message = "备注不超过 500 字") String remark) {
    }

    public record AssetUpdate(
            @Size(max = 80) String assetName,
            @Size(max = 40) String beianNo,
            @Size(max = 24) String ipSegment,
            Map<String, Object> extra,
            Long unitId,
            Long deptId,
            @Size(max = 30) String ownerName,
            @Size(max = 20) String ownerPhone,
            @Size(max = 120) String deployLocation,
            LocalDate onlineDate,
            @Size(max = 500) String remark,
            @Size(max = 500, message = "变更原因不超过 500 字") String reason) {
    }

    public record StatusChange(
            @NotBlank(message = "目标状态不能为空") String status,
            @NotBlank(message = "变更原因不能为空") @Size(min = 10, max = 500, message = "变更原因不少于 10 个字") String reason) {
    }

    public record BatchStatus(
            @NotEmpty(message = "请先选择资产") List<Long> ids,
            @NotBlank(message = "目标状态不能为空") String status,
            @NotBlank(message = "变更原因不能为空") @Size(min = 5, max = 500, message = "变更原因不少于 5 个字") String reason) {
    }

    public record BatchField(
            @NotEmpty(message = "请先选择资产") List<Long> ids,
            @NotBlank(message = "修改字段不能为空") String field,
            @NotBlank(message = "修改值不能为空") String value,
            @NotBlank(message = "变更原因不能为空") @Size(min = 5, max = 500, message = "变更原因不少于 5 个字") String reason) {
    }

    public record Verify(
            @NotBlank(message = "核对结论不能为空") String result,
            @Size(max = 300) String remark) {
    }

    public record TransferApply(
            @NotNull(message = "接收人不能为空") Long toUserId,
            @NotBlank(message = "变更原因不能为空") @Size(min = 5, max = 300) String reason) {
    }

    public record TransferDecision(
            @NotBlank(message = "处理意见不能为空") String decision,
            @Size(max = 300) String opinion) {
    }

    /** 组合查询条件 */
    public record AssetQuery(String keyword, String typeCode, Long unitId, String status,
                             LocalDate from, LocalDate to, String owner) {
    }
}
