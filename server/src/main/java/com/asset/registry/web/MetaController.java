package com.asset.registry.web;

import com.asset.registry.common.ApiResponse;
import com.asset.registry.config.CurrentUser;
import com.asset.registry.config.UserContext;
import com.asset.registry.dto.Views;
import com.asset.registry.service.MetaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 字典与当前用户。
 */
@RestController
@RequestMapping("/api")
public class MetaController {

    private final MetaService metaService;

    public MetaController(MetaService metaService) {
        this.metaService = metaService;
    }

    @GetMapping("/meta")
    public ApiResponse<Views.Meta> meta() {
        return ApiResponse.ok(metaService.meta());
    }

    @GetMapping("/meta/fields")
    public ApiResponse<?> fields(String typeCode) {
        return ApiResponse.ok(metaService.fields(typeCode));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUser> me() {
        return ApiResponse.ok(UserContext.get());
    }
}
