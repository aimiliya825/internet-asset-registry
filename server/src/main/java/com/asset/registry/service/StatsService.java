package com.asset.registry.service;

import com.asset.registry.common.RunStatus;
import com.asset.registry.dto.Views;
import com.asset.registry.entity.AssetType;
import com.asset.registry.entity.OrgUnit;
import com.asset.registry.repository.AssetRepository;
import com.asset.registry.repository.OwnerTransferRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 统计与看板：多维聚合、图表序列、工作台待办。
 */
@Service
public class StatsService {

    private final AssetRepository assetRepository;
    private final OwnerTransferRepository transferRepository;
    private final MetaService metaService;
    private final ChangeLogService changeLogService;

    public StatsService(AssetRepository assetRepository, OwnerTransferRepository transferRepository,
                        MetaService metaService, ChangeLogService changeLogService) {
        this.assetRepository = assetRepository;
        this.transferRepository = transferRepository;
        this.metaService = metaService;
        this.changeLogService = changeLogService;
    }

    /** 多维统计：dim = type | unit | status */
    public Views.Stats overview(String dim, Long unitId, LocalDate from, LocalDate to) {
        String d = (dim == null || dim.isBlank()) ? "type" : dim.trim().toLowerCase();
        List<Object[]> rows = switch (d) {
            case "unit" -> assetRepository.groupByUnit(unitId, from, to);
            case "status" -> assetRepository.groupByStatus(unitId, from, to);
            default -> assetRepository.groupByType(unitId, from, to);
        };
        Map<String, AssetType> types = metaService.typeIndex();
        Map<Long, OrgUnit> units = metaService.unitIndex();

        List<Views.SeriesItem> series = new ArrayList<>();
        for (Object[] r : rows) {
            String key = String.valueOf(r[0]);
            long value = ((Number) r[1]).longValue();
            if ("unit".equals(d)) {
                OrgUnit u = units.get(Long.valueOf(key));
                series.add(new Views.SeriesItem(key, u == null ? key : u.getUnitName(), value, "#1F5EFF"));
            } else if ("status".equals(d)) {
                series.add(new Views.SeriesItem(key, RunStatus.of(key).getLabel(), value, statusColor(key)));
            } else {
                AssetType t = types.get(key);
                series.add(new Views.SeriesItem(key, t == null ? key : t.getTypeName(), value,
                        t == null ? "#5B6B82" : t.getColor()));
            }
        }
        series.sort(Comparator.comparingLong(Views.SeriesItem::value).reversed());

        long total = series.stream().mapToLong(Views.SeriesItem::value).sum();
        long unitCount = assetRepository.groupByUnit(unitId, from, to).size();
        long typeCount = assetRepository.groupByType(unitId, from, to).size();
        long all = assetRepository.count();
        double rate = all == 0 ? 0d : Math.round(assetRepository.countWithAttachment() * 1000d / all) / 10d;

        return new Views.Stats(d, total, series, unitCount, typeCount, rate);
    }

    /** 工作台聚合 */
    public Views.Dashboard dashboard() {
        long total = assetRepository.count();
        long active = assetRepository.countByRunStatus(RunStatus.ACTIVE.getCode());
        long due = assetRepository.countByVerifyStatus("due") + assetRepository.countByVerifyStatus("overdue");
        long dead = total - active;
        double activeRate = total == 0 ? 0d : Math.round(active * 1000d / total) / 10d;

        List<Views.SeriesItem> byType = overview("type", null, null, null).series();
        List<Views.SeriesItem> byUnit = overview("unit", null, null, null).series();
        List<Views.SeriesItem> byStatus = overview("status", null, null, null).series();

        List<Views.LogItem> logs = changeLogService.timeline(null);
        List<Views.LogItem> recent = logs.size() > 6 ? logs.subList(0, 6) : logs;

        List<Views.TodoItem> todos = new ArrayList<>();
        if (due > 0) {
            todos.add(new Views.TodoItem("warn", due + " 项资产信息待核对",
                    "系统已推送核对提醒，超期项需单位管理员介入", "去核对", "verify"));
        }
        long pendingTransfers = transferRepository.findByStatusOrderByCreatedAtDesc("pending").size();
        if (pendingTransfers > 0) {
            todos.add(new Views.TodoItem("info", pendingTransfers + " 项责任人变更待处理",
                    "接收人确认后方可生效，期间责任仍在原责任人", "去处理", "transfer"));
        }
        if (dead > 0) {
            todos.add(new Views.TodoItem("info", dead + " 项资产处于停用/报废隔离",
                    "已退出在用统计口径，历史记录与材料完整保留", "查看隔离清单", "ledger"));
        }
        if (due == 0 && pendingTransfers == 0) {
            todos.add(new Views.TodoItem("ok", "本季度核对已全部完成", "台账信息与实际情况一致", "查看看板", "stats"));
        }

        return new Views.Dashboard(total, active, due, dead, activeRate,
                byType, byUnit, byStatus, recent, todos);
    }

    static String statusColor(String code) {
        return switch (code) {
            case "active" -> "#0F9D6E";
            case "stopped" -> "#C2740A";
            default -> "#5B6B82";
        };
    }
}
