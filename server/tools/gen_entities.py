# -*- coding: utf-8 -*-
"""根据字段清单生成 JPA 实体（私有字段 + 标准 getter/setter，不依赖 Lombok）。"""
import os, re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = os.path.join(ROOT, "src", "main", "java", "com", "asset", "registry", "entity")

# 字段三元组: (java类型, 字段名, 列名, 额外注解)
def f(t, name, col=None, extra=""):
    return (t, name, col or re.sub(r'([A-Z])', lambda m: '_' + m.group(1).lower(), name), extra)

TS = [f("LocalDateTime", "createdAt", "created_at", "@CreationTimestamp"),
      f("LocalDateTime", "updatedAt", "updated_at", "@UpdateTimestamp")]

ENTITIES = [
    ("OrgUnit", "sys_org_unit", "归属单位", [
        f("Long", "id", "id"),
        f("String", "unitCode", "unit_code"),
        f("String", "unitName", "unit_name"),
        f("String", "shortName", "short_name"),
        f("Integer", "sortNo", "sort_no"),
        f("Boolean", "enabled", "enabled"),
    ] + TS),
    ("Dept", "sys_dept", "部门", [
        f("Long", "id", "id"),
        f("Long", "unitId", "unit_id"),
        f("String", "deptName", "dept_name"),
        f("Integer", "sortNo", "sort_no"),
        f("Boolean", "enabled", "enabled"),
    ] + TS),
    ("SysUser", "sys_user", "系统用户（SSO 同步）", [
        f("Long", "id", "id"),
        f("String", "username", "username"),
        f("String", "externalId", "external_id"),
        f("String", "realName", "real_name"),
        f("String", "phone", "phone"),
        f("String", "email", "email"),
        f("Long", "unitId", "unit_id"),
        f("Long", "deptId", "dept_id"),
        f("String", "roleCode", "role_code"),
        f("Boolean", "status", "status"),
        f("LocalDateTime", "lastLoginAt", "last_login_at"),
    ] + TS),
    ("AssetType", "asset_type", "资产类型字典", [
        f("Long", "id", "id"),
        f("String", "typeCode", "type_code"),
        f("String", "typeName", "type_name"),
        f("String", "color", "color"),
        f("String", "evidenceHint", "evidence_hint"),
        f("String", "icon", "icon"),
        f("Integer", "sortNo", "sort_no"),
        f("Boolean", "enabled", "enabled"),
    ]),
    ("AssetTypeField", "asset_type_field", "资产类型字段模板", [
        f("Long", "id", "id"),
        f("String", "typeCode", "type_code"),
        f("String", "fieldKey", "field_key"),
        f("String", "fieldLabel", "field_label"),
        f("String", "fieldType", "field_type"),
        f("Boolean", "required", "required"),
        f("Boolean", "uniqueFlag", "unique_flag"),
        f("String", "options", "options"),
        f("String", "placeholder", "placeholder"),
        f("Integer", "sortNo", "sort_no"),
    ]),
    ("Asset", "asset", "互联网资产台账", [
        f("Long", "id", "id"),
        f("String", "assetNo", "asset_no"),
        f("String", "assetName", "asset_name"),
        f("String", "typeCode", "type_code"),
        f("String", "assetIdent", "asset_ident"),
        f("String", "identNorm", "ident_norm"),
        f("String", "beianNo", "beian_no"),
        f("String", "ipSegment", "ip_segment"),
        f("String", "extraJson", "extra_json", '@Column(name = "extra_json", columnDefinition = "TEXT")'),
        f("Long", "unitId", "unit_id"),
        f("Long", "deptId", "dept_id"),
        f("String", "deptName", "dept_name"),
        f("Long", "ownerId", "owner_id"),
        f("String", "ownerName", "owner_name"),
        f("String", "ownerPhone", "owner_phone"),
        f("String", "deployLocation", "deploy_location"),
        f("LocalDate", "onlineDate", "online_date"),
        f("String", "runStatus", "run_status"),
        f("String", "verifyStatus", "verify_status"),
        f("LocalDate", "verifyDueDate", "verify_due_date"),
        f("String", "remark", "remark"),
        f("String", "createdBy", "created_by"),
    ] + TS),
    ("AssetAttachment", "asset_attachment", "资产佐证材料", [
        f("Long", "id", "id"),
        f("Long", "assetId", "asset_id"),
        f("String", "materialType", "material_type"),
        f("String", "fileName", "file_name"),
        f("String", "fileExt", "file_ext"),
        f("Long", "fileSize", "file_size"),
        f("String", "contentType", "content_type"),
        f("String", "storagePath", "storage_path"),
        f("String", "md5", "md5"),
        f("String", "uploadedBy", "uploaded_by"),
        f("LocalDateTime", "uploadedAt", "uploaded_at", "@CreationTimestamp"),
    ]),
    ("AssetChangeLog", "asset_change_log", "资产变更日志", [
        f("Long", "id", "id"),
        f("Long", "assetId", "asset_id"),
        f("String", "assetName", "asset_name"),
        f("String", "changeType", "change_type"),
        f("String", "fieldKey", "field_key"),
        f("String", "fieldLabel", "field_label"),
        f("String", "oldValue", "old_value"),
        f("String", "newValue", "new_value"),
        f("String", "reason", "reason"),
        f("String", "batchNo", "batch_no"),
        f("Long", "operatorId", "operator_id"),
        f("String", "operatorName", "operator_name"),
        f("LocalDateTime", "createdAt", "created_at", "@CreationTimestamp"),
    ]),
    ("AssetVerifyRecord", "asset_verify_record", "资产核对记录", [
        f("Long", "id", "id"),
        f("Long", "assetId", "asset_id"),
        f("String", "period", "period"),
        f("LocalDateTime", "sentAt", "sent_at"),
        f("Long", "respondentId", "respondent_id"),
        f("String", "respondentName", "respondent_name"),
        f("LocalDateTime", "respondedAt", "responded_at"),
        f("String", "result", "result"),
        f("String", "remark", "remark"),
    ]),
    ("OwnerTransfer", "owner_transfer", "责任人变更申请", [
        f("Long", "id", "id"),
        f("Long", "assetId", "asset_id"),
        f("Long", "fromUserId", "from_user_id"),
        f("String", "fromName", "from_name"),
        f("Long", "toUserId", "to_user_id"),
        f("String", "toName", "to_name"),
        f("String", "applyReason", "apply_reason"),
        f("String", "status", "status"),
        f("Long", "applicantId", "applicant_id"),
        f("Long", "approverId", "approver_id"),
        f("LocalDateTime", "approvedAt", "approved_at"),
        f("LocalDateTime", "effectiveAt", "effective_at"),
        f("LocalDateTime", "createdAt", "created_at", "@CreationTimestamp"),
    ]),
    ("AuditLog", "sys_audit_log", "操作审计日志", [
        f("Long", "id", "id"),
        f("Long", "userId", "user_id"),
        f("String", "userName", "user_name"),
        f("String", "action", "action"),
        f("String", "targetType", "target_type"),
        f("String", "targetId", "target_id"),
        f("String", "detail", "detail"),
        f("String", "ip", "ip"),
        f("LocalDateTime", "createdAt", "created_at", "@CreationTimestamp"),
    ]),
]

HEADER = """package com.asset.registry.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
{imports}import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {comment}
 */
@Entity
@Table(name = "{table}")
public class {cls} {{
"""

def cap(s):
    return s[0].upper() + s[1:]

def build(cls, table, comment, fields):
    needs_create = any("@CreationTimestamp" in x[3] for x in fields)
    needs_update = any("@UpdateTimestamp" in x[3] for x in fields)
    imports = ""
    if not needs_create and not needs_update:
        imports = ""
    body = [HEADER.format(cls=cls, table=table, comment=comment, imports=imports)]
    for i, (t, name, col, extra) in enumerate(fields):
        if name == "id":
            body.append("    @Id\n    @GeneratedValue(strategy = GenerationType.IDENTITY)\n")
        if extra:
            body.append("    " + extra + "\n")
        else:
            nullable = "true" if name != "id" else "false"
            body.append('    @Column(name = "%s")\n' % col)
        body.append("    private %s %s;\n\n" % (t, name))
    # getters / setters
    for (t, name, col, extra) in fields:
        body.append("    public %s get%s() {\n        return %s;\n    }\n\n" % (t, cap(name), name))
        body.append("    public void set%s(%s %s) {\n        this.%s = %s;\n    }\n\n" % (cap(name), t, name, name, name))
    body.append("}\n")
    return "".join(body)

os.makedirs(BASE, exist_ok=True)
for cls, table, comment, fields in ENTITIES:
    code = build(cls, table, comment, fields)
    # 未使用的 import 会产生 warning，但不影响编译；按需精简
    if "@CreationTimestamp" not in code:
        code = code.replace("import org.hibernate.annotations.CreationTimestamp;\n", "")
    if "@UpdateTimestamp" not in code:
        code = code.replace("import org.hibernate.annotations.UpdateTimestamp;\n", "")
    if "LocalDate " not in code.replace("import java.time.LocalDate;", ""):
        code = code.replace("import java.time.LocalDate;\n", "")
    if "LocalDateTime " not in code.replace("import java.time.LocalDateTime;", ""):
        code = code.replace("import java.time.LocalDateTime;\n", "")
    with open(os.path.join(BASE, cls + ".java"), "w", encoding="utf-8") as fh:
        fh.write(code)
    print("generated", cls + ".java")
