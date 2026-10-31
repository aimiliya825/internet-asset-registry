-- =============================================================================
--  互联网资产与信息系统管理平台 —— 初始化数据
--  执行：mysql -uroot -p < 02_data.sql
--  说明：示例数据与时序与 UI 原型一致（虚构单位与域名），可直接用于接口联调。
-- =============================================================================
SET NAMES utf8mb4;
USE `asset_registry`;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------- 归属单位 ----------------------------
INSERT INTO `sys_org_unit` (`id`,`unit_code`,`unit_name`,`short_name`,`sort_no`) VALUES
 (1,'HQ','集团总部','总部',1),
 (2,'IT','信息技术部','信息',2),
 (3,'BIZ','业务运营部','业务',3),
 (4,'FIN','财务部','财务',4),
 (5,'HR','人力资源部','人资',5),
 (6,'EAST','华东分公司','华东',6),
 (7,'SOUTH','华南分公司','华南',7);

-- ---------------------------- 部门 ----------------------------
INSERT INTO `sys_dept` (`id`,`unit_id`,`dept_name`,`sort_no`) VALUES
 (1 ,2,'网站运维组',1),
 (2 ,2,'应用架构组',2),
 (3 ,2,'网络组',3),
 (4 ,2,'数据库组',4),
 (5 ,2,'基础平台组',5),
 (6 ,2,'基础设施组',6),
 (7 ,3,'客户服务中心',1),
 (8 ,3,'研发支持组',2),
 (9 ,1,'行政部',1),
 (10,1,'办公室',2),
 (11,4,'财务信息化岗',1),
 (12,5,'薪酬绩效组',1),
 (13,5,'招聘组',2),
 (14,6,'综合管理部',1),
 (15,7,'技术支撑部',1);

-- ---------------------------- 用户（SSO 同步目录） ----------------------------
INSERT INTO `sys_user` (`id`,`username`,`external_id`,`real_name`,`phone`,`email`,`unit_id`,`dept_id`,`role_code`) VALUES
 (1,'limubai'  ,'sso-10001','李慕白','13800001000','limubai@yunqi-group.com' ,1,10,'ROLE_ADMIN'),
 (2,'zhangming','sso-10002','张明'  ,'13800002210','zhangming@yunqi-group.com',2, 1,'ROLE_OWNER'),
 (3,'wanglin'  ,'sso-10003','王琳'  ,'13700005566','wanglin@yunqi-group.com'  ,2, 2,'ROLE_UNIT_ADMIN'),
 (4,'chenhao'  ,'sso-10004','陈昊'  ,'13500003388','chenhao@yunqi-group.com'  ,2, 3,'ROLE_OWNER'),
 (5,'zhaoqian' ,'sso-10005','赵倩'  ,'13600007712','zhaoqian@yunqi-group.com' ,4,11,'ROLE_OWNER'),
 (6,'sunwei'   ,'sso-10006','孙伟'  ,'13900004411','sunwei@yunqi-group.com'   ,6,14,'ROLE_OWNER'),
 (7,'zhouhang' ,'sso-10007','周航'  ,'13700009902','zhouhang@yunqi-group.com' ,3, 7,'ROLE_OWNER'),
 (8,'liufang'  ,'sso-10008','刘芳'  ,'13800006633','liufang@yunqi-group.com'  ,1, 9,'ROLE_OWNER'),
 (9,'wumin'    ,'sso-10009','吴敏'  ,'13500002255','wumin@yunqi-group.com'    ,5,12,'ROLE_OWNER'),
 (10,'auditor' ,'sso-10010','审计观察员','13000000000','audit@yunqi-group.com' ,1,10,'ROLE_AUDITOR');

-- ---------------------------- 资产类型 ----------------------------
INSERT INTO `asset_type` (`id`,`type_code`,`type_name`,`color`,`evidence_hint`,`icon`,`sort_no`) VALUES
 (1,'domain'  ,'域名'    ,'#1F5EFF','域名备案证明'       ,'globe'   ,1),
 (2,'ip'      ,'IP 地址' ,'#0F9D6E','IP 分配文件'        ,'network' ,2),
 (3,'app'     ,'应用系统','#7C3AED','系统上线审批表'     ,'app'     ,3),
 (4,'server'  ,'服务器'  ,'#C2740A','设备采购合同'       ,'server'  ,4),
 (5,'terminal','终端设备','#5B6B82','设备采购合同 / 领用单','monitor',5);

-- ---------------------------- 类型字段模板 ----------------------------
INSERT INTO `asset_type_field` (`type_code`,`field_key`,`field_label`,`field_type`,`required`,`unique_flag`,`options`,`placeholder`,`sort_no`) VALUES
 ('domain','beianNo'  ,'域名备案号'  ,'text'  ,1,0,NULL                                   ,'如：沪ICP备2024012388号',1),
 ('domain','resolveIp','解析 IP 地址','text'  ,1,1,NULL                                   ,'如：10.20.31.5'         ,2),
 ('domain','registrar','域名注册商'  ,'text'  ,0,0,NULL                                   ,'如：阿里云'             ,3),

 ('ip'    ,'ipSegment','IP 网段'     ,'text'  ,1,0,NULL                                   ,'如：10.20.31.0/24'      ,1),
 ('ip'    ,'allocType','分配方式'    ,'select',1,0,'静态分配,DHCP 动态,NAT 映射'           ,NULL                     ,2),
 ('ip'    ,'usage'    ,'使用用途'    ,'text'  ,0,0,NULL                                   ,'如：对外 API 出口'      ,3),

 ('app'   ,'shortName','系统简称'    ,'text'  ,0,0,NULL                                   ,'如：SSO'                ,1),
 ('app'   ,'level'    ,'等级保护级别','select',1,0,'一级,二级,三级,四级'                   ,NULL                     ,2),
 ('app'   ,'vendor'   ,'开发/运维方' ,'text'  ,0,0,NULL                                   ,'如：自研 / 某某科技'    ,3),

 ('server','os'       ,'操作系统'    ,'select',1,0,'CentOS 7.9,Ubuntu 22.04,Windows Server 2019,麒麟 V10',NULL,1),
 ('server','sn'       ,'资产编号 / S/N','text',0,0,NULL                                  ,'如：SN-2024-00187'      ,2),
 ('server','spec'     ,'配置规格'    ,'text'  ,0,0,NULL                                   ,'如：16C / 64G / 2T SSD' ,3),

 ('terminal','deviceType','设备类型'  ,'select',1,0,'台式机,笔记本,打印机,视频会议终端,移动终端',NULL           ,1),
 ('terminal','mac'     ,'MAC 地址'   ,'text'  ,1,1,NULL                                   ,'如：8C:16:45:9D:22:A1'  ,2),
 ('terminal','user'    ,'使用人'     ,'text'  ,0,0,NULL                                   ,'如：吴敏'               ,3);

-- ---------------------------- 资产台账 ----------------------------
INSERT INTO `asset`
 (`id`,`asset_no`,`asset_name`,`type_code`,`asset_ident`,`ident_norm`,`beian_no`,`ip_segment`,`extra_json`,
  `unit_id`,`dept_id`,`dept_name`,`owner_id`,`owner_name`,`owner_phone`,`deploy_location`,`online_date`,
  `run_status`,`verify_status`,`verify_due_date`,`remark`,`created_by`,`created_at`,`updated_at`) VALUES
 (1,'A1001','集团门户网站','domain','www.yunqi-group.com','www.yunqi-group.com','沪ICP备2024012388号',NULL,
  '{"beianNo":"沪ICP备2024012388号","resolveIp":"10.20.31.5","registrar":"阿里云"}',
  2,1,'网站运维组',2,'张明','13800002210','公有云（华东）','2024-03-12','active','ok','2027-03-31','对外统一门户','李慕白','2024-03-12 09:20:00','2026-10-18 09:15:00'),
 (2,'A1002','统一身份认证系统','app','https://sso.yunqi-group.com','https://sso.yunqi-group.com',NULL,NULL,
  '{"shortName":"SSO","level":"三级","vendor":"自研"}',
  2,2,'应用架构组',3,'王琳','13700005566','自建机房 A-03','2024-07-01','active','due','2026-09-30','全公司统一登录入口','李慕白','2024-07-01 14:05:00','2026-10-12 14:03:00'),
 (3,'A1003','对外业务 API 网关','ip','10.20.31.5','10.20.31.5',NULL,'10.20.31.0/24',
  '{"ipSegment":"10.20.31.0/24","allocType":"静态分配","usage":"对外 API 出口"}',
  2,3,'网络组',4,'陈昊','13500003388','自建机房 A-01 / 机柜07','2024-09-20','active','ok','2027-01-31',NULL,'李慕白','2024-09-20 10:00:00','2026-09-28 17:55:00'),
 (4,'A1004','核心数据库服务器','server','10.20.31.21','10.20.31.21',NULL,NULL,
  '{"os":"CentOS 7.9","sn":"SN-2023-00412","spec":"32C / 128G / 4T SSD"}',
  2,4,'数据库组',4,'陈昊','13500003388','自建机房 A-02 / 机柜03','2023-11-08','active','ok','2027-01-31','承载核心业务库','李慕白','2023-11-08 16:30:00','2026-10-08 11:12:00'),
 (5,'A1005','财务报销系统','app','https://fin.yunqi-group.com','https://fin.yunqi-group.com',NULL,NULL,
  '{"shortName":"FIN","level":"二级","vendor":"某某科技"}',
  4,11,'财务信息化岗',5,'赵倩','13600007712','自建机房 A-03','2025-01-15','active','ok','2027-01-31',NULL,'李慕白','2025-01-15 09:00:00','2026-10-20 16:42:00'),
 (6,'A1006','华东分公司出口 IP 段','ip','10.88.0.0/24','10.88.0.0/24',NULL,'10.88.0.0/24',
  '{"ipSegment":"10.88.0.0/24","allocType":"静态分配","usage":"分公司出口"}',
  6,14,'综合管理部',6,'孙伟','13900004411','华东分公司机房','2025-03-06','active','due','2026-09-30',NULL,'李慕白','2025-03-06 11:20:00','2026-08-19 10:05:00'),
 (7,'A1007','企业邮箱系统','domain','mail.yunqi-group.com','mail.yunqi-group.com','沪ICP备2024012388号',NULL,
  '{"beianNo":"沪ICP备2024012388号","resolveIp":"10.20.31.66","registrar":"阿里云"}',
  2,5,'基础平台组',2,'张明','13800002210','公有云（华北）','2024-05-22','active','ok','2027-01-31',NULL,'李慕白','2024-05-22 10:10:00','2026-07-30 15:00:00'),
 (8,'A1008','客服工单系统','app','https://ticket.yunqi-group.com','https://ticket.yunqi-group.com',NULL,NULL,
  '{"shortName":"TICKET","level":"二级","vendor":"SaaS"}',
  3,7,'客户服务中心',7,'周航','13700009902','公有云（华东）','2025-06-11','active','ok','2027-01-31',NULL,'李慕白','2025-06-11 13:40:00','2026-09-14 09:30:00'),
 (9,'A1009','视频会议终端 VC-02','terminal','MAC 3C:A9:F4:2B:71:0E','mac 3c:a9:f4:2b:71:0e',NULL,NULL,
  '{"deviceType":"视频会议终端","mac":"3C:A9:F4:2B:71:0E","user":"刘芳"}',
  1,9,'行政部',8,'刘芳','13800006633','总部 12F 会议室','2025-02-18','active','ok','2027-01-31',NULL,'李慕白','2025-02-18 15:20:00','2026-06-21 10:00:00'),
 (10,'A1010','办公终端 PC-2024-0187','terminal','MAC 8C:16:45:9D:22:A1','mac 8c:16:45:9d:22:a1',NULL,NULL,
  '{"deviceType":"台式机","mac":"8C:16:45:9D:22:A1","user":"吴敏"}',
  5,12,'薪酬绩效组',9,'吴敏','13500002255','总部 9F 工位 A-12','2024-12-03','active','ok','2027-01-31',NULL,'李慕白','2024-12-03 09:45:00','2026-05-16 16:20:00'),
 (11,'A1011','备份存储服务器 BK-02','server','10.20.31.40','10.20.31.40',NULL,NULL,
  '{"os":"CentOS 7.6","sn":"SN-2022-00088","spec":"8C / 32G / 24T HDD"}',
  2,6,'基础设施组',4,'陈昊','13500003388','自建机房 A-02 / 机柜05','2022-08-04','stopped','ok','2027-01-31','设备老化待替换，已隔离','李慕白','2022-08-04 10:00:00','2026-09-05 10:08:00'),
 (12,'A1012','旧版 OA 协同办公系统','app','https://oa-old.yunqi-group.com','https://oa-old.yunqi-group.com',NULL,NULL,
  '{"shortName":"OA-OLD","level":"二级","vendor":"某某软件"}',
  1,10,'办公室',8,'刘芳','13800006633','已下线','2019-04-10','scrapped','ok','2026-06-30','业务迁移至新协同平台','李慕白','2019-04-10 09:00:00','2026-07-18 15:31:00'),
 (13,'A1013','招聘管理平台','domain','careers.yunqi-group.com','careers.yunqi-group.com','沪ICP备2024012389号',NULL,
  '{"beianNo":"沪ICP备2024012389号","resolveIp":"10.20.31.88","registrar":"腾讯云"}',
  5,13,'招聘组',9,'吴敏','13500002255','公有云（华东）','2026-04-08','active','due','2026-09-30',NULL,'李慕白','2026-04-08 11:00:00','2026-10-09 11:20:00'),
 (14,'A1014','测试环境应用服务器 T-05','server','10.20.44.15','10.20.44.15',NULL,NULL,
  '{"os":"Ubuntu 22.04","sn":"SN-2021-01023","spec":"4C / 16G / 500G"}',
  3,8,'研发支持组',7,'周航','13700009902','已下线','2021-10-21','scrapped','ok','2026-03-31','随测试环境迁移下线','李慕白','2021-10-21 14:00:00','2026-03-11 09:10:00');

-- ---------------------------- 佐证材料 ----------------------------
INSERT INTO `asset_attachment` (`asset_id`,`material_type`,`file_name`,`file_ext`,`file_size`,`content_type`,`storage_path`,`md5`,`uploaded_by`,`uploaded_at`) VALUES
 (1 ,'域名备案证明','集团门户网站-ICP备案证明.pdf'  ,'pdf',1258291,'application/pdf','storage/2026/03/A1001/icp.pdf'      ,'9f2c1a4b6d8e0f3a5b7c9d1e2f4a6b8c','李慕白','2024-03-12 09:22:00'),
 (1 ,'责任人确认单','集团门户网站-责任人确认单.pdf','pdf', 395264,'application/pdf','storage/2026/03/A1001/confirm.pdf'  ,'1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d','李慕白','2026-03-31 10:00:00'),
 (2 ,'系统上线审批表','统一身份认证系统-上线审批表.pdf','pdf', 874112,'application/pdf','storage/2024/07/A1002/online.pdf' ,'2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e','李慕白','2024-07-01 14:10:00'),
 (3 ,'IP 分配文件'  ,'API网关-IP分配单.pdf'            ,'pdf', 512000,'application/pdf','storage/2024/09/A1003/ip.pdf'     ,'3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f','李慕白','2024-09-20 10:05:00'),
 (4 ,'设备采购合同' ,'核心数据库服务器-采购合同.pdf'  ,'pdf',2140000,'application/pdf','storage/2023/11/A1004/contract.pdf','4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a','李慕白','2023-11-08 16:35:00'),
 (5 ,'系统上线审批表','财务报销系统-上线审批表.pdf'  ,'pdf', 640000,'application/pdf','storage/2025/01/A1005/online.pdf' ,'5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b','李慕白','2025-01-15 09:05:00'),
 (6 ,'IP 分配文件'  ,'华东分公司-IP分配单.pdf'        ,'pdf', 430000,'application/pdf','storage/2025/03/A1006/ip.pdf'     ,'6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c','李慕白','2025-03-06 11:25:00'),
 (7 ,'域名备案证明','企业邮箱-ICP备案证明.pdf'        ,'pdf',1190000,'application/pdf','storage/2024/05/A1007/icp.pdf'    ,'7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d','李慕白','2024-05-22 10:15:00'),
 (8 ,'系统上线审批表','客服工单系统-上线审批表.pdf'  ,'pdf', 720000,'application/pdf','storage/2025/06/A1008/online.pdf' ,'8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e','李慕白','2025-06-11 13:45:00'),
 (9 ,'设备采购合同' ,'视频会议终端-采购合同.pdf'    ,'pdf',1560000,'application/pdf','storage/2025/02/A1009/contract.pdf','9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f','李慕白','2025-02-18 15:25:00'),
 (10,'设备采购合同' ,'办公终端-采购合同.pdf'          ,'pdf', 980000,'application/pdf','storage/2024/12/A1010/contract.pdf','0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a','李慕白','2024-12-03 09:50:00'),
 (13,'域名备案证明','招聘平台-ICP备案证明.pdf'        ,'pdf',1320000,'application/pdf','storage/2026/04/A1013/icp.pdf'    ,'1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b','李慕白','2026-04-08 11:05:00');

-- ---------------------------- 变更日志 ----------------------------
INSERT INTO `asset_change_log` (`asset_id`,`asset_name`,`change_type`,`field_key`,`field_label`,`old_value`,`new_value`,`reason`,`operator_id`,`operator_name`,`created_at`) VALUES
 (1 ,'集团门户网站'        ,'状态变更','runStatus','运行状态','停用','启用','版本升级完成，恢复对外服务',1,'张明','2026-10-18 09:15:00'),
 (5 ,'财务报销系统'        ,'信息修改','deployLocation','部署位置','自建机房 A-02','自建机房 A-03','机房资源调整',5,'赵倩','2026-10-20 16:42:00'),
 (2 ,'统一身份认证系统'    ,'信息修改','ownerId','责任人','李强','王琳','岗位调整，附交接说明',3,'王琳','2026-10-12 14:03:00'),
 (13,'招聘管理平台'        ,'信息修改','ownerPhone','联系方式','13500002255','13500002255','更新为最新内线',9,'吴敏','2026-10-09 11:20:00'),
 (4 ,'核心数据库服务器'    ,'批量更新','deployLocation','部署位置','A-01','A-02','机房扩容，服务器类资产统一迁移',4,'陈昊','2026-09-28 17:55:00'),
 (11,'备份存储服务器 BK-02','状态变更','runStatus','运行状态','启用','停用','设备老化待替换，先行停用并隔离',4,'陈昊','2026-09-05 10:08:00'),
 (12,'旧版 OA 协同办公系统','状态变更','runStatus','运行状态','停用','报废','业务已迁移至新协同平台，资产报废',8,'刘芳','2026-07-18 15:31:00');

-- ---------------------------- 核对记录 ----------------------------
INSERT INTO `asset_verify_record` (`asset_id`,`period`,`sent_at`,`respondent_id`,`respondent_name`,`responded_at`,`result`,`remark`) VALUES
 (2 ,'2026Q4','2026-09-30 08:00:00',3,'王琳',NULL,NULL,'已推送站内信 + 短信，待确认'),
 (6 ,'2026Q4','2026-09-30 08:00:00',6,'孙伟',NULL,NULL,'已推送站内信 + 短信，待确认'),
 (13,'2026Q4','2026-09-30 08:00:00',9,'吴敏',NULL,NULL,'已推送站内信 + 短信，待确认'),
 (1 ,'2026Q3','2026-06-30 08:00:00',2,'张明','2026-07-02 09:10:00','ok','信息与实际情况一致'),
 (8 ,'2026Q3','2026-06-30 08:00:00',7,'周航','2026-07-01 15:30:00','ok','信息与实际情况一致');

-- ---------------------------- 责任人变更申请 ----------------------------
INSERT INTO `owner_transfer` (`asset_id`,`from_user_id`,`from_name`,`to_user_id`,`to_name`,`apply_reason`,`status`,`applicant_id`,`approver_id`,`approved_at`,`effective_at`,`created_at`) VALUES
 (11,4,'陈昊',NULL,'待指派','原责任人调岗，需明确停用资产责任人','pending',3,NULL,NULL,NULL,'2026-10-08 10:20:00');

-- ---------------------------- 审计日志 ----------------------------
INSERT INTO `sys_audit_log` (`user_id`,`user_name`,`action`,`target_type`,`target_id`,`detail`,`ip`,`created_at`) VALUES
 (1,'李慕白','EXPORT_ASSET','ASSET','ALL','导出全量资产台账，共 14 条','127.0.0.1','2026-10-20 17:02:00'),
 (3,'王琳'  ,'BATCH_STATUS','ASSET','6','批量状态更新为停用，共 2 条','127.0.0.1','2026-10-12 15:00:00');

SET FOREIGN_KEY_CHECKS = 1;

SELECT '✅ data loaded' AS result,
       (SELECT COUNT(*) FROM `sys_org_unit`)   AS units,
       (SELECT COUNT(*) FROM `sys_user`)       AS users,
       (SELECT COUNT(*) FROM `asset_type`)     AS types,
       (SELECT COUNT(*) FROM `asset`)          AS assets,
       (SELECT COUNT(*) FROM `asset_attachment`) AS attachments,
       (SELECT COUNT(*) FROM `asset_change_log`) AS logs;
