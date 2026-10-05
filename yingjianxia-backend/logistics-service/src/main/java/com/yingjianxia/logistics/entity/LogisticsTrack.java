package com.yingjianxia.logistics.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 物流轨迹 — logistics_tracks 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("logistics_tracks")
public class LogisticsTrack extends BaseEntity {

    /** 关联发货记录 */
    private Long shipmentId;

    /** 状态：已揽收/运输中/到达分拣/派送中/已签收/异常 */
    private String status;

    /** 状态码：1揽收 2运输 3到达 4派送 5签收 6异常 */
    private Integer statusCode;

    /** 地点描述 */
    private String location;

    /** 详细信息 */
    private String description;

    /** 快递员姓名（派送时） */
    private String courierName;

    /** 快递员电话 */
    private String courierPhone;

    /** 轨迹时间 */
    private LocalDateTime trackedAt;

    /* ========== 本表不存在：deleted/version/updated_at ========== */
    @TableField(exist = false) private Integer deleted;
    @TableField(exist = false) private Integer version;
    @TableField(exist = false) private LocalDateTime updatedAt;

    /* ========== 状态码常量 ========== */
    public static final int CODE_PICKED = 1;
    public static final int CODE_IN_TRANSIT = 2;
    public static final int CODE_ARRIVED = 3;
    public static final int CODE_DELIVERING = 4;
    public static final int CODE_RECEIVED = 5;
    public static final int CODE_EXCEPTION = 6;
}
