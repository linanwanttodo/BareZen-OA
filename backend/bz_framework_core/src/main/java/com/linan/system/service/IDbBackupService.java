package com.linan.system.service;

import java.util.List;
import java.util.Map;

/**
 * 数据库备份 Service 接口
 *
 * @author BareZen Team
 */
public interface IDbBackupService {

    /**
     * 创建数据库备份
     *
     * @return 备份文件信息
     */
    Map<String, Object> createBackup();

    /**
     * 获取备份列表
     */
    List<Map<String, Object>> listBackups();

    /**
     * 删除备份文件
     */
    boolean deleteBackup(String filename);

    /**
     * 获取备份文件路径
     */
    String getBackupPath(String filename);
}
