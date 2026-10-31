package com.asset.registry.web;

import com.asset.registry.common.ApiResponse;
import com.asset.registry.dto.Payloads;
import com.asset.registry.dto.Views;
import com.asset.registry.service.AssetService;
import com.asset.registry.service.StatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 统计报表、工作台看板与导出。
 */
@RestController
@RequestMapping("/api")
public class StatsController {

    private final StatsService statsService;
    private final AssetService assetService;

    public StatsController(StatsService statsService, AssetService assetService) {
        this.statsService = statsService;
        this.assetService = assetService;
    }

    @GetMapping("/stats/overview")
    public ApiResponse<Views.Stats> overview(
            @RequestParam(defaultValue = "type") String dim,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(statsService.overview(dim, unitId, from, to));
    }

    @GetMapping("/dashboard/summary")
    public ApiResponse<Views.Dashboard> dashboard() {
        return ApiResponse.ok(statsService.dashboard());
    }

    /** 导出当前筛选结果，CSV 带 BOM，Excel 直接打开不乱码 */
    @GetMapping("/exports/assets.csv")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Payloads.AssetQuery q = new Payloads.AssetQuery(keyword, type, unitId, status, from, to, owner);
        byte[] data = assetService.exportCsv(q);
        String name = URLEncoder.encode("互联网资产台账_" + LocalDate.now() + ".csv", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + name)
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(data);
    }
}
