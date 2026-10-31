package com.asset.registry.config;

import com.asset.registry.entity.SysUser;
import com.asset.registry.repository.SysUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 解析当前用户。
 * 生产环境应由 SSO 网关回传受信任的主体标识（如 X-Auth-Sub），本地联调使用 X-User-Id 指定用户。
 */
@Component
public class CurrentUserFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID = "X-User-Id";
    private final SysUserRepository userRepository;

    public CurrentUserFilter(SysUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String raw = request.getHeader(HEADER_USER_ID);
            Long userId = 1L;
            if (raw != null && !raw.isBlank()) {
                try {
                    userId = Long.parseLong(raw.trim());
                } catch (NumberFormatException ignored) {
                    userId = 1L;
                }
            }
            final Long uid = userId;
            userRepository.findById(uid)
                    .map(u -> new CurrentUser(u.getId(), u.getUsername(), u.getRealName(), u.getRoleCode(),
                            u.getUnitId(), u.getDeptId()))
                    .ifPresent(UserContext::set);
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
