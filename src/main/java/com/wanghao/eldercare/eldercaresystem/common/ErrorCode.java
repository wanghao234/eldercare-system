package com.wanghao.eldercare.eldercaresystem.common;

public final class ErrorCode {
    private ErrorCode() {
    }

    public static final String SUCCESS = "0";
    public static final String BAD_REQUEST = "40001";
    public static final String UNAUTHORIZED = "40101";
    public static final String FORBIDDEN = "40301";
    public static final String NOT_FOUND = "40401";
    public static final String SYSTEM_ERROR = "50000";

    // Legacy aliases for backward compatibility in existing business code.
    public static final String AUTH_INVALID_CREDENTIALS = UNAUTHORIZED;
    public static final String AUTH_ACCOUNT_DISABLED = FORBIDDEN;
    public static final String AUTH_UNAUTHORIZED = UNAUTHORIZED;
    public static final String AUTH_FORBIDDEN = FORBIDDEN;
    public static final String AUTH_TOKEN_INVALID = UNAUTHORIZED;
    public static final String VALIDATION_ERROR = BAD_REQUEST;
    public static final String INTERNAL_SERVER_ERROR = SYSTEM_ERROR;
}
