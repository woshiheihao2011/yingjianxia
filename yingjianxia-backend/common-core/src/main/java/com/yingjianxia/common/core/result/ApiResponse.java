package com.yingjianxia.common.core.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 硬件侠平台统一响应体
 * <p>
 * 规范：所有微服务对外API必须返回此结构，禁止直接返回业务对象。
 * </p>
 *
 * @param <T> 业务数据泛型
 * @author 硬件侠后端团队
 * @version 1.0.0
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 响应码：0 表示成功，非 0 表示失败
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 业务数据
     */
    private T data;

    /**
     * 响应时间戳（毫秒）
     */
    private Long timestamp;

    /**
     * 请求追踪ID（用于链路追踪）
     */
    private String traceId;

    /* ========== 构造函数 ========== */
    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResponse(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /* ========== 成功静态方法 ========== */

    /**
     * 成功无数据
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * 成功有数据
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功有数据和自定义消息
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /* ========== 失败静态方法 ========== */

    /**
     * 失败（使用通用错误码）
     */
    public static <T> ApiResponse<T> fail() {
        return new ApiResponse<>(ResultCode.FAILURE.getCode(), ResultCode.FAILURE.getMessage(), null);
    }

    /**
     * 失败自定义消息
     */
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(ResultCode.FAILURE.getCode(), message, null);
    }

    /**
     * 失败使用错误码
     */
    public static <T> ApiResponse<T> fail(IErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 失败使用错误码 + 自定义消息
     */
    public static <T> ApiResponse<T> fail(IErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null);
    }

    /**
     * 失败指定错误码和消息
     */
    public static <T> ApiResponse<T> fail(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /* ========== 便捷判断 ========== */

    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(this.code);
    }

    public boolean isFailed() {
        return !isSuccess();
    }
}
