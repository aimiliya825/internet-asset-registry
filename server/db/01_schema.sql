-- =============================================================================
--  互联网资产与信息系统管理平台 —— 数据库结构（MySQL 8.0+ / 9.x）
--  库名：asset_registry        字符集：utf8mb4 / utf8mb4_0900_ai_ci
--  设计要点：
--    1) 资产类型与字段模板配置化（asset_type / asset_type_field），新增类型不改代码；
--    2) 标识规范化列 ident_norm + 唯一索引，实现 IP/域名/MAC 的库级查重兜底；
--    3) 变更日志只增不改，资产不做物理删除（报废仅改状态），满足审计留痕；
--    4) 统计与导出统一走视图 v_asset_detail，避免口径分裂。
--  执行：mysql -uroot -p < 01_schema.sql
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `asset_registry`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `asset_registry`;

DROP VIEW  IF EXISTS `v_asset_detail`;
DROP TABLE IF EXISTS `sys_audit_log`;
DROP TABLE IF EXISTS `asset_verify_record`;
DROP TABLE IF EXISTS `owner_transfer`;
DROP TABLE IF EXISTS `asset_change_log`;
DROP TABLE IF EXISTS `asset_attachment`;
DROP TABLE IF EXISTS `asset`;
DROP TABLE IF EXISTS `asset_type_field`;
DROP TABLE IF EXISTS `asset_type`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `sys_dept`;
DROP TABLE IF EXISTS `sys_org_unit`;

-- -----------------------------------------------------------------------------
-- 1. 组织：归属单位
-- -----------------------------------------------------------------------------
CREATE TABLE `sys_org_unit` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `unit_code`   VARCHAR(32)     NOT NULL                COMMENT '单位编码，如 IT / FIN',
  `unit_name`   VARCHAR(60)     NOT NULL                COMMENT '单位名称',
  `short_name`  VARCHAR(32)         NULL                COMMENT '简称，用于图表轴标签',
  `sort_no`     INT             NOT NULL DEFAULT 0      COMMENT '排序号',
  `enabled`     TINYINT         NOT NULL DEFAULT 1      COMMENT '是否启用 0/1',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_unit_code` (`unit_code`),
  UNIQUE KEY `uk_unit_name` (`unit_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归属单位';

-- -----------------------------------------------------------------------------
-- 2. 组织：部门（单位下二级）
-- -----------------------------------------------------------------------------
CREATE TABLE `sys_dept` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `unit_id`    BIGINT UNSIGNED NOT NULL                COMMENT '所属单位',
  `dept_name`  VARCHAR(60)     NOT NULL                COMMENT '部门名称',
  `sort_no`    INT             NOT NULL DEFAULT 0,
  `enabled`    TINYINT         NOT NULL DEFAULT 1,
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_unit_name` (`unit_id`, `dept_name`),
  CONSTRAINT `fk_dept_unit` FOREIGN KEY (`unit_id`) REFERENCES `sys_org_unit` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门';

-- -----------------------------------------------------------------------------
-- 3. 用户（由统一身份认证同步，本系统不落密码，仅保存 SSO 主体标识与角色）
-- -----------------------------------------------------------------------------
CREATE TABLE `sys_user` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username`      VARCHAR(40)     NOT NULL              COMMENT '登录名（同步自 SSO）',
  `external_id`   VARCHAR(64)         NULL              COMMENT 'SSO 主体标识 sub',
  `real_name`     VARCHAR(30)     NOT NULL              COMMENT '姓名',
  `phone`         VARCHAR(20)         NULL              COMMENT '手机/内线',
  `email`         VARCHAR(80)         NULL,
  `unit_id`       BIGINT UNSIGNED     NULL              COMMENT '所属单位',
  `dept_id`       BIGINT UNSIGNED     NULL              COMMENT '所属部门',
  `role_code`     VARCHAR(32)     NOT NULL              COMMENT 'ROLE_ADMIN/ROLE_UNIT_ADMIN/ROLE_OWNER/ROLE_AUDITOR',
  `status`        TINYINT         NOT NULL DEFAULT 1    COMMENT '1 在职 0 离职',
  `last_login_at` DATETIME            NULL,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_name` (`username`),
  KEY `idx_user_unit` (`unit_id`),
  CONSTRAINT `fk_user_unit` FOREIGN KEY (`unit_id`) REFERENCES `sys_org_unit` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户（SSO 同步）';

-- -----------------------------------------------------------------------------
-- 4. 资产类型（五类：域名 / IP 地址 / 应用系统 / 服务器 / 终端设备）
-- -----------------------------------------------------------------------------
CREATE TABLE `asset_type` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `type_code`     VARCHAR(20)     NOT NULL              COMMENT 'domain/ip/app/server/terminal',
  `type_name`     VARCHAR(30)     NOT NULL              COMMENT '中文名',
  `color`         VARCHAR(16)     NOT NULL              COMMENT '类型色标，前端图表与标签使用',
  `evidence_hint` VARCHAR(120)        NULL              COMMENT '推荐佐证材料名称',
  `icon`          VARCHAR(40)         NULL              COMMENT '图标标识',
  `sort_no`       INT             NOT NULL DEFAULT 0,
  `enabled`       TINYINT         NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产类型字典';

-- -----------------------------------------------------------------------------
-- 5. 类型字段模板（驱动登记表单动态渲染与后端校验）
-- -----------------------------------------------------------------------------
CREATE TABLE `asset_type_field` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `type_code`   VARCHAR(20)     NOT NULL                COMMENT '所属类型',
  `field_key`   VARCHAR(40)     NOT NULL                COMMENT '字段键，如 beianNo',
  `field_label` VARCHAR(60)     NOT NULL                COMMENT '字段中文名',
  `field_type`  VARCHAR(16)     NOT NULL DEFAULT 'text' COMMENT 'text/select/date/number',
  `required`    TINYINT         NOT NULL DEFAULT 0      COMMENT '是否必填',
  `unique_flag` TINYINT         NOT NULL DEFAULT 0      COMMENT '是否参与唯一性校验',
  `options`     VARCHAR(255)        NULL                COMMENT 'select 选项，逗号分隔',
  `placeholder` VARCHAR(80)         NULL,
  `sort_no`     INT             NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_field_type_key` (`type_code`, `field_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产类型字段模板';

-- -----------------------------------------------------------------------------
-- 6. 资产主表
--    ident_norm = LOWER(TRIM(asset_ident))，唯一索引做查重的最后一道防线；
--    资产不做物理删除：停用/报废只改 run_status，历史记录全部保留。
-- -----------------------------------------------------------------------------
CREATE TABLE `asset` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `asset_no`        VARCHAR(20)         NULL            COMMENT '资产编号 A1001，入库后回填',
  `asset_name`      VARCHAR(80)     NOT NULL            COMMENT '资产名称/系统名称',
  `type_code`       VARCHAR(20)     NOT NULL            COMMENT '资产类型',
  `asset_ident`     VARCHAR(120)    NOT NULL            COMMENT '资产标识：域名/IP/访问地址/MAC',
  `ident_norm`      VARCHAR(120)    NOT NULL            COMMENT '规范化标识（小写去空格），唯一索引用',
  `beian_no`        VARCHAR(40)         NULL            COMMENT 'ICP 备案号',
  `ip_segment`      VARCHAR(24)         NULL            COMMENT 'IP 网段 CIDR',
  `extra_json`      TEXT                NULL            COMMENT '类型专属字段 JSON',
  `unit_id`         BIGINT UNSIGNED NOT NULL            COMMENT '归属单位',
  `dept_id`         BIGINT UNSIGNED     NULL            COMMENT '归属部门',
  `dept_name`       VARCHAR(60)         NULL            COMMENT '部门名称快照',
  `owner_id`        BIGINT UNSIGNED     NULL            COMMENT '责任人',
  `owner_name`      VARCHAR(30)     NOT NULL            COMMENT '责任人姓名快照',
  `owner_phone`     VARCHAR(20)     NOT NULL            COMMENT '联系方式',
  `deploy_location` VARCHAR(120)    NOT NULL            COMMENT '部署位置',
  `online_date`     DATE            NOT NULL            COMMENT '上线时间',
  `run_status`      VARCHAR(16)     NOT NULL DEFAULT 'active' COMMENT 'active/stopped/scrapped',
  `verify_status`   VARCHAR(16)     NOT NULL DEFAULT 'ok'     COMMENT 'ok/due/overdue',
  `verify_due_date` DATE                NULL            COMMENT '下次核对到期日',
  `remark`          VARCHAR(500)        NULL,
  `created_by`      VARCHAR(30)         NULL            COMMENT '登记人',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_asset_ident_norm` (`ident_norm`),
  UNIQUE KEY `uk_asset_no` (`asset_no`),
  KEY `idx_asset_type` (`type_code`),
  KEY `idx_asset_unit` (`unit_id`),
  KEY `idx_asset_status` (`run_status`),
  KEY `idx_asset_owner` (`owner_id`),
  KEY `idx_asset_online` (`online_date`),
  KEY `idx_asset_updated` (`updated_at`),
  KEY `idx_asset_name` (`asset_name`),
  CONSTRAINT `fk_asset_type` FOREIGN KEY (`type_code`) REFERENCES `asset_type` (`type_code`),
  CONSTRAINT `fk_asset_unit` FOREIGN KEY (`unit_id`) REFERENCES `sys_org_unit` (`id`),
  CONSTRAINT `ck_asset_extra_json` CHECK (`extra_json` IS NULL OR JSON_VALID(`extra_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='互联网资产台账';

-- -----------------------------------------------------------------------------
-- 7. 佐证材料
-- -----------------------------------------------------------------------------
CREATE TABLE `asset_attachment` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `asset_id`      BIGINT UNSIGNED NOT NULL,
  `material_type` VARCHAR(60)     NOT NULL              COMMENT '材料类型，如 域名备案证明',
  `file_name`     VARCHAR(200)    NOT NULL,
  `file_ext`      VARCHAR(16)         NULL,
  `file_size`     BIGINT          NOT NULL DEFAULT 0    COMMENT '字节',
  `content_type`  VARCHAR(100)        NULL,
  `storage_path`  VARCHAR(300)    NOT NULL              COMMENT '本地/对象存储路径',
  `md5`           CHAR(32)            NULL              COMMENT '文件校验值，防篡改',
  `uploaded_by`   VARCHAR(30)         NULL,
  `uploaded_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_att_asset` (`asset_id`),
  CONSTRAINT `fk_att_asset` FOREIGN KEY (`asset_id`) REFERENCES `asset` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产佐证材料';

-- -----------------------------------------------------------------------------
-- 8. 变更日志（只增不改不删）
-- -----------------------------------------------------------------------------
CREATE TABLE `asset_change_log` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `asset_id`      BIGINT UNSIGNED NOT NULL,
  `asset_name`    VARCHAR(80)     NOT NULL              COMMENT '名称快照，便于日志页直接展示',
  `change_type`   VARCHAR(20)     NOT NULL              COMMENT '新增/信息修改/状态变更/责任人变更/批量更新/核对',
  `field_key`     VARCHAR(40)         NULL              COMMENT '变更字段键，新增时为空',
  `field_label`   VARCHAR(60)         NULL,
  `old_value`     VARCHAR(500)        NULL,
  `new_value`     VARCHAR(500)        NULL,
  `reason`        VARCHAR(500)        NULL              COMMENT '变更原因，状态变更必填',
  `batch_no`      VARCHAR(40)         NULL              COMMENT '批量操作批次号',
  `operator_id`   BIGINT UNSIGNED     NULL,
  `operator_name` VARCHAR(30)         NULL,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_log_asset` (`asset_id`),
  KEY `idx_log_time` (`created_at`),
  KEY `idx_log_type` (`change_type`),
  CONSTRAINT `fk_log_asset` FOREIGN KEY (`asset_id`) REFERENCES `asset` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产变更日志';

-- -----------------------------------------------------------------------------
-- 9. 核对记录（季度核对，防僵尸资产）
-- -----------------------------------------------------------------------------
CREATE TABLE `asset_verify_record` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `asset_id`        BIGINT UNSIGNED NOT NULL,
  `period`          VARCHAR(10)     NOT NULL            COMMENT '核对周期，如 2026Q4',
  `sent_at`         DATETIME            NULL            COMMENT '提醒发出时间',
  `respondent_id`   BIGINT UNSIGNED     NULL,
  `respondent_name` VARCHAR(30)         NULL,
  `responded_at`    DATETIME            NULL,
  `result`          VARCHAR(20)         NULL            COMMENT 'ok 一致 / need_fix 需修改 / invalid 已失效',
  `remark`          VARCHAR(300)        NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_verify_asset_period` (`asset_id`, `period`),
  CONSTRAINT `fk_verify_asset` FOREIGN KEY (`asset_id`) REFERENCES `asset` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产核对记录';

-- -----------------------------------------------------------------------------
-- 10. 责任人变更申请
-- -----------------------------------------------------------------------------
CREATE TABLE `owner_transfer` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `asset_id`      BIGINT UNSIGNED NOT NULL,
  `from_user_id`  BIGINT UNSIGNED     NULL,
  `from_name`     VARCHAR(30)         NULL,
  `to_user_id`    BIGINT UNSIGNED     NULL,
  `to_name`       VARCHAR(30)     NOT NULL,
  `apply_reason`  VARCHAR(300)        NULL,
  `status`        VARCHAR(16)     NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected/cancelled',
  `applicant_id`  BIGINT UNSIGNED     NULL,
  `approver_id`   BIGINT UNSIGNED     NULL,
  `approved_at`   DATETIME            NULL,
  `effective_at`  DATETIME            NULL,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_transfer_asset` (`asset_id`),
  KEY `idx_transfer_status` (`status`),
  CONSTRAINT `fk_transfer_asset` FOREIGN KEY (`asset_id`) REFERENCES `asset` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='责任人变更申请';

-- -----------------------------------------------------------------------------
-- 11. 审计日志（导出、批量、报废恢复等敏感操作）
-- -----------------------------------------------------------------------------
CREATE TABLE `sys_audit_log` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED     NULL,
  `user_name`   VARCHAR(30)         NULL,
  `action`      VARCHAR(40)     NOT NULL            COMMENT 'EXPORT_ASSET / BATCH_STATUS / RESUME_SCRAPPED ...',
  `target_type` VARCHAR(30)         NULL,
  `target_id`   VARCHAR(40)         NULL,
  `detail`      VARCHAR(500)        NULL,
  `ip`          VARCHAR(64)         NULL,
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_time` (`created_at`),
  KEY `idx_audit_action` (`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';

-- -----------------------------------------------------------------------------
-- 12. 资产明细视图（统计 / 导出统一口径）
-- -----------------------------------------------------------------------------
CREATE VIEW `v_asset_detail` AS
SELECT
  a.`id`,
  a.`asset_no`                                   AS asset_no,
  a.`asset_name`                                 AS asset_name,
  a.`type_code`                                  AS type_code,
  t.`type_name`                                  AS type_name,
  a.`asset_ident`                                AS asset_ident,
  a.`beian_no`                                   AS beian_no,
  a.`ip_segment`                                 AS ip_segment,
  a.`run_status`                                 AS run_status,
  a.`verify_status`                              AS verify_status,
  a.`online_date`                                AS online_date,
  u.`id`                                         AS unit_id,
  u.`unit_name`                                  AS unit_name,
  COALESCE(d.`dept_name`, a.`dept_name`)         AS dept_name,
  a.`owner_id`                                   AS owner_id,
  a.`owner_name`                                 AS owner_name,
  a.`owner_phone`                                AS owner_phone,
  a.`deploy_location`                            AS deploy_location,
  a.`updated_at`                                 AS updated_at,
  (SELECT COUNT(*) FROM `asset_attachment` x WHERE x.`asset_id` = a.`id`) AS attach_count
FROM `asset` a
JOIN `asset_type`   t ON t.`type_code` = a.`type_code`
JOIN `sys_org_unit` u ON u.`id`        = a.`unit_id`
LEFT JOIN `sys_dept` d ON d.`id`       = a.`dept_id`;

SET FOREIGN_KEY_CHECKS = 1;

SELECT '✅ schema created' AS result,
       (SELECT COUNT(*) FROM information_schema.tables
        WHERE table_schema='asset_registry' AND table_type='BASE TABLE') AS tables,
       (SELECT COUNT(*) FROM information_schema.views
        WHERE table_schema='asset_registry') AS views;
