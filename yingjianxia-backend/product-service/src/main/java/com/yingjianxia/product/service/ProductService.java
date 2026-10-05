package com.yingjianxia.product.service;

import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.product.dto.BatchImportResult;
import com.yingjianxia.product.dto.BatchPriceUpdateReq;
import com.yingjianxia.product.dto.BatchStockUpdateReq;
import com.yingjianxia.product.dto.ProductDetailResp;
import com.yingjianxia.product.dto.ProductQueryReq;
import com.yingjianxia.product.dto.ProductSaveReq;
import com.yingjianxia.product.dto.SellerStatsResp;

/**
 * 商品服务接口
 * <p>
 * 负责：商品 CRUD、状态机（草稿→审核中→在售/拒绝→已售/下架）、
 *       浏览量缓存+异步回写、库存预扣/释放/扣减、快照生成、搜索ES降级
 *
 * @author 硬件侠后端团队
 */
public interface ProductService {

    /* ============ 卖家端 ============ */

    /**
     * 保存/更新商品（草稿或提交审核）— 根据 submitForReview 决定状态
     *
     * @param sellerId 卖家用户ID
     * @param productId 为null时新增，否则修改
     * @param req 保存内容
     * @return 商品ID
     */
    Long saveProduct(Long sellerId, Long productId, ProductSaveReq req);

    /**
     * 提交审核 — 草稿/审核拒绝 → 审核中
     */
    void submitForReview(Long sellerId, Long productId);

    /**
     * 卖家主动下架 — 在售 → 下架
     */
    void offShelf(Long sellerId, Long productId);

    /**
     * 卖家重新上架 — 下架 → 审核中（重新审核）
     */
    void reSubmitForReview(Long sellerId, Long productId);

    /**
     * 删除商品 — 仅草稿/审核拒绝/下架状态可删除（逻辑删除 products.deleted，图片级联清除）
     */
    void deleteProduct(Long sellerId, Long productId);

    /**
     * 批量改价 — 校验商品归属，更新价格（仅草稿/拒绝/下架/在售可改）
     *
     * @param sellerId 卖家ID
     * @param req      待改价商品列表
     */
    void batchUpdatePrice(Long sellerId, BatchPriceUpdateReq req);

    /**
     * 批量改库存 — 校验商品归属，更新库存与预警阈值
     *
     * @param sellerId 卖家ID
     * @param req      待改库存商品列表
     */
    void batchUpdateStock(Long sellerId, BatchStockUpdateReq req);

    /**
     * 卖家商品统计 — 发布/上架/审核/销量/浏览量等聚合数据
     */
    SellerStatsResp getSellerStats(Long sellerId);

    /**
     * 批量导入商品（Excel/CSV）— 每行创建一个草稿，失败行不影响其他
     *
     * @param sellerId 卖家ID
     * @param file     Excel(.xlsx)或CSV文件
     * @return 导入结果统计
     */
    BatchImportResult batchImport(Long sellerId, org.springframework.web.multipart.MultipartFile file);

    /* ============ 审核端（内部接口，供 audit-service 调用） ============ */

    /**
     * 审核通过 — 审核中 → 在售
     */
    void auditPass(Long productId, Long auditorId);

    /**
     * 审核拒绝 — 审核中 → 审核拒绝
     */
    void auditReject(Long productId, Long auditorId, String rejectReason);

    /* ============ 买家端 ============ */

    /**
     * 商品详情 — 写入 Redis 浏览量 INCR + dirty SET
     *
     * @param productId 商品ID
     * @param viewerId 当前用户（未登录为null）
     * @return 详情
     */
    ProductDetailResp getDetail(Long productId, Long viewerId);

    /**
     * 内部调用获取商品详情（不过滤状态、不增加浏览量）
     * 供 order-service 等 Feign 调用使用
     */
    ProductDetailResp getDetailInternal(Long productId);

    /**
     * 商品列表/搜索（ES优先，Canal同步延迟时降级MySQL多条件查询）
     */
    PageResult<ProductDetailResp> query(ProductQueryReq req);

    /* ============ 库存（订单TCC/内部API） ============ */

    /**
     * Try：预扣库存（幂等键防重复扣减）
     *
     * @param productId  商品ID
     * @param quantity   数量
     * @param idempotentKey 分布式事务幂等键（订单号）
     */
    boolean tryDeductStock(Long productId, int quantity, String idempotentKey);

    /**
     * Confirm：确认扣减（Try 成功后执行，将库存预扣→实扣，status变已售等）
     */
    boolean confirmDeductStock(Long productId, int quantity, String idempotentKey);

    /**
     * Cancel：取消（释放库存）
     */
    boolean cancelDeductStock(Long productId, int quantity, String idempotentKey);

    /* ============ 快照（订单服务调用） ============ */

    /**
     * 生成商品快照（下单时生成，订单域商品快照防篡改）
     *
     * @param productId 商品
     * @param orderId   订单号
     * @return 快照ID
     */
    Long createSnapshot(Long productId, Long orderId);

    /* ============ 浏览量 ============ */

    /**
     * 增加浏览量（Redis INCR + dirty SET，异步回写DB）
     */
    void incrementViewCount(Long productId);

    /* ============ XXL-Job 任务：浏览量 MySQL 回写 ============ */
    void syncViewCountFromRedis();
}
