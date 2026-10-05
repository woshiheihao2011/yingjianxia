package com.yingjianxia.common.core.result;

/**
 * 错误码接口 — 所有业务错误枚举必须实现此接口
 * <p>
 * 错误码分段规则：
 * <ul>
 *   <li>0          : 通用成功</li>
 *   <li>1 ~ 999    : 通用错误（参数、系统、网络）</li>
 *   <li>1000~1999  : 用户域错误</li>
 *   <li>2000~2999  : 商品域错误</li>
 *   <li>3000~3999  : 验机服务域错误</li>
 *   <li>4000~4999  : 订单交易域错误</li>
 *   <li>5000~5999  : 担保资金域错误</li>
 *   <li>6000~6999  : 物流域错误</li>
 *   <li>7000~7999  : 售后域错误</li>
 *   <li>8000~8999  : 消息社区域错误</li>
 *   <li>9000~9999  : 客服/营销/风控域错误</li>
 * </ul>
 *
 * @author 硬件侠后端团队
 */
public interface IErrorCode {

    /**
     * 获取错误码
     */
    Integer getCode();

    /**
     * 获取错误消息
     */
    String getMessage();
}
