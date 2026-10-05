package com.yingjianxia.common.mybatis.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 硬件侠平台通用 BaseMapper
 * <p>
 * 后续可在此扩展：
 * <ul>
 *   <li>批量插入/更新（自定义 SQL 注入器）</li>
 *   <li>审计字段自动注入</li>
 *   <li>自定义逻辑删除后恢复 API</li>
 * </ul>
 * 所有微服务的 Mapper 接口继承此接口。
 *
 * @author 硬件侠后端团队
 */
public interface BaseRepository<T> extends BaseMapper<T> {

}
