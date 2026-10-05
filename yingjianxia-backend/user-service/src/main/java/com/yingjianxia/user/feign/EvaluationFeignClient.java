package com.yingjianxia.user.feign;

import com.yingjianxia.common.core.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 评价服务 Feign Client — user-service 调用 evaluation-service 获取店铺评价统计（bug-20260908201428）
 *
 * @author 硬件侠后端团队
 */
@FeignClient(name = "evaluation-service", contextId = "userEvaluationStats", fallback = EvaluationFeignFallback.class)
public interface EvaluationFeignClient {

    /**
     * 按卖家ID查询评价统计（评价总数、平均评分、好评率、评分分布）
     *
     * @param sellerId 卖家用户ID
     * @return 评价统计 Map
     */
    @GetMapping("/api/v1/evaluation/sellers/{sellerId}/stats")
    ApiResponse<Map<String, Object>> sellerReviewStats(@PathVariable("sellerId") Long sellerId);
}
