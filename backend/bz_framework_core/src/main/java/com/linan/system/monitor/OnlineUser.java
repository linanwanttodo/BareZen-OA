package com.linan.system.monitor;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class OnlineUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private String token;
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
