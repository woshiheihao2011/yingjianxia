package com.yingjianxia.common.mybatis.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.yingjianxia.common.core.constants.BusinessConstants;
import com.yingjianxia.common.core.utils.SnowflakeIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 自动配置
 * <p>
 * 提供：
 * <ul>
 *   <li>雪花 ID 生成器（覆盖默认的 Sequence）</li>
 *   <li>create_at / update_at / deleted 自动填充</li>
 *   <li>分页插件（溢出分页合理化，防止查第0页）</li>
 *   <li>乐观锁插件（version 字段自动+1）</li>
 * </ul>
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Configuration
public class MybatisPlusAutoConfig {

    /**
     * 雪花 ID 生成器 — 所有实体 @TableId(type=ASSIGN_ID) 调用此 Bean
     */
    @Bean
    public IdentifierGenerator identifierGenerator() {
        return entity -> SnowflakeIdGenerator.getInstance().nextId();
    }

    /**
     * 自动填充：创建时填 created_at / updated_at / deleted=0，更新时填 updated_at
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                log.debug("[MP] 插入自动填充 created_at / updated_at / deleted");
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "deleted", Integer.class, BusinessConstants.DELETED_NO);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }

    /**
     * MP 插件集合：分页 + 乐观锁
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 分页插件（选 MySQL 方言，后续 ShardingSphere 仍兼容）
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(
                com.baomidou.mybatisplus.annotation.DbType.MYSQL);
        pagination.setOverflow(true);                              // 页码溢出则查最后一页
        pagination.setMaxLimit(BusinessConstants.MAX_PAGE_SIZE);   // 单次查询上限

        // 2. 乐观锁：version 字段 WHERE 条件 + 自增
        OptimisticLockerInnerInterceptor optimistic = new OptimisticLockerInnerInterceptor();

        interceptor.addInnerInterceptor(pagination);
        interceptor.addInnerInterceptor(optimistic);
        return interceptor;
    }
}
