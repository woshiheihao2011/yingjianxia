package com.yingjianxia.aftersales.controller;

import com.yingjianxia.aftersales.dto.*;
import com.yingjianxia.aftersales.entity.AfterSale;
import com.yingjianxia.aftersales.entity.AfterSaleMessage;
import com.yingjianxia.aftersales.entity.ArbitrationRecord;
import com.yingjianxia.aftersales.service.AfterSalesService;
import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 售后服务 Controller
 */
@Tag(name = "售后服务", description = "申请售后/卖家处理/退货退款/平台仲裁/沟通/查询")
@RestController
@RequestMapping("/api/v1/aftersales")
@RequiredArgsConstructor
public class AfterSalesController {

    private final AfterSalesService service;

    /* ========== 买家端 ========== */

    @Operation(summary = "[买家] 申请售后")
    @PostMapping({"", "/apply"})
    public ApiResponse<Long> apply(@Valid @RequestBody AfterSaleCreateReq req) {
        return ApiResponse.success(service.applyAfterSale(req, UserContext.requiredUserId()));
    }

    @Operation(summary = "[买家] 取消售后")
    @PostMapping("/{afterSaleId}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long afterSaleId) {
        service.cancelAfterSale(afterSaleId, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "[买家] 填写退货物流")
    @PostMapping({"/{afterSaleId}/return-shipment", "/return-shipment"})
    public ApiResponse<Void> fillReturnShipment(@PathVariable(required = false) Long afterSaleId,
                                                 @Valid @RequestBody ReturnShipmentReq req) {
        if (req.getAfterSaleId() == null) {
            req.setAfterSaleId(afterSaleId);
        }
        service.fillReturnShipment(req, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "[买家] 申请平台仲裁")
    @PostMapping({"/{afterSaleId}/arbitration/apply", "/{afterSaleId}/arbitration"})
    public ApiResponse<Void> applyArbitration(@PathVariable Long afterSaleId) {
        service.applyArbitration(afterSaleId, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "[买家] 发送售后沟通消息")
    @PostMapping("/messages/buyer")
    public ApiResponse<Void> buyerMessage(@Valid @RequestBody AsMessageReq req) {
        service.sendMessage(req, UserContext.requiredUserId(), 1);
        return ApiResponse.success();
    }

    /* ========== 卖家端 ========== */

    @Operation(summary = "[卖家] 处理售后（同意/拒绝）")
    @PostMapping("/seller/handle")
    public ApiResponse<Void> sellerHandle(@Valid @RequestBody SellerHandleReq req) {
        service.sellerHandle(req, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "[卖家] 确认收到退货（触发退款）")
    @PostMapping({"/{afterSaleId}/seller/confirm-return", "/{afterSaleId}/confirm-returned"})
    public ApiResponse<Void> confirmReturn(@PathVariable Long afterSaleId) {
        service.confirmReturnReceived(afterSaleId, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "[卖家] 同意售后")
    @PostMapping("/{afterSaleId}/approve")
    public ApiResponse<Void> approve(@PathVariable Long afterSaleId) {
        SellerHandleReq req = new SellerHandleReq();
        req.setAfterSaleId(afterSaleId);
        req.setAgree(1);
        service.sellerHandle(req, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "[卖家] 拒绝售后")
    @PostMapping("/{afterSaleId}/reject")
    public ApiResponse<Void> reject(@PathVariable Long afterSaleId,
                                    @RequestBody(required = false) java.util.Map<String, Object> body) {
        SellerHandleReq req = new SellerHandleReq();
        req.setAfterSaleId(afterSaleId);
        req.setAgree(2);
        if (body != null && body.get("reason") != null) {
            req.setRefuseReason(String.valueOf(body.get("reason")));
        }
        service.sellerHandle(req, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "[卖家] 发送售后沟通消息")
    @PostMapping("/messages/seller")
    public ApiResponse<Void> sellerMessage(@Valid @RequestBody AsMessageReq req) {
        service.sendMessage(req, UserContext.requiredUserId(), 2);
        return ApiResponse.success();
    }

    /* ========== 客服端 ========== */

    @Operation(summary = "[客服] 平台仲裁")
    @PostMapping("/arbitrate")
    public ApiResponse<Void> arbitrate(@Valid @RequestBody ArbitrationReq req,
                                      @RequestHeader("X-User-Roles") String roles) {
        // TODO: 客服角色校验（ROLE_CUSTOMER_SERVICE）
        service.arbitrate(req, UserContext.requiredUserId(), "客服" + UserContext.requiredUserId());
        return ApiResponse.success();
    }

    /* ========== 查询 ========== */

    @Operation(summary = "售后单详情")
    @GetMapping("/{afterSaleId}")
    public ApiResponse<AfterSale> detail(@PathVariable Long afterSaleId) {
        return ApiResponse.success(service.getDetail(afterSaleId));
    }

    @Operation(summary = "查询售后单仲裁记录")
    @GetMapping("/{afterSaleId}/arbitration")
    public ApiResponse<ArbitrationRecord> arbitration(@PathVariable Long afterSaleId) {
        return ApiResponse.success(service.getArbitration(afterSaleId));
    }

    @Operation(summary = "售后沟通记录列表")
    @GetMapping("/{afterSaleId}/messages")
    public ApiResponse<List<AfterSaleMessage>> messages(@PathVariable Long afterSaleId) {
        return ApiResponse.success(service.listMessages(afterSaleId));
    }

    @Operation(summary = "售后单分页查询")
    @PostMapping("/page")
    public ApiResponse<PageResult<AfterSale>> page(@RequestBody AfterSaleQueryReq req) {
        return ApiResponse.success(service.pageQuery(req, UserContext.requiredUserId()));
    }

    @Operation(summary = "售后单列表（前端兼容 GET 方式）")
    @GetMapping
    public ApiResponse<PageResult<AfterSale>> list(
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "type", required = false) Integer type,
            @RequestParam(value = "role", required = false) String role) {
        AfterSaleQueryReq req = new AfterSaleQueryReq();
        req.setPageNum(page);
        req.setPageSize(size);
        req.setStatus(status);
        req.setType(type);
        // role 支持字符串（buyer/seller/support）和数字（1/2/3）两种格式
        if (role != null) {
            switch (role.toLowerCase()) {
                case "buyer": case "1": req.setRole(1); break;
                case "seller": case "2": req.setRole(2); break;
                case "support": case "admin": case "3": req.setRole(3); break;
                default: req.setRole(null); // 未知值不按角色过滤
            }
        }
        return ApiResponse.success(service.pageQuery(req, UserContext.requiredUserId()));
    }
}
