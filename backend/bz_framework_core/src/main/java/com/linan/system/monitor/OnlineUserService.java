package com.linan.system.monitor;

import java.util.List;

public interface OnlineUserService {

    void recordLogin(Long userId, String userName, String nickName, String token, String loginIp);

    List<OnlineUser> listOnlineUsers();

    long countOnlineUsers();

    void forceLogout(String token);

    boolean isOnline(String token);
}
