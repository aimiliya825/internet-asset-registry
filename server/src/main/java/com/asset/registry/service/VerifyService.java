package com.asset.registry.service;

import com.asset.registry.common.BizException;
import com.asset.registry.config.CurrentUser;
import com.asset.registry.config.UserContext;
import com.asset.registry.dto.Payloads;
import com.asset.registry.dto.Views;
import com.asset.registry.entity.Asset;
import com.asset.registry.entity.AssetVerifyRecord;
import com.asset.registry.repository.AssetRepository;
import com.asset.registry.repository.AssetVerifyRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定期核对：解决"僵尸资产"问题，核对结果写入核对记录并刷新下次到期日。
 */
@Service
public class VerifyService {

    private final AssetRepository assetRepository;
    private final AssetVerifyRecordRepository recordRepository;
    private final MetaService metaService;
    private final ChangeLogService changeLogService;

    public VerifyService(AssetRepository assetRepository, AssetVerifyRecordRepository recordRepository,
                         MetaService metaService, ChangeLogService changeLogService) {
        this.assetRepository = assetRepository;
        this.recordRepository = recordRepository;
        this.metaService = metaService;
        this.changeLogService = changeLogService;
    }

    public List<Views.AssetItem> dueList() {
        return assetRepository.findByVerifyStatusInOrderByVerifyDueDateAsc(List.of("due", "overdue")).stream()
                .map(a -> AssetService.toItem(a, metaService.typeIndex(), metaService.unitIndex()))
                .toList();
    }

    @Transactional
    public Views.AssetItem confirm(Long assetId, Payloads.Verify req) {
        Asset a = assetRepository.findById(assetId)
                .orElseThrow(() -> BizException.notFound("资产不存在（id=" + assetId + "）"));
        String result = req.result().trim();
        if (!List.of("ok", "need_fix", "invalid").contains(result)) {
            throw BizException.badRequest("核对结论仅支持 ok / need_fix / invalid");
        }
        String period = currentPeriod();
        CurrentUser u = UserContext.get();

        AssetVerifyRecord record = recordRepository.findByAssetIdAndPeriod(assetId, period)
                .orElseGet(() -> {
                    AssetVerifyRecord r = new AssetVerifyRecord();
                    r.setAssetId(assetId);
                    r.setPeriod(period);
                    return r;
                });
        record.setRespondentId(u.id());
        record.setRespondentName(u.realName());
        record.setRespondedAt(LocalDateTime.now());
        record.setResult(result);
        record.setRemark(req.remark());
        recordRepository.save(record);

        if ("ok".equals(result)) {
            a.setVerifyStatus("ok");
            a.setVerifyDueDate(LocalDate.now().plusMonths(3));
            assetRepository.save(a);
        } else if ("invalid".equals(result)) {
            a.setVerifyStatus("overdue");
            assetRepository.save(a);
        }

        String label = switch (result) {
            case "ok" -> "信息与实际情况一致";
            case "need_fix" -> "信息需修改";
            default -> "资产实际已失效";
        };
        changeLogService.record(a.getId(), a.getAssetName(), ChangeLogService.TYPE_VERIFY,
                "verifyStatus", "核对结论", a.getVerifyStatus(), label,
                req.remark() == null ? label : req.remark(), null);

        return AssetService.toItem(a, metaService.typeIndex(), metaService.unitIndex());
    }

    public static String currentPeriod() {
        LocalDate now = LocalDate.now();
        int quarter = (now.getMonthValue() - 1) / 3 + 1;
        return now.getYear() + "Q" + quarter;
    }
}
