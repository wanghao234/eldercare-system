package com.wanghao.eldercare.eldercaresystem.common;

import com.wanghao.eldercare.eldercaresystem.audit.AuditConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(AuditConstants.TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        request.setAttribute(AuditConstants.TRACE_ID_REQUEST_ATTR, traceId);
        response.setHeader(AuditConstants.TRACE_ID_HEADER, traceId);
        MDC.put(AuditConstants.TRACE_ID_MDC_KEY, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(AuditConstants.TRACE_ID_MDC_KEY);
        }
    }
}

