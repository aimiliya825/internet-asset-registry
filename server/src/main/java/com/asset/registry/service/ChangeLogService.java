package com.asset.registry.service;

import com.asset.registry.config.CurrentUser;
import com.asset.registry.config.UserContext;
import com.asset.registry.dto.Views;
import com.asset.registry.entity.AssetChangeLog;
import com.asset.registry.repository.AssetChangeLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 变更留痕：所有写操作统一经此落日志（只增不改不删）。
 */
@Service
public class ChangeLogService {

    public static final String TYPE_CREATE = "新增";
    public static final String TYPE_INFO = "信息修改";
    public static final String TYPE_STATUS = "状态变更";
    public static final String TYPE_OWNER = "责任人变更";
    public static final String TYPE_BATCH = "批量更新";
    public static final String TYPE_VERIFY = "核对";

    private final AssetChangeLogRepository repository;

    public ChangeLogService(AssetChangeLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(Long assetId, String assetName, String changeType, String fieldKey, String fieldLabel,
                       String oldValue, String newValue, String reason) {
        record(assetId, assetName, changeType, fieldKey, fieldLabel, oldValue, newValue, reason, null);
    }

    @Transactional
    public void record(Long assetId, String assetName, String changeType, String fieldKey, String fieldLabel,
                       String oldValue, String newValue, String reason, String batchNo) {
        CurrentUser u = UserContext.get();
        AssetChangeLog log = new AssetChangeLog();
        log.setAssetId(assetId);
        log.setAssetName(assetName);
        log.setChangeType(changeType);
        log.setFieldKey(fieldKey);
        log.setFieldLabel(fieldLabel);
        log.setOldValue(trim(oldValue));
        log.setNewValue(trim(newValue));
        log.setReason(reason);
        log.setBatchNo(batchNo);
        log.setOperatorId(u.id());
        log.setOperatorName(u.realName());
        repository.save(log);
    }

    public List<Views.LogItem> timeline(String changeType) {
        List<AssetChangeLog> list = (changeType == null || changeType.isBlank())
                ? repository.findTop200ByOrderByCreatedAtDesc()
                : repository.findByChangeTypeOrderByCreatedAtDesc(changeType);
        return list.stream().map(ChangeLogService::toItem).toList();
    }

    public List<Views.LogItem> ofAsset(Long assetId) {
        return repository.findByAssetIdOrderByCreatedAtDesc(assetId).stream()
                .map(ChangeLogService::toItem).toList();
    }

    public static Views.LogItem toItem(AssetChangeLog l) {
        return new Views.LogItem(l.getId(), l.getAssetId(), l.getAssetName(), l.getChangeType(),
                l.getFieldKey(), l.getFieldLabel(), l.getOldValue(), l.getNewValue(),
                l.getReason(), l.getOperatorName(), l.getCreatedAt());
    }

    private static String trim(String v) {
        if (v == null) {
            return null;
        }
        return v.length() > 480 ? v.substring(0, 480) + "..." : v;
    }
}
