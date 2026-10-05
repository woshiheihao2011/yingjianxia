package com.yingjianxia.aftersales.service;

import com.yingjianxia.aftersales.dto.*;
import com.yingjianxia.aftersales.entity.AfterSale;
import com.yingjianxia.aftersales.entity.AfterSaleMessage;
import com.yingjianxia.aftersales.entity.ArbitrationRecord;
import com.yingjianxia.common.core.result.PageResult;

import java.util.List;

/**
 * 售后服务接口
 */
public interface AfterSalesService {

    /* ====== 买家端 ====== */

    /** 申请售后（校验7天期限/金额/重复申请） */
    Long applyAfterSale(AfterSaleCreateReq req, Long buyerId);

    /** 买家取消售后 */
    void cancelAfterSale(Long afterSaleId, Long buyerId);

    /** 买家填写退货物流 */
    void fillReturnShipment(ReturnShipmentReq req, Long buyerId);

    /** 买家申请平台仲裁 */
    void applyArbitration(Long afterSaleId, Long buyerId);

    /* ====== 卖家端 ====== */

    /** 卖家处理售后（同意/拒绝） */
    void sellerHandle(SellerHandleReq req, Long sellerId);

    /** 卖家确认收到退货 → 触发退款（调 escrow-service） */
    void confirmReturnReceived(Long afterSaleId, Long sellerId);

    /* ====== 客服端 ====== */

    /** 平台仲裁（客服介入） */
    void arbitrate(ArbitrationReq req, Long arbitratorId, String arbitratorName);

    /* ====== 沟通 ====== */

    /** 发送售后沟通消息 */
    void sendMessage(AsMessageReq req, Long senderId, Integer senderType);

    /** 查询售后沟通记录 */
    List<AfterSaleMessage> listMessages(Long afterSaleId);

    /* ====== 查询 ====== */

    /** 售后单详情 */
    AfterSale getDetail(Long afterSaleId);

    /** 售后单详情（含仲裁记录） */
    AfterSale getDetailWithArbitration(Long afterSaleId);

    /** 查询仲裁记录 */
    ArbitrationRecord getArbitration(Long afterSaleId);

    /** 分页查询售后单 */
    PageResult<AfterSale> pageQuery(AfterSaleQueryReq req, Long operatorId);

    /* ====== 定时任务 ====== */

    /** 卖家超时未处理自动同意 */
    void autoAgreeTimeout();
}
