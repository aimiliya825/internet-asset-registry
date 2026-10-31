package com.asset.registry.service;

import com.asset.registry.common.BizException;
import com.asset.registry.dto.Views;
import com.asset.registry.entity.AssetType;
import com.asset.registry.entity.AssetTypeField;
import com.asset.registry.entity.Dept;
import com.asset.registry.entity.OrgUnit;
import com.asset.registry.entity.SysUser;
import com.asset.registry.repository.AssetTypeFieldRepository;
import com.asset.registry.repository.AssetTypeRepository;
import com.asset.registry.repository.DeptRepository;
import com.asset.registry.repository.OrgUnitRepository;
import com.asset.registry.repository.SysUserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 字典与元数据：资产类型、字段模板、单位、部门、用户、状态。
 */
@Service
public class MetaService {

    private final AssetTypeRepository typeRepository;
    private final AssetTypeFieldRepository fieldRepository;
    private final OrgUnitRepository unitRepository;
    private final DeptRepository deptRepository;
    private final SysUserRepository userRepository;

    public MetaService(AssetTypeRepository typeRepository, AssetTypeFieldRepository fieldRepository,
                       OrgUnitRepository unitRepository, DeptRepository deptRepository,
                       SysUserRepository userRepository) {
        this.typeRepository = typeRepository;
        this.fieldRepository = fieldRepository;
        this.unitRepository = unitRepository;
        this.deptRepository = deptRepository;
        this.userRepository = userRepository;
    }

    public Views.Meta meta() {
        List<AssetTypeField> allFields = fieldRepository.findAllByOrderByTypeCodeAscSortNoAsc();
        Map<String, List<AssetTypeField>> grouped = allFields.stream()
                .collect(Collectors.groupingBy(AssetTypeField::getTypeCode));

        List<Views.TypeMeta> types = typeRepository.findAllByEnabledTrueOrderBySortNoAsc().stream()
                .map(t -> new Views.TypeMeta(t.getTypeCode(), t.getTypeName(), t.getColor(), t.getEvidenceHint(),
                        t.getIcon(), toFieldMeta(grouped.getOrDefault(t.getTypeCode(), List.of()))))
                .toList();

        List<Dept> depts = deptRepository.findAllByOrderByUnitIdAscSortNoAsc();
        Map<Long, List<Dept>> deptGroup = depts.stream().collect(Collectors.groupingBy(Dept::getUnitId, LinkedHashMap::new, Collectors.toList()));

        List<Views.UnitMeta> units = unitRepository.findAllByEnabledTrueOrderBySortNoAsc().stream()
                .map(u -> new Views.UnitMeta(u.getId(), u.getUnitCode(), u.getUnitName(), u.getShortName(),
                        deptGroup.getOrDefault(u.getId(), List.of()).stream()
                                .map(d -> new Views.DeptMeta(d.getId(), d.getUnitId(), d.getDeptName()))
                                .toList()))
                .toList();

        Map<Long, String> unitNames = unitRepository.findAll().stream()
                .collect(Collectors.toMap(OrgUnit::getId, OrgUnit::getUnitName, (a, b) -> a));

        List<Views.UserMeta> users = userRepository.findAllByOrderByIdAsc().stream()
                .filter(u -> Boolean.TRUE.equals(u.getStatus()))
                .map(u -> new Views.UserMeta(u.getId(), u.getRealName(), u.getPhone(), u.getRoleCode(),
                        u.getUnitId(), unitNames.get(u.getUnitId())))
                .toList();

        List<Views.StatusMeta> statuses = List.of(
                new Views.StatusMeta("active", "启用"),
                new Views.StatusMeta("stopped", "停用"),
                new Views.StatusMeta("scrapped", "报废"));

        return new Views.Meta(types, units, statuses, users);
    }

    public List<Views.FieldMeta> fields(String typeCode) {
        return toFieldMeta(fieldRepository.findByTypeCodeOrderBySortNoAsc(typeCode));
    }

    public Map<String, AssetType> typeIndex() {
        return typeRepository.findAll().stream()
                .collect(Collectors.toMap(AssetType::getTypeCode, Function.identity(), (a, b) -> a));
    }

    public Map<Long, OrgUnit> unitIndex() {
        return unitRepository.findAll().stream()
                .collect(Collectors.toMap(OrgUnit::getId, Function.identity(), (a, b) -> a));
    }

    public String deptName(Long deptId) {
        if (deptId == null) {
            return null;
        }
        return deptRepository.findById(deptId).map(Dept::getDeptName).orElse(null);
    }

    public AssetType requireType(String typeCode) {
        return typeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> BizException.badRequest("未知的资产类型：" + typeCode));
    }

    private static List<Views.FieldMeta> toFieldMeta(List<AssetTypeField> fields) {
        List<Views.FieldMeta> list = new ArrayList<>();
        for (AssetTypeField f : fields) {
            List<String> options = (f.getOptions() == null || f.getOptions().isBlank())
                    ? List.of()
                    : Arrays.stream(f.getOptions().split(",")).map(String::trim).toList();
            list.add(new Views.FieldMeta(f.getFieldKey(), f.getFieldLabel(), f.getFieldType(),
                    Boolean.TRUE.equals(f.getRequired()), Boolean.TRUE.equals(f.getUniqueFlag()),
                    options, f.getPlaceholder()));
        }
        return list;
    }
}
