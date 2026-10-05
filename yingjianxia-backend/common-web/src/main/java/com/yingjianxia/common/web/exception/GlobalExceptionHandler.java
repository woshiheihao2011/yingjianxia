package com.yingjianxia.common.web.exception;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 全局统一异常处理器
 * <p>
 * 捕获各层异常，转换为标准 {@link ApiResponse} 返回前端，避免堆栈泄漏。
 * 所有异常同时打印日志，便于排查。
 * </p>
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* ========== 1. 业务异常（可控） ========== */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("[业务异常] code={}, msg={}", e.getCode(), e.getMessage());
        return ApiResponse.fail(e.getCode(), e.getMessage());
    }

    /* ========== 2. 参数校验异常 ========== */

    /** @RequestBody @Valid 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("[参数校验] MethodArgumentNotValid: {}", msg);
        return ApiResponse.fail(ResultCode.PARAM_BIND_ERROR, msg);
    }

    /** GET 参数 @Validated 校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("[参数校验] ConstraintViolation: {}", msg);
        return ApiResponse.fail(ResultCode.PARAM_ERROR, msg);
    }

    /** 表单绑定异常（@ModelAttribute） */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBindException(BindException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe == null ? "参数绑定失败" : (fe.getField() + ": " + fe.getDefaultMessage());
        log.warn("[参数绑定] BindException: {}", msg);
        return ApiResponse.fail(ResultCode.PARAM_BIND_ERROR, msg);
    }

    /* ========== 3. HTTP 请求类异常 ========== */

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ApiResponse.fail(ResultCode.REQUEST_METHOD_NOT_SUPPORTED, e.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ApiResponse<Void> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        return ApiResponse.fail(ResultCode.REQUEST_CONTENT_TYPE_NOT_SUPPORTED, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        return ApiResponse.fail(ResultCode.REQUEST_BODY_MISSING, "请求体JSON格式错误或缺失");
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String msg = String.format("参数 %s 类型错误，期望值 %s", e.getName(),
                e.getRequiredType() == null ? "" : e.getRequiredType().getSimpleName());
        return ApiResponse.fail(ResultCode.PARAM_ERROR, msg);
    }

    /* ========== 4. Sentinel 限流/熔断 ========== */

    @ExceptionHandler(BlockException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiResponse<Void> handleBlockException(BlockException e) {
        if (e instanceof FlowException) {
            return ApiResponse.fail(ResultCode.SERVICE_BLOCKED, "请求过于频繁，请稍后再试");
        }
        if (e instanceof DegradeException) {
            return ApiResponse.fail(ResultCode.SERVICE_DOWNGRADE, "服务降级中，请稍后再试");
        }
        if (e instanceof AuthorityException) {
            return ApiResponse.fail(ResultCode.FORBIDDEN, "该来源无访问权限");
        }
        return ApiResponse.fail(ResultCode.SERVICE_BLOCKED);
    }

    /* ========== 5. 数据库约束异常 ========== */

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("[DB唯一约束] DuplicateKey: {}", e.getMessage());
        return ApiResponse.fail(ResultCode.DATA_ALREADY_EXISTS, "唯一约束冲突，数据已存在");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("[DB完整性] DataIntegrityViolation: {}", e.getMessage());
        return ApiResponse.fail(ResultCode.DATA_INTEGRITY_VIOLATION);
    }

    /* ========== 6. Bean 实例化 ========== */
    @ExceptionHandler(BeanInstantiationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleBeanInstantiation(BeanInstantiationException e) {
        log.error("[Bean实例化失败]", e);
        return ApiResponse.fail(ResultCode.SYSTEM_ERROR);
    }

    /* ========== 99. 兜底异常（未知） ========== */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("[未捕获异常] msg={}", e.getMessage(), e);
        return ApiResponse.fail(ResultCode.SYSTEM_ERROR);
    }
}
