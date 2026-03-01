package com.wanghao.eldercare.eldercaresystem.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            writeUnauthorized(response, "无效或已过期的令牌");
            return;
        }

        Long userId = jwtTokenProvider.getUserId(token);
        String username = jwtTokenProvider.getUsername(token);
        String role = jwtTokenProvider.getRole(token);
        String status = jwtTokenProvider.getStatus(token);
        if (userId == null || !StringUtils.hasText(username) || !StringUtils.hasText(role)) {
            writeUnauthorized(response, "令牌缺少必要用户信息");
            return;
        }
        if (StringUtils.hasText(status) && !"active".equalsIgnoreCase(status)) {
            writeUnauthorized(response, "账号已禁用");
            return;
        }

        try {
            UserPrincipal userDetails = new UserPrincipal(
                    userId,
                    username,
                    "",
                    role,
                    status == null ? "active" : status,
                    new LinkedHashSet<>(jwtTokenProvider.getPerms(token))
            );
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        } catch (Exception ex) {
            writeUnauthorized(response, "认证信息无效");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(ErrorCode.UNAUTHORIZED, "Unauthorized", null)
        ));
    }
}
