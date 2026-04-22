package com.winter.fund.common.result;

/**
 * 统一返回结构文件，负责规范 API 的响应包裹格式。
 */

public record ApiResponse<T>(int code, String message, T data) {

    /**
     * 返回success结果。
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    /**
     * 返回failure结果。
     */
    public static <T> ApiResponse<T> failure(int code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }
}
