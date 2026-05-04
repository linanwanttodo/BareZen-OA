package com.linan.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 开放平台 API Key 实体，对应 sys_api_key 表
 *
 * <p>供外部系统通过 {@code X-Api-Key} 请求头调用开放接口（/api/open/**）。
 * 仅存储 SHA-256 哈希（api_key_hash），明文只在生成时返回一次。</p>
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_api_key")
public class SysApiKey extends BaseEntity {

    /** 密钥名称（用途标识） */
    private String keyName;

    /** API Key SHA-256 哈希（不存明文） */
    private String apiKeyHash;

    /** 权限范围，逗号分隔，如 open:approval:read */
    private String scopes;

    /** 状态：0=启用 1=禁用 */
    private String status;

    /** 过期时间，NULL=永不过期 */
    private LocalDateTime expireTime;
}
