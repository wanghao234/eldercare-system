package com.wanghao.eldercare.eldercaresystem.common.audit;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;

public final class AuditConstants {
    private AuditConstants() {
    }

    public static final String TRACE_ID_HEADER = "X-Request-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String TRACE_ID_REQUEST_ATTR = "TRACE_ID";
    public static final String AUDIT_FAILURE_LOGGED_ATTR = "AUDIT_FAILURE_LOGGED";
}

