package com.asset.registry.config;

/**
 * 当前登录用户（由 SSO 网关注入，本地联调可用 X-User-Id 头模拟）。
 */
public record CurrentUser(Long id, String username, String realName, String roleCode, Long unitId, Long deptId) {

    public static CurrentUser system() {
        return new CurrentUser(0L, "system", "系统", "ROLE_ADMIN", null, null);
    }
}
