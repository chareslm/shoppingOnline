package com.chareslm.shopping.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.security.context.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 限制仍在使用临时密码的账号只能完成身份确认、改密和退出。
 */
@Component
public class MustChangePasswordFilter extends OncePerRequestFilter {
    // /me 供前端确认强制改密状态；/password 是解除限制的唯一业务入口；/logout 保留安全退出能力。
    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/auth/me", "/api/auth/password", "/api/auth/logout");

    private final ObjectMapper objectMapper;

    public MustChangePasswordFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser user
                && user.mustChangePassword() && !ALLOWED_PATHS.contains(request.getRequestURI())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(),
                    ApiResponse.failure(ErrorCode.PASSWORD_CHANGE_REQUIRED, request.getHeader("X-Trace-Id")));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
