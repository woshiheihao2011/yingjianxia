package com.yingjianxia.product.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.yingjianxia.common.core.constants.RedisKeyConstants;
import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.common.core.result.ResultCode;
import com.yingjianxia.product.constants.ProductConstants;
import com.yingjianxia.product.dto.BatchImportResult;
import com.yingjianxia.product.dto.BatchPriceUpdateReq;
import com.yingjianxia.product.dto.BatchStockUpdateReq;
import com.yingjianxia.product.dto.ProductDetailResp;
import com.yingjianxia.product.dto.ProductQueryReq;
import com.yingjianxia.product.dto.ProductSaveReq;
import com.yingjianxia.product.dto.SellerStatsResp;
import com.yingjianxia.product.entity.*;
import com.yingjianxia.product.enums.ProductErrorCode;
import com.yingjianxia.product.feign.AuditFeignClient;
import com.yingjianxia.product.feign.ShopFeignClient;
import com.yingjianxia.product.feign.ShopFeignResp;
import com.yingjianxia.product.mapper.*;
import com.yingjianxia.product.service.CategoryFavoriteService;
import com.yingjianxia.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 商品服务实现 — 状态机 + CRUD + 浏览量 + 库存 + 快照
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final ProductImageMapper imageMapper;
    private final ProductSpecMapper specMapper;
    private final ProductSnapshotMapper snapshotMapper;
    private final CategoryMapper categoryMapper;
    private final CategoryFavoriteService favoriteService;
    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;
    private final AuditFeignClient auditFeignClient;
    private final ShopFeignClient shopFeignClient;

    public ProductServiceImpl(ProductMapper productMapper, ProductImageMapper imageMapper,
                              ProductSpecMapper specMapper, ProductSnapshotMapper snapshotMapper,
                              CategoryMapper categoryMapper,
                              @Lazy CategoryFavoriteService favoriteService, // 避免循环依赖
                              RedissonClient redisson, ObjectMapper objectMapper,
                              AuditFeignClient auditFeignClient,
                              ShopFeignClient shopFeignClient) {
        this.productMapper = productMapper;
        this.imageMapper = imageMapper;
        this.specMapper = specMapper;
        this.snapshotMapper = snapshotMapper;
        this.categoryMapper = categoryMapper;
        this.favoriteService = favoriteService;
        this.redisson = redisson;
        this.objectMapper = objectMapper;
        this.auditFeignClient = auditFeignClient;
        this.shopFeignClient = shopFeignClient;
    }

    /* ============================================================
     *  保存/更新商品
     * ============================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveProduct(Long sellerId, Long productId, ProductSaveReq req) {
        boolean isSubmit = Boolean.TRUE.equals(req.getSubmitForReview());

        // 1. 分类处理：优先用 categoryId，为空时按 categoryName（可能是 name/code/id字符串）查
        Integer categoryId = req.getCategoryId();
        if (categoryId == null && StrUtil.isNotBlank(req.getCategoryName())) {
            categoryId = resolveCategoryId(req.getCategoryName());
        }
        Category category = null;
        if (categoryId != null) {
            category = categoryMapper.selectById(categoryId);
            if (category == null) throw new BusinessException(ProductErrorCode.CATEGORY_NOT_FOUND);
            if (Category.STATUS_DISABLE == category.getStatus()) {
                throw new BusinessException(ProductErrorCode.CATEGORY_DISABLED);
            }
        } else if (isSubmit) {
            // 提交审核时分类必填
            throw new BusinessException(ProductErrorCode.CATEGORY_NOT_FOUND);
        }

        // 2. 成色处理：优先用 conditionLevel，为空时按 conditionName 映射
        Integer conditionLevel = req.getConditionLevel();
        if (conditionLevel == null && StrUtil.isNotBlank(req.getConditionName())) {
            conditionLevel = mapConditionName(req.getConditionName());
        }

        // 3. 图片校验：仅提交审核时强制非空
        if (req.getImages() != null && req.getImages().size() > ProductConstants.MAX_IMAGE_COUNT) {
            throw new BusinessException(ProductErrorCode.IMAGE_LIMIT_EXCEEDED);
        }
        if (isSubmit && (req.getImages() == null || req.getImages().isEmpty())) {
            throw new BusinessException(ProductErrorCode.IMAGE_REQUIRED);
        }

        // 4. 价格校验：仅提交审核时强制非空且 > 0
        if (isSubmit && (req.getPrice() == null || req.getPrice().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BusinessException(ProductErrorCode.PRICE_INVALID);
        }

        // 5. 显卡/CPU必须验机的强制规则（仅在分类存在时判断）
        if (category != null) {
            boolean mustInspect = ProductConstants.INSPECT_REQUIRED_CODES.contains(category.getCode());
            if (mustInspect && !Boolean.TRUE.equals(req.getNeedInspection())) {
                req.setNeedInspection(true);
            }
        }

        Product product;
        boolean isCreate = (productId == null);

        if (isCreate) {
            product = new Product();
            product.setSellerId(sellerId);
            product.setViewCount(0);
            product.setFavoriteCount(0);
            product.setStatus(Product.STATUS_DRAFT);
            product.setStock(req.getStock() != null ? req.getStock() : 1);
            product.setWarningThreshold(req.getWarningThreshold() != null ? req.getWarningThreshold() : 5);
            // 回填 shopId（bug-20260908170645）：创建商品时按 sellerId 查店铺
            try {
                var shopResp = shopFeignClient.getBySeller(sellerId);
                if (shopResp != null && shopResp.isSuccess() && shopResp.getData() != null) {
                    product.setShopId(shopResp.getData().getId());
                }
            } catch (Exception e) {
                log.warn("[创建商品-回填shopId失败] sellerId={}, err={}", sellerId, e.getMessage());
            }
        } else {
            product = productMapper.selectById(productId);
            if (product == null) throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
            if (!product.getSellerId().equals(sellerId)) {
                throw new BusinessException(ProductErrorCode.PRODUCT_NOT_BELONG_SELLER);
            }
            if (!allowEdit(product.getStatus())) {
                throw new BusinessException(ProductErrorCode.STATUS_ILLEGAL_TRANSITION,
                        "仅草稿/审核拒绝/已下架的商品可编辑");
            }
        }

        // 6. 赋值通用字段（categoryId/conditionLevel 草稿模式允许为 null）
        if (categoryId != null) product.setCategoryId(categoryId);
        product.setTitle(req.getTitle());
        product.setSubTitle(req.getSubTitle());
        product.setBrand(req.getBrand());
        product.setPrice(req.getPrice());
        product.setOriginalPrice(req.getOriginalPrice());
        product.setConditionLevel(conditionLevel);
        product.setPurchaseChannel(req.getPurchaseChannel());
        product.setHasBox(Boolean.TRUE.equals(req.getHasBox()));
        product.setLocation(req.getLocation());
        product.setDescription(req.getDescription());
        // BUG-001 新增字段：运费/质保/售后/瑕疵/服务标签
        product.setShipFree(Boolean.TRUE.equals(req.getShipFree()));
        product.setShipTemplate(req.getShipTemplate());
        product.setWarranty(req.getWarranty());
        product.setAftersalesType(req.getAftersalesType());
        product.setDefectsJson(toJson(req.getDefects()));
        product.setServiceTagsJson(toJson(req.getTags()));

        if (req.getStock() != null) product.setStock(req.getStock());
        if (req.getWarningThreshold() != null) product.setWarningThreshold(req.getWarningThreshold());

        if (isCreate) {
            productMapper.insert(product);
        } else {
            product.setUpdatedAt(LocalDateTime.now());
            productMapper.updateById(product);
        }

        // 5. 图片 + 规格：先删后重插（保持编辑简单幂等）
        if (!isCreate) {
            imageMapper.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, product.getId()));
            specMapper.delete(new LambdaQueryWrapper<ProductSpec>().eq(ProductSpec::getProductId, product.getId()));
        }
        insertImages(product.getId(), req.getImages());
        if (req.getSpecs() != null && !req.getSpecs().isEmpty()) {
            insertSpecs(product.getId(), req.getSpecs());
        }

        // 6. 如果要求提交审核
        if (Boolean.TRUE.equals(req.getSubmitForReview())) {
            submitForReview(sellerId, product.getId());
        }

        log.info("【商品保存】seller={}, productId={}, isCreate={}, submitReview={}",
                sellerId, product.getId(), isCreate, req.getSubmitForReview());
        return product.getId();
    }

    private boolean allowEdit(Integer status) {
        return Product.STATUS_DRAFT == status
                || Product.STATUS_REJECTED == status
                || Product.STATUS_OFF_SHELF == status;
    }

    /* ============================================================
     *  状态机流转
     * ============================================================ */

    @Override
    public void submitForReview(Long sellerId, Long productId) {
        Product p = assertOwnProduct(sellerId, productId);
        if (p.getStatus() != Product.STATUS_DRAFT && p.getStatus() != Product.STATUS_REJECTED
                && p.getStatus() != Product.STATUS_OFF_SHELF) {
            throw new BusinessException(ProductErrorCode.STATUS_MUST_BE_DRAFT_OR_REJECTED);
        }
        // 必检分类需要关联验机报告ID（这里占位；验机服务完成后生成报告回填 inspectionId）
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .set(Product::getStatus, Product.STATUS_REVIEWING)
                .set(Product::getRejectReason, null)
                .set(Product::getUpdatedAt, LocalDateTime.now()));
        log.info("【商品提交审核】productId={}, sellerId={}", productId, sellerId);

        // 清理同卖家同标题的孤立草稿（防止前端重复创建草稿副本 — bug-20260908122226）
        if (StrUtil.isNotBlank(p.getTitle())) {
            List<Product> orphanDrafts = productMapper.selectList(new LambdaQueryWrapper<Product>()
                    .eq(Product::getSellerId, sellerId)
                    .eq(Product::getTitle, p.getTitle())
                    .eq(Product::getStatus, Product.STATUS_DRAFT)
                    .ne(Product::getId, productId));
            if (!orphanDrafts.isEmpty()) {
                List<Long> orphanIds = orphanDrafts.stream().map(Product::getId).collect(Collectors.toList());
                productMapper.delete(new LambdaQueryWrapper<Product>().in(Product::getId, orphanIds));
                imageMapper.delete(new LambdaQueryWrapper<ProductImage>().in(ProductImage::getProductId, orphanIds));
                specMapper.delete(new LambdaQueryWrapper<ProductSpec>().in(ProductSpec::getProductId, orphanIds));
                log.info("【清理孤立草稿】sellerId={}, title={}, count={}, ids={}",
                        sellerId, p.getTitle(), orphanIds.size(), orphanIds);
            }
        }

        // Feign 同步调用 audit-service 创建审核记录
        try {
            var resp = auditFeignClient.submitAudit(1, productId, java.util.Collections.emptyMap()); // targetType=1(商品)
            if (resp != null && resp.isSuccess()) {
                log.info("【提交审核-通知audit-service成功】productId={}, recordId={}", productId, resp.getData());
            } else {
                log.error("【提交审核-通知audit-service失败】productId={}, resp={}", productId, resp);
            }
        } catch (Exception e) {
            log.error("【提交审核-通知audit-service异常】productId={}, err={}", productId, e.getMessage(), e);
            // 不回滚商品状态（最终一致性由补偿机制保证）
        }
    }

    @Override
    public void offShelf(Long sellerId, Long productId) {
        Product p = assertOwnProduct(sellerId, productId);
        if (p.getStatus() != Product.STATUS_ON_SALE) {
            throw new BusinessException(ProductErrorCode.STATUS_MUST_BE_ON_SALE);
        }
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .set(Product::getStatus, Product.STATUS_OFF_SHELF)
                .set(Product::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    public void reSubmitForReview(Long sellerId, Long productId) {
        // 与 submitForReview 等价（需编辑后重新提交审核）
        submitForReview(sellerId, productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long sellerId, Long productId) {
        Product p = assertOwnProduct(sellerId, productId);
        if (Product.STATUS_ON_SALE == p.getStatus() || Product.STATUS_REVIEWING == p.getStatus()
                || Product.STATUS_SOLD_OUT == p.getStatus()) {
            throw new BusinessException(ProductErrorCode.STATUS_ILLEGAL_TRANSITION,
                    "在售/审核中/已售的商品不可直接删除，请先下架");
        }
        productMapper.deleteById(productId); // 逻辑删除
        imageMapper.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, productId));
        specMapper.delete(new LambdaQueryWrapper<ProductSpec>().eq(ProductSpec::getProductId, productId));
        log.info("【商品删除】productId={}, sellerId={}", productId, sellerId);
    }

    /* ============================================================
     *  批量改价 / 批量改库存 / 卖家统计
     * ============================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdatePrice(Long sellerId, BatchPriceUpdateReq req) {
        if (req == null || req.getItems() == null || req.getItems().isEmpty()) {
            throw BusinessException.paramError("改价列表不能为空");
        }
        int ok = 0, fail = 0;
        for (BatchPriceUpdateReq.Item item : req.getItems()) {
            try {
                if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    fail++;
                    continue;
                }
                Product p = productMapper.selectById(item.getProductId());
                if (p == null || !sellerId.equals(p.getSellerId())) { fail++; continue; }
                // 审核中不允许改价，其余状态可改
                if (Product.STATUS_REVIEWING == p.getStatus()) { fail++; continue; }
                productMapper.update(null, new LambdaUpdateWrapper<Product>()
                        .eq(Product::getId, item.getProductId())
                        .set(Product::getPrice, item.getPrice())
                        .set(Product::getUpdatedAt, LocalDateTime.now()));
                ok++;
            } catch (Exception e) {
                log.warn("[批量改价] productId={} 失败：{}", item.getProductId(), e.getMessage());
                fail++;
            }
        }
        log.info("【批量改价】sellerId={}, 成功={}, 失败={}", sellerId, ok, fail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStock(Long sellerId, BatchStockUpdateReq req) {
        if (req == null || req.getItems() == null || req.getItems().isEmpty()) {
            throw BusinessException.paramError("改库存列表不能为空");
        }
        int ok = 0, fail = 0;
        for (BatchStockUpdateReq.Item item : req.getItems()) {
            try {
                if (item.getStock() == null || item.getStock() < 0) {
                    fail++;
                    continue;
                }
                Product p = productMapper.selectById(item.getProductId());
                if (p == null || !sellerId.equals(p.getSellerId())) { fail++; continue; }
                // 审核中不允许改库存，其余状态可改
                if (Product.STATUS_REVIEWING == p.getStatus()) { fail++; continue; }
                LambdaUpdateWrapper<Product> uw = new LambdaUpdateWrapper<Product>()
                        .eq(Product::getId, item.getProductId())
                        .set(Product::getStock, item.getStock())
                        .set(Product::getUpdatedAt, LocalDateTime.now());
                // 库存预警阈值（可选）
                if (item.getWarningThreshold() != null && item.getWarningThreshold() > 0) {
                    uw.set(Product::getWarningThreshold, item.getWarningThreshold());
                }
                // 库存为 0 且在售 → 自动转已售罄；库存 > 0 且已售罄 → 自动回在售
                if (item.getStock() == 0 && Product.STATUS_ON_SALE == p.getStatus()) {
                    uw.set(Product::getStatus, Product.STATUS_SOLD_OUT);
                } else if (item.getStock() > 0 && Product.STATUS_SOLD_OUT == p.getStatus()) {
                    uw.set(Product::getStatus, Product.STATUS_ON_SALE);
                }
                productMapper.update(null, uw);
                ok++;
            } catch (Exception e) {
                log.warn("[批量改库存] productId={} 失败：{}", item.getProductId(), e.getMessage());
                fail++;
            }
        }
        log.info("【批量改库存】sellerId={}, 成功={}, 失败={}", sellerId, ok, fail);
    }

    @Override
    public SellerStatsResp getSellerStats(Long sellerId) {
        SellerStatsResp resp = new SellerStatsResp();
        // 按状态分组统计数量
        List<Product> all = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getSellerId, sellerId));
        long draft = 0, reviewing = 0, onSale = 0, soldOut = 0, rejected = 0, offShelf = 0;
        long totalSales = 0, totalViews = 0, totalFav = 0;
        for (Product p : all) {
            if (p.getStatus() == null) continue;
            switch (p.getStatus()) {
                case Product.STATUS_DRAFT -> draft++;
                case Product.STATUS_REVIEWING -> reviewing++;
                case Product.STATUS_ON_SALE -> onSale++;
                case Product.STATUS_SOLD_OUT -> soldOut++;
                case Product.STATUS_REJECTED -> rejected++;
                case Product.STATUS_OFF_SHELF -> offShelf++;
            }
            if (p.getSales() != null) totalSales += p.getSales();
            if (p.getViewCount() != null) totalViews += p.getViewCount();
            if (p.getFavoriteCount() != null) totalFav += p.getFavoriteCount();
        }
        resp.setTotalProducts((long) all.size());
        resp.setDraftCount(draft);
        resp.setReviewingCount(reviewing);
        resp.setOnSaleCount(onSale);
        resp.setSoldOutCount(soldOut);
        resp.setRejectedCount(rejected);
        resp.setOffShelfCount(offShelf);
        resp.setTotalSales(totalSales);
        resp.setTotalViews(totalViews);
        resp.setTotalFavorites(totalFav);
        return resp;
    }

    @Override
    public BatchImportResult batchImport(Long sellerId, MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) filename = "";
        String lowerName = filename.toLowerCase();

        BatchImportResult result = new BatchImportResult();
        List<Map<String, String>> rows;

        try {
            if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
                rows = parseExcel(file);
            } else if (lowerName.endsWith(".csv")) {
                rows = parseCsv(file);
            } else {
                throw new BusinessException(ProductErrorCode.PARAM_INVALID.getCode(), "仅支持 .xlsx/.xls/.csv 格式");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析导入文件失败", e);
            throw new BusinessException(ProductErrorCode.PARAM_INVALID.getCode(), "文件解析失败: " + e.getMessage());
        }

        result.setTotalDataRows(rows.size());
        int rowNum = 1; // 数据行从第1行开始（第0行是表头）
        for (Map<String, String> row : rows) {
            rowNum++;
            String title = row.getOrDefault("商品标题", "").trim();
            String categoryName = row.getOrDefault("分类名称", "").trim();
            String priceStr = row.getOrDefault("价格", "").trim();
            String stockStr = row.getOrDefault("库存", "").trim();
            String conditionName = row.getOrDefault("成色", "").trim();
            String brand = row.getOrDefault("品牌", "").trim();
            String description = row.getOrDefault("描述", "").trim();
            String imagesStr = row.getOrDefault("图片URL", "").trim();

            try {
                // 校验必填字段
                if (title.isEmpty()) {
                    result.addFailure(rowNum, title, "商品标题不能为空");
                    continue;
                }
                if (title.length() < 2 || title.length() > 30) {
                    result.addFailure(rowNum, title, "标题长度须2-30字");
                    continue;
                }
                if (categoryName.isEmpty()) {
                    result.addFailure(rowNum, title, "分类名称不能为空");
                    continue;
                }
                if (priceStr.isEmpty()) {
                    result.addFailure(rowNum, title, "价格不能为空");
                    continue;
                }
                BigDecimal price;
                try {
                    price = new BigDecimal(priceStr);
                } catch (NumberFormatException e) {
                    result.addFailure(rowNum, title, "价格格式错误: " + priceStr);
                    continue;
                }
                if (price.compareTo(BigDecimal.ZERO) <= 0) {
                    result.addFailure(rowNum, title, "价格必须大于0");
                    continue;
                }
                if (conditionName.isEmpty()) {
                    result.addFailure(rowNum, title, "成色不能为空");
                    continue;
                }

                // 构建保存请求
                ProductSaveReq req = new ProductSaveReq();
                req.setTitle(title);
                req.setCategoryName(categoryName);
                req.setPrice(price);
                req.setStock(stockStr.isEmpty() ? 1 : Integer.parseInt(stockStr));
                req.setConditionName(conditionName);
                if (!brand.isEmpty()) req.setBrand(brand);
                if (!description.isEmpty()) req.setDescription(description);
                if (!imagesStr.isEmpty()) {
                    req.setImages(Arrays.asList(imagesStr.split("[;；]")));
                }
                req.setSubmitForReview(false); // 批量导入保存为草稿

                saveProduct(sellerId, null, req);
                result.addSuccess();
            } catch (Exception e) {
                String reason = e.getMessage();
                if (e instanceof BusinessException be) {
                    reason = be.getMessage();
                }
                result.addFailure(rowNum, title, reason);
            }
        }
        return result;
    }

    /**
     * 解析 Excel (.xlsx) 文件
     */
    private List<Map<String, String>> parseExcel(MultipartFile file) throws Exception {
        List<Map<String, String>> result = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            // 读取表头
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BusinessException(ProductErrorCode.PARAM_INVALID.getCode(), "Excel 文件无表头");
            }
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(formatter.formatCellValue(cell).trim());
            }

            // 读取数据行
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> map = new LinkedHashMap<>();
                boolean hasData = false;
                for (int c = 0; c < headers.size(); c++) {
                    String value = c < row.getLastCellNum() ? formatter.formatCellValue(row.getCell(c)).trim() : "";
                    if (!value.isEmpty()) hasData = true;
                    map.put(headers.get(c), value);
                }
                if (hasData) result.add(map);
            }
        }
        return result;
    }

    /**
     * 解析 CSV 文件（使用 Hutool）
     */
    private List<Map<String, String>> parseCsv(MultipartFile file) throws Exception {
        List<Map<String, String>> result = new ArrayList<>();
        cn.hutool.core.text.csv.CsvReader reader = cn.hutool.core.text.csv.CsvUtil.getReader();
        try (java.io.InputStreamReader isr = new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)) {
            cn.hutool.core.text.csv.CsvData csvData = reader.read(isr);
            List<cn.hutool.core.text.csv.CsvRow> lines = csvData.getRows();
            if (lines.isEmpty()) {
                throw new BusinessException(ProductErrorCode.PARAM_INVALID.getCode(), "CSV 文件为空");
            }
            cn.hutool.core.text.csv.CsvRow headers = lines.get(0);
            for (int i = 1; i < lines.size(); i++) {
                cn.hutool.core.text.csv.CsvRow row = lines.get(i);
                Map<String, String> map = new LinkedHashMap<>();
                boolean hasData = false;
                for (int c = 0; c < headers.size(); c++) {
                    String value = c < row.size() ? row.get(c).trim() : "";
                    if (!value.isEmpty()) hasData = true;
                    map.put(headers.get(c).trim(), value);
                }
                if (hasData) result.add(map);
            }
        }
        return result;
    }

    @Override
    public void auditPass(Long productId, Long auditorId) {
        Product p = productMapper.selectById(productId);
        if (p == null) throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
        if (p.getStatus() != Product.STATUS_REVIEWING) {
            throw new BusinessException(ProductErrorCode.STATUS_MUST_BE_REVIEWING);
        }
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .set(Product::getStatus, Product.STATUS_ON_SALE)
                .set(Product::getPublishedAt, LocalDateTime.now())
                .set(Product::getUpdatedAt, LocalDateTime.now()));
        // TODO: 发送 MQ → 店铺 onSaleCount +1 / ES同步商品索引
        log.info("【商品审核通过】productId={}, auditorId={}", productId, auditorId);
    }

    @Override
    public void auditReject(Long productId, Long auditorId, String rejectReason) {
        Product p = productMapper.selectById(productId);
        if (p == null) throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
        if (p.getStatus() != Product.STATUS_REVIEWING) {
            throw new BusinessException(ProductErrorCode.STATUS_MUST_BE_REVIEWING);
        }
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .set(Product::getStatus, Product.STATUS_REJECTED)
                .set(Product::getRejectReason, rejectReason)
                .set(Product::getUpdatedAt, LocalDateTime.now()));
        log.info("【商品审核拒绝】productId={}, auditorId={}, reason={}", productId, auditorId, rejectReason);
    }

    /* ============================================================
     *  详情 & 列表
     * ============================================================ */

    @Override
    public ProductDetailResp getDetail(Long productId, Long viewerId) {
        Product p = productMapper.selectById(productId);
        if (p == null) throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
        // 状态过滤：除卖家本人，其他人只能看"在售"
        if (viewerId == null || !viewerId.equals(p.getSellerId())) {
            if (p.getStatus() != Product.STATUS_ON_SALE) {
                throw new BusinessException(ProductErrorCode.PRODUCT_NOT_ON_SALE);
            }
        }

        // 浏览量 Redis INCR + dirty SET（异步回写）
        String viewKey = String.format(RedisKeyConstants.PRODUCT_VIEW_COUNT, productId);
        RAtomicLong counter = redisson.getAtomicLong(viewKey);
        counter.incrementAndGet();
        counter.expire(24, TimeUnit.HOURS);
        redisson.getSet(RedisKeyConstants.PRODUCT_VIEW_DIRTY_SET).add(String.valueOf(productId));

        return assembleDetail(p, viewerId, /*mine*/ viewerId != null && viewerId.equals(p.getSellerId()));
    }

    @Override
    public ProductDetailResp getDetailInternal(Long productId) {
        Product p = productMapper.selectById(productId);
        if (p == null) throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
        // 内部调用：不过滤状态、不增加浏览量
        return assembleDetail(p, null, false);
    }

    @Override
    public void incrementViewCount(Long productId) {
        String viewKey = String.format(RedisKeyConstants.PRODUCT_VIEW_COUNT, productId);
        RAtomicLong counter = redisson.getAtomicLong(viewKey);
        counter.incrementAndGet();
        counter.expire(24, TimeUnit.HOURS);
        redisson.getSet(RedisKeyConstants.PRODUCT_VIEW_DIRTY_SET).add(String.valueOf(productId));
    }

    @Override
    public PageResult<ProductDetailResp> query(ProductQueryReq req) {
        // TODO: 生产优先走 ES（Canal同步商品→ES），ES超时或无数据时走MySQL 兜底
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();

        // 关键字（降级走 LIKE 标题/描述/品牌/分类名）
        if (StrUtil.isNotBlank(req.getKeyword())) {
            String safeKw = req.getKeyword().replace("'", "''");
            qw.and(w -> w.like(Product::getTitle, req.getKeyword())
                    .or().like(Product::getDescription, req.getKeyword())
                    .or().like(Product::getBrand, req.getKeyword())
                    .or().apply("category_id IN (SELECT id FROM categories WHERE name LIKE CONCAT('%', '" + safeKw + "', '%'))"));
        }
        if (req.getCategoryIds() != null && !req.getCategoryIds().isEmpty()) {
            qw.in(Product::getCategoryId, req.getCategoryIds());
        }
        if (req.getConditionLevels() != null && !req.getConditionLevels().isEmpty()) {
            qw.in(Product::getConditionLevel, req.getConditionLevels());
        }
        if (req.getPriceMin() != null) qw.ge(Product::getPrice, req.getPriceMin());
        if (req.getPriceMax() != null) qw.le(Product::getPrice, req.getPriceMax());
        if (StrUtil.isNotBlank(req.getLocation())) qw.like(Product::getLocation, req.getLocation());
        if (Boolean.TRUE.equals(req.getOnlyWithBox())) qw.eq(Product::getHasBox, true);

        // [卖家端 或 平台端] 按 seller/status
        if (req.getSellerId() != null) {
            qw.eq(Product::getSellerId, req.getSellerId());
            if (req.getStatus() != null) qw.eq(Product::getStatus, req.getStatus());
            else qw.in(Product::getStatus, Product.STATUS_DRAFT, Product.STATUS_REVIEWING,
                    Product.STATUS_ON_SALE, Product.STATUS_OFF_SHELF, Product.STATUS_REJECTED);
        } else {
            // [买家端] 仅在售
            qw.eq(Product::getStatus, Product.STATUS_ON_SALE);
        }

        // 排序
        boolean asc = "asc".equalsIgnoreCase(req.getSortDirection());
        switch (StrUtil.blankToDefault(req.getSortField(), "publishedAt")) {
            case "price" -> qw.orderBy(true, asc, Product::getPrice);
            case "viewCount" -> qw.orderBy(true, asc, Product::getViewCount);
            default -> qw.orderByDesc(Product::getPublishedAt);
        }

        Page<Product> page = new Page<>(req.getPageNum(), req.getPageSize());
        IPage<Product> result = productMapper.selectPage(page, qw);

        Long viewerId = UserContext.currentUserId();
        List<ProductDetailResp> list = result.getRecords().stream()
                .map(p -> assembleDetail(p, viewerId, false))
                .collect(Collectors.toList());

        return PageResult.of(req.getPageNum(), req.getPageSize(), result.getTotal(), list);
    }

    /* ============================================================
     *  库存（TCC 接口 - 被订单域调用）
     *  实际生产：库存 Redis 预扣 + DB 实扣，Outbox + TCC
     * ============================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean tryDeductStock(Long productId, int quantity, String idempotentKey) {
        if (quantity <= 0) throw BusinessException.paramError("数量错误");
        // 幂等：已 Try 过直接返回 true
        RBucket<Object> tried = redisson.getBucket("yjx:product:stock:tried:" + idempotentKey);
        if (tried.isExists()) return true;

        // DB 行级乐观扣减：stock >= qty 时成功
        int rows = productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .ge(Product::getStock, quantity)
                .setSql("stock = stock - " + quantity));
        if (rows == 0) {
            throw new BusinessException(ProductErrorCode.STOCK_LOCK_FAIL);
        }
        tried.set(quantity, 30, TimeUnit.MINUTES);
        return true;
    }

    @Override
    public boolean confirmDeductStock(Long productId, int quantity, String idempotentKey) {
        // Try 已经减 DB stock，Confirm 只需维护幂等标记+检查状态
        RBucket<Object> confirm = redisson.getBucket("yjx:product:stock:confirm:" + idempotentKey);
        confirm.set(1, 24, TimeUnit.HOURS);
        // 库存为 0 则置 status=已售
        Product p = productMapper.selectById(productId);
        if (p != null && p.getStock() != null && p.getStock() <= 0 && p.getStatus() == Product.STATUS_ON_SALE) {
            productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, productId)
                    .set(Product::getStatus, Product.STATUS_SOLD_OUT)
                    .set(Product::getUpdatedAt, LocalDateTime.now()));
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelDeductStock(Long productId, int quantity, String idempotentKey) {
        RBucket<Object> cancel = redisson.getBucket("yjx:product:stock:cancel:" + idempotentKey);
        if (cancel.isExists()) return true;
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .setSql("stock = stock + " + quantity));
        cancel.set(1, 24, TimeUnit.HOURS);
        // 清除 tried 标记，避免重复 Cancel
        redisson.getBucket("yjx:product:stock:tried:" + idempotentKey).delete();
        return true;
    }

    /* ============================================================
     *  商品快照（下单时生成）
     * ============================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSnapshot(Long productId, Long orderId) {
        Product p = productMapper.selectById(productId);
        if (p == null) throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
        List<ProductImage> images = imageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, productId).orderByAsc(ProductImage::getSortOrder));
        List<ProductSpec> specs = specMapper.selectList(new LambdaQueryWrapper<ProductSpec>()
                .eq(ProductSpec::getProductId, productId).orderByAsc(ProductSpec::getSortOrder));

        ProductSnapshot snap = new ProductSnapshot();
        snap.setProductId(productId);
        snap.setOrderId(orderId);
        snap.setSellerId(p.getSellerId());
        snap.setCategoryId(p.getCategoryId());
        snap.setTitle(p.getTitle());
        snap.setPrice(p.getPrice());
        snap.setOriginalPrice(p.getOriginalPrice());
        snap.setConditionLevel(p.getConditionLevel());
        snap.setPurchaseChannel(p.getPurchaseChannel());
        snap.setHasBox(p.getHasBox());
        snap.setDescription(p.getDescription());
        snap.setLocation(p.getLocation());

        List<String> urls = images.stream().map(ProductImage::getImageUrl).toList();
        List<ProductSaveReq.SpecItem> specItems = specs.stream().map(s -> {
            ProductSaveReq.SpecItem it = new ProductSaveReq.SpecItem();
            it.setName(s.getSpecName());
            it.setValue(s.getSpecValue());
            it.setSort(s.getSortOrder());
            return it;
        }).toList();

        try {
            snap.setImagesJson(objectMapper.writeValueAsString(urls));
            snap.setSpecsJson(objectMapper.writeValueAsString(specItems));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "快照序列化失败");
        }
        snapshotMapper.insert(snap);
        return snap.getId();
    }

    /* ============================================================
     *  浏览量 Redis → DB 回写（XXL-Job，每5分钟）
     * ============================================================ */

    @Override
    @XxlJob("productViewCountSyncJob")
    @Transactional(rollbackFor = Exception.class)
    public void syncViewCountFromRedis() {
        var dirtySet = redisson.getSet(RedisKeyConstants.PRODUCT_VIEW_DIRTY_SET);
        Iterator<Object> it = dirtySet.iterator();
        int synced = 0, batch = 0;
        while (it.hasNext()) {
            String pid = String.valueOf(it.next());
            String viewKey = String.format(RedisKeyConstants.PRODUCT_VIEW_COUNT, pid);
            RAtomicLong c = redisson.getAtomicLong(viewKey);
            long add = c.getAndSet(0);
            if (add > 0) {
                productMapper.update(null, new LambdaUpdateWrapper<Product>()
                        .eq(Product::getId, Long.parseLong(pid))
                        .setSql("view_count = view_count + " + add));
                synced++;
            }
            it.remove();
            batch++;
            if (batch % 200 == 0) {
                redisson.getBucket(viewKey).expire(24, TimeUnit.HOURS);
            }
        }
        log.info("【浏览量回写】完成商品数={}", synced);
    }

    /* ============================================================
     *  内部工具
     * ============================================================ */

    private Product assertOwnProduct(Long sellerId, Long productId) {
        Product p = productMapper.selectById(productId);
        if (p == null) throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
        if (!p.getSellerId().equals(sellerId)) {
            throw new BusinessException(ProductErrorCode.PRODUCT_NOT_BELONG_SELLER);
        }
        return p;
    }

    private void insertImages(Long productId, List<String> images) {
        if (images == null || images.isEmpty()) return;
        boolean coverFound = false;
        for (int i = 0; i < images.size(); i++) {
            String url = images.get(i);
            ProductImage pImg = new ProductImage();
            pImg.setProductId(productId);
            pImg.setImageUrl(url);
            pImg.setSortOrder(i);
            boolean isCover = !coverFound && i == 0;
            pImg.setIsCover(isCover);
            if (isCover) coverFound = true;
            imageMapper.insert(pImg);
        }
    }

    /**
     * 解析前端传入的分类标识：支持 name（显卡）、code（gpu）、id字符串（"1"）三种形式
     */
    private Integer resolveCategoryId(String categoryValue) {
        if (StrUtil.isBlank(categoryValue)) return null;
        String val = categoryValue.trim();

        // 1. 先尝试按 name 查找
        Category byName = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getName, val)
                .last("LIMIT 1"));
        if (byName != null) return byName.getId();

        // 2. 再尝试按 code 查找
        Category byCode = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getCode, val)
                .last("LIMIT 1"));
        if (byCode != null) return byCode.getId();

        // 3. 最后尝试将字符串解析为 id
        try {
            int id = Integer.parseInt(val);
            Category byId = categoryMapper.selectById(id);
            if (byId != null) return byId.getId();
        } catch (NumberFormatException ignored) {
            // 不是数字，忽略
        }

        return null;
    }

    /** 成色名称 → 成色等级映射（5 档） */
    private Integer mapConditionName(String name) {
        if (name == null) return null;
        return switch (name.trim()) {
            case "全新" -> Product.COND_NEW;
            case "99新", "准新" -> Product.COND_99;
            case "95成新", "95新" -> Product.COND_95;
            case "9成新", "九成新" -> Product.COND_90;
            case "战损版", "8成新", "8成新及以下" -> Product.COND_WORN;
            default -> null;
        };
    }

    private void insertSpecs(Long productId, List<ProductSaveReq.SpecItem> specs) {
        for (ProductSaveReq.SpecItem s : specs) {
            ProductSpec ps = new ProductSpec();
            ps.setProductId(productId);
            ps.setSpecName(s.getName());
            ps.setSpecValue(s.getValue());
            ps.setSortOrder(s.getSort() == null ? 0 : s.getSort());
            specMapper.insert(ps);
        }
    }

    private ProductDetailResp assembleDetail(Product p, Long viewerId, boolean mine) {
        ProductDetailResp r = new ProductDetailResp();
        r.setId(p.getId());
        r.setSellerId(p.getSellerId());
        r.setShopId(p.getShopId());
        // 店铺信息回填（bug-20260908170645）：shopName/shopLogoUrl/shopVerified 通过 Feign 查 user-service
        try {
            var shopResp = shopFeignClient.getBySeller(p.getSellerId());
            if (shopResp != null && shopResp.isSuccess() && shopResp.getData() != null) {
                ShopFeignResp shop = shopResp.getData();
                if (p.getShopId() == null) {
                    r.setShopId(shop.getId());
                }
                r.setShopName(shop.getShopName());
                r.setShopLogoUrl(shop.getLogoUrl());
                r.setShopVerified(shop.getVerified());
            }
        } catch (Exception e) {
            log.warn("[商品详情-回填店铺信息失败] productId={}, sellerId={}, err={}",
                    p.getId(), p.getSellerId(), e.getMessage());
        }
        r.setCategoryId(p.getCategoryId());
        Category cat = categoryMapper.selectById(p.getCategoryId());
        if (cat != null) r.setCategoryName(cat.getName());
        r.setTitle(p.getTitle());
        r.setSubTitle(p.getSubTitle());
        r.setBrand(p.getBrand());
        r.setPrice(p.getPrice());
        r.setOriginalPrice(p.getOriginalPrice());
        r.setConditionLevel(p.getConditionLevel());
        r.setConditionName(conditionName(p.getConditionLevel()));
        r.setPurchaseChannel(p.getPurchaseChannel());
        r.setHasBox(p.getHasBox());
        r.setDescription(p.getDescription());
        r.setLocation(p.getLocation());
        r.setStock(p.getStock());
        r.setWarningThreshold(p.getWarningThreshold());
        r.setStatus(p.getStatus());
        r.setRejectReason(mine ? p.getRejectReason() : null);
        // 浏览量 = DB + Redis 增量（getDetail 时已 INCR，这里兜底展示）
        int viewIncr = 0;
        RAtomicLong cnt = redisson.getAtomicLong(String.format(RedisKeyConstants.PRODUCT_VIEW_COUNT, p.getId()));
        viewIncr = (int) cnt.get();
        r.setViewCount((p.getViewCount() == null ? 0 : p.getViewCount()) + viewIncr);
        r.setFavoriteCount(p.getFavoriteCount() == null ? 0 : p.getFavoriteCount());
        r.setWantCount(p.getWantCount() == null ? 0 : p.getWantCount());
        r.setSales(p.getSales() == null ? 0 : p.getSales());
        r.setSku(p.getSku());
        if (viewerId != null) r.setFavoredByMe(favoriteService.isFavored(viewerId, p.getId()));
        r.setPublishedAt(p.getPublishedAt());
        r.setCreatedAt(p.getCreatedAt());

        // 是否验机：必检分类（显卡/CPU）默认验机，其余按卖家选择
        boolean needInspect = cat != null && ProductConstants.INSPECT_REQUIRED_CODES.contains(cat.getCode());
        r.setHasInspection(needInspect);

        // 图片
        List<ProductImage> imgs = imageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, p.getId()).orderByAsc(ProductImage::getSortOrder));
        r.setImageUrls(imgs.stream().map(ProductImage::getImageUrl).collect(Collectors.toList()));
        // 规格
        List<ProductSpec> specs = specMapper.selectList(new LambdaQueryWrapper<ProductSpec>()
                .eq(ProductSpec::getProductId, p.getId()).orderByAsc(ProductSpec::getSortOrder));
        r.setSpecs(specs.stream().map(s -> {
            ProductSaveReq.SpecItem si = new ProductSaveReq.SpecItem();
            si.setName(s.getSpecName());
            si.setValue(s.getSpecValue());
            si.setSort(s.getSortOrder());
            return si;
        }).collect(Collectors.toList()));

        // BUG-001 新增字段：运费/质保/售后/瑕疵/服务标签
        r.setShipFree(p.getShipFree());
        r.setShipTemplate(p.getShipTemplate());
        r.setWarranty(p.getWarranty());
        r.setAftersalesType(p.getAftersalesType());
        r.setDefects(fromJsonList(p.getDefectsJson(), ProductSaveReq.DefectItem.class));
        r.setServiceTags(fromJsonStringList(p.getServiceTagsJson()));

        return r;
    }

    /* ============================================================
     *  JSON 工具（瑕疵列表 / 服务标签序列化）
     * ============================================================ */

    /** 对象列表 → JSON 字符串（null/空 → null） */
    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("[商品] JSON 序列化失败，将以 null 存储：{}", e.getMessage());
            return null;
        }
    }

    /** JSON 字符串 → 对象列表（null/空 → 空列表） */
    private <T> List<T> fromJsonList(String json, Class<T> clazz) {
        if (StrUtil.isBlank(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            log.warn("[商品] JSON 反序列化失败 type={}, err={}", clazz.getSimpleName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /** JSON 字符串 → String 列表（null/空 → 空列表） */
    private List<String> fromJsonStringList(String json) {
        if (StrUtil.isBlank(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            log.warn("[商品] tags JSON 反序列化失败：{}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private static String conditionName(Integer level) {
        return switch (level == null ? 0 : level) {
            case Product.COND_NEW -> "全新";
            case Product.COND_99 -> "99新";
            case Product.COND_95 -> "95成新";
            case Product.COND_90 -> "9成新";
            case Product.COND_WORN -> "战损版";
            default -> "未知";
        };
    }
}
