package com.yingjianxia.user.feign;

import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * EvaluationFeignClient 降级处理
 * evaluation-service 不可用时返回空统计，不阻塞店铺详情流程
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Component
public class EvaluationFeignFallback implements EvaluationFeignClient {

    @Override
    public ApiResponse<Map<String, Object>> sellerReviewStats(Long sellerId) {
        log.warn("[Sentinel 降级] evaluation-service#sellerReviewStats 不可用, sellerId={}", sellerId);
        Map<String, Object> empty = new HashMap<>();
        empty.put("totalCount", 0);
        empty.put("averageRating", 0.0);
        empty.put("positiveRate", 0.0);
        return ApiResponse.fail(ResultCode.SERVICE_DOWNGRADE.getCode(), "评价服务暂不可用");
    }
}
