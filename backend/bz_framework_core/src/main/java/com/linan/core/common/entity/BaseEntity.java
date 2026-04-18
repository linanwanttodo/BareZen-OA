package com.linan.core.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类公共基类
 *
 * 所有数据库实体类都应该继承此类，统一管理公共字段。
 * 避免每个实体类重复定义 id、createTime、updateTime 等字段。
 *
 * 继承了 MyBatis-Plus 的以下特性：
 * - 主键（@TableId）：使用雪花算法自动生成 ID
 * - 创建时间（@TableField + fill）：插入时自动填充当前时间
 * - 更新时间（@TableField + fill）：更新时自动填充当前时间
 * - 逻辑删除（@TableLogic）：删除操作仅标记字段，不物理删除
 *
 * 注意：继承了 Serializable 接口，支持序列化（如分布式缓存场景）。
 *
 * @author Zen-Base Team
 * @since 1.0-SNAPSHOT
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     *
     * 使用雪花算法（Snowflake）自动生成，替代传统自增 ID。
     * 雪花算法的优势：
     * 1. 分布式唯一：多台服务器可以独立生成 ID，不会冲突
     * 2. 趋势递增：ID 总体按时间递增，对 MySQL 索引友好
     * 3. 无需数据库自增字段：避免分库分表时的 ID 冲突
     *
     * IdType.ASSIGN_ID：由 MyBatis-Plus 自动分配雪花 ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 创建时间
     *
     * 使用 LocalDateTime 类型（Java 8 引入），替代旧版的 java.util.Date。
     * LocalDateTime 是不可变对象，线程安全，API 更友好。
     *
     * @JsonFormat 指定 JSON 序列化时的日期格式。
     * @TableField(fill = FieldFill.INSERT) 在插入时由 MetaObjectHandler 自动填充。
     * @JsonIgnore 在查询时序列化，不需要隐藏。
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     *
     * @TableField(fill = FieldFill.INSERT_UPDATE) 在插入和更新时都自动填充。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 创建者
     *
     * 记录创建此数据的用户 ID。
     * 在插入时由 MetaObjectHandler 自动填充为当前登录用户的 ID。
     *
     * @JsonIgnore 表示序列化 JSON 时不包含此字段，
     * 因为前端通常不需要显示创建者 ID，而是显示创建者姓名（通过关联查询）。
     * 如果前端需要此字段，可以移除 @JsonIgnore 注解。
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonIgnore
    private Long createBy;

    /**
     * 更新者
     *
     * 记录最后更新此数据的用户 ID。
     * 在插入和更新时由 MetaObjectHandler 自动填充为当前登录用户的 ID。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonIgnore
    private Long updateBy;

    /**
     * 逻辑删除标记
     *
     * 0 表示未删除，1 表示已删除。
     *
     * 逻辑删除的好处：
     * 1. 数据可恢复：误删后可以通过修改标记恢复
     * 2. 保留历史关联：订单等关联数据不会因为用户被删除而丢失
     * 3. 审计需求：满足数据追溯的合规要求
     *
     * @TableLogic 标记为逻辑删除字段。
     * MyBatis-Plus 会自动将 DELETE 语句转换为 UPDATE del_flag = 1，
     * 并在查询时自动追加 WHERE del_flag = 0。
     *
     * @JsonIgnore 删除标记不需要返回给前端。
     */
    @TableLogic
    @JsonIgnore
    private Integer delFlag;
}