package com.yingjianxia.product.feign;

import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ShopFeignClient 降级处理
 * user-service 不可用时返回 null，不阻塞商品创建/详情流程
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Component
public class ShopFeignFallback implements ShopFeignClient {

    @Override
    public ApiResponse<ShopFeignResp> getBySeller(Long sellerId) {
        log.warn("[Sentinel 降级] user-service#getBySeller 不可用, sellerId={}", sellerId);
        return ApiResponse.fail(ResultCode.SERVICE_DOWNGRADE.getCode(), "店铺服务暂不可用");
    }
}
