package com.asset.registry.web;

import com.asset.registry.common.ApiResponse;
import com.asset.registry.dto.Payloads;
import com.asset.registry.dto.Views;
import com.asset.registry.service.ChangeLogService;
import com.asset.registry.service.TransferService;
import com.asset.registry.service.VerifyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 更新维护：变更日志、定期核对、责任人变更。
 */
@RestController
@RequestMapping("/api")
public class MaintenanceController {

    private final ChangeLogService changeLogService;
    private final VerifyService verifyService;
    private final TransferService transferService;

    public MaintenanceController(ChangeLogService changeLogService, VerifyService verifyService,
                                 TransferService transferService) {
        this.changeLogService = changeLogService;
        this.verifyService = verifyService;
        this.transferService = transferService;
    }

    @GetMapping("/change-logs")
    public ApiResponse<List<Views.LogItem>> changeLogs(@RequestParam(required = false) String type) {
        return ApiResponse.ok(changeLogService.timeline(type));
    }

    @GetMapping("/verify/due")
    public ApiResponse<List<Views.AssetItem>> dueList() {
        return ApiResponse.ok(verifyService.dueList());
    }

    @PostMapping("/verify/{assetId}")
    public ApiResponse<Views.AssetItem> confirm(@PathVariable Long assetId,
                                                @Valid @RequestBody Payloads.Verify req) {
        return ApiResponse.ok(verifyService.confirm(assetId, req));
    }

    @GetMapping("/owner-transfers")
    public ApiResponse<List<Views.TransferItem>> transfers(@RequestParam(required = false) String status) {
        return ApiResponse.ok(transferService.list(status));
    }

    @PostMapping("/assets/{assetId}/owner-transfers")
    public ApiResponse<Views.TransferItem> apply(@PathVariable Long assetId,
                                                 @Valid @RequestBody Payloads.TransferApply req) {
        return ApiResponse.ok(transferService.apply(assetId, req));
    }

    @PostMapping("/owner-transfers/{id}/decision")
    public ApiResponse<Views.TransferItem> decide(@PathVariable Long id,
                                                  @Valid @RequestBody Payloads.TransferDecision req) {
        return ApiResponse.ok(transferService.decide(id, req));
    }
}
