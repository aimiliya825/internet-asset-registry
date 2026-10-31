package com.asset.registry.service;

import com.asset.registry.common.BizException;
import com.asset.registry.common.PageResult;
import com.asset.registry.common.RunStatus;
import com.asset.registry.config.UserContext;
import com.asset.registry.dto.Payloads;
import com.asset.registry.dto.Views;
import com.asset.registry.entity.Asset;
import com.asset.registry.entity.AssetAttachment;
import com.asset.registry.entity.AssetType;
import com.asset.registry.entity.OrgUnit;
import com.asset.registry.entity.SysUser;
import com.asset.registry.repository.AssetAttachmentRepository;
import com.asset.registry.repository.AssetRepository;
import com.asset.registry.repository.AssetVerifyRecordRepository;
import com.asset.registry.repository.SysUserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 资产主流程：登记（唯一性校验）、查询、修改、状态流转、批量操作、附件、导出。
 */
@Service
public class AssetService {

    private static final Logger log = LoggerFactory.getLogger(AssetService.class);

    private static final int MAX_PAGE_SIZE = 200;
    private static final int MAX_EXPORT = 20000;
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "jpg", "jpeg", "png", "zip");

    /** 导出用时间格式：避免 Excel 里出现 2026-10-12T14:03 这种带 T 的写法 */
    private static final DateTimeFormatter EXPORT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AssetRepository assetRepository;
    private final AssetAttachmentRepository attachmentRepository;
    private final AssetVerifyRecordRepository verifyRecordRepository;
    private final SysUserRepository userRepository;
    private final MetaService metaService;
    private final ChangeLogService changeLogService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final String storageRoot;

    public AssetService(AssetRepository assetRepository,
                        AssetAttachmentRepository attachmentRepository,
                        AssetVerifyRecordRepository verifyRecordRepository,
                        SysUserRepository userRepository,
                        MetaService metaService,
                        ChangeLogService changeLogService,
                        AuditService auditService,
                        ObjectMapper objectMapper,
                        @Value("${app.storage-root:./storage}") String storageRoot) {
        this.assetRepository = assetRepository;
        this.attachmentRepository = attachmentRepository;
        this.verifyRecordRepository = verifyRecordRepository;
        this.userRepository = userRepository;
        this.metaService = metaService;
        this.changeLogService = changeLogService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.storageRoot = storageRoot;
    }

    /* ------------------------------------------------------------------ 查询 */

    public PageResult<Views.AssetItem> search(Payloads.AssetQuery q, int page, int size) {
        int pageNo = Math.max(page, 1);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<Asset> result = assetRepository.findAll(spec(q), pageable);
        Map<String, AssetType> types = metaService.typeIndex();
        Map<Long, OrgUnit> units = metaService.unitIndex();
        List<Views.AssetItem> items = result.getContent().stream()
                .map(a -> toItem(a, types, units))
                .toList();
        return PageResult.of(items, result.getTotalElements(), pageNo, pageSize);
    }

    public Views.AssetDetail detail(Long id) {
        Asset a = require(id);
        List<Views.AttachmentItem> attachments = attachmentRepository
                .findByAssetIdOrderByUploadedAtDesc(id).stream()
                .map(AssetService::toAttachment)
                .toList();
        String period = verifyRecordRepository.findTopByAssetIdOrderByPeriodDesc(id)
                .map(r -> r.getPeriod())
                .orElse(null);
        return new Views.AssetDetail(toItem(a, metaService.typeIndex(), metaService.unitIndex()),
                parseExtra(a.getExtraJson()), attachments, changeLogService.ofAsset(id), period);
    }

    private Specification<Asset> spec(Payloads.AssetQuery q) {
        return (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (hasText(q.typeCode())) {
                ps.add(cb.equal(root.get("typeCode"), q.typeCode()));
            }
            if (q.unitId() != null) {
                ps.add(cb.equal(root.get("unitId"), q.unitId()));
            }
            if (hasText(q.status())) {
                ps.add(cb.equal(root.get("runStatus"), q.status()));
            }
            if (q.from() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("onlineDate"), q.from()));
            }
            if (q.to() != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("onlineDate"), q.to()));
            }
            if (hasText(q.owner())) {
                ps.add(cb.like(root.get("ownerName"), "%" + q.owner().trim() + "%"));
            }
            if (hasText(q.keyword())) {
                String like = "%" + q.keyword().trim().toLowerCase(Locale.ROOT) + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("assetName")), like),
                        cb.like(cb.lower(root.get("assetIdent")), like),
                        cb.like(cb.lower(root.get("ownerName")), like),
                        cb.like(cb.lower(root.get("assetNo")), like)));
            }
            return ps.isEmpty() ? cb.conjunction() : cb.and(ps.toArray(new Predicate[0]));
        };
    }

    /* ------------------------------------------------------------------ 登记 */

    @Transactional
    public Views.AssetDetail create(Payloads.AssetCreate req) {
        metaService.requireType(req.typeCode());
        String norm = norm(req.assetIdent());

        assetRepository.findByIdentNorm(norm).ifPresent(exists -> {
            Map<Long, OrgUnit> units = metaService.unitIndex();
            OrgUnit unit = units.get(exists.getUnitId());
            throw BizException.conflict("该资产标识已登记：" + exists.getAssetName()
                    + "（" + (unit == null ? "未知单位" : unit.getUnitName())
                    + " · " + RunStatus.of(exists.getRunStatus()).getLabel() + "），请勿重复登记");
        });

        validateExtra(req.typeCode(), req.extra());

        Asset a = new Asset();
        a.setAssetName(req.assetName().trim());
        a.setTypeCode(req.typeCode());
        a.setAssetIdent(req.assetIdent().trim());
        a.setIdentNorm(norm);
        a.setBeianNo(req.beianNo());
        a.setIpSegment(req.ipSegment());
        a.setExtraJson(writeExtra(req.extra()));
        a.setUnitId(req.unitId());
        a.setDeptId(req.deptId());
        a.setDeptName(deptName(req.deptId()));
        a.setOwnerName(req.ownerName().trim());
        a.setOwnerId(userRepository.findFirstByRealName(req.ownerName().trim()).map(SysUser::getId).orElse(null));
        a.setOwnerPhone(req.ownerPhone().trim());
        a.setDeployLocation(req.deployLocation().trim());
        a.setOnlineDate(req.onlineDate());
        a.setRunStatus(RunStatus.ACTIVE.getCode());
        a.setVerifyStatus("ok");
        a.setVerifyDueDate(LocalDate.now().plusMonths(3));
        a.setRemark(req.remark());
        a.setCreatedBy(UserContext.get().realName());
        a = assetRepository.save(a);

        a.setAssetNo("A" + (1000 + a.getId()));
        a = assetRepository.save(a);

        changeLogService.record(a.getId(), a.getAssetName(), ChangeLogService.TYPE_CREATE,
                "assetIdent", "资产标识", null, a.getAssetIdent(), "登记入库", null);
        log.info("资产登记成功 id={} no={} ident={}", a.getId(), a.getAssetNo(), a.getAssetIdent());
        return detail(a.getId());
    }

    private void validateExtra(String typeCode, Map<String, Object> extra) {
        List<Views.FieldMeta> fields = metaService.fields(typeCode);
        for (Views.FieldMeta f : fields) {
            if (!f.required()) {
                continue;
            }
            Object v = extra == null ? null : extra.get(f.key());
            if (v == null || String.valueOf(v).isBlank()) {
                throw BizException.badRequest("「" + f.label() + "」为必填项，请补充后再提交");
            }
        }
    }

    /* ------------------------------------------------------------------ 修改 */

    @Transactional
    public Views.AssetDetail update(Long id, Payloads.AssetUpdate req) {
        Asset a = require(id);
        String reason = hasText(req.reason()) ? req.reason() : "信息维护";
        Map<String, String[]> diffs = new LinkedHashMap<>();

        if (req.assetName() != null && !req.assetName().equals(a.getAssetName())) {
            put(diffs, "assetName", "资产名称", a.getAssetName(), req.assetName());
            a.setAssetName(req.assetName().trim());
        }
        if (req.beianNo() != null && !req.beianNo().equals(a.getBeianNo())) {
            put(diffs, "beianNo", "ICP 备案号", a.getBeianNo(), req.beianNo());
            a.setBeianNo(req.beianNo().trim());
        }
        if (req.ipSegment() != null && !req.ipSegment().equals(a.getIpSegment())) {
            put(diffs, "ipSegment", "IP 网段", a.getIpSegment(), req.ipSegment());
            a.setIpSegment(req.ipSegment().trim());
        }
        if (req.unitId() != null && !req.unitId().equals(a.getUnitId())) {
            Map<Long, OrgUnit> units = metaService.unitIndex();
            put(diffs, "unitId", "归属单位", unitName(units, a.getUnitId()), unitName(units, req.unitId()));
            a.setUnitId(req.unitId());
        }
        if (req.deptId() != null && !req.deptId().equals(a.getDeptId())) {
            put(diffs, "deptId", "归属部门", a.getDeptName(), deptName(req.deptId()));
            a.setDeptId(req.deptId());
            a.setDeptName(deptName(req.deptId()));
        }
        if (req.ownerName() != null && !req.ownerName().equals(a.getOwnerName())) {
            put(diffs, "ownerName", "责任人", a.getOwnerName(), req.ownerName());
            a.setOwnerName(req.ownerName().trim());
            a.setOwnerId(userRepository.findFirstByRealName(req.ownerName().trim()).map(SysUser::getId).orElse(null));
        }
        if (req.ownerPhone() != null && !req.ownerPhone().equals(a.getOwnerPhone())) {
            put(diffs, "ownerPhone", "联系方式", a.getOwnerPhone(), req.ownerPhone());
            a.setOwnerPhone(req.ownerPhone().trim());
        }
        if (req.deployLocation() != null && !req.deployLocation().equals(a.getDeployLocation())) {
            put(diffs, "deployLocation", "部署位置", a.getDeployLocation(), req.deployLocation());
            a.setDeployLocation(req.deployLocation().trim());
        }
        if (req.onlineDate() != null && !req.onlineDate().equals(a.getOnlineDate())) {
            put(diffs, "onlineDate", "上线时间", String.valueOf(a.getOnlineDate()), String.valueOf(req.onlineDate()));
            a.setOnlineDate(req.onlineDate());
        }
        if (req.remark() != null && !req.remark().equals(a.getRemark())) {
            put(diffs, "remark", "备注", a.getRemark(), req.remark());
            a.setRemark(req.remark());
        }
        if (req.extra() != null && !req.extra().isEmpty()) {
            Map<String, Object> merged = parseExtra(a.getExtraJson());
            Map<String, String> labels = new LinkedHashMap<>();
            for (Views.FieldMeta f : metaService.fields(a.getTypeCode())) {
                labels.put(f.key(), f.label());
            }
            String before = writeExtra(merged);
            merged.putAll(req.extra());
            String after = writeExtra(merged);
            if (!Objects.equals(before, after)) {
                for (Map.Entry<String, Object> e : req.extra().entrySet()) {
                    put(diffs, "extra." + e.getKey(), labels.getOrDefault(e.getKey(), e.getKey()),
                            null, String.valueOf(e.getValue()));
                }
                a.setExtraJson(after);
            }
        }

        if (diffs.isEmpty()) {
            throw BizException.badRequest("未检测到任何字段变化");
        }

        a = assetRepository.save(a);
        for (Map.Entry<String, String[]> e : diffs.entrySet()) {
            String[] v = e.getValue();
            changeLogService.record(a.getId(), a.getAssetName(), ChangeLogService.TYPE_INFO,
                    e.getKey(), v[0], v[1], v[2], reason, null);
        }
        return detail(a.getId());
    }

    /* -------------------------------------------------------------- 状态流转 */

    @Transactional
    public Views.AssetDetail changeStatus(Long id, Payloads.StatusChange req) {
        Asset a = require(id);
        RunStatus target = RunStatus.of(req.status());
        RunStatus current = RunStatus.of(a.getRunStatus());

        if (target == current) {
            throw BizException.badRequest("状态未发生变化，当前已是「" + current.getLabel() + "」");
        }
        if (!current.canTransitTo(target)) {
            throw BizException.badRequest("不允许的状态流转：" + current.getLabel() + " → " + target.getLabel());
        }

        a.setRunStatus(target.getCode());
        if (target == RunStatus.ACTIVE) {
            a.setVerifyStatus("ok");
            a.setVerifyDueDate(LocalDate.now().plusMonths(3));
        }
        assetRepository.save(a);
        changeLogService.record(a.getId(), a.getAssetName(), ChangeLogService.TYPE_STATUS,
                "runStatus", "运行状态", current.getLabel(), target.getLabel(), req.reason(), null);

        if (current == RunStatus.SCRAPPED && target == RunStatus.ACTIVE) {
            auditService.log("RESUME_SCRAPPED", "ASSET", String.valueOf(id), "报废资产恢复启用：" + a.getAssetName());
        }
        return detail(a.getId());
    }

    @Transactional
    public int batchStatus(Payloads.BatchStatus req) {
        RunStatus target = RunStatus.of(req.status());
        String batchNo = "B" + System.currentTimeMillis();
        int changed = 0;
        for (Long id : req.ids()) {
            Asset a = assetRepository.findById(id).orElse(null);
            if (a == null) {
                continue;
            }
            RunStatus current = RunStatus.of(a.getRunStatus());
            if (current == target || !current.canTransitTo(target)) {
                continue;
            }
            a.setRunStatus(target.getCode());
            assetRepository.save(a);
            changeLogService.record(a.getId(), a.getAssetName(), ChangeLogService.TYPE_BATCH,
                    "runStatus", "运行状态", current.getLabel(), target.getLabel(), req.reason(), batchNo);
            changed++;
        }
        if (changed == 0) {
            throw BizException.badRequest("所选资产均无法流转到「" + target.getLabel() + "」，请检查当前状态");
        }
        auditService.log("BATCH_STATUS", "ASSET", String.join(",", req.ids().stream().map(String::valueOf).toList()),
                "批量状态更新为 " + target.getLabel() + "，实际变更 " + changed + " 条");
        return changed;
    }

    @Transactional
    public int batchField(Payloads.BatchField req) {
        String field = req.field().trim();
        String value = req.value().trim();
        String batchNo = "B" + System.currentTimeMillis();
        int changed = 0;

        for (Long id : req.ids()) {
            Asset a = assetRepository.findById(id).orElse(null);
            if (a == null) {
                continue;
            }
            String label;
            String oldValue;
            String newValue = value;
            switch (field) {
                case "unitId" -> {
                    Long v = parseLong(value, "归属单位");
                    if (v.equals(a.getUnitId())) {
                        continue;
                    }
                    Map<Long, OrgUnit> units = metaService.unitIndex();
                    label = "归属单位";
                    oldValue = unitName(units, a.getUnitId());
                    a.setUnitId(v);
                    newValue = unitName(units, v);
                }
                case "deptId" -> {
                    Long v = parseLong(value, "归属部门");
                    if (v.equals(a.getDeptId())) {
                        continue;
                    }
                    label = "归属部门";
                    oldValue = a.getDeptName();
                    a.setDeptId(v);
                    a.setDeptName(deptName(v));
                }
                case "ownerName" -> {
                    if (value.equals(a.getOwnerName())) {
                        continue;
                    }
                    label = "责任人";
                    oldValue = a.getOwnerName();
                    a.setOwnerName(value);
                    a.setOwnerId(userRepository.findFirstByRealName(value).map(SysUser::getId).orElse(null));
                }
                case "ownerPhone" -> {
                    if (value.equals(a.getOwnerPhone())) {
                        continue;
                    }
                    label = "联系方式";
                    oldValue = a.getOwnerPhone();
                    a.setOwnerPhone(value);
                }
                case "deployLocation" -> {
                    if (value.equals(a.getDeployLocation())) {
                        continue;
                    }
                    label = "部署位置";
                    oldValue = a.getDeployLocation();
                    a.setDeployLocation(value);
                }
                default -> throw BizException.badRequest("不支持的批量修改字段：" + field);
            }
            assetRepository.save(a);
            changeLogService.record(a.getId(), a.getAssetName(), ChangeLogService.TYPE_BATCH,
                    field, label, oldValue, newValue, req.reason(), batchNo);
            changed++;
        }
        if (changed == 0) {
            throw BizException.badRequest("所选资产的目标值与当前值一致，未产生变更");
        }
        auditService.log("BATCH_FIELD", "ASSET", String.join(",", req.ids().stream().map(String::valueOf).toList()),
                "批量修改 " + field + " 为 " + value + "，实际变更 " + changed + " 条");
        return changed;
    }

    /* ------------------------------------------------------------------ 附件 */

    @Transactional
    public Views.AttachmentItem upload(Long assetId, MultipartFile file, String materialType) {
        Asset a = require(assetId);
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("请选择要上传的文件");
        }
        String original = file.getOriginalFilename() == null ? "material.bin" : file.getOriginalFilename();
        original = Paths.get(original).getFileName().toString();
        String ext = extOf(original);
        if (!ALLOWED_EXT.contains(ext)) {
            throw BizException.badRequest("仅支持 PDF / JPG / PNG / ZIP 格式");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw BizException.badRequest("单个文件不超过 20MB，当前 " + (file.getSize() / 1024 / 1024) + "MB");
        }
        LocalDate now = LocalDate.now();
        String rel = String.format("%d/%02d/%d/%s.%s", now.getYear(), now.getMonthValue(), assetId,
                UUID.randomUUID().toString().replace("-", ""), ext);
        try {
            byte[] bytes = file.getBytes();
            Path target = Paths.get(storageRoot, rel).toAbsolutePath().normalize();
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);

            AssetAttachment att = new AssetAttachment();
            att.setAssetId(assetId);
            att.setMaterialType(hasText(materialType) ? materialType : "佐证材料");
            att.setFileName(original);
            att.setFileExt(ext);
            att.setFileSize((long) bytes.length);
            att.setContentType(file.getContentType());
            att.setStoragePath(rel);
            att.setMd5(md5(bytes));
            att.setUploadedBy(UserContext.get().realName());
            att = attachmentRepository.save(att);

            changeLogService.record(assetId, a.getAssetName(), ChangeLogService.TYPE_INFO,
                    "attachment", "佐证材料", null, original, "上传佐证材料", null);
            return toAttachment(att);
        } catch (IOException e) {
            throw new BizException(500, "文件写入失败：" + e.getMessage());
        }
    }

    /* ------------------------------------------------------------------ 导出 */

    public byte[] exportCsv(Payloads.AssetQuery q) {
        Pageable limit = PageRequest.of(0, MAX_EXPORT, Sort.by(Sort.Direction.ASC, "id"));
        List<Asset> list = assetRepository.findAll(spec(q), limit).getContent();
        Map<String, AssetType> types = metaService.typeIndex();
        Map<Long, OrgUnit> units = metaService.unitIndex();

        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("资产编号,资产名称,资产类型,资产标识,归属单位,归属部门,责任人,联系方式,运行状态,上线时间,部署位置,ICP备案号,最近变更\n");
        for (Asset a : list) {
            Views.AssetItem v = toItem(a, types, units);
            sb.append(csv(v.assetNo())).append(',')
                    .append(csv(v.assetName())).append(',')
                    .append(csv(v.typeName())).append(',')
                    .append(csv(v.assetIdent())).append(',')
                    .append(csv(v.unitName())).append(',')
                    .append(csv(v.deptName())).append(',')
                    .append(csv(v.ownerName())).append(',')
                    .append(csv(v.ownerPhone())).append(',')
                    .append(csv(v.runStatusLabel())).append(',')
                    .append(csv(String.valueOf(v.onlineDate()))).append(',')
                    .append(csv(v.deployLocation())).append(',')
                    .append(csv(v.beianNo())).append(',')
                    .append(csv(v.updatedAt() == null ? "" : v.updatedAt().format(EXPORT_TIME))).append('\n');
        }
        auditService.log("EXPORT_ASSET", "ASSET", "QUERY", "导出资产台账，共 " + list.size() + " 条");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /* ------------------------------------------------------------------ 工具 */

    private Asset require(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("资产不存在或已删除（id=" + id + "）"));
    }

    public static Views.AssetItem toItem(Asset a, Map<String, AssetType> types, Map<Long, OrgUnit> units) {
        AssetType t = types.get(a.getTypeCode());
        OrgUnit u = units.get(a.getUnitId());
        return new Views.AssetItem(
                a.getId(), a.getAssetNo(), a.getAssetName(), a.getTypeCode(),
                t == null ? a.getTypeCode() : t.getTypeName(),
                t == null ? "#5B6B82" : t.getColor(),
                a.getAssetIdent(), a.getBeianNo(), a.getIpSegment(),
                a.getUnitId(), u == null ? null : u.getUnitName(),
                a.getDeptId(), a.getDeptName(), a.getOwnerId(), a.getOwnerName(), a.getOwnerPhone(),
                a.getDeployLocation(), a.getOnlineDate(), a.getRunStatus(),
                RunStatus.of(a.getRunStatus()).getLabel(), a.getVerifyStatus(), a.getVerifyDueDate(),
                a.getRemark(), a.getUpdatedAt());
    }

    private static Views.AttachmentItem toAttachment(AssetAttachment att) {
        return new Views.AttachmentItem(att.getId(), att.getMaterialType(), att.getFileName(), att.getFileExt(),
                att.getFileSize(), att.getUploadedBy(), att.getUploadedAt());
    }

    public static String norm(String ident) {
        if (ident == null) {
            return "";
        }
        return ident.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private Map<String, Object> parseExtra(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("类型扩展字段解析失败：{}", json);
            return new LinkedHashMap<>();
        }
    }

    private String writeExtra(Map<String, Object> extra) {
        if (extra == null || extra.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(extra);
        } catch (Exception e) {
            throw BizException.badRequest("扩展字段格式不正确");
        }
    }

    private String deptName(Long deptId) {
        if (deptId == null) {
            return null;
        }
        return metaService.deptName(deptId);
    }

    private String unitName(Map<Long, OrgUnit> units, Long unitId) {
        OrgUnit u = units.get(unitId);
        return u == null ? null : u.getUnitName();
    }

    private static void put(Map<String, String[]> diffs, String key, String label, String oldV, String newV) {
        if (!Objects.equals(oldV, newV)) {
            diffs.put(key, new String[]{label, oldV, newV});
        }
    }

    private static Long parseLong(String v, String label) {
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            throw BizException.badRequest(label + "需填写数字 ID");
        }
    }

    private static String extOf(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private static String md5(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static String csv(String v) {
        if (v == null || "null".equals(v)) {
            return "";
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
