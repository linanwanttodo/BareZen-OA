package com.linan.system.monitor;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class OnlineUser implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 完整 JWT，仅服务端内部使用，默认不序列化给前端 */
    @JsonIgnore
    private String token;

    /** Token 摘要，用于强制下线 */
    private String tokenDigest;

    private Long userId;
    private String userName;
    private String nickName;
    private String loginIp;
    private String loginLocation;
    private String browser;
    private String os;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;
}
