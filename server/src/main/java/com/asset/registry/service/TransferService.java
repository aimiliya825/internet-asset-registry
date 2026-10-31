package com.asset.registry.service;

import com.asset.registry.common.BizException;
import com.asset.registry.config.CurrentUser;
import com.asset.registry.config.UserContext;
import com.asset.registry.dto.Payloads;
import com.asset.registry.dto.Views;
import com.asset.registry.entity.Asset;
import com.asset.registry.entity.OwnerTransfer;
import com.asset.registry.entity.SysUser;
import com.asset.registry.repository.AssetRepository;
import com.asset.registry.repository.OwnerTransferRepository;
import com.asset.registry.repository.SysUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 责任人变更：申请 → 审批 → 生效，保证任何时点责任都有明确归属。
 */
@Service
public class TransferService {

    private final OwnerTransferRepository transferRepository;
    private final AssetRepository assetRepository;
    private final SysUserRepository userRepository;
    private final ChangeLogService changeLogService;

    public TransferService(OwnerTransferRepository transferRepository, AssetRepository assetRepository,
                           SysUserRepository userRepository, ChangeLogService changeLogService) {
        this.transferRepository = transferRepository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.changeLogService = changeLogService;
    }

    public List<Views.TransferItem> list(String status) {
        List<OwnerTransfer> list = (status == null || status.isBlank())
                ? transferRepository.findAllByOrderByCreatedAtDesc()
                : transferRepository.findByStatusOrderByCreatedAtDesc(status);
        return list.stream().map(t -> new Views.TransferItem(t.getId(), t.getAssetId(),
                assetRepository.findById(t.getAssetId()).map(Asset::getAssetName).orElse(null),
                t.getFromName(), t.getToName(), t.getApplyReason(), t.getStatus(), t.getCreatedAt())).toList();
    }

    @Transactional
    public Views.TransferItem apply(Long assetId, Payloads.TransferApply req) {
        Asset a = assetRepository.findById(assetId)
                .orElseThrow(() -> BizException.notFound("资产不存在（id=" + assetId + "）"));
        SysUser to = userRepository.findById(req.toUserId())
                .orElseThrow(() -> BizException.badRequest("接收人不存在"));
        if (!Boolean.TRUE.equals(to.getStatus())) {
            throw BizException.badRequest("接收人已离职，无法接收资产");
        }
        if (to.getRealName().equals(a.getOwnerName())) {
            throw BizException.badRequest("接收人与当前责任人相同，无需变更");
        }
        CurrentUser u = UserContext.get();
        OwnerTransfer t = new OwnerTransfer();
        t.setAssetId(assetId);
        t.setFromUserId(a.getOwnerId());
        t.setFromName(a.getOwnerName());
        t.setToUserId(to.getId());
        t.setToName(to.getRealName());
        t.setApplyReason(req.reason());
        t.setStatus("pending");
        t.setApplicantId(u.id());
        t = transferRepository.save(t);
        return new Views.TransferItem(t.getId(), assetId, a.getAssetName(), t.getFromName(), t.getToName(),
                t.getApplyReason(), t.getStatus(), t.getCreatedAt());
    }

    @Transactional
    public Views.TransferItem decide(Long id, Payloads.TransferDecision req) {
        OwnerTransfer t = transferRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("变更申请不存在"));
        if (!"pending".equals(t.getStatus())) {
            throw BizException.badRequest("该申请已处理，当前状态：" + t.getStatus());
        }
        String decision = req.decision().trim().toLowerCase();
        CurrentUser u = UserContext.get();
        t.setApproverId(u.id());
        t.setApprovedAt(LocalDateTime.now());

        if ("approve".equals(decision)) {
            if (t.getToUserId() == null) {
                throw BizException.badRequest("该申请尚未指定接收人，请先补充接收人后再审批");
            }
            Asset a = assetRepository.findById(t.getAssetId())
                    .orElseThrow(() -> BizException.notFound("资产不存在"));
            String oldOwner = a.getOwnerName();
            a.setOwnerName(t.getToName());
            a.setOwnerId(t.getToUserId());
            userRepository.findById(t.getToUserId()).map(SysUser::getPhone).ifPresent(a::setOwnerPhone);
            assetRepository.save(a);
            t.setStatus("approved");
            t.setEffectiveAt(LocalDateTime.now());
            changeLogService.record(a.getId(), a.getAssetName(), ChangeLogService.TYPE_OWNER,
                    "ownerName", "责任人", oldOwner, t.getToName(),
                    req.opinion() == null ? t.getApplyReason() : req.opinion(), null);
        } else if ("reject".equals(decision)) {
            t.setStatus("rejected");
        } else {
            throw BizException.badRequest("处理意见仅支持 approve / reject");
        }
        transferRepository.save(t);
        String assetName = assetRepository.findById(t.getAssetId()).map(Asset::getAssetName).orElse(null);
        return new Views.TransferItem(t.getId(), t.getAssetId(), assetName, t.getFromName(), t.getToName(),
                t.getApplyReason(), t.getStatus(), t.getCreatedAt());
    }
}
