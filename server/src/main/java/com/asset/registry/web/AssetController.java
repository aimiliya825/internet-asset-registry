package com.asset.registry.web;

import com.asset.registry.common.ApiResponse;
import com.asset.registry.common.PageResult;
import com.asset.registry.dto.Payloads;
import com.asset.registry.dto.Views;
import com.asset.registry.service.AssetService;
import com.asset.registry.service.ChangeLogService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 资产登记 / 台账 / 维护接口。
 */
@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;
    private final ChangeLogService changeLogService;

    public AssetController(AssetService assetService, ChangeLogService changeLogService) {
        this.assetService = assetService;
        this.changeLogService = changeLogService;
    }

    /** 台账查询：关键词模糊 + 类型/单位/状态/时间/责任人组合筛选 */
    @GetMapping
    public ApiResponse<PageResult<Views.AssetItem>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Payloads.AssetQuery q = new Payloads.AssetQuery(keyword, type, unitId, status, from, to, owner);
        return ApiResponse.ok(assetService.search(q, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Views.AssetDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(assetService.detail(id));
    }

    /** 登记：自动查重，重复即 409 */
    @PostMapping
    public ApiResponse<Views.AssetDetail> create(@Valid @RequestBody Payloads.AssetCreate req) {
        return ApiResponse.ok(assetService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<Views.AssetDetail> update(@PathVariable Long id,
                                                 @Valid @RequestBody Payloads.AssetUpdate req) {
        return ApiResponse.ok(assetService.update(id, req));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Views.AssetDetail> changeStatus(@PathVariable Long id,
                                                       @Valid @RequestBody Payloads.StatusChange req) {
        return ApiResponse.ok(assetService.changeStatus(id, req));
    }

    @PostMapping("/batch/status")
    public ApiResponse<Map<String, Object>> batchStatus(@Valid @RequestBody Payloads.BatchStatus req) {
        int changed = assetService.batchStatus(req);
        return ApiResponse.ok(Map.of("changed", changed));
    }

    @PostMapping("/batch/field")
    public ApiResponse<Map<String, Object>> batchField(@Valid @RequestBody Payloads.BatchField req) {
        int changed = assetService.batchField(req);
        return ApiResponse.ok(Map.of("changed", changed));
    }

    @GetMapping("/{id}/logs")
    public ApiResponse<List<Views.LogItem>> logs(@PathVariable Long id) {
        return ApiResponse.ok(changeLogService.ofAsset(id));
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Views.AttachmentItem> upload(@PathVariable Long id,
                                                    @RequestPart("file") MultipartFile file,
                                                    @RequestParam(required = false) String materialType) {
        return ApiResponse.ok(assetService.upload(id, file, materialType));
    }
}
