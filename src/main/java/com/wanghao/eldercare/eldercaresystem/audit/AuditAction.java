package com.wanghao.eldercare.eldercaresystem.audit;

public final class AuditAction {
    private AuditAction() {
    }

    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAIL = "LOGIN_FAIL";
    public static final String LOGOUT = "LOGOUT";
    public static final String CREATE = "CREATE";
    public static final String UPDATE = "UPDATE";
    public static final String DELETE = "DELETE";
    public static final String TRANSITION = "TRANSITION";
    public static final String APPROVE = "APPROVE";
    public static final String REJECT = "REJECT";
    public static final String CLAIM = "CLAIM";
    public static final String COMPLETE = "COMPLETE";
    public static final String TRANSFER = "TRANSFER";
    public static final String ROLLBACK = "ROLLBACK";
    public static final String VIEW_SENSITIVE = "VIEW_SENSITIVE";
    public static final String UPLOAD = "UPLOAD";
}
