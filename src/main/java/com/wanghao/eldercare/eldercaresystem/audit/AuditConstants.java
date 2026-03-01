package com.wanghao.eldercare.eldercaresystem.audit;

public final class AuditConstants {
    private AuditConstants() {
    }

    public static final String TRACE_ID_HEADER = "X-Request-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String TRACE_ID_REQUEST_ATTR = "TRACE_ID";
    public static final String AUDIT_FAILURE_LOGGED_ATTR = "AUDIT_FAILURE_LOGGED";
}

