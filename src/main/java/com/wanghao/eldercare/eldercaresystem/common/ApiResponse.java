package com.wanghao.eldercare.eldercaresystem.common;

public class ApiResponse<T> {
    private String code;
    private String message;
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS, "success", data);
    }

    public static <T> ApiResponse<T> fail(String code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    // Backward-compatible aliases for existing code paths.
    public static <T> ApiResponse<T> success(T data) {
        return ok(data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS, message, data);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return fail(code, message);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
