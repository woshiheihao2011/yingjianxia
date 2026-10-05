package com.yingjianxia.product.constants;

import java.util.Set;

/**
 * 商品域业务常量
 */
public interface ProductConstants {
    /** 最多 20 张图 */
    int MAX_IMAGE_COUNT = 20;
    /** 必须验机的分类编码（平台强规则）：显卡 / CPU */
    Set<String> INSPECT_REQUIRED_CODES = Set.of("gpu", "cpu");
}
