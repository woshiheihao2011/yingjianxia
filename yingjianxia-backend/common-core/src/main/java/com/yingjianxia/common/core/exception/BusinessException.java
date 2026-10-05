package com.yingjianxia.common.core.exception;

import com.yingjianxia.common.core.result.IErrorCode;
import com.yingjianxia.common.core.result.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常基类
 * <p>
 * 所有业务层手动抛出的异常必须使用此类（或其子类），全局异常处理器会捕获
 * 并转换为标准 {@link com.yingjianxia.common.core.result.ApiResponse} 返回前端。
 * </p>
 * <p>
 * 使用示例：
 * <pre>
 *   throw new BusinessException(ResultCode.PARAM_ERROR, "手机号格式错误");
 *   throw new BusinessException(4001, "订单不存在");
 * </pre>
 *
 * @author 硬件侠后端团队
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 构造：指定错误码枚举
     */
    public BusinessException(IErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 构造：指定错误码枚举 + 自定义消息（覆盖默认消息）
     */
    public BusinessException(IErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    /**
     * 构造：直接指定 code + message
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造：仅消息（默认通用错误码 1）
     */
    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.FAILURE.getCode();
    }

    /**
     * 便捷静态工厂：参数校验失败
     */
    public static BusinessException paramError(String message) {
        return new BusinessException(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 便捷静态工厂：数据不存在
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(ResultCode.DATA_NOT_FOUND, message);
    }

    /**
     * 便捷静态工厂：无权限
     */
    public static BusinessException forbidden(String message) {
        return new BusinessException(ResultCode.FORBIDDEN, message);
    }
}
