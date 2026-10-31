# 互联网资产与信息系统管理平台

> Internet Asset & Information System Registry — 需求规格说明书（SRS）+ 可交互 UI 原型 + Spring Boot 后端 + MySQL 库表

一套面向单位内部互联网资产治理的「登记 → 维护 → 统计」闭环方案。本仓库覆盖交付全链路：**需求规格说明书**（A4 排版，PDF + 可编辑 HTML）、**零依赖可交互原型**（单文件 HTML，双击即可运行）、**MySQL 库表设计**（11 表 + 1 视图）与 **Spring Boot 后端**（Java 21 / Spring Boot 3.5，25 项功能接口）。

![资产台账](docs/screenshots/ledger.png)

---

## 目录

- [项目简介](#项目简介)
- [在线预览](#在线预览)
- [目录结构](#目录结构)
- [快速开始](#快速开始)
- [功能覆盖](#功能覆盖)
- [设计系统](#设计系统)
- [需求规格说明书](#需求规格说明书)
- [后端服务](#后端服务)
- [数据库设计](#数据库设计)
- [技术实现](#技术实现)
- [待确认事项](#待确认事项)

---

## 项目简介

单位内的互联网资产（域名、IP、应用系统、服务器、终端设备）多分散在各单位、以 Excel 台账人工维护，普遍存在**登记口径不一、重复登记、责任人不明、状态更新滞后、停用报废资产长期"挂在账上"、统计口径无法支撑核查与审计**等问题。

本项目把原始业务需求翻译为一套可落地的产品方案，分三层闭环：

| 层次 | 目标 | 关键机制 |
| --- | --- | --- |
| 登记标准化 | 源头保证数据真实、合规、不冗余 | 五类资产独立字段模板 + 佐证材料上传 + IP/域名实时查重拦截 |
| 维护全周期 | 责任可追溯，杜绝"僵尸资产" | 启用/停用/报废状态流转、变更四要素留痕、批量操作、隔离管理、季度核对 |
| 查询多维化 | 支撑决策与外部核查 | 五维度组合筛选、模糊 + 精准检索、可视化图表、标准报表导出 |

## 在线预览

- **UI 原型（交互演示）**：https://aimiliya825.github.io/internet-asset-registry/
- **需求规格说明书（HTML 版）**：[在线查看](https://aimiliya825.github.io/internet-asset-registry/%E9%9C%80%E6%B1%82%E8%A7%84%E6%A0%BC%E8%AF%B4%E6%98%8E%E4%B9%A6.html)

也可用 URL hash 直达原型指定页面：

```
#dashboard   资产全景工作台
#register    资产登记
#ledger      资产台账
#maintain    更新维护
#stats       统计报表
```

例如：https://aimiliya825.github.io/internet-asset-registry/#register

## 目录结构

```
.
├─ index.html                     # 可交互 UI 原型（单文件，零外部依赖）
├─ 需求规格说明书.html             # 需求规格说明书（A4 屏显版，可直接编辑）
├─ 需求规格说明书.pdf              # 需求规格说明书（定稿 PDF）
├─ docs/
│  └─ screenshots/                # 五个页面的界面截图
└─ server/                        # Spring Boot 后端 + MySQL 库表
   ├─ db/
   │  ├─ 01_schema.sql            # 建库建表（11 表 + 1 视图，可重复执行）
   │  └─ 02_data.sql              # 示例数据（与原型同口径）
   ├─ src/main/java/com/asset/registry/
   │  ├─ web/                     # 控制器：资产 / 维护 / 统计
   │  ├─ service/                 # 业务：登记查重、状态机、留痕、核对、变更
   │  ├─ repository/              # JPA 仓储
   │  ├─ entity/                  # 实体（脚本生成，无 Lombok）
   │  ├─ dto/                     # Payloads 入参校验 / Views 出参
   │  ├─ common/                  # 统一响应、分页、异常、状态枚举
   │  └─ config/                  # 用户上下文、CORS、JSON 序列化
   ├─ src/test/                   # H2 端到端冒烟测试
   ├─ tools/gen_entities.py       # 实体生成脚本
   ├─ pom.xml
   └─ README.md                   # 后端文档（接口清单、设计取舍、实测结果）
```

## 快速开始

```bash
git clone https://github.com/aimiliya825/internet-asset-registry.git
cd internet-asset-registry
# 直接双击 index.html，或用任意静态服务器
python -m http.server 8080
```

原型不依赖任何 CDN、图标库或构建工具，离线打开即可完整交互（图标为内联 SVG，图表为 SVG + CSS 绘制）。

## 功能覆盖

### 资产登记上传

- 五类资产（域名 / IP 地址 / 应用系统 / 服务器 / 终端设备）类型卡片切换，**表单字段、必填规则、材料要求同步变更**；
- 核心字段分组呈现：登记信息 / 责任与管理 / 时间与位置；
- **实时唯一性校验**：输入 IP、域名、MAC 即比对全量台账，命中重复时红色提示「重复对象 + 归属单位 + 当前状态」并阻止提交；
- 佐证材料拖拽上传（域名备案证明、IP 分配文件、系统上线审批表、设备采购合同）；
- 右侧常驻「唯一性校核」面板，实时反映可否提交；四条步骤条指引登记进度。

![资产登记](docs/screenshots/register.png)

### 资产更新维护

- 状态变更弹窗：启用 / 停用 / 报废三态卡片式选择，**变更原因必填**，提交即生成变更日志；
- 变更日志记录变更人、时间、类型、前后值、原因，在资产详情中以时间线呈现完整生命周期；
- 批量修改（归属单位/部门/责任人/部署位置）与批量状态更新，执行前展示影响条数；
- 停用、报废资产自动进入「停用与报废」隔离视图，退出在用统计口径但保留全部历史与材料，可恢复启用；
- 待核对资产卡片式清单 + 一键催办，超期项在台账与看板双重标记。

![更新维护](docs/screenshots/maintain.png)

### 资产统计查询

- 五维度组合筛选：类型 / 归属单位 / 运行状态 / 上线时间区间 / 负责人；
- 各单位资产数量对比柱状图（可按单位与类型切换维度）、各类资产占比环形图；
- 关键词模糊查询（名称、IP、域名、负责人、编号）＋ 编号精准定位；
- 详情抽屉：基本信息 / 佐证材料 / 变更记录三 Tab 同屏切换；
- 报表导出：单位资产清单、资产分布报表、资产状态统计表、备案核查清单，**真实生成 CSV 下载**。

![统计报表](docs/screenshots/stats.png)

### 资产全景工作台

指标卡（总数 / 在用 / 待核对 / 停用报废）随数据实时计算，维度切换图表、待办跳转与最近变更动态一屏总览。

![工作台](docs/screenshots/dashboard.png)

## 设计系统

视觉上收敛为一套令牌，保证台账、图表、详情三处状态语义一致。

| 令牌 | 取值 | 用途 |
| --- | --- | --- |
| 主色 | `#1F5EFF` / hover `#1A4FD6` / 浅底 `#EEF3FF` | 主按钮、链接、选中态、图表主序列 |
| 语义色 | 启用 `#0F9D6E` · 停用 `#C2740A` · 报废 `#5B6B82` · 危险 `#DC2626` | 状态标签、校验反馈、风险提示 |
| 类型色 | 域名 `#1F5EFF` · IP `#0F9D6E` · 应用系统 `#7C3AED` · 服务器 `#C2740A` · 终端 `#5B6B82` | 类型圆点与图表序列 |
| 中性色 | 主文 `#0F172A` · 次文 `#48566B` · 辅助 `#8A97AB` · 分隔 `#E6EAF2` · 页底 `#F4F6FB` | 文字层级与容器 |
| 字号 | 12 / 13 / 14 / 16 / 21 / 28 px | 辅助说明 → 正文 → 标题 → 页面标题 → 关键指标 |
| 间距 | 4 的倍数（4/8/12/16/24/32） | 页面节奏与组件内外边距 |
| 圆角 | 8（控件）/ 14（卡片）/ 18（弹窗） | 层级区分的视觉语言 |

可达性：正文字号 ≥ 12px、状态色均配文字标签（不单靠颜色传达）、焦点环可见、支持 `Esc` 关闭浮层与键盘遍历。

## 需求规格说明书

`需求规格说明书.pdf`（12 节 A4 排版，含封面、修订记录、目录、页脚编号）：

| 章节 | 内容 |
| --- | --- |
| 1 引言 | 编写目的、项目背景与建设目标、术语与缩略语 |
| 2 总体描述 | 系统边界、四类角色权限矩阵、运行环境与设计约束 |
| 3 功能需求 | FR-01～FR-25，含字段对应表、唯一性校验规则、状态流转规则 |
| 4 数据需求 | 资产主表字段、四类衍生实体、数据质量与保留策略 |
| 5 非功能需求 | 性能、安全、易用性、可用性、兼容性、可维护性、合规共 16 条指标 |
| 6 接口需求 | SSO、组织人员、通知、CMDB/监控、备案核验、导出 |
| 7 界面与交互规范 | 设计令牌、四层信息架构、反馈三件套、危险操作隔离 |
| 8 验收标准 | AC-01～AC-09 验收场景与通过标准 |
| 附录 A | 需求—原型追溯矩阵（FR ↔ 页面 ↔ 交互） |
| 附录 B | 5 项待业务确认事项 |

HTML 版右上角提供「打印 / 导出 PDF」按钮，修改内容后可自行重新导出。

## 后端服务

`server/` 是可直接运行的 Spring Boot 工程，实现 SRS 中 FR-01～FR-25 的全部接口。

**技术栈**：Spring Boot 3.5.16 · Spring Data JPA · MySQL 9 · Java 21 编译（JDK 24 运行）· Maven

**启动**（密码走环境变量，不落代码）：

```bash
cd server
export DB_PASSWORD='你的密码'      # Windows PowerShell: $env:DB_PASSWORD="你的密码"
mvn spring-boot:run
# 或 java -jar target/asset-registry-server.jar --server.port=8080
```

服务监听 `http://localhost:8080`，本地联调用 `X-User-Id: 1` 指定用户（接 SSO 后由网关注入）。

**接口一览**（统一响应 `{code, message, data}`，业务错误返回对应 HTTP 状态码）：

| 分组 | 接口 |
| --- | --- |
| 资产 | `GET /api/assets` 组合查询 · `GET /api/assets/{id}` 详情 · `POST /api/assets` 登记 · `PUT /api/assets/{id}` 修改 · `PATCH /api/assets/{id}/status` 状态流转 · `POST /api/assets/batch/status` 批量状态 · `POST /api/assets/batch/field` 批量改字段 · `POST /api/assets/{id}/attachments` 上传材料 |
| 维护 | `GET /api/change-logs` 变更流水 · `GET /api/verify/due` 待核对 · `POST /api/verify/{assetId}` 提交核对 · `GET /api/owner-transfers` 责任变更 · `POST /api/assets/{assetId}/owner-transfers` 申请 · `POST /api/owner-transfers/{id}/decision` 审批 |
| 统计 | `GET /api/stats/overview` 多维统计 · `GET /api/dashboard/summary` 工作台 · `GET /api/exports/assets.csv` 导出（带 BOM） |
| 字典 | `GET /api/meta` 类型/字段模板/单位/用户 · `GET /api/meta/fields` · `GET /api/me` |

**验证**：`mvn test` 用 H2（MySQL 兼容模式）跑真实 HTTP 冒烟测试，覆盖登记成功、重复标识 409、模糊查询、原因长度校验、状态流转留痕、看板与统计口径一致。本地 MySQL 9.7.0 建库后已逐一实测全部接口，明细见 [`server/README.md`](server/README.md)。

## 数据库设计

库名 `asset_registry`，11 张表 + 1 个视图，脚本在 [`server/db/`](server/db)。

```bash
cd server/db
mysql -uroot -p --default-character-set=utf8mb4 < 01_schema.sql
mysql -uroot -p --default-character-set=utf8mb4 < 02_data.sql
```

| 表 | 作用 |
| --- | --- |
| `sys_org_unit` / `sys_dept` | 归属单位 → 部门两级组织 |
| `sys_user` | SSO 同步的用户目录（只存主体标识与角色，不落密码） |
| `asset_type` / `asset_type_field` | 五类资产 + **字段模板表**，新增类型只插数据、不改代码 |
| `asset` | 台账主表，`ident_norm` 小写去空格后建**唯一索引**兜住并发重复登记 |
| `asset_attachment` | 佐证材料（含 MD5 校验值防篡改） |
| `asset_change_log` | 字段级变更日志，只增不改，`batch_no` 串联批量操作 |
| `asset_verify_record` / `owner_transfer` | 季度核对 / 责任人变更申请 |
| `sys_audit_log` | 导出、批量、报废恢复等敏感操作留痕 |

四个关键取舍：**查重下沉到数据库**（应用层提示 + 唯一索引兜并发）、**类型差异走模板表而非宽表**、**日志记字段级前后值**（一次改 5 个字段落 5 行，才能反查"谁改过部署位置"）、**不做物理删除**（停用/报废只改状态，历史全留）。

## 技术实现

**原型（`index.html`）**

- **单文件、零依赖**：无框架、无构建、无外部字体与图标，全部内联；图表以 SVG（环形图 `stroke-dasharray`）与 CSS（柱状图）手绘。
- **视图路由**：`data-view` 驱动的轻量视图切换，配合 `location.hash` 支持深链接与刷新保持。
- **交互逻辑**：类型模板配置化（`FIELDS` 字典），新增资产类型只需扩展配置；唯一性校验做 500ms 防抖；批量操作与状态变更统一走「弹窗确认 + 原因必填 + 日志写入」通道。
- **Mock 数据**：14 条资产、7 条变更日志，全部为虚构示例（`yunqi-group.com` 等域名均为占位）。

**后端（`server/`）**

- **分层**：`web → service → repository → entity`，入参校验收在 `dto/Payloads`，出参结构收在 `dto/Views`，控制器不写业务逻辑。
- **查重双保险**：应用层先查 `ident_norm` 给出友好提示（附已登记资产名称与单位），数据库唯一索引兜住并发穿透，冲突统一转 409。
- **状态机显式建模**：`RunStatus.canTransitTo()` 表达「启用 ⇄ 停用 → 报废 → 启用」三种流转，非法流转与"目标状态等于当前状态"都拒绝。
- **留痕内聚**：所有写操作经 `ChangeLogService` 落日志，一次修改多个字段就落多行，批量操作共享 `batch_no` 便于回溯同一次操作。
- **不用 Lombok**：JDK 24 下注解处理器兼容性不稳，实体由 `tools/gen_entities.py` 从 SQL 表结构生成标准 getter/setter，避免运行期踩坑。
- **表结构由脚本管理**：`ddl-auto: none`，禁止 Hibernate 运行期改生产库结构。

## 待确认事项

以下问题已写入说明书附录 B，确认后可在原型中补全对应分支：

1. 责任人变更是否需新责任人在线确认？确认超时（如 3 个工作日）后的默认策略。
2. 报废资产恢复启用是否一律需单位管理员审批，或按资产类型分级授权？
3. 域名备案号是否接入外部核验接口，或仅做格式校验 + 人工上传材料？
4. 跨单位资产调拨（设备移交）的审批流与台账归属切换时点。
5. 历史 Excel 台账批量导入模板与去重规则。

---

本项目为设计与需求交付物，不包含后端实现；如需对接真实数据，可按说明书第 6 节接口需求独立开发服务端。
